package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.DockerCompose
import sun.misc.IOUtils
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


        val dc = DockerCompose(inheritIO = true)

        val applyResult = dc.run("terraform", arrayOf("apply", "-var-file=/user/user.tfvars", "-var-file=/local/terraform.tfvars", "/local"))

        val dc2 = DockerCompose()

        val cassIps = dc2.run("terraform", arrayOf("output", "cassandra_ips"))
        val internalCassIps = dc2.run("terraform", arrayOf("output", "cassandra_internal_ips"))
        val stressIps = dc2.run("terraform", arrayOf("output", "stress_ips"))

        cassIps.fold({
                File("hosts.txt").writeText(convertToUsefulFile(it.output))
            },
            {
                println("Could not run docker-compose commands - missing yaml.  Run tlp-cluster init to fix")
                System.exit(1)
            }
        )

        internalCassIps.onSuccess {
            File("seeds.txt").writeText(convertToUsefulFile(it.output.lines().take(3).joinToString("\n")))
        }
        // TODO: handle failure

        stressIps.onSuccess {
            File("stress_ips.txt").writeText(convertToUsefulFile(it.output.lines().take(3).joinToString("\n")))
        }
        // TODO: handle failure

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