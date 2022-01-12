package com.thelastpickle.tlpcluster.terraform

import com.thelastpickle.tlpcluster.Context
import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConfigurationTest {

    private val c = Configuration("TEST-1", "Testing tags", "tomorrow", "us-west-2", context = Context.testContext())

    @Test
    fun ensureSetVariableWorks() {
        println(c.toJSON())
    }
}