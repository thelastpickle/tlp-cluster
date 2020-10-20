package com.thelastpickle.tlpcluster

enum class Containers(val containerName: String, val tag: String) {
    PSSH("thelastpickle/pssh", "1.0"),
    CASSANDRA_BUILD("thelastpickle/cassandra-build", "1.0"),
    TERRAFORM("hashicorp/terraform", "0.11.14");

    val imageWithTag : String
        get() = "$containerName:$tag"
}
