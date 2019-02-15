package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.Terraform
import java.io.File

@Parameters(commandDescription = "Starts instances")
class Up(val context: Context) : ICommand {
    override fun execute() {
        // we have to list both the variable files explicitly here
        // even though we have a terraform.tvars
        // we need the local one to apply at the highest priority
        // specifying the user one makes it take priority over the local one
        // so we have to explicitly specify the local one to ensure it gets
        // priority over user
        val terraform = Terraform(context)

        terraform.up().onFailure {
            println(it.message)
            println("Some resources may have been unsuccessfully provisioned.")
            return
        }

        terraform.cassandraIps().onSuccess {
            File("hosts.txt").writeText(convertToUsefulFile(it))
        }

        terraform.cassandraInternalIps().onSuccess {
            File("seeds.txt").writeText(convertToUsefulFile(it.lines().take(3).joinToString("\n")))
        }

        terraform.stressIps().onSuccess {
            File("stress_ips.txt").writeText(it)
        }

        println("""Instances have been provisioned.  Cassandra hosts are located in hosts.txt.
Seeds are using internal IPs and are located in seeds.txt.
Stress nodes (if provisioned) are in stress.txt.

You can edit the provisioning scripts before running them, they've been copied to ./provisioning.

Next you'll probably want to run tlp-cluster build to create a new build, or use if you already have a Cassandra build you'd like to deploy.""")
    }

    /**
     * Cleans up the blah,\nblah2 style lines
     */
    internal fun convertToUsefulFile(output: String) : String {
        return output.lines().map { it.replace(",", "") }.joinToString("\n")
    }
}