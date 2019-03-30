package com.thelastpickle.tlpcluster.instances

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InstancesTest {
    @Test
    fun testLoad() {
        val data = Instances.load()
        println(data.instances.first())
    }
}