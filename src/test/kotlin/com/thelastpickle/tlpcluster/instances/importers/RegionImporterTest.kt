package com.thelastpickle.tlpcluster.instances.importers

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegionImporterTest {
    @Test
    fun testLoad() {
        val data = RegionImporter.loadFromJson()
    }
}