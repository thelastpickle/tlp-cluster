package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.InputStream
import java.util.LinkedHashMap



class TFState(val context: Context,
              val file: InputStream) {

    private var tree  = context.json.readTree(file)
    private var log = logger()
    companion object {
        fun parse(context: Context, path: File) : TFState {
            return TFState(context, path.inputStream())
        }
    }

    fun getHosts(serverType: ServerType) : List<Host> {
        val nodes = tree.path("modules")
                        .first()
                        .path("resources")

        val result = mutableListOf<Host>()
        val resources = context.json.convertValue(nodes, Map::class.java)

        val hostRegex = """aws_instance\.(.*).(\d+)""".toRegex()

        for((name, resource) in resources.entries) {
//            log.debug { "Resource ($name): $resource" }

            resource as Map<String, *>

            val attrs = (resource.get("primary") as LinkedHashMap<*, *>).get("attributes") as LinkedHashMap<String, String>

            val private = attrs.get("private_ip")
            val public = attrs.get("public_ip")

            val serverName = name as String


            if(serverName.contains(serverType.serverType)) {
                log.debug { "Found instance $serverName ips: $private, $public" }

                val tmp = hostRegex.find(serverName)!!.groups

                log.debug { "Regex find: $tmp" }
                val host = Host(public!!, private!!, tmp[1]?.value.toString() + tmp[2]?.value.toString())
                log.info { "Adding host: $host" }
                result.add(host)
            }

        }
        return result
    }
}