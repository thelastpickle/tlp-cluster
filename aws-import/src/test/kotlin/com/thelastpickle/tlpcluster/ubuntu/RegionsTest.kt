package com.thelastpickle.tlpcluster.ubuntu

import com.thelastpickle.tlpcluster.ubuntu.Regions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RegionsTest {

    val regions = Regions.load()


    @BeforeEach
    fun resetContext() {
    }

    @Test
    fun testLoading() {
        assertThat(regions.regions).containsKey("us-west-2")

    }

    @Test
    fun testCreateRegions() {
        val data = Regions.createRegions()
        println("hello")
    }

}


