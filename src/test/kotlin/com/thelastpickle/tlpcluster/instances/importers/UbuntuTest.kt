package com.thelastpickle.tlpcluster.instances.importers

import com.thelastpickle.tlpcluster.instances.importers.Ubuntu

import org.junit.jupiter.api.Test

internal class UbuntuTest {

    @Test
    fun testLoading() {
        val ubuntu = Ubuntu.loadFromResource()
    }
}