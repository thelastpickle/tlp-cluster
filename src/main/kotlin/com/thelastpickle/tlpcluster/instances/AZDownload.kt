package com.thelastpickle.tlpcluster.instances.import

/**
 * This is a utility class for generating the yaml we ship in the resources
 * We may move towards a more dynamic approach, but for now we'll ship the hard coded yaml
 *
 * As a result, it's required that you already have the AWS credentials set up in the manner that the AWS
 * library is expecting them - the same way the CLI tool works.
 *
 * Will aim to unify this at some point, but I dont' expect we'll do this more than once a year so I'm not into wasting
 * time to make it perfect
 *
 * Ec2 instances: https://raw.githubusercontent.com/powdahound/ec2instances.info/master/www/instances.json
 */
class AZDownload {


}