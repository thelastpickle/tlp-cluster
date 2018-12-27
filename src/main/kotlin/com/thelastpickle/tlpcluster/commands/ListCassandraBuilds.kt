package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context

@Parameters(commandDescription = "List available builds", commandNames = ["list", "ls"])
class ListCassandraBuilds(val context: Context) : ICommand {
    override fun execute() {
        val files = context.cassandraRepo.listBuilds()
        for(f in files) {
            println(f)
        }
    }
}