package com.mohaned.clashoverlay.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ElixirRulesTest {
    @Test fun phaseMultiplierDefaultsToStandardTimeline(){
        var now=0L; val e=ElixirEngine{now}; e.reset(0f); now=119_000; assertEquals(1f,e.estimate().current,0.05f)
        now=121_000; val after=e.estimate().current; assertEquals(1.43f,after,0.12f)
    }
}
