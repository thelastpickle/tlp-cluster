package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.thelastpickle.tlpcluster.YamlDelegate
import java.io.File
import java.io.OutputStream

/**
 * This is the top level object that holds all prometheus server configuration
 * Global is a normal scrape config
 * Generally speaking you only need to worry about making changes to the config in
 * Prometheus.writeConfiguration
 *
 * The rest is simple classes that will get taken for you automatically
 */
class Prometheus(var global: ScrapeConfig = ScrapeConfig(scrape_interval = "15s"),
                 var scrape_configs: MutableList<ScrapeConfig> = mutableListOf()) {

    /**
     * You will never need to call this directly
     * @see <a href="https://prometheus.io/docs/prometheus/latest/configuration/configuration/#scrape_config">Prometheus Scrape Config</a>
     */
    class ScrapeConfig(@JsonInclude(JsonInclude.Include.NON_EMPTY) var job_name: String = "",
                       @JsonInclude(JsonInclude.Include.NON_NULL)
                       var scrape_interval: String? = null,

                       @JsonProperty("static_configs")
                       @JsonInclude(JsonInclude.Include.NON_EMPTY)
                       var staticConfigList: MutableList<StaticConfig> = mutableListOf()) {

        fun static_config(block: StaticConfig.() -> Unit) {
            staticConfigList.add(StaticConfig().apply(block))

        }
    }

    /**
     * belongs to scrape config.  We're only using the targets list for now, so I haven't included anything else here
     * The only other thing we could use is labels, and that doesn't appear to be applicable here
     *
     * @see <a href="https://prometheus.io/docs/prometheus/latest/configuration/configuration/#static_config">Static Config</a>
     *
     */
    class StaticConfig(var targets: List<String> = listOf())

    /**
     * Used as Prometheus.global { }
     *
     * @param block Lambda that will be applied to the new ScapeConfig.
     */
    fun global(block: ScrapeConfig.() -> Unit) {
        global = ScrapeConfig().apply(block)
    }

    /**
     * Used as Prometheus.global {}
     *
     * @param block Lambda that will be applied to the new ScapeConfig.
     *
     * @see com.thelastpickle.tlpcluster.configuration.PrometheusTest for usage
     */
    fun scrape_config(block: ScrapeConfig.() -> Unit) {
        val config = ScrapeConfig().apply(block)
        scrape_configs.add(config)
    }

    companion object {

        val yaml by YamlDelegate()

        fun writeConfiguration(cassandra: List<String>, stress: List<String>, out: OutputStream) {

            val config = prometheus {
                scrape_config {
                    job_name = "prometheus"

                    static_config {
                        targets = listOf("localhost:9090")
                    }
                }
                scrape_config {
                    job_name = "cassandra"

                    static_config {
                        targets = cassandra.map { "$it:9500" }
                    }
                }
                scrape_config {
                    job_name = "stress"

                    static_config {
                        targets = stress.map { "$it:9501" }
                    }
                }


            }
            yaml.writeValue(out, config)
        }
    }
}

fun prometheus(block: Prometheus.() -> Unit) : Prometheus {
    return Prometheus().apply(block)
}


/**
 * Convenience for generating sample configs
 * This is for local testing and debugging
 *
 */
fun main() {

    val c = listOf("192.168.1.1", "192.168.1.2")
    val s = listOf("192.168.2.1", "192.168.2.2")
    val out = File("prometheus.yml").outputStream()


    Prometheus.writeConfiguration(c, s, out)
}


