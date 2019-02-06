package com.thelastpickle.tlpcluster

class Container {
    companion object {
        fun from(image: String) : Container {
            return Container()
        }
    }

    fun exec(args: String) : Container {
        return this
    }
}