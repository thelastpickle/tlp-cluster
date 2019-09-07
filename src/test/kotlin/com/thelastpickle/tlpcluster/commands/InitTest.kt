package com.thelastpickle.tlpcluster.commands

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.terraform.Configuration
import io.mockk.every
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InitTest {
    val testContext = Context.testContext()
    val testConfiguration = Configuration("ticket", "client", "purpose", "us-west-2", testContext)

    @Test
    fun testExpand() {
        val result = Init.expand("us-west-1", listOf("a", "b"))
        assertThat(result).isEqualTo(listOf("us-west-1a", "us-west-1b"))
    }


    @Test
    fun testAZsGetSetCorrectly() {
        val init = Init(testContext).apply {
            tags = mutableListOf("client", "ticket", "purpose")
            azs = listOf("a", "b", "c")
        }

        mockkObject(init)

        every { init.writeTerraformConfig(any()) } returns Result.success("")
        every { init.initializeDirectory(any(), any(), any()) } returns testConfiguration


        init.execute()


    }
}