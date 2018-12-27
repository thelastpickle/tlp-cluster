package com.thelastpickle.tlpcluster.commands

import java.io.File

class Clean : ICommand {
    override fun execute() {
        File("seeds.txt").delete()
        File("terraform.tfstate").delete()
        File("terraform.tfstate.backup").delete()
        File("stress_ips.txt").delete()
        File("hosts.txt").delete()
    }

}