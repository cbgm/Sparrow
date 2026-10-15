package com.cbgm.sparrow.buildlogic.workflow.parse

/** Everything the workflow catalog shows about one workflow state machine. */
internal data class WorkflowDescription(
    val className: String,
    val isPublic: Boolean,
    val command: String,
    val dependencies: List<String>,
    val lockKey: String?,
    val kdoc: String,
    val firstSteps: List<String>,
    val steps: List<String>,
    val stepOutcomes: Map<String, Set<String>>,
    val transitions: Map<Pair<String, String>, Set<String>>,
    val sourcePath: String,
    val folder: String,
)
