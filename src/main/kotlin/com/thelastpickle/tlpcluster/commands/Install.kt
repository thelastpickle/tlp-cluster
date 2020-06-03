package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.github.ajalt.mordant.TermColors
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.ServerType
import com.thelastpickle.tlpcluster.containers.Pssh
import org.apache.commons.io.FileUtils
import java.io.File

@Parameters(commandNames = ["install"], commandDescription = "Install Everything")
class Install(val context: Context) : ICommand {
    private val retryMessage = "This could be a transient error, trying again..."
    private val failureMessage = "This could be a transient error; try rerunning the install command."

    override fun execute() {
        val sshKeyPath = context.userConfig.sshKeyPath

        check(sshKeyPath.isNotBlank())

        if (!File(sshKeyPath).exists()) {
            println("Unable to find SSH key $sshKeyPath. Aborting install.")
            return
        }

        // check to make sure there's a cassandra deb package
        val files = FileUtils.listFiles(File("provisioning", "cassandra"), arrayOf("deb"), false)
        if(files.isEmpty()) {
            println("Massive fail, no deb package for C*, you lose.")
            System.exit(1)
        }

        var installSuccessful: Boolean? = null
        var attempts = 0
        while (installSuccessful != true && attempts < 5) {
            installSuccessful = null
            attempts++
            val parallelSsh = Pssh(context, sshKeyPath)

            // iterate through the different sever types and run their install scripts (via the Pssh container)
            // we use a filter to select only the instances we have provisioned e.g. if we only have Cassandra nodes, then
            // we will only run the install functions for the cassandra hosts.
            ServerType
                    .values()
                    .filter { context.tfstate.getHosts(it).count() > 0 }
                    .forEach {
                        // only do the install if we have had previous successful installs otherwise we should skip doing any
                        // further installs.
                        // we are unable to put a break in because it is unsupported in a forEach loop in Kotlin.
                        if (installSuccessful != false) {
                            installSuccessful = provisionServer(it, parallelSsh)
                        } else {
                            println("Skipping ${it.serverType} provisioning and install due to previous errors.")
                        }
                    }

            if (installSuccessful == true) {
                with(TermColors()) {
                    println("Now run ${green("tlp-cluster start")} to fire up the cluster.")

                }
            }
        }

        if (installSuccessful == false) {
            with(TermColors()) {
                println("${red("Install failed!")} $failureMessage installSuccessful=$installSuccessful and attempts=$attempts")

            }
        }
    }

    private fun provisionServer(server: ServerType, parallelSsh: Pssh) : Boolean {
        println("Provisioning ${server.serverType}")
        val serverTypeItr = server
        // we only want to run the install if the copy was successful
        parallelSsh.copyProvisioningResources(serverTypeItr).fold({
            // need to create a new instance here b/c of duplicate volume mapping issues
            parallelSsh.provisionNode(serverTypeItr).fold({
                println("Keys, provisioning scripts, and packages have been pushed to the nodes " +
                        "and installed on ${serverTypeItr.serverType} nodes.")
            }, {
                println("Failed to provision all ${serverTypeItr.serverType} nodes. ${it.message}")
                return false
            })
        }, {
            println("Failed to copy provisioning resources to all ${serverTypeItr.serverType} nodes. ${it.message}")
            return false
        })

        return true
    }
}