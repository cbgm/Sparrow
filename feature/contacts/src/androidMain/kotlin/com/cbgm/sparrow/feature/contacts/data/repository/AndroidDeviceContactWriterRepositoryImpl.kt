package com.cbgm.sparrow.feature.contacts.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactRequest
import com.cbgm.sparrow.feature.contacts.domain.model.device.AddDeviceContactResult
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDeviceContactWriterRepositoryImpl(
    context: Context
) : DeviceContactWriterRepository {
    private val applicationContext = context.applicationContext

    private val contentResolver: ContentResolver = applicationContext.contentResolver

    override suspend fun addIfNotExists(request: AddDeviceContactRequest): AddDeviceContactResult {
        val phoneNumber = request.phoneNumber.trim()

        if (!isValidPhoneNumber(phoneNumber)) {
            return AddDeviceContactResult.InvalidPhoneNumber
        }

        if (!hasContactsPermissions()) {
            return AddDeviceContactResult.PermissionDenied
        }

        return withContext(Dispatchers.IO) {
            runCatching {
                if (phoneNumberExists(phoneNumber)) {
                    AddDeviceContactResult.AlreadyExists
                } else {
                    insertContact(
                        request =
                            request.copy(
                                displayName =
                                    request.displayName
                                        ?.trim()
                                        ?.takeIf { it.isNotEmpty() },
                                phoneNumber = phoneNumber,
                                email =
                                    request.email
                                        ?.trim()
                                        ?.takeIf { it.isNotEmpty() },
                                company =
                                    request.company
                                        ?.trim()
                                        ?.takeIf { it.isNotEmpty() }
                            )
                    )

                    AddDeviceContactResult.Added
                }
            }.getOrElse { throwable ->
                AddDeviceContactResult.Failure(
                    throwable = throwable
                )
            }
        }
    }

    private fun phoneNumberExists(phoneNumber: String): Boolean {
        val lookupUri =
            Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )

        contentResolver
            .query(
                lookupUri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID
                ),
                null,
                null,
                null
            )?.use { cursor ->
                return cursor.moveToFirst()
            }

        return false
    }

    private fun insertContact(request: AddDeviceContactRequest) {
        val operations = ArrayList<ContentProviderOperation>()

        val rawContactInsertIndex = operations.size

        operations +=
            ContentProviderOperation
                .newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build()

        request.displayName?.let { displayName ->
            operations +=
                ContentProviderOperation
                    .newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    ).withValue(
                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        displayName
                    ).build()
        }

        operations +=
            ContentProviderOperation
                .newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                ).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, request.phoneNumber)
                .withValue(
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                ).build()

        contentResolver.applyBatch(
            ContactsContract.AUTHORITY,
            operations
        )
    }

    private fun hasContactsPermissions(): Boolean {
        val readGranted =
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        val writeGranted =
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.WRITE_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        return readGranted && writeGranted
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean = phoneNumber.count(Char::isDigit) >= 7
}
