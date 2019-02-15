package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.Pssh
import org.apache.commons.io.FileUtils
import java.io.File

@Parameters(commandNames = ["install"], commandDescription = "Install Everything")
class Install(val context: Context) : ICommand {
    @Parameter(description = "Private key used in keypair", names = ["--sshkey", "-k"])
    var sshKey: String = File(System.getProperty("user.home"), "/.ssh/id_rsa").absolutePath

    override fun execute() {
        check(sshKey.isNotBlank())

        if (!File(sshKey).exists()) {
            println("Unable to find SSH key $sshKey.")
        }

        // check to make sure there's a cassandra deb package
        val files = FileUtils.listFiles(File("provisioning", "cassandra"), arrayOf("deb"), false)
        if(files.isEmpty()) {
            println("Massive fail, no deb package for C*, you lose.")
            System.exit(1)
        }

        val successMessage = """
            Keys, provisioning scripts, and packages have been pushed to the nodes and installed.  Use tlp-cluster start to fire up the cluster.
        """.trimIndent()
        val retryMessage = " This could be a transient error; try rerunning the install command."
        val parallelSsh = Pssh(context, sshKey)

        parallelSsh.buildContainer()

        // we only want to run the install if the copy was successful
        parallelSsh.copyProvisioningResources().fold(
            {
                parallelSsh.provisionNode("cassandra").fold(
                    { println(successMessage) },
                    { println("Failed to provision all nodes. " + it.message + retryMessage) }
                )
            },
            { println("Failed to copy provisioning resources to all nodes. " + it.message + retryMessage) }
        )
    }
}