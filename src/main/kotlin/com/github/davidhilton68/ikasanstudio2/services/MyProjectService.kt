package com.github.davidhilton68.ikasanstudio2.services

import com.intellij.openapi.project.Project
import com.github.davidhilton68.ikasanstudio2.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
