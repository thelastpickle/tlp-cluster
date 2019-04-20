package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.github.ajalt.mordant.TermColors
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.YamlDelegate
import com.thelastpickle.tlpcluster.configuration.ServerType
import com.thelastpickle.tlpcluster.configuration.CassandraYaml
import com.thelastpickle.tlpcluster.configuration.prometheus
import com.thelastpickle.tlpcluster.containers.CassandraUnpack
import org.apache.commons.io.FileUtils
import org.apache.logging.log4j.kotlin.logger
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.util.*

@Parameters(commandDescription = "Use a Cassandra build")
class UseCassandra(val context: Context) : ICommand {
    @Parameter
    var name: String = ""

    val log = logger()

    @Parameter(description = "Configuration settings to change in the cassandra.yaml file specified in the format key:value,...", names = ["--config", "-c"])
    var configSettings = listOf<String>()

    val yaml by YamlDelegate()

    override fun execute() {
        check(name.isNotBlank())
        try {
            context.tfstate
        } catch (e: FileNotFoundException) {
            println("Error: terraform config file not found.  Please run tlp-cluster up first to establish IP addresses for seed listing.")
            System.exit(1)
        }

        // setup the provisioning directory
        val artifactDest = File("provisioning/cassandra/")

        println("Destination artifacts: $artifactDest")
        artifactDest.mkdirs()

        // delete existing deb packages
        for(deb in artifactDest.listFiles(FileFilter { it.extension.equals("deb") })) {
            deb.delete()
        }

        // if we're been passed a version, use the debs we get from apache
        val versionRegex = """\d+\.\d+\.\d+""".toRegex()


        if(versionRegex.matches(name)) {
            val cacheLocation = File(System.getProperty("user.home"), ".tlp-cluster/cache")
            println("Using released version $name")
            val unpacker = CassandraUnpack(context, name, artifactDest.toPath(), Optional.of(cacheLocation.toPath()))
            unpacker.download()
            unpacker.extractConf()

        } else {
            // otherwise it's a custom build

            val buildDir = File(context.cassandraRepo.buildDir, name)

            if (!buildDir.exists()) {
                println("Unable to find build $name in the list of builds. Has it been built using the 'build' command?")
                return
            }

            val conf = File(buildDir, "conf")
            val debs = File(buildDir, "deb")


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
        }


        // update the seeds list
        val cassandraYamlLocation = "provisioning/cassandra/conf/cassandra.yaml"
        val cassandraEnvLocation = "provisioning/cassandra/conf/cassandra-env.sh"
        val cassandraYaml = CassandraYaml.create(File(cassandraYamlLocation))

        cassandraYaml.setProperty("endpoint_snitch", "Ec2Snitch")

        val cassandraHosts = context.tfstate.getHosts(ServerType.Cassandra)
        val seeds = cassandraHosts.take(3)

        cassandraYaml.setSeeds(seeds.map { it.private })

        configSettings.forEach {
            val keyValue = it.split(":")
            if (keyValue.count() > 1) {
                cassandraYaml.setProperty(keyValue[0], keyValue[1])
            }
        }

        log.debug { "Writing Cassandra YAML to $cassandraYamlLocation" }
        cassandraYaml.write(cassandraYamlLocation)

        val stressHosts = context.tfstate.getHosts(ServerType.Stress)

        // if using a monitoring instance, set the hosts to pull metrics from
        if (context.tfstate.getHosts(ServerType.Monitoring).count() > 0) {
            val prometheusYamlLocation = "provisioning/monitoring/config/prometheus/prometheus.yml"

            // TODO: Move out of here and make it more testable
            val prometheus = prometheus {
                scrape_config {
                    job_name = "prometheus"

                    static_config {
                        job_name = "prometheus"
                        targets = listOf("localhost:9090")
                    }
                    static_config {
                        job_name = "cassandra"
                        targets = cassandraHosts.map { "${it.private}:9500" }

                    }
                    static_config {
                        job_name = "stress"
                        targets = stressHosts.map { "${it.private}:9501" }
                    }
                }
            }

            val file = File(prometheusYamlLocation)
            log.debug { "Writing Prometheus YAML to $prometheusYamlLocation" }
            yaml.writeValue(file, prometheus)
        }

        val env = File(cassandraEnvLocation)
        env.appendText("\nJVM_OPTS=\"\$JVM_OPTS -Dcassandra.consistent.rangemovement=false\"\n")


        with(TermColors()) {
            println("Cassandra deb and config copied to provisioning/.  Config files are located in provisioning/cassandra. \n Use ${green("tlp-cluster install")} to push the artifacts to the nodes.")
        }
    }
}