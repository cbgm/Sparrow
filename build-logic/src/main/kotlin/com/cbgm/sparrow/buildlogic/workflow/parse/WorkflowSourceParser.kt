package com.cbgm.sparrow.buildlogic.workflow.parse

import com.cbgm.sparrow.buildlogic.workflow.parse.KotlinSourceScanner.functionBody
import com.cbgm.sparrow.buildlogic.workflow.parse.KotlinSourceScanner.matching
import com.cbgm.sparrow.buildlogic.workflow.parse.KotlinSourceScanner.whenBranches

/**
 * Reads one `*Workflow.kt` file. The state diagram is derived from `firstStep`, the `when (step)` in
 * `perform` (which outcomes a step can report) and the `when (outcome)` in `transition` (which step follows),
 * including private helper functions those branches call.
 */
internal object WorkflowSourceParser {

    fun parse(source: String, sourcePath: String): WorkflowDescription {
        val text = source.replace("\r\n", "\n")
        val className = Regex("""class (\w+Workflow)\b""").find(text)!!.groupValues[1]
        val supertype = Regex("""\) : (ConversationWorkflow|WorkflowStateMachine)<\s*([\w.]+)""").find(text)
            ?: error("$sourcePath does not implement ConversationWorkflow or WorkflowStateMachine")
        val helpers = helperBodies(text)
        val steps = declaredSteps(text)
        val stepOutcomes = stepOutcomes(text, steps, helpers)
        val packageName = Regex("""^package ([\w.]+)""", RegexOption.MULTILINE).find(text)!!.groupValues[1]
        return WorkflowDescription(
            className = className,
            isPublic = supertype.groupValues[1] == "ConversationWorkflow",
            command = supertype.groupValues[2],
            dependencies = constructorDependencies(text, className),
            lockKey = functionBody(text, Regex("""override fun lockKey\("""))
                .takeIf(String::isNotEmpty)
                ?.trimStart('=')
                ?.split(Regex("""\s+"""))
                ?.filter(String::isNotEmpty)
                ?.joinToString(" "),
            kdoc = kdocBefore(text, className),
            firstSteps = firstSteps(text),
            steps = steps,
            stepOutcomes = stepOutcomes,
            transitions = transitions(text, steps, stepOutcomes, helpers),
            sourcePath = sourcePath,
            folder = packageName.substringAfter(".domain.workflow.", ""),
        )
    }

    private fun constructorDependencies(text: String, className: String): List<String> {
        val declaration = text.substring(text.indexOf("class $className"))
        val open = declaration.indexOf('(')
        val constructor = declaration.substring(open, matching(declaration, open, '(', ')') + 1)
        return Regex("""private val \w+: ([\w.<>?, ]+?)(?:,|\n|\))""")
            .findAll(constructor)
            .map { it.groupValues[1].trim() }
            .toList()
    }

    private fun firstSteps(text: String): List<String> {
        val body = functionBody(text, Regex("""override fun firstStep\("""))
        return STEP_REFERENCE.findAll(body).map { it.groupValues[1] }.distinct().toList()
    }

    private fun declaredSteps(text: String): List<String> {
        val start = text.indexOf("sealed interface Step")
        val end = text.indexOf("sealed interface Outcome")
        if (start < 0) return emptyList()
        val section = if (end > start) text.substring(start, end) else text.substring(start)
        return Regex("""^ {8}(?:data )?(?:class|object) (\w+)""", RegexOption.MULTILINE)
            .findAll(section)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun stepOutcomes(
        text: String,
        steps: List<String>,
        helpers: Map<String, String>,
    ): Map<String, Set<String>> {
        val perform = functionBody(text, Regex("""override suspend fun perform\("""))
        val allOutcomes = outcomesIn(perform, helpers, mutableSetOf())
        val result = steps.associateWith { mutableSetOf<String>() }.toMutableMap()
        val branches = whenBranches(perform, "step")
        if (branches == null) {
            steps.forEach { step -> result.getValue(step) += allOutcomes }
            return result
        }
        val branchOutcomes = mutableSetOf<String>()
        branches.forEach { branch ->
            val found = outcomesIn(branch.code, helpers, mutableSetOf())
            branchOutcomes += found
            branch.labels.forEach { label -> result.getOrPut(label) { mutableSetOf() } += found }
        }
        // A step whose branch reports nothing returns an outcome shared after the `when`.
        val shared = allOutcomes - branchOutcomes
        steps.filter { step -> result.getValue(step).isEmpty() }.forEach { step -> result.getValue(step) += shared }
        return result
    }

    private fun transitions(
        text: String,
        steps: List<String>,
        stepOutcomes: Map<String, Set<String>>,
        helpers: Map<String, String>,
    ): Map<Pair<String, String>, Set<String>> {
        val transition = functionBody(text, Regex("""override fun transition\("""))
        val branches = whenBranches(transition, "outcome")
        val result = linkedMapOf<Pair<String, String>, Set<String>>()
        steps.forEach { step ->
            stepOutcomes[step].orEmpty().forEach { outcome ->
                val code = if (branches != null) {
                    branches.firstOrNull { outcome in it.labels }?.code
                        ?: branches.firstOrNull { "else" in it.labels }?.code
                        ?: ""
                } else {
                    transition
                }
                result[step to outcome] = targetsAfter(step, code, helpers)
            }
        }
        return result
    }

    /** Targets of a transition branch; a helper with its own `when (step)` decides per step. */
    private fun targetsAfter(step: String, code: String, helpers: Map<String, String>): Set<String> {
        val full = expand(code, helpers, emptySet())
        val perStep = whenBranches(full, "step")
        if (perStep != null) {
            val branch = perStep.firstOrNull { step in it.labels } ?: perStep.firstOrNull { "else" in it.labels }
            if (branch != null) return targetsIn(expand(branch.code, helpers, emptySet()))
        }
        return targetsIn(full)
    }

    private fun targetsIn(code: String): Set<String> {
        val targets = Regex("""(?<!is )Step\.(\w+)\b(?!\s*->)""").findAll(code).map { it.groupValues[1] }.toMutableSet()
        if ("Transition.Complete" in code) targets += COMPLETE
        if ("Transition.Reject" in code) targets += REJECTED
        return targets
    }

    private fun outcomesIn(code: String, helpers: Map<String, String>, seen: MutableSet<String>): Set<String> {
        val found = Regex("""Outcome(?:\.|::)(\w+)""").findAll(code).map { it.groupValues[1] }.toMutableSet()
        helpers.forEach { (name, body) ->
            if (name !in seen && Regex("""\b$name\(""").containsMatchIn(code)) {
                seen += name
                found += outcomesIn(body, helpers, seen)
            }
        }
        return found
    }

    private fun expand(code: String, helpers: Map<String, String>, seen: Set<String>): String {
        val visited = seen.toMutableSet()
        val out = StringBuilder(code)
        helpers.forEach { (name, body) ->
            if (name !in visited && Regex("""\b$name\(""").containsMatchIn(code)) {
                visited += name
                out.append('\n').append(expand(body, helpers, visited))
            }
        }
        return out.toString()
    }

    private fun helperBodies(text: String): Map<String, String> {
        val bodies = linkedMapOf<String, String>()
        Regex("""(?:private |internal )?(?:suspend )?fun (\w+)\(""").findAll(text).forEach { match ->
            val name = match.groupValues[1]
            runCatching {
                functionBody(text, Regex("""(?:private |internal )?(?:suspend )?fun $name\("""))
            }.onSuccess { body -> bodies[name] = body }
        }
        return bodies
    }

    private fun kdocBefore(text: String, className: String): String {
        val match = Regex(
            """(/\*\*(?:(?!\*/).)*\*/)\s*\n(?:@\w+(?:\([^)]*\))?\s*\n)*(?:internal )?class $className\b""",
            RegexOption.DOT_MATCHES_ALL,
        ).find(text) ?: return ""
        return match.groupValues[1]
            .split('\n')
            .joinToString("\n") { line -> line.trim().replace(KDOC_DECORATION, "").trimEnd() }
            .trim()
    }

    const val COMPLETE = "[*]"
    const val REJECTED = "Rejected"
    private val STEP_REFERENCE = Regex("""Step\.(\w+)""")
    private val KDOC_DECORATION = Regex("""^/\*\*\s?|\*/$|^\*\s?""")
}
