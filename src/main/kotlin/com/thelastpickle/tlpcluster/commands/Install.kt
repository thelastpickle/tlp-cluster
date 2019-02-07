package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Utils
import java.io.File

@Parameters(commandNames = ["install"], commandDescription = "Install Everything")
class Install : ICommand {
    @Parameter(description = "Number of Cassandra instances", names = ["--sshkey", "-k"])
    var sshKey: String = ""

    override fun execute() {
        check(sshKey.isNotBlank())

        if (!File(sshKey).exists()) {
            println("Unable to find SSH key $sshKey.")
        }

        val successMessage = "Keys, provisioning scripts, and packages have been pushed to the nodes and installed.  Use tlp-cluster start to fire up the cluster."
        val retryMessage = " This could be a transient error; try rerunning the install command."

        // we only want to run the install if the copy was successful
        Utils.copyProvisioningScripts(sshKey).fold(
            {
                Utils.install(sshKey).fold(
                    { println(successMessage) },
                    { println("Failed to provision all nodes. " + it.message + retryMessage) }
                )
            },
            { println("Failed to copy provisioning resources to all nodes. " + it.message + retryMessage) }
        )
    }
}