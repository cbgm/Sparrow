package com.cbgm.sparrow.feature.contacts.di

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cbgm.sparrow.feature.contacts.data.datasource.AndroidDeviceContactWriterDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.AndroidDeviceContactsDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.DeviceContactWriterDataSource
import com.cbgm.sparrow.feature.contacts.data.datasource.DeviceContactsDataSource
import com.cbgm.sparrow.feature.contacts.domain.repository.DeviceContactsPermissionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidContactsModule =
    module {
        single<ContentResolver> {
            androidContext().contentResolver
        }

        single<DeviceContactsDataSource> {
            AndroidDeviceContactsDataSource(contentResolver = get())
        }

        single<DeviceContactWriterDataSource> {
            AndroidDeviceContactWriterDataSource(context = androidContext())
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
