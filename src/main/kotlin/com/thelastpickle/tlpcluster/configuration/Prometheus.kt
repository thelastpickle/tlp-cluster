package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonProperty

class Prometheus(val global: Global,
                 val scrape_configs: List<Job>) {
    class Global(@JsonProperty("scrape_interval") val scapeInterval: String)
    class Job(val job_name: String)
    

}