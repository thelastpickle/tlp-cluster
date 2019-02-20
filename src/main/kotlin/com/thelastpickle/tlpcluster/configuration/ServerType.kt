package com.thelastpickle.tlpcluster.configuration

enum class ServerType(val serverType: String) {
    Cassandra("cassandra"),
    Stress("stress"),
    Monitoring("monitoring"),
}