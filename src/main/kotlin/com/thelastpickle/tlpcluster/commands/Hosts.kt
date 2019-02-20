package com.thelastpickle.tlpcluster.commands

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.HostList
import com.thelastpickle.tlpcluster.configuration.ServerType

class Hosts(val context: Context) : ICommand {

    data class HostOutput(val cassandra: HostList, val stress: HostList, val monitoring: HostList)

    override fun execute() {
        val output = with(context.tfstate) {
            HostOutput(getHosts(ServerType.Cassandra),
                    getHosts(ServerType.Stress),
                    getHosts(ServerType.Monitoring))
        }
        context.yaml.writeValue(System.out, output)
    }
}