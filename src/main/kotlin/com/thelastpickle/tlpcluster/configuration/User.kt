package com.thelastpickle.tlpcluster.configuration

import com.thelastpickle.tlpcluster.Context
import com.thelastpickle.tlpcluster.Utils
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.logging.log4j.kotlin.logger
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ec2.Ec2Client
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupEgressRequest
import java.io.File
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest
import java.util.*


data class User(
    var email : String,
    var region: String,
    var securityGroup: String,
    var keyName: String,
    var sshKeyPath: String,

    // if true we'll load the profile from the AWS credentials rather than this file
    // can over
    var awsProfile: String,
    // fallback for people who haven't set up the aws cli

    var awsAccessKey: String,
    var awsSecret: String
) {
    companion object {

        var log = logger()
        /**
         * Asks a bunch of questions and generates the user file
         */
        fun createInteractively(context: Context, location: File) {
            println("Welcome to the tlp-cluster interactive setup.")
            println("We just need to know a few things before we get started.")

            val email = Utils.prompt("What's your email?", "")

            // we're not honoring it, so we'll take this out
            //val region = Utils.prompt("What AWS region do you prefer?", "us-west-2")
//            val region = "us-west-2"

            val awsAccessKey = Utils.prompt("AWS Access Key?", "")
            val awsSecret = Utils.prompt("Aws Secret?", "")
            // create the key pair

//            Ec2Client.builder().
            // TODO: rename
            println("Attempting to validate credentials and generate tlp-cluster login keys")

            val region = Region.US_WEST_2

            val creds = AwsBasicCredentials.create(awsAccessKey, awsSecret)

            // TODO: Abstract the provider out
            // tlp cluster should have its own provider that uses the following order:
            // tlp-cluster config, AWS config
            val uuid = UUID.randomUUID().toString()

            val ec2 = Ec2Client.builder().region(region)
                    .credentialsProvider { creds }
                    .build()

            val keyName = "tlp-cluster-$uuid"
            val request = CreateKeyPairRequest.builder()
                    .keyName(keyName).build()

            val response = ec2.createKeyPair(request)


            // write the private key into the ~/.tlp-cluster/profiles/<profile>/ dir

            val secret = File(context.profileDir, "secret.pem")
            secret.writeText(response.keyMaterial())


            val securityGroup = "$email-$uuid"
            val scRequest = CreateSecurityGroupRequest.builder()
                    .groupName(email)
                    .description("tlp-cluster security group")
                    .build()

            log.info { "Creating security group $scRequest" }
            val newSG = ec2.createSecurityGroup(scRequest)

            AuthorizeSecurityGroupEgressRequest

            //val securityGroup = Utils.prompt("What security group can we put our instances in?  (Must already exist.)", "")
            //val sshKeyPath = Utils.resolveSshKeyPath(Utils.prompt("What is the path to the private key associated with your AWS SSH key pair?", default = ""))


            val user = User(email,
                    "us-west-2",
                    securityGroup,
                    keyName,
                    secret.absolutePath, "",
                    awsAccessKey,
                    awsSecret)

            context.yaml.writeValue(location, user)
        }

    }
}



