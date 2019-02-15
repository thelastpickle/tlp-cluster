package com.thelastpickle.tlpcluster.commands

import java.io.File

class Clean : ICommand {
    override fun execute() {
        val toDelete = listOf(
                "create_provisioning_resources.sh",
                "seeds.txt",
                "terraform.tfstate",
                "terraform.tfstate.backup",
                "stress_ips.txt",
                "hosts.txt",
                "terraform.tf.json",
                "terraform.tfvars"
        )

        for(f in toDelete) {
            File(f).delete()
        }
        File(".terraform").deleteRecursively()
        File("provisioning").deleteRecursively()
    }

}