package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import org.apache.logging.log4j.kotlin.logger
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.util.LinkedHashMap


typealias HostList = List<Host>

class TFState(val context: Context,
              val file: InputStream) {

    private var tree  = context.json.readTree(file)
    private var log = logger()
    companion object {
        fun parse(context: Context, path: File) : TFState {
            return TFState(context, path.inputStream())
        }
    }



    fun getHosts(serverType: ServerType) : HostList {
        val nodes = tree.path("modules")
                        .first()
                        .path("resources")

        val result = mutableListOf<Host>()
        val resources = context.json.convertValue(nodes, Map::class.java)


        for((name, resource) in resources.entries) {
            resource as Map<String, *>

            val attrs = (resource.get("primary") as LinkedHashMap<*, *>).get("attributes") as LinkedHashMap<String, String>

            val private = attrs.get("private_ip")
            val public = attrs.get("public_ip")

            val serverName = name as String


            if(serverName.contains(serverType.serverType)) {
                val host = Host.fromTerraformString(serverName, public!!, private!!)
                log.info { "Adding host: $host" }
                result.add(host)
            }

        }
        return result
    }

    fun writeSshConfig(config: BufferedWriter) {
        val cassandra = getHosts(ServerType.Cassandra)
        val stress = getHosts(ServerType.Stress)

        // write standard stuff first
        config.appendln("StrictHostKeyChecking=no")
        config.appendln("User ubuntu")

        for (host in cassandra) {
            // aliases
            // adding node0 or c0 would be a reasonable time saver
            config.appendln("Host ${host.alias}")
            config.appendln("  Hostname ${host.public}")
            config.appendln()
        }

        for(host in stress) {
            config.appendln("Host ${host.alias}")
            config.appendln("  Hostname ${host.public}")
            config.appendln()
        }
        config.flush()

    }


}