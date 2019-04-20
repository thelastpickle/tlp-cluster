package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonProperty

class Prometheus(var global: Global = Global("15s"),
                 var scrape_configs: MutableList<Job> = mutableListOf()) {

    class Global(var scape_interval: String)
    class Job(var job_name: String)


}

fun prometheus(block: Prometheus.() -> Unit) : Prometheus {
    return Prometheus().apply(block)
}

fun Prometheus.global(block: Prometheus.Global.() -> Unit) {

}