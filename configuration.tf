provider "aws" {
    region = "${var.region}"
    shared_credentials_file = "/credentials"
    profile = "${var.profile}"
}


variable cassandra_instance_type {
    default = "m5d.xlarge"
}

variable stress_instance_type {
    default = "c5d.2xlarge"
}

variable email {}
variable security_groups {
    type = "list"
}

variable purpose {}
variable ticket {}
variable client {}
variable key_name {}
variable cassandra_count {
    default = "3"
}
variable cassandra_instance_name {
    default = "cassandra-node"
}

variable stress_count {
    default = "0"
}
variable stress_instance_name {
    default = "stress-instance"
}

variable profile {}
variable region {
    default = "us-west-2"
}

variable zones {
  type = "list"
    default = ["us-west-2a", "us-west-2b", "us-west-2c"]
}

resource "aws_instance" "cassandra" {
    ami = "ami-51537029"
    instance_type = "m5d.xlarge"
    tags {
        ticket = "${var.ticket}"
        client = "${var.client}"
        purpose = "${var.purpose}"
        email = "${var.email}"
        Name = "${var.email} ${var.ticket} cassandra-node"
    }
    security_groups = "${var.security_groups}"
    key_name = "${var.key_name}"
    count = "${var.cassandra_count}"
    availability_zone = "${element(var.zones, count.index)}"
}

resource "aws_instance" "stress" {
    ami = "ami-51537029"
    instance_type = "c5d.2xlarge"
    tags {
        ticket = "${var.ticket}"
        client = "${var.client}"
        purpose = "${var.purpose}"
        email = "${var.email}"
        Name = "${var.email} {$var.ticket} stress"
    }
    security_groups = "${var.security_groups}"
    key_name = "${var.key_name}"
    count = "${var.stress_count}"

}


output "cassandra_ips" {
    value = ["${aws_instance.cassandra.*.public_ip}"]
}

output "stress_ips" {
    value = ["${aws_instance.stress.*.public_ip}"]
}

output "cassandra_internal_ips" {
    value = ["${aws_instance.cassandra.*.private_ip}"]
}