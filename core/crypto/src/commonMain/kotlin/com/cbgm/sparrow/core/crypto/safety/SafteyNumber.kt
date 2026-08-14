package com.cbgm.sparrow.core.crypto.safety

data class SafetyNumber(
    val groups: List<String>
) {
    init {
        require(groups.size == EXPECTED_GROUP_COUNT) {
            "Safety number must contain $EXPECTED_GROUP_COUNT groups"
        }

        require(
            groups.all { group ->
                group.length == DIGITS_PER_GROUP &&
                    group.all { character ->
                        character.isDigit()
                    }
            }
        ) {
            "Every safety-number group must contain exactly five digits"
        }
    }

    val singleLine: String
        get() = groups.joinToString(separator = " ")

    val formatted: String
        get() =
            groups
                .chunked(size = GROUPS_PER_LINE)
                .joinToString(separator = "\n") { line ->
                    line.joinToString(separator = " ")
                }

    companion object {
        const val EXPECTED_GROUP_COUNT = 16
        const val DIGITS_PER_GROUP = 5
        const val GROUPS_PER_LINE = 4
    }
}
