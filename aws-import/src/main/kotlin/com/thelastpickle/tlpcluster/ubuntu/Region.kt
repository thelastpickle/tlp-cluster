package com.thelastpickle.tlpcluster.ubuntu

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

// maps instance type to ami
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class Region(val azs: List<String>,
                  var ebs_ami : String = "",
                  var instance_ami : String = "") {
    /**
     * returns the family class code for a given instance type
     * c5d.2xlarge -> c5d
     */
    fun familyClass(instance_type: String) = instance_type.split(".").first()
    fun isInstanceRoot(instance_type: String) = familyClass(instance_type) in setOf("i3", "m5d", "m5ad", "m5dn",  "r5d", "r5ad", "r5dn", "x1e", "x1", "z1d", "p3dn", "f1", "i3en")

    fun getAmi(instance_type: String) = if (isInstanceRoot(instance_type)) instance_ami else ebs_ami


}