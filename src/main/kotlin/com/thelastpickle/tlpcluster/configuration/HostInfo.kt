package com.thelastpickle.tlpcluster.configuration

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

data class HostInfo(@JsonIgnore  var address: String = "",
                    @JsonProperty("instance") var name: String = "",
                    var environment: String = "",
                    var cluster: String = "",
                    var datacenter: String = "",
                    var rack: String = "")
