package com.thelastpickle.tlpcluster

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.thelastpickle.tlpcluster.commands.*
import java.io.File

class MainArgs {
    @Parameter(names = ["--help", "-h"], description = "Shows this help.")
    var help = false
}

fun main(arguments: Array<String>) {

    val tlpclusterUserDirectory = File(System.getProperty("user.home"), "/.tlp-cluster/")

    // this will automatically clone the C* repo
    val cassFp = File(System.getProperty("user.home"), "/.tlp-cluster/cassandra")
    val cass = Cassandra(cassFp)

    // make sure we can do cassandra builds

    val context = Context(tlpclusterUserDirectory, cass)

    val jcommander = JCommander.newBuilder().programName("tlp-cluster")

    val args = MainArgs()
    jcommander.addObject(args)

    val commands = mapOf("init" to Init(context),
                         "up" to Up(context),
                         "start" to Start(context),
                         "stop" to Stop(),
                         "install" to Install(context),
                         "down" to Down(context),
                         "build" to BuildCassandra(context),
                         "ls" to ListCassandraBuilds(context),
                         "use" to UseCassandra(context),
                         "clean" to Clean())

    for(c in commands.entries) {
        jcommander.addCommand(c.key, c.value)
    }

    val jc = jcommander.build()
    jc.parse(*arguments)

    val commandObj = commands[jc.parsedCommand]

    if(commandObj != null)
        commandObj.execute()
    else
        jc.usage()


    println("Done")

    // currently this is a work around to break out of a thread (netty?) that is never shut down.
    System.exit(0)
}

