package com.thelastpickle.tlpcluster.configuration


class Prometheus(var global: Global = Global("15s"),
                 var scrape_configs: MutableList<ScrapeConfig> = mutableListOf()) {

    class Global(var scape_interval: String = "")

    class ScrapeConfig(var job_name: String = "",
                       var scape_interval: String = "",
                       var static_configs: StaticConfig = StaticConfig()) {

        fun static_config(block: StaticConfig.() -> Unit) {
            static_configs = StaticConfig().apply(block)
        }
    }


    // belongs to scrape config
    class StaticConfig(var targetsList: MutableList<String> = mutableListOf()) {
        fun targets(block: String.() -> Unit) {

        }
    }


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

operator fun String.unaryPlus() {

}

