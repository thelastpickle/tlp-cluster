package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonProperty


class Prometheus(var global: Global = Global("15s"),
                 var scrape_configs: MutableList<ScrapeConfig> = mutableListOf()) {

    class Global(var scape_interval: String = "")

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

}

fun prometheus(block: Prometheus.() -> Unit) : Prometheus {
    return Prometheus().apply(block)
}



