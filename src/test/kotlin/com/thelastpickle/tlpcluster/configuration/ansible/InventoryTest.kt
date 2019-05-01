package com.thelastpickle.tlpcluster.configuration.ansible

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

internal class InventoryTest {

    @Test
    fun write() {
        val inv = Inventory(listOf("test1"), listOf("test2"), "test3")
        val output = ByteArrayOutputStream()
        inv.write(output)
        val result = output.toString()

        assertThat(result).contains("[cassandra]\n")
        assertThat(result).contains("[stress]\n")
        assertThat(result).contains("[monitoring]\n")
        assertThat(result).contains("test1\n")
        assertThat(result).contains("test2\n")
        assertThat(result).contains("test3\n")

    }
}