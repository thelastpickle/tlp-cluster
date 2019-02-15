package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.Pssh
import java.io.File

@Parameters(commandDescription = "Start cassandra on all nodes via service command")
class Start(val context: Context) : ICommand {
    @Parameter(description = "Private key used in keypair", names = ["--sshkey", "-k"])
    var sshKey: String = File(System.getProperty("user.home"), "/.ssh/id_rsa").absolutePath

    override fun execute() {
        check(sshKey.isNotBlank())

        if (!File(sshKey).exists()) {
            println("Unable to find SSH key $sshKey.")
        }

        println("Starting all nodes.")
        val parallelSsh = Pssh(context, sshKey)

        parallelSsh.buildContainer()
        parallelSsh.startService("cassandra")
    }
}