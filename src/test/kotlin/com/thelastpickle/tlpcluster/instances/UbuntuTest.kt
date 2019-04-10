package com.thelastpickle.tlpcluster.instances

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UbuntuTest {

    @Test
    fun testLoading() {
        val ubuntu = Ubuntu.loadFromResource()
    }
}