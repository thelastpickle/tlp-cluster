package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.Seeds
import com.thelastpickle.tlpcluster.configuration.Yaml
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileFilter
import java.io.FilenameFilter
import java.io.InputStream

@Parameters(commandDescription = "Use a Cassandra build")
class UseCassandra(val context: Context) : ICommand {
    @Parameter
    var name: String = ""

    override fun execute() {
        check(name.isNotBlank())

        val buildDir = File(context.cassandraRepo.buildDir, name)
        val conf = File(buildDir, "conf")
        val debs = File(buildDir, "deb")

        val artifactDest = File("provisioning/cassandra/")

        println("Destination artifacts: $artifactDest")
        artifactDest.mkdirs()

        // delete existing deb packages
        for(deb in artifactDest.listFiles(FileFilter { it.extension.equals("deb") })) {
            deb.delete()
        }

        for(deb in debs.listFiles().filter { it.isFile }) {
            println("Copying $deb")
            FileUtils.copyFileToDirectory(deb, artifactDest)
        }

        val configDest = File(artifactDest, "conf")
        configDest.mkdir()

        for(config in conf.listFiles()) {
            println("Copying configuration $config")
            if(config.isDirectory) {
                FileUtils.copyDirectory(config, configDest)
            } else {
                FileUtils.copyFileToDirectory(config, configDest)
            }
        }

        val seedFile = "seeds.txt"
        val seeds = Seeds.open(File(seedFile).inputStream())

        // update the seeds list
        val yamlLocation = "provisioning/cassandra/conf/cassandra.yaml"
        val envLocation = "provisioning/cassandra/conf/cassandra-env.sh"

        val fp = File(yamlLocation)
        val yaml = Yaml.create(fp)

        yaml.setSeeds(seeds)
        yaml.setProperty("endpoint_snitch", "Ec2Snitch")

        yaml.write(yamlLocation)

        val env = File(envLocation)
        env.appendText("\nJVM_OPTS=\"\$JVM_OPTS -Dcassandra.consistent.rangemovement=false\"\n")

        println("Cassandra deb and config copied to provisioning/.  Config files are located in provisioning/cassandra.  Use tlp-cluster install to push the artifacts to the nodes.")


    }
}