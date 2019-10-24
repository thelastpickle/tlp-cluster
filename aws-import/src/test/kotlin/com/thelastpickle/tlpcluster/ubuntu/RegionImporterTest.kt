package com.thelastpickle.tlpcluster.ubuntu

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class RegionImporterTest {
    @Test
    fun testLoad() {
        val data = RegionImporter.loadFromJson()
        val example = data.regions.first()


        val region = data.getRegion("us-east-1")
        assertThat(region.zones.size).isEqualTo(6)
    }
}