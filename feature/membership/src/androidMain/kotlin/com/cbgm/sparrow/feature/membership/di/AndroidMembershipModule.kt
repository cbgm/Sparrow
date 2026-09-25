package com.cbgm.sparrow.feature.membership.di

import com.cbgm.sparrow.core.crypto.group.GroupKeyStore
import com.cbgm.sparrow.feature.membership.device.AndroidGroupKeyStore
import org.koin.dsl.module

/** Membership owns the persisted group keys and their Android KeyStore backing. */
val androidMembershipModule = module {
    single<GroupKeyStore> { AndroidGroupKeyStore(dataStore = get()) }
}
