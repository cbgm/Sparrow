package com.cbgm.sparrow.core.extensions

/**
 * Escapes text for the current Sparrow share payload format.
 *
 * Characters outside a small safe set are encoded using
 * percent-wrapped hexadecimal Unicode values.
 *
 * Examples:
 *
 * space -> %20%
 * +     -> %2b%
 * |     -> %7c%
 */
fun String.escapeShareValue(): String =
    buildString {
        this@escapeShareValue.forEach { character ->

            if (
                character.isLetterOrDigit() ||
                character == '-' ||
                character == '_' ||
                character == '.'
            ) {
                append(character)
            } else {
                append('%')

                append(
                    character.code.toString(
                        radix = 16
                    )
                )

                append('%')
            }
        }
    }

/**
 * Reverses [escapeShareValue].
 */
fun String.unescapeShareValue(): String =
    buildString {
        var index = 0

        while (
            index < this@unescapeShareValue.length
        ) {
            val character =
                this@unescapeShareValue[index]

            if (character != '%') {
                append(character)
                index++
                continue
            }

            val closingIndex =
                this@unescapeShareValue.indexOf(
                    char = '%',
                    startIndex = index + 1
                )

            require(
                closingIndex > index + 1
            ) {
                "Malformed escaped share value"
            }

            val hexadecimalCode =
                this@unescapeShareValue.substring(
                    startIndex = index + 1,
                    endIndex = closingIndex
                )

            append(
                hexadecimalCode
                    .toInt(radix = 16)
                    .toChar()
            )

            index =
                closingIndex + 1
        }
    }
