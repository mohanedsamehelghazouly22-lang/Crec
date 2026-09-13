package com.mohaned.clashoverlay.recognition

import com.mohaned.clashoverlay.core.model.CardObservation

class TemporalVerifier(private val highThreshold:Float=.88f,private val mediumThreshold:Float=.68f,private val requiredHighFrames:Int=2,private val requiredMediumFrames:Int=4,private val windowMs:Long=900L){
    private data class Track(var cardId:String,val samples:ArrayDeque<Pair<Long,Float>>)
    private val tracks=mutableMapOf<String,Track>()
    fun reset()=tracks.clear()
    fun verify(roiId:String,candidate:CardObservation):CardObservation?{
        if(candidate.confidence<mediumThreshold)return null
        val t=tracks.getOrPut(roiId){Track(candidate.cardId,ArrayDeque())}
        if(t.cardId!=candidate.cardId){t.samples.clear();t.cardId=candidate.cardId;return null}
        t.samples.addLast(candidate.timestampMs to candidate.confidence)
        while(t.samples.isNotEmpty()&&candidate.timestampMs-t.samples.first().first>windowMs)t.samples.removeFirst()
        val highCount=t.samples.count{it.second>=highThreshold}; val required=if(candidate.confidence>=highThreshold)requiredHighFrames else requiredMediumFrames
        if(t.samples.size<required && highCount<requiredHighFrames)return null
        return candidate.copy(confidence=t.samples.map{it.second}.average().toFloat(),source="vision+temporal")
    }
}
