package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import org.apache.logging.log4j.kotlin.logger
import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TFStateTest {

    val context = Context.testContext()
    val state = TFState(context, this.javaClass.getResourceAsStream("terraform.tfstate"))
    val log = logger()

    @Test
    fun testBasicStuffIsReturned() {
        val nodes = state.getHosts(ServerType.Cassandra)
        assertThat(nodes.count()).isEqualTo(3)

        log.info { "Node0: ${nodes[0]}" }
        val node0 = nodes[0]
        assertThat(node0.alias).isEqualTo("cassandra0")
        assertThat(node0.private).isNotBlank()
        assertThat(node0.public).isNotBlank()

    }

    @Test
    fun testWrongStuffIsntReturned() {
        val nodes = state.getHosts(ServerType.Monitoring)
        assertThat(nodes).isEmpty()
    }
}