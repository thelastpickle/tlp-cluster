package com.thelastpickle.tlpcluster.instances.importers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InstancesImporterTest {
    @Test
    fun testLoad() {
        val data = InstancesImporter.loadFromCSV()
        println(data.instances.first())


        val instance = data.getInstance("i3.2xlarge")
        assertThat(instance.isInstanceRootVolume).isTrue()

    }
}