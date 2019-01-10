package com.thelastpickle.tlpcluster.commands

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.thelastpickle.tlpcluster.Cassandra
import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.DockerCompose
import java.io.File

@Parameters(commandDescription = "Build Cassandra (either tag or custom dir)")
class BuildCassandra(val context: Context)  : ICommand {

    @Parameter(description = "Build Name and path (or just a tag)")
    var params: List<String> = mutableListOf()

    override fun execute() {

        val name = params[0]
        val pathOrVersion = params[1]

        val tmp = File(pathOrVersion)

        // does the build already exist?  if so, bail out


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


        Cassandra.build(name, location)
    }
}