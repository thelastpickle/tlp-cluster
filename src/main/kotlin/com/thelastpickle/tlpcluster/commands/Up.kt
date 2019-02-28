package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
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

        terraform.up(autoApprove).onFailure {
            println(it.message)
            println("Some resources may have been unsuccessfully provisioned.")
            return
        }

        println("""Instances have been provisioned.

You can edit the provisioning scripts before running them, they've been copied to ./provisioning.

Next you'll probably want to run tlp-cluster build to create a new build, or use if you already have a Cassandra build you'd like to deploy.""")

        println("Writing ssh config file to sshConfig.")

        println("""The following alias will allow you to easily ssh to the cluster:
            |
            |alias ssh="ssh -F sshConfig"
            |
            |""".trimMargin())

        val config = File("sshConfig").bufferedWriter()
        context.tfstate.writeSshConfig(config)

    }

}