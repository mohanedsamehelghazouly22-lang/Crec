package com.mohaned.clashoverlay.core.engine
import org.junit.Assert.assertEquals
import org.junit.Test
class ElixirEngineTest {
 @Test fun spendNeverGoesBelowZero() { var t=0L; val e=ElixirEngine{t}; e.reset(2f); e.observeSpent(5); assertEquals(0f,e.estimate().current,0.001f) }
 @Test fun regenerationIsTimeBased() { var t=0L; val e=ElixirEngine{t}; e.reset(0f); t=2800L; assertEquals(1f,e.estimate().current,0.05f) }
}
