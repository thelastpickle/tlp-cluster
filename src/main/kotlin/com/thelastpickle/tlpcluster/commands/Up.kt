package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.ajalt.mordant.TermColors
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.ServerType
import com.thelastpickle.tlpcluster.containers.Terraform
import java.io.File

@Parameters(commandDescription = "Starts instances")
class Up(val context: Context) : ICommand {

    @Parameter(description = "Auto approve changes", names = ["--auto-approve", "-a", "--yes", "-y"])
    var autoApprove = false

    override fun execute() {
        // we have to list both the variable files explicitly here
        // even though we have a terraform.tvars
        // we need the local one to apply at the highest priority
        // specifying the user one makes it take priority over the local one
        // so we have to explicitly specify the local one to ensure it gets
        // priority over user
        val terraform = Terraform(context)

        with(TermColors()) {

            terraform.up(autoApprove).onFailure {
                println(it.message)
                println("${red("Some resources may have been unsuccessfully provisioned.")}  Rerun ${green("tlp-cluster up")} to provision the remaining resources.")
            }.onSuccess {

                println("""Instances have been provisioned.

    You can edit the provisioning scripts before running them, they've been copied to ./provisioning.

    Next you'll probably want to run tlp-cluster build to create a new build, or ${green("tlp-cluster use <version>")} if you already have a Cassandra build you'd like to deploy.""")

                println("Writing ssh config file to sshConfig.")

                println("""The following alias will allow you to easily work with the cluster:
                |
                |${green("source env.sh")}
                |
                |""".trimMargin())
            }
        }
        
        val config = File("sshConfig").bufferedWriter()
        context.tfstate.writeSshConfig(config)

        val envFile = File("env.sh").bufferedWriter()
        context.tfstate.writeEnvironmentFile(envFile)
        var i = 0
        context.tfstate.getHosts(ServerType.Cassandra).forEach {
            envFile.write("export CLUSTER_CONTACT_POINT$i=${it.public}")
            envFile.newLine()
            i++
        }
        envFile.flush()
    }
}