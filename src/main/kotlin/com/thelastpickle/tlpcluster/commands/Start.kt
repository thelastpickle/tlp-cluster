package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.*
import com.thelastpickle.tlpcluster.containers.Pssh
import java.io.File

@Parameters(commandDescription = "Start cassandra on all nodes via service command")
class Start(val context: Context) : ICommand {

    override fun execute() {
        val sshKeyPath = context.userConfig.sshKeyPath

        check(sshKeyPath.isNotBlank())

        if (!File(sshKeyPath).exists()) {
            println("Unable to find SSH key $sshKeyPath. Aborting start.")
            return
        }

        println("Starting all nodes.")
        val parallelSsh = Pssh(context, sshKeyPath)
        val successMessage = "service successfully started"
        val failureMessage = "service failed to started"

        var serviceName = "cassandra"
        parallelSsh.startService(ServerType.Cassandra, serviceName, NodeFilter.ALL).fold({
            println("$serviceName $successMessage")}, {
            println("$serviceName $failureMessage. ${it.message}")
            return
        })

        val monitoringHost = context.tfstate.getHosts(ServerType.Monitoring)
        val stargateHost = context.tfstate.getHosts(ServerType.Stargate)

        serviceName = "stargate_first"
        if (stargateHost.count() > 0) {
            // Start the first target node
//            parallelSsh.startStargateService(NodeFilter.FIRST).fold({
            parallelSsh.startService(ServerType.Stargate, serviceName, NodeFilter.FIRST).fold({
                println("$serviceName $successMessage")}, {
                println("$serviceName $failureMessage. ${it.message}")
                return
            })
        }

        serviceName = "stargate_all"
        if (stargateHost.count() > 1) {
//            parallelSsh.startStargateService(NodeFilter.ALL_BUT_FIRST).fold({
            parallelSsh.startService(ServerType.Stargate, serviceName, NodeFilter.ALL_BUT_FIRST).fold({
                println("$serviceName $successMessage")}, {
                println("$serviceName $failureMessage. ${it.message}")
                return
            })
        }

        serviceName = "prometheus"
        parallelSsh.startService(ServerType.Monitoring, serviceName, NodeFilter.ALL).fold({
            println("$serviceName $successMessage")

            serviceName = "grafana-server"
            parallelSsh.startService(ServerType.Monitoring, serviceName, NodeFilter.ALL).fold({
                println("Grafana started")

                println("$serviceName $successMessage")
                println("""
You can access the monitoring UI using the following URLs:
- Prometheus: http://${monitoringHost.first().public}:9090
- Grafana:    http://${monitoringHost.first().public}:3000
                    """)
            }, {
                // error starting grafana
                println("$serviceName $failureMessage. ${it.message}")
            })
        }, {
            // error starting prometheus
            println("$serviceName $failureMessage. ${it.message}")
        })
    }
}