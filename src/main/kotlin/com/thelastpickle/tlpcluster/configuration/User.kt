package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Utils
import java.io.File


data class User(
    var email : String,
    var region: String,
    var securityGroup: String,
    var keyName: String,
    var profile: String
) {
    companion object {
        /**
         * Asks a bunch of questions and generates the user file
         */
        fun createInteractively(context: Context, location: File) {
            println("Welcome to the tlp-cluster interactive setup.")
            println("We just need to know a few things before we get started.")


            val email = Utils.prompt("What's your email?", "")
            val region = Utils.prompt("What AWS region do you prefer?", "us-west-2")
            val securityGroup = Utils.prompt("What security group can we put our instances in?  (Must already exist.)", "")
            val keyName = Utils.prompt("What key do you use on this machine to log into instances?", "")
            val profile = Utils.prompt("AWS profile to use?", "")


            val user = User(email, region, securityGroup, keyName, profile)
            context.yaml.writeValue(location, user)

        }

    }
}



