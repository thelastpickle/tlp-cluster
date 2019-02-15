package com.thelastpickle.tlpcluster.terraform

import com.thelastpickle.tlpcluster.Context
import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConfigurationTest {
    val tags = mutableMapOf("ticket" to "TEST-1",
            "client" to "TEST CLIENT",
            "purpose" to "Testing tags",
            "email" to "test@test.com")

    private val c = Configuration(tags, "us-west-2", context = Context.testContext())

    @Test
    fun ensureSetVariableWorks() {
        println(c.toJSON())
    }
}