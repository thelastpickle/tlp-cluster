package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.InputStream
import java.util.LinkedHashMap


typealias HostList = List<Host>

/**
 * Returns a list of IPs ready for PSSH's -H format, exported as an ENV
 */
fun HostList.toEnv() = "PSSH_HOSTNAMES=" + map { "-H ${it.public}" }.joinToString(" ")


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


}