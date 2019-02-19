package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.Terraform

@Parameters(commandDescription = "Shut down a cluster")
class Down(val context: Context) : ICommand {
    @Parameter(description = "Auto approve changes", names = ["--auto-approve", "-a", "--yes"])
    var autoApprove = false

    override fun execute() {
        println("Crushing dreams, terminating instances.")

        Terraform(context).down(autoApprove)
    }
}