package com.thelastpickle.tlpcluster

import com.thelastpickle.tlpcluster.commands.CopyResourceResult
import org.apache.commons.io.FileUtils
import java.io.File

class Utils {
    companion object {
        fun copyProvisioningScripts(sshKey: String) : OutputResult {
            val dc = DockerCompose(inheritIO = true)
            /*
            pssh parallel-rsync -avrz  \
                            -h hosts.txt -l ubuntu \
                            -O StrictHostKeyChecking=no  \
                            -O UserKnownHostsFile=/local/known_hosts \
                            ./provisioning/ /home/ubuntu/provisioning/
             */
            dc.setSshKeyPath(sshKey)
            return dc.run("pssh", arrayOf("/bin/sh", "/local/copy_provisioning_resources.sh"))
        }

        /**
         * Copies the resource to the appropriate file in the current workspace if it doesn't exist
         * Returns the CopyResourceResult with the file object
         */
        fun maybeCopyResource(name: String, fp: File) : CopyResourceResult {
            if(!fp.exists()) {
                val data =  this::class.java.getResourceAsStream(name)
                FileUtils.copyInputStreamToFile(data, fp)
                return CopyResourceResult.Created(fp)
            }
            return CopyResourceResult.Existed(fp)
        }

        fun install(sshKey: String) : OutputResult {
            // check to make sure there's a cassandra deb package

            val files = FileUtils.listFiles(File("provisioning", "cassandra"), arrayOf("deb"), false)
            if(files.size == 0) {
                println("Massive fail, no deb package for C*, you lose.")
                System.exit(1)
            }

            // pssh /usr/bin/parallel-ssh $PSSH_COMMON_OPTIONS -h /local/hosts.txt 'cd provisioning; sudo sh install.sh'
            val dc = DockerCompose(inheritIO = true)
            dc.setSshKeyPath(sshKey)
            return dc.run("pssh", arrayOf("/bin/sh", "/local/provision_cassandra.sh"))
        }


        fun startCassandra() {
            val dc = DockerCompose(inheritIO = true)
            dc.run("pssh", arrayOf("parallel-ssh", "-ivl", "ubuntu",
                    "-O", "StrictHostKeyChecking=no",
                    "-O", "UserKnownHostsFile=/local/known_hosts",
                    "-h", "/local/hosts.txt",
                    "sudo service cassandra start"))

        }

        fun prompt(question: String, default: String) : String {
            print("$question [$default]: ")
            var line = (readLine() ?: default).trim()

            if(line.equals(""))
                line = default

            return line
        }

    }
}