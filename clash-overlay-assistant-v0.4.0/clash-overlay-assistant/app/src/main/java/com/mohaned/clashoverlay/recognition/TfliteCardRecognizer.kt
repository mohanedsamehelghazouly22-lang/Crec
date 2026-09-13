package com.mohaned.clashoverlay.recognition

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.roundToInt

/** Replaceable on-device classifier. Missing/invalid models produce no observations. */
class TfliteCardRecognizer(context: Context) : AutoCloseable {
    private val interpreter: Interpreter?
    private val labels: List<String>
    val availability: Availability

    enum class Availability { READY, MODEL_UNAVAILABLE, LABELS_UNAVAILABLE, INVALID_MODEL }

    init {
        val model = runCatching { context.assets.openFd("models/card_classifier.tflite") }.getOrNull()
        interpreter = runCatching {
            model?.let {
                val mapped = FileInputStream(it.fileDescriptor).channel.map(
                    FileChannel.MapMode.READ_ONLY, it.startOffset, it.declaredLength
                )
                Interpreter(mapped, Interpreter.Options().apply {
                    setNumThreads(2)
                    setUseXNNPACK(true)
                })
            }
        }.getOrNull()
        labels = runCatching {
            context.assets.open("models/labels.txt").bufferedReader().readLines().filter(String::isNotBlank)
        }.getOrDefault(emptyList())
        availability = when {
            interpreter == null -> if (model == null) Availability.MODEL_UNAVAILABLE else Availability.INVALID_MODEL
            labels.isEmpty() -> Availability.LABELS_UNAVAILABLE
            else -> Availability.READY
        }
    }

    fun isAvailable() = availability == Availability.READY

    fun classify(bitmap: Bitmap): Pair<String, Float>? {
        val tflite = interpreter ?: return null
        if (!isAvailable()) return null
        val input = tflite.getInputTensor(0)
        val shape = input.shape()
        if (shape.size != 4 || shape[0] != 1 || shape[3] != 3) return null
        val h = shape[1]; val w = shape[2]
        val resized = Bitmap.createScaledBitmap(bitmap, w, h, true)
        try {
            val inputBuffer = makeInput(resized, input)
            val outputTensor = tflite.getOutputTensor(0)
            val classes = outputTensor.shape().lastOrNull() ?: return null
            if (classes != labels.size) return null
            val probs = readOutput(tflite, outputTensor, classes)
            val best = probs.indices.maxByOrNull { probs[it] } ?: return null
            return labels.getOrNull(best)?.let { it to probs[best].coerceIn(0f, 1f) }
        } finally { resized.recycle() }
    }

    private fun readOutput(tflite: Interpreter, tensor: org.tensorflow.lite.Tensor, classes: Int): FloatArray {
        return when (tensor.dataType()) {
            DataType.FLOAT32 -> {
                val out = Array(1) { FloatArray(classes) }
                tflite.runForMultipleInputsOutputs(arrayOf(lastInput), mapOf(0 to out))
                out[0]
            }
            DataType.UINT8 -> {
                val out = Array(1) { ByteArray(classes) }
                tflite.runForMultipleInputsOutputs(arrayOf(lastInput), mapOf(0 to out))
                val q = tensor.quantizationParams()
                out[0].map { ((it.toInt() and 0xFF) - q.zeroPoint) * q.scale }.toFloatArray()
            }
            DataType.INT8 -> {
                val out = Array(1) { ByteArray(classes) }
                tflite.runForMultipleInputsOutputs(arrayOf(lastInput), mapOf(0 to out))
                val q = tensor.quantizationParams()
                out[0].map { (it.toInt() - q.zeroPoint) * q.scale }.toFloatArray()
            }
            else -> FloatArray(classes)
        }
    }

    private lateinit var lastInput: ByteBuffer

    private fun makeInput(bitmap: Bitmap, tensor: org.tensorflow.lite.Tensor): ByteBuffer {
        val type = tensor.dataType()
        val bytes = if (type == DataType.FLOAT32) 4 else 1
        val b = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 3 * bytes).order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val q = tensor.quantizationParams()
        pixels.forEach { p ->
            intArrayOf((p shr 16) and 255, (p shr 8) and 255, p and 255).forEach { c ->
                if (type == DataType.FLOAT32) b.putFloat(c / 255f)
                else {
                    val quantized = (c / 255f / q.scale + q.zeroPoint).roundToInt()
                    if (type == DataType.UINT8) b.put(quantized.coerceIn(0, 255).toByte())
                    else b.put(quantized.coerceIn(-128, 127).toByte())
                }
            }
        }
        b.rewind(); lastInput = b; return b
    }

    override fun close() { interpreter?.close() }
}
