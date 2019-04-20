package com.thelastpickle.tlpcluster.configuration

import org.junit.jupiter.api.Assertions.*

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PrometheusTest {

    @Test
    fun testDSLBuilder() {
        val prometheus = prometheus {
            global {
                scape_interval = "15s"
            }
            scrape_config {
                job_name = "prometheus"
                scape_interval = "5s"

                static_config {
                    targets {
                        +"127.0.0.1:8000"
                    }

                }

            }

        }
    }


}