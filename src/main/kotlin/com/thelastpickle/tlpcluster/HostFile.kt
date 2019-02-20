package com.thelastpickle.tlpcluster

data class Host(val public: String,
                val private: String,
                val alias: String)

typealias HostList = List<Host>

data class HostFile(val cassandraPublic: HostList,
                    val cassandraPrivate: HostList,
                    val stressPublic: HostList) {

    companion object {
        fun (cassandraPublic: String, cassandraPrivate: Any): Unit {

        }
        val pubHosts = cPublic.forEachIndexed { index, s -> Host(s, "node$index")  }
    }


}
