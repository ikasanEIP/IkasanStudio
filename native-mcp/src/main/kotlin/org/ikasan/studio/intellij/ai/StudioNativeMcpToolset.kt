package org.ikasan.studio.intellij.ai

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.project
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Thin optional transport; validation, consent and model ownership stay in StudioAiService. */
@Suppress("FunctionName", "unused")
class StudioNativeMcpToolset : McpToolset {
    override fun isEnabled() = com.intellij.openapi.application.ApplicationInfo.getInstance().build.baselineVersion >= 262
    override fun alwaysIncluded() = false
    override fun isExperimental() = false

    @McpTool
    @McpDescription("Read the live Ikasan Studio module and revision. Use before proposing edits. Known credentials are redacted. Open Studio and enable Connect AI first. Never edit model.json behind Studio.")
    suspend fun studio_snapshot(): String = call("studio_snapshot", "{}")

    @McpTool
    @McpDescription("Read Ikasan Studio component keys, help, properties, payload contracts, recipe configuration examples, implementation/ownership flags and supported proposal operations for the selected version. Generated stubs may still need implementation and tests. Use before proposing components.")
    suspend fun studio_catalogue(): String = call("studio_catalogue", "{}")

    @McpTool
    @McpDescription("Validate linear-flow changes. Validated supported changes apply automatically unless Confirm deletes requires review (enabled by default for deletions and replacements) or Always ask for approval is enabled; potential developer-owned code replacement always requires review and Apply. Check the returned status and studio_proposal_status; ask for Apply only when awaiting_review. Use the revision from studio_snapshot. Operations: addFlow {type,flow}; deleteFlow {type,flow} removes the entire flow and attached test harnesses, retaining developer-owned source files; addComponent {type,flow,key,name,properties?}; setProperty {type,flow,component,property,value}; renameComponent {type,flow,component,name}; deleteComponent {type,flow,component}; replaceComponent {type,flow,component,key,name,properties?} preserves position, replaces consumers only with consumers, preserves developer-owned source files; connect {type,flow,order:[all component names,consumer first]}. New flow and component names must match [A-Za-z][A-Za-z0-9_ ]{0,79}: start with a letter, then letters, digits, spaces or underscores, maximum 80 characters. For numbered flows use Flow01 or Demo01, never a leading number. Existing flow/component references must use their exact saved names. Empty flows and incremental flow construction are supported; incomplete flows show review warnings and must be completed before running. No router editing, flow renaming or version changes. Read studio_proposal_status afterwards.")
    suspend fun studio_propose(
        @McpDescription("Opaque revision from studio_snapshot") revision: String,
        @McpDescription("Ordered array of 1 to 100 operation objects") operations: JsonArray
    ): String = call("studio_propose", buildJsonObject { put("revision", revision); put("operations", operations) }.toString())

    @McpTool
    @McpDescription("Read an Ikasan Studio proposal's review and generation status: awaiting_review, cancelled, generating, applied, generation_failed or undone.")
    suspend fun studio_proposal_status(@McpDescription("ID returned by studio_propose") proposalId: String): String =
        call("studio_proposal_status", buildJsonObject { put("proposalId", proposalId) }.toString())

    private suspend fun call(name: String, arguments: String): String {
        val project = currentCoroutineContext().project
        return withContext(Dispatchers.IO) {
            StudioAiService.nativeCall(project, name, arguments)
        }
    }
}
