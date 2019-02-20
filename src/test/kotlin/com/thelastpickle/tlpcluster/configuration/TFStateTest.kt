package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TFStateTest {

    val context = Context.testContext()
    val state = TFState(context, this.javaClass.getResourceAsStream("terraform.tfstate"))

    @Test
    fun blah() {
        val nodes = state.getHosts(ServerType.Cassandra)
        assertThat(nodes.count()).isEqualTo(3)

        assertThat(nodes[0].alias).isEqualTo("cassandra0")
    }
}