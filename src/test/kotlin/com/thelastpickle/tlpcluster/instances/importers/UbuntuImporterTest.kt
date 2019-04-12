package com.thelastpickle.tlpcluster.instances.importers

import org.junit.jupiter.api.Test

internal class UbuntuImporterTest {

    @Test
    fun testLoading() {
        val ubuntu = UbuntuImporter.loadFromResource()
    }
}