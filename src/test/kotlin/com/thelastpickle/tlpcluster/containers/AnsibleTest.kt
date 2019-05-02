package com.thelastpickle.tlpcluster.containers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)


@Testcontainers
internal class AnsibleTest {

    @Container
    var ubuntu = KGenericContainer("ubuntu:bionic")
            .withExposedPorts(22)

    @Test
    fun basicAnsibleTest() {
        println("Hello")
        assertTrue(ubuntu.isRunning)
    }
}