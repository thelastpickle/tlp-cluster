package com.thelastpickle.tlpcluster.terraform

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ConfigurationTest {
    val c = Configuration()

    @Test
    fun ensureSetVariableWorks() {
        println(c.toJSON())
    }
}