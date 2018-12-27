package com.thelastpickle.tlpcluster.configuration

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class YamlTest {
    val yaml : Yaml
    init {
        val tmp = this.javaClass.getResourceAsStream("cassandra.yaml")
        yaml = Yaml.create(tmp)
    }

    @Test
    fun setPropertyTest() {
        yaml.setSeeds(listOf("192.168.0.1"))
    }
}