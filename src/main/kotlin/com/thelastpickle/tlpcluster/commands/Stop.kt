package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.ServerType
import com.thelastpickle.tlpcluster.containers.Pssh
import java.io.File

@Parameters(commandDescription = "Stop cassandra on all nodes via service command")
class Stop(val context: Context) : ICommand {

    @Parameter(description = "Start all services on all instances. This overrides all other options", names = ["--all", "-a"])
    var startAll = false

    @Parameter(description = "Start services on monitoring instances", names = ["--monitoring", "-m"])
    var startMonitoring = false

    override fun execute() {
        val sshKeyPath = context.userConfig.sshKeyPath

        check(sshKeyPath.isNotBlank())

        if (!File(sshKeyPath).exists()) {
            println("Unable to find SSH key $sshKeyPath. Aborting stop.")
            return
        }

        if (startAll) {
            startMonitoring = true
        }

        println("Stopping cassandra service on all nodes.")
        val parallelSsh = Pssh(context, sshKeyPath)

        parallelSsh.stopService(ServerType.Cassandra, "cassandra")

        if (startMonitoring && (context.tfstate.getHosts(ServerType.Monitoring).count() > 0)) {
            println("Stopping services on monitoring host.")
            parallelSsh.stopService(ServerType.Monitoring, "grafana-server")
            parallelSsh.stopService(ServerType.Monitoring, "prometheus")
        }
    }
}