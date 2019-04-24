package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.YamlDelegate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PrometheusTest {

    val yaml by YamlDelegate()

    val config = prometheus {
        global {
            scrape_interval = "15s"
        }
        scrape_config {
            job_name = "prometheus"
            scrape_interval = "5s"

            static_config {
                targets = listOf("127.0.0.1:8000", "192.168.1:8000")
            }
        }
    }


    @Test
    fun testGlobalConfig() {
        assertThat(config.global.scrape_interval).isEqualTo("15s")
    }

    @Test
    fun testScrapeConfigs() {
        val scrape = config.scrape_configs
        assertThat(scrape).isNotEmpty()

        val prom = config.scrape_configs.first()
        assertThat(prom.job_name).isEqualTo("prometheus")

    }

    @Test
    fun testStaticConfigs() {
        val targets = config.scrape_configs.first().staticConfigList.first().targets
        assertThat(targets).containsExactly("127.0.0.1:8000", "192.168.1:8000")
    }

    @Test
    fun testConvertToYaml() {
        var data = yaml.writeValueAsString(config)
    }

}