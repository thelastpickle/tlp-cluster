package com.thelastpickle.tlpcluster.commands

import com.thelastpickle.tlpcluster.Context

class RebuildDocker(val context: Context) : ICommand {
    override fun execute() {
        // remove the old image
        context.stress.maybeCreateImage()
    }
}