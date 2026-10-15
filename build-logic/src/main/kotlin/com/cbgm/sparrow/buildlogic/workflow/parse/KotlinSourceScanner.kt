package com.cbgm.sparrow.buildlogic.workflow.parse

/**
 * Small, string-based helpers to read workflow sources without a Kotlin compiler.
 * They only need to understand the regular shape of a workflow state machine.
 */
internal object KotlinSourceScanner {

    /** Index of the bracket that closes the one at [start]; strings and line comments are skipped. */
    fun matching(text: String, start: Int, open: Char, close: Char): Int {
        var depth = 0
        var i = start
        var inString = false
        while (i < text.length) {
            val c = text[i]
            if (inString) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == '"') inString = false
            } else if (c == '"') {
                inString = true
            } else if (text.startsWith("//", i)) {
                i = text.indexOf('\n', i).takeIf { it >= 0 } ?: text.length
                continue
            } else if (c == open) {
                depth++
            } else if (c == close) {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        throw IllegalArgumentException("Unbalanced '$open' at $start")
    }

    /** Block or expression body of the first function whose signature matches [signature]. */
    fun functionBody(text: String, signature: Regex): String {
        val match = signature.find(text) ?: return ""
        val parametersOpen = text.indexOf('(', match.range.first)
        var i = matching(text, parametersOpen, '(', ')') + 1
        var depth = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '<' || c == '(' -> depth++
                c == '>' || c == ')' -> depth--
                depth == 0 && c == '{' -> return text.substring(i, matching(text, i, '{', '}') + 1)
                depth == 0 && c == '=' -> break
            }
            i++
        }
        val rest = text.substring(i)
        val end = EXPRESSION_BODY_ENDS
            .mapNotNull { regex -> regex.find(rest)?.range?.first }
            .minOrNull()
        return if (end == null) rest else rest.substring(0, end)
    }

    /**
     * Splits `when (subject) { ... }` into its top-level branches. Each branch has the `Step`/`Outcome`
     * names of its condition (or `else`) and the code after `->`. Returns null without such a `when`.
     */
    fun whenBranches(body: String, subject: String): List<WhenBranch>? {
        val match = Regex("""when \(\s*$subject\s*\)\s*\{""").find(body) ?: return null
        val openIndex = body.indexOf('{', match.range.first)
        val inner = body.substring(openIndex + 1, matching(body, openIndex, '{', '}'))
        val starts = mutableListOf<Int>()
        var depth = 0
        var i = 0
        var inString = false
        while (i < inner.length) {
            val c = inner[i]
            if (inString) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == '"') inString = false
            } else if (c == '"') {
                inString = true
            } else if (c in "({[") {
                depth++
            } else if (c in ")}]") {
                depth--
            } else if (depth == 0 && (i == 0 || inner[i - 1] == '\n')) {
                val lineEnd = inner.indexOf('\n', i).takeIf { it >= 0 } ?: inner.length
                val line = inner.substring(i, lineEnd)
                val continuesLabels = inner.substring(0, i).trimEnd().endsWith(',') && starts.isNotEmpty()
                if (!continuesLabels && BRANCH_START.find(line)?.range?.first == 0) {
                    starts += i
                }
            }
            i++
        }
        starts += inner.length
        return starts.zipWithNext { from, to ->
            val chunk = inner.substring(from, to)
            val arrow = chunk.indexOf("->")
            val head = if (arrow >= 0) chunk.substring(0, arrow) else chunk
            val code = if (arrow >= 0) chunk.substring(arrow + 2) else ""
            val labels = if (ELSE_HEAD.matches(head)) {
                listOf("else")
            } else {
                LABEL.findAll(head).map { it.groupValues[1] }.toList()
            }
            WhenBranch(labels, code)
        }
    }

    private val EXPRESSION_BODY_ENDS = listOf(
        Regex("""\n\s*\n"""),
        Regex("""\n {4}(?:private|override|internal|sealed|companion|data|fun|val)\b"""),
        Regex("""\n}"""),
    )
    private val BRANCH_START =
        Regex("""\s*(is\s+)?(Step|Outcome)\.\w+[^\n]*(->|,\s*$)|\s*else\s*->|\s*(is\s+)?\w+\.\w+.*->""")
    private val ELSE_HEAD = Regex("""\s*else\s*""")
    private val LABEL = Regex("""(?:Step|Outcome)\.(\w+)""")
}

internal data class WhenBranch(
    val labels: List<String>,
    val code: String,
)
