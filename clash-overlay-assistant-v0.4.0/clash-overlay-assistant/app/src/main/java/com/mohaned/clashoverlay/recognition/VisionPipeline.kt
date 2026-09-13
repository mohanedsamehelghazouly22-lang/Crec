package com.mohaned.clashoverlay.recognition

import android.media.Image
import com.mohaned.clashoverlay.core.model.CardObservation

class VisionPipeline(private val recognizer:TfliteCardRecognizer, private val intervalMs:Long=100L):AutoCloseable {
    private val rois=RoiExtractor(); private val verifier=TemporalVerifier(); private var lastProcessedMs=0L
    var lastLatencyMs:Long=0; private set
    fun process(image:Image,timestampMs:Long):List<CardObservation>{
        if(timestampMs-lastProcessedMs<intervalMs) return emptyList(); lastProcessedMs=timestampMs
        if(!recognizer.isAvailable()) return emptyList()
        val start=System.nanoTime(); val frame=ImageFrameConverter.rgbaToBitmap(image)
        return try {
            buildList {
                for(spec in ClashRois.all){ val crop=rois.extract(frame,spec) ?: continue
                    try{ recognizer.classify(crop)?.let{(id,score)-> verifier.verify(spec.id,CardObservation(id,score,timestampMs,"tflite",spec.id))?.let(::add) } }
                    finally{crop.recycle()}
                }
            }
        } finally { frame.recycle(); lastLatencyMs=(System.nanoTime()-start)/1_000_000 }
    }
    fun reset(){verifier.reset();lastProcessedMs=0}
    override fun close(){recognizer.close()}
}
