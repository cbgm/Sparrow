package com.cbgm.sparrow.feature.membership.domain.policy

/**
 * Two admins can change a group at the same moment and both sign their own version of the next epoch. Every device
 * keeps the same one: the version signed with the lower signing key (compared as unsigned bytes). The other change
 * is dropped, and its admin is told so; nobody applies it again.
 */
object ConcurrentGroupEpochPolicy {
    /** Whether a version of the epoch signed with [candidateSigningPublicKey] replaces the installed one. */
    fun replacesInstalled(candidateSigningPublicKey: ByteArray, installedSigningPublicKey: ByteArray): Boolean =
        compareUnsigned(candidateSigningPublicKey, installedSigningPublicKey) < 0

    private fun compareUnsigned(first: ByteArray, second: ByteArray): Int {
        for (index in 0 until minOf(first.size, second.size)) {
            val difference = (first[index].toInt() and BYTE_MASK) - (second[index].toInt() and BYTE_MASK)
            if (difference != 0) return difference
        }
        return first.size - second.size
    }

    private const val BYTE_MASK = 0xFF
}
