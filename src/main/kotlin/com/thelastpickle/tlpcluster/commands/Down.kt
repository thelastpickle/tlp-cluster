package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.Terraform

@Parameters(commandDescription = "Shut down a cluster")
class Down(val context: Context) : ICommand {
    override fun execute() {
        println("Crushing dreams, terminating instances.")

        Terraform(context).down()
    }
}