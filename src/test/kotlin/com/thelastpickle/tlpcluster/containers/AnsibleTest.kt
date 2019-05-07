package com.thelastpickle.tlpcluster.containers

import com.thelastpickle.tlpcluster.Context
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


internal class AnsibleTest {

    private lateinit var server : UbuntuSSHTestServer
    // empty for convenience
    private lateinit var context: Context

    val containerId = ""

    @BeforeEach
    fun setupContainer() {
        context = Context.testContext()
        server = UbuntuSSHTestServer(context)

    }

    @AfterEach
    fun stopContainer() {

    }

    @Test
    fun basicAnsibleTest() {
        println("Hello")
//        assertTrue(ubuntu.isRunning)
    }
}