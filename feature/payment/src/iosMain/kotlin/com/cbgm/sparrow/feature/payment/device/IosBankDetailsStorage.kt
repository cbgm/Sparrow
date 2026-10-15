@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.cbgm.sparrow.feature.payment.device

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecAttrSynchronizable
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/** Device-only, non-synchronising Keychain items accessible while the device is unlocked. */
internal class IosBankDetailsStorage : BankDetailsStorage {
    override suspend fun read(key: String): String? = withQuery(key) { query ->
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        memScoped {
            val result = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, result.ptr)) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data = requireNotNull(result.value) as CFDataRef
                    try {
                        val bytes = requireNotNull(CFDataGetBytePtr(data)).readBytes(CFDataGetLength(data).toInt())
                        try { bytes.decodeToString() } finally { bytes.fill(0) }
                    } finally { CFRelease(data) }
                }
                else -> error("Secure bank-details read failed ($status)")
            }
        }
    }

    override suspend fun write(key: String, value: String) {
        val bytes = value.encodeToByteArray()
        try {
            bytes.usePinned { pinned ->
                val data = requireNotNull(CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong()))
                try {
                    withQuery(key) { query ->
                        val attributes = dictionary()
                        try {
                            CFDictionarySetValue(attributes, kSecValueData, data)
                            CFDictionarySetValue(attributes, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                            val status = SecItemUpdate(query, attributes)
                            if (status == errSecItemNotFound) {
                                CFDictionarySetValue(query, kSecValueData, data)
                                CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
                                check(SecItemAdd(query, null) == errSecSuccess) { "Secure bank-details save failed" }
                            } else check(status == errSecSuccess) { "Secure bank-details update failed" }
                        } finally { CFRelease(attributes) }
                    }
                } finally { CFRelease(data) }
            }
        } finally { bytes.fill(0) }
    }

    override suspend fun delete(key: String) {
        withQuery(key) { query ->
            val status = SecItemDelete(query)
            check(status == errSecSuccess || status == errSecItemNotFound) { "Secure bank-details delete failed" }
        }
    }

    // Queries borrow constants and strings whose lifetimes enclose the synchronous Security call.
    private fun dictionary(): CFMutableDictionaryRef = requireNotNull(CFDictionaryCreateMutable(
        null, 0, null, null
    ))

    private fun <T> withQuery(key: String, block: (CFMutableDictionaryRef) -> T): T {
        val query = dictionary()
        val service = requireNotNull(CFStringCreateWithCString(null, "com.cbgm.sparrow.payment.bank-details.v1", kCFStringEncodingUTF8))
        val account = requireNotNull(CFStringCreateWithCString(null, key, kCFStringEncodingUTF8))
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            CFDictionarySetValue(query, kSecAttrSynchronizable, kCFBooleanFalse)
            return block(query)
        } finally { CFRelease(query); CFRelease(service); CFRelease(account) }
    }
}
