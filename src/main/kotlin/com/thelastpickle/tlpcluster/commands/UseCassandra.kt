package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.configuration.Seeds
import com.thelastpickle.tlpcluster.configuration.Yaml
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileFilter

@Parameters(commandDescription = "Use a Cassandra build")
class UseCassandra(val context: Context) : ICommand {
    @Parameter
    var name: String = ""

    @Parameter(description = "Configuration settings to change in the cassandra.yaml file specified in the format key:value,...", names = ["--config", "-c"])
    var configSettings = listOf<String>()

    override fun execute() {
        check(name.isNotBlank())

        val buildDir = File(context.cassandraRepo.buildDir, name)
        val currentFile = File(context.cassandraRepo.buildDir, "CURRENT")

        if (!buildDir.exists()) {
            println("Unable to find build $name in the list of builds. Has it been built using the 'build' command?")
            return
        }

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

        // update the seeds list
        val yamlLocation = "provisioning/cassandra/conf/cassandra.yaml"
        val envLocation = "provisioning/cassandra/conf/cassandra-env.sh"

        val fp = File(yamlLocation)
        val yaml = Yaml.create(fp)

        yaml.setProperty("endpoint_snitch", "Ec2Snitch")

        val seedFile = File("seeds.txt")
        if (seedFile.exists()) {
            yaml.setSeeds(Seeds.open(seedFile.inputStream()))
        } else {
            println("WARNING: unable to find seeds.txt file. We failed to update the 'seed_provider' setting! Use tlp-cluster up to start the cluster so can get the seed node list.")
        }

        configSettings.forEach {
            val keyValue = it.split(":")
            if (keyValue.count() > 1) {
                yaml.setProperty(keyValue[0], keyValue[1])
            }
        }

        yaml.write(yamlLocation)

        val env = File(envLocation)
        env.appendText("\nJVM_OPTS=\"\$JVM_OPTS -Dcassandra.consistent.rangemovement=false\"\n")

        currentFile.writeText(name)

        println("Cassandra deb and config copied to provisioning/.  Config files are located in provisioning/cassandra.  Use tlp-cluster install to push the artifacts to the nodes.")
    }
}