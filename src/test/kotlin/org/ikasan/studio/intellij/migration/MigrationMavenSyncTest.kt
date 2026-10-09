package org.ikasan.studio.intellij.migration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.buildtool.MavenSyncSpec
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.idea.maven.model.MavenProjectProblem
import org.jetbrains.idea.maven.project.MavenSyncListener
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

class MigrationMavenSyncTest : LightPlatformTestCase() {
    private lateinit var scope: CoroutineScope
    private lateinit var manager: MavenProjectsManager
    private lateinit var continuation: Continuation<Unit>
    private lateinit var owner: Project
    private var compiled = false
    private var failed = false

    override fun setUp() {
        super.setUp()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        owner = mock(Project::class.java)
        `when`(owner.messageBus).thenReturn(project.messageBus)
        manager = mock(MavenProjectsManager::class.java)
        compiled = false
        failed = false
        runBlocking {
            doAnswer {
                val spec = it.getArgument<MavenSyncSpec>(0)
                assertTrue(spec.forceReading())
                assertTrue(spec.isExplicit)
                @Suppress("UNCHECKED_CAST")
                continuation = it.rawArguments[1] as Continuation<Unit>
                publisher().syncStarted(owner)
                COROUTINE_SUSPENDED
            }.`when`(manager).updateAllMavenProjects(any<MavenSyncSpec>() ?: MavenSyncSpec.full("test"))
        }
    }

    override fun tearDown() {
        try { if (::scope.isInitialized) scope.cancel() } finally { super.tearDown() }
    }

    private fun publisher() = ApplicationManager.getApplication().messageBus.syncPublisher(MavenSyncListener.TOPIC)

    fun testProjectServiceReceivesItsCoroutineScope() {
        assertNotNull(project.getService(MigrationMavenSync::class.java))
    }

    private fun start() {
        MigrationMavenSync(owner, scope).importBeforeCompile(manager, Runnable { compiled = true }, Runnable { failed = true })
        assertFalse("Compilation must await the suspend function", compiled)
        assertFalse(failed)
    }

    fun testCompilationWaitsForImportAndPostProcessing() {
        start()
        publisher().importFinished(owner, emptyList(), emptyList())
        assertFalse("Workspace import alone does not mean post-processing is complete", compiled)
        continuation.resumeWith(Result.success(Unit))
        assertTrue(compiled)
        assertFalse(failed)
    }

    fun testImportExceptionPreventsCompilation() {
        start()
        continuation.resumeWith(Result.failure(IllegalStateException("Import failed")))
        assertFalse(compiled)
        assertTrue(failed)
    }

    fun testSwallowedImportFailurePreventsCompilation() {
        start()
        continuation.resumeWith(Result.success(Unit))
        assertFalse(compiled)
        assertTrue(failed)
    }

    fun testPomErrorsPreventCompilationAfterImport() {
        val mavenProject = mock(MavenProject::class.java)
        `when`(mavenProject.problems).thenReturn(listOf(MavenProjectProblem.createStructureProblem("pom.xml", "Invalid POM", true)))
        `when`(manager.projects).thenReturn(listOf(mavenProject))
        start()
        publisher().importFinished(owner, listOf(mavenProject), emptyList())
        continuation.resumeWith(Result.success(Unit))
        assertFalse(compiled)
        assertTrue(failed)
    }

    fun testOtherProjectImportCannotEnableCompilation() {
        start()
        publisher().importFinished(project, emptyList(), emptyList())
        continuation.resumeWith(Result.success(Unit))
        assertFalse(compiled)
        assertTrue(failed)
    }

    fun testClosingProjectPreventsCompletionCallbacks() {
        start()
        `when`(owner.isDisposed).thenReturn(true)
        scope.cancel()
        continuation.resumeWith(Result.success(Unit))
        assertFalse(compiled)
        assertFalse(failed)
    }
}
