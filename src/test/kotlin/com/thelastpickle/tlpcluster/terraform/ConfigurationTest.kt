package com.thelastpickle.tlpcluster.terraform

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConfigurationTest {
    val tags = mapOf("ticket" to "TEST-1",
            "client" to "TEST CLIENT",
            "purpose" to "Testing tags",
            "email" to "test@test.com")

    val c = Configuration(tags)

    @Test
    fun ensureSetVariableWorks() {
        println(c.toJSON())
    }
}