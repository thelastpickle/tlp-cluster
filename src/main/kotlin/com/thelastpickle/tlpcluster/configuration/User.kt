package com.thelastpickle.tlpcluster.configuration




data class User(
    val email : String,
    val region: String,
    val securityGroups: List<String>,
    val keyName: String,
    val profile: String
)



