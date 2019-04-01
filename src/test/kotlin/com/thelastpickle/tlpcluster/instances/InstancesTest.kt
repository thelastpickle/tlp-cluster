package com.thelastpickle.tlpcluster.instances

import org.junit.jupiter.api.Test

internal class InstancesTest {
    @Test
    fun testLoad() {
        val data = Instances.loadFromCSV()
        println(data.instances.first())
    }
}