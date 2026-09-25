package com.cbgm.sparrow.feature.avatar.data.datasource

import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.avatar.domain.model.Avatar
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AvatarDataSource(
    private val localProfilePictureProvider: LocalProfilePictureProvider,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider,
    private val groupAvatarProvider: GroupAvatarProvider
) {
    fun observe(target: AvatarTarget): Flow<Avatar> =
        when (target) {
            AvatarTarget.LocalUser ->
                localProfilePictureProvider.observe().map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }

            is AvatarTarget.User ->
                remoteProfilePictureProvider.observe(target.id).map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }

            is AvatarTarget.Group ->
                groupAvatarProvider.observeSnapshot(target.id).map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }
        }
}
