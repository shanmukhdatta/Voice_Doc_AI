package com.example.medical

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrowSafetyInterruptTest {

    private val interrupt = NarrowSafetyInterrupt()

    @Test
    fun testCrushingChestPainTriggersEmergency() {
        val res = interrupt.check("I have severe crushing chest pain")
        assertTrue(res.isEmergency)
        assertNotNull(res.message)
        assertTrue(res.message!!.contains("911") && res.message!!.contains("112"))
    }

    @Test
    fun testStrokeSignsTriggerEmergency() {
        val res = interrupt.check("My face is drooping and I have arm weakness")
        assertTrue(res.isEmergency)
    }

    @Test
    fun testSuicidalIdeationTriggersEmergency() {
        val res = interrupt.check("I want to end my life")
        assertTrue(res.isEmergency)
    }

    @Test
    fun testNegatedChestPainDoesNotTriggerEmergency() {
        val res = interrupt.check("I have a mild cough, no chest pain")
        assertFalse("Negated chest pain should not trigger emergency", res.isEmergency)
    }

    @Test
    fun testQuotedOrHypotheticalDoesNotTriggerEmergency() {
        val res = interrupt.check("In a movie someone said they have severe chest pain")
        assertFalse("Movie context should not trigger emergency", res.isEmergency)
    }

    @Test
    fun testMildSymptomDoesNotTriggerEmergency() {
        val res = interrupt.check("I have had a runny nose and slight headache for 2 days")
        assertFalse(res.isEmergency)
    }
}
