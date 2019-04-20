package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonProperty

class Prometheus(var global: Global = Global("15s"),
                 var scrape_configs: MutableList<ScrapeConfig> = mutableListOf()) {

    class Global(var scape_interval: String = "")
    class ScrapeConfig(var job_name: String = "",
                       var scape_interval: String = "",
                       var static_configs: StaticConfig = StaticConfig())

    // belongs to scrape config
    class StaticConfig(var targets: MutableList<String> = mutableListOf())


}

fun prometheus(block: Prometheus.() -> Unit) : Prometheus {
    return Prometheus().apply(block)
}

fun Prometheus.global(block: Prometheus.Global.() -> Unit) {
    global = Prometheus.Global().apply(block)
}

fun Prometheus.scrape_config(block: Prometheus.ScrapeConfig.() -> Unit) {
    val config = Prometheus.ScrapeConfig().apply(block)
    scrape_configs.add(config)
}

fun Prometheus.ScrapeConfig.static_configs(block: Prometheus.ScrapeConfig.() -> Unit) {

}

fun Prometheus.StaticConfig.targets(block: Prometheus.StaticConfig.() -> Unit) {

}