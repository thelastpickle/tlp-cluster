package com.thelastpickle.tlpcluster

class Container(val image: String) {

    companion object {
        fun from(image: String) : Container {
            return Container(image)
        }
    }

    fun exec(args: String) : Container {
        return this
    }
}