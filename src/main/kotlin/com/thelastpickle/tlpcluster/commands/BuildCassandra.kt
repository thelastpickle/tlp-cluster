package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.containers.CassandraBuildJava8
import java.io.File

@Parameters(commandDescription = "Build Cassandra (either tag or custom dir)")
class BuildCassandra(val context: Context)  : ICommand {

    @Parameter(description = "build_name [path | tag]")
    var params: List<String> = mutableListOf()

    override fun execute() {

        val name = params[0]
        val pathOrVersion = params[1]

        val tmp = File(pathOrVersion)

        // is this a directory?
        val location = if(tmp.exists()) {
            tmp
        } else {
            // not a dir, must be a tag
            println("Directory not found, attempting to build ref $pathOrVersion")
            context.cassandraRepo.checkoutVersion(pathOrVersion)
            context.cassandraRepo.gitLocation
        }

        context.createBuildSkeleton(name)

        val cassandraBuilder = CassandraBuildJava8(context)

        // create the container
        cassandraBuilder.buildContainer()
        println("Starting cassandra build process")
        
        cassandraBuilder.runBuild(location.absolutePath, name)
    }
}