package com.thelastpickle.tlpcluster.instances

import com.thelastpickle.tlpcluster.Context
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
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

        assertThat(regions.regions.get("us-west-2")!!.amis.ebs).isEqualTo("ami-0013ea6a76d3b8874")

    }



}


