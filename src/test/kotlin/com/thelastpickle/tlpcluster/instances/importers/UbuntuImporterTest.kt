package com.thelastpickle.tlpcluster.instances.importers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class UbuntuImporterTest {

    @Test
    fun testLoading() {
        val ubuntu = UbuntuImporter.loadFromResource()
    }

    @Test
    fun testUrlAmiExtraction() {
        val url = "<a href=\"https://console.aws.amazon.com/ec2/home?region=us-west-1#launchAmi=ami-01a05ad286059d905\">ami-01a05ad286059d905</a>"

        val ami = UbuntuImporter.extractAmi(url)
        assertThat(ami).isEqualTo("ami-01a05ad286059d905")
    }

}