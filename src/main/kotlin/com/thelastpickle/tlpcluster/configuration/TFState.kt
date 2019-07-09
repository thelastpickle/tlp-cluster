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
            val az = attrs.get("availability_zone")

            val serverName = name as String?

            if(public != null && private != null && serverName != null) {

                if(serverName.contains(serverType.serverType)) {
                    val host = Host.fromTerraformString(serverName as String, public, private, az as String)
                    log.info { "Adding host: $host" }
                    result.add(host)
                }
            } else {
                log.error("Invalid terraform state: null IP for $serverName, check terraform.tfstate to debug.")
            }

        }
        return result
    }

    fun writeSshConfig(config: BufferedWriter) {
        // write standard stuff first
        config.appendln("StrictHostKeyChecking=no")
        config.appendln("User ubuntu")
        config.appendln("IdentityFile ${context.userConfig.sshKeyPath}")

        // get each server type and get the hosts for type and add it to the sshConfig.
        ServerType.values().forEach {
            getHosts(it).forEach {
                config.appendln("Host ${it.alias}")
                config.appendln(" Hostname ${it.public}")
                config.appendln()
            }
        }
        config.flush()
    }

    fun writeEnvironmentFile(fp: BufferedWriter) {

        // write the initial SSH aliases

        fp.appendln("#!/bin/bash")
        fp.appendln()

        var i = 0
        fp.append("SERVERS=(")
        getHosts(ServerType.Cassandra).forEach {
            fp.append("cassandra$i ")
            i++
        }
        fp.appendln(")")

        i=0
        getHosts(ServerType.Cassandra).forEach {
            fp.appendln("alias c${i}=\"ssh cassandra${i}\"")
            i++
        }

        fp.appendln()

        val content = this.javaClass.getResourceAsStream("env.sh").bufferedReader()
        val lines = content.readLines().toMutableList()

        for(line in lines) {
            fp.appendln(line)
        }
        fp.flush()
    }


}