package com.thelastpickle.tlpcluster

import com.thelastpickle.tlpcluster.commands.CopyResourceResult
import org.apache.commons.io.FileUtils
import java.io.File

class Utils {
    companion object {
        fun copyProvisioningScripts() {
            val dc = DockerCompose(inheritIO = true)
            /*
            pssh parallel-rsync -avrz  \
                            -h hosts.txt -l ubuntu \
                            -O StrictHostKeyChecking=no  \
                            -O UserKnownHostsFile=/local/known_hosts \
                            ./provisioning/ /home/ubuntu/provisioning/
             */
            dc.run("pssh", arrayOf("parallel-rsync", "-avrz",
                    "-h", "hosts.txt", "-l", "ubuntu",
                    "-O", "StrictHostKeyChecking=no",
                    "-O", "UserKnownHostsFile=/local/known_hosts",
                    "./provisioning/", "/home/ubuntu/provisioning/"))

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

        fun install() {
            // check to make sure there's a cassandra deb package

            val files = FileUtils.listFiles(File("provisioning", "cassandra"), arrayOf("deb"), false)
            if(files.size == 0) {
                println("Massive fail, no deb package for C*, you lose.")
                System.exit(1)
            }

            // pssh /usr/bin/parallel-ssh $PSSH_COMMON_OPTIONS -h /local/hosts.txt 'cd provisioning; sudo sh install.sh'
            val dc = DockerCompose(inheritIO = true)
            dc.run("pssh", arrayOf("parallel-ssh", "-ivl", "ubuntu",
                    "-O", "StrictHostKeyChecking=no",
                    "-O", "UserKnownHostsFile=/local/known_hosts",
                    "-h", "/local/hosts.txt",
                    "cd provisioning; chmod +x install.sh; sudo ./install.sh cassandra"))


        }


        fun startCassandra() {
            val dc = DockerCompose(inheritIO = true)
            dc.run("pssh", arrayOf("parallel-ssh", "-ivl", "ubuntu",
                    "-O", "StrictHostKeyChecking=no",
                    "-O", "UserKnownHostsFile=/local/known_hosts",
                    "-h", "/local/hosts.txt",
                    "sudo service cassandra start"))

        }

    }
}