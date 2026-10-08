package com.cbgm.sparrow.feature.avatar.data.datasource

import com.cbgm.sparrow.feature.avatar.domain.model.Avatar
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.protocol.avatar.GroupAvatarProvider
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureProvider
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
