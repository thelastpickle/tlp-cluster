package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonProperty
import com.thelastpickle.tlpcluster.YamlDelegate
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.OutputStream

// TODO: This needs some additional options
// Also need to validate the config generated here is valid prometheus config
class Prometheus(var global: Global = Global("15s"),
                 var scrape_configs: MutableList<ScrapeConfig> = mutableListOf()) {

    class Global(@JsonProperty("scrape_intervals") var scape_interval: String = "")

    class ScrapeConfig(var job_name: String = "",
                       var scape_interval: String = "",

                       @JsonProperty("static_configs")
                       var staticConfigs: StaticConfig = StaticConfig()) {

        fun static_config(block: StaticConfig.() -> Unit) {
            staticConfigs = StaticConfig().apply(block)
        }
    }


    // belongs to scrape config
    class StaticConfig(var targets: List<String> = listOf())


    fun global(block: Global.() -> Unit) {
        global = Global().apply(block)
    }

    fun scrape_config(block: ScrapeConfig.() -> Unit) {
        val config = ScrapeConfig().apply(block)
        scrape_configs.add(config)
    }

    companion object {

        val yaml by YamlDelegate()

        fun writeConfiguration(cassandra: List<String>, stress: List<String>, out: OutputStream) {

            // TODO: Move out of here and make it more testable
            val config = prometheus {
                scrape_config {
                    job_name = "prometheus"

                    static_config {
                        job_name = "prometheus"
                        targets = listOf("localhost:9090")
                    }
                    static_config {
                        job_name = "cassandra"
                        targets = cassandra.map { "$it:9500" }

                    }
                    static_config {
                        job_name = "stress"
                        targets = stress.map { "$it}:9501" }
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
 * TODO: Fill this out
 */
fun main() {

    val c = listOf("192.168.1.1", "192.168.1.2")
    val s = listOf("192.168.2.1", "192.168.2.2")
    val out = File("prometheus.yml").outputStream()


    Prometheus.writeConfiguration(c, s, out)
}


