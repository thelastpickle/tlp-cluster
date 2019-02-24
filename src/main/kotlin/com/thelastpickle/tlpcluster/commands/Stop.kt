package com.thelastpickle.tlpcluster.commands

import com.thelastpickle.tlpcluster.Context

class Stop(context: Context) : ICommand {
    override fun execute() {
        /*
        cassandra_ssh "sudo service cassandra stop"
         */
        println("Stopping all nodes")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}