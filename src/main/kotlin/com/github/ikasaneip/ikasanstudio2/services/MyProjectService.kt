package com.github.ikasaneip.ikasanstudio.services

import com.github.ikasaneip.ikasanstudio.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
