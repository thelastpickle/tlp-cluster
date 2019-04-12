package com.thelastpickle.tlpcluster.instances.importers

import org.junit.jupiter.api.Test

internal class InstancesImporterTest {
    @Test
    fun testLoad() {
        val data = InstancesImporter.loadFromCSV()
        println(data.instances.first())
    }
}