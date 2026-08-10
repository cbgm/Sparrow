package com.cbgm.securechat.feature.contacts.di

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactWriter
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactsDataSource
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactWriter
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactsDataSource
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactsPermissionChecker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidContactsModule =
    module {
        single<ContentResolver> {
            androidContext().contentResolver
        }

        single<DeviceContactsDataSource> {
            AndroidDeviceContactsDataSource(
                contentResolver = get()
            )
        }

        single<DeviceContactWriter> {
            AndroidDeviceContactWriter(
                context = androidContext()
            )
        }

        single<DeviceContactsPermissionChecker> {
            val context = androidContext()
            DeviceContactsPermissionChecker {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
