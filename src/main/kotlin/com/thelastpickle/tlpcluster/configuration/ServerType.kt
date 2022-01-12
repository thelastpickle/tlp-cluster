package com.thelastpickle.tlpcluster.configuration

enum class ServerType(val serverType: String, val shortServerType: String) {
    Cassandra("cassandra", "c"),
    Stargate("stargate", "sg"),
    Stress("stress", "s"),
    Monitoring("monitoring", "m"),
}