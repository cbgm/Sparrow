package com.cbgm.sparrow.feature.contacts.di

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.feature.contacts.device.AndroidDeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.device.AndroidDeviceContactsRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactWriterRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidContactsModule =
    module {
        single<ContentResolver> {
            androidContext().contentResolver
        }

        single<DeviceContactsRepository> {
            AndroidDeviceContactsRepository(contentResolver = get())
        }

        single<DeviceContactWriterRepository> {
            AndroidDeviceContactWriterRepository(context = androidContext())
        }

        single<DeviceContactsPermissionRepository> {
            val context = androidContext()
            DeviceContactsPermissionRepository {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
