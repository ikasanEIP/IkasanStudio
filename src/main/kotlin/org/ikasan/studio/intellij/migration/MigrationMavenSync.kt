package org.ikasan.studio.intellij.migration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import org.jetbrains.idea.maven.buildtool.MavenSyncSpec
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.MavenSyncListener
import java.util.concurrent.atomic.AtomicBoolean

/** The injected scope is cancelled when the project closes or the plugin unloads. */
@Service(Service.Level.PROJECT)
class MigrationMavenSync(private val project: Project, private val scope: CoroutineScope) {
    fun importBeforeCompile(manager: MavenProjectsManager, onImported: Runnable, onFailure: Runnable) {
        scope.launch {
            val imported = AtomicBoolean(false)
            val connection = ApplicationManager.getApplication().messageBus.connect()
            try {
                connection.subscribe(MavenSyncListener.TOPIC, object : MavenSyncListener {
                    override fun syncStarted(project: Project) {
                        if (project === this@MigrationMavenSync.project) imported.set(false)
                    }

                    override fun importFinished(project: Project, importedProjects: Collection<MavenProject>,
                                                newModules: List<com.intellij.openapi.module.Module>) {
                        if (project === this@MigrationMavenSync.project) imported.set(true)
                    }
                })
                // Await this full sync, rather than scheduling a sync and compiling against stale roots.
                // Older Maven implementations swallow some import exceptions: require a committed import too.
                manager.updateAllMavenProjects(MavenSyncSpec.full("Ikasan Studio migration", true))
                ensureActive()
                if (!project.isDisposed) {
                    if (imported.get() && manager.projects.all { it.problems.isEmpty() }) onImported.run()
                    else onFailure.run()
                }
            } catch (cancelled: CancellationException) {
                if (scope.isActive && !project.isDisposed) onFailure.run()
                throw cancelled
            } catch (failure: Exception) {
                Logger.getInstance(MigrationMavenSync::class.java).warn("Maven sync after Ikasan migration failed", failure)
                if (!project.isDisposed) onFailure.run()
            } finally {
                connection.disconnect()
            }
        }
    }
}
