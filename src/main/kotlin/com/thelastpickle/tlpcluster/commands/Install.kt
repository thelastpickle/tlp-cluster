package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Utils

@Parameters(commandNames = ["install"], commandDescription = "Install Everything")
class Install : ICommand {
    override fun execute() {
        Utils.copyProvisioningScripts()
        Utils.install()

        println("Keys, provisioning scripts, and packages have been pushed to the nodes and installed.  Use tlp-cluster start to fire up the cluster.")
    }
}