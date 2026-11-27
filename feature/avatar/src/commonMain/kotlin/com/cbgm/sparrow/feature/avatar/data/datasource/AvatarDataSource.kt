package com.cbgm.sparrow.feature.avatar.data.datasource

import com.cbgm.sparrow.feature.avatar.domain.model.Avatar
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.protocol.avatar.GroupAvatarSource
import com.cbgm.sparrow.protocol.profile.LocalProfilePictureSource
import com.cbgm.sparrow.protocol.profile.RemoteProfilePictureSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class AvatarDataSource(
    private val localProfilePictureSource: LocalProfilePictureSource,
    private val remoteProfilePictureSource: RemoteProfilePictureSource,
    private val groupAvatarSource: GroupAvatarSource
) {
    fun observe(target: AvatarTarget): Flow<Avatar> =
        when (target) {
            AvatarTarget.LocalUser ->
                localProfilePictureSource.observe().map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }

            is AvatarTarget.User ->
                remoteProfilePictureSource.observe(target.id).map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }

            is AvatarTarget.Group ->
                groupAvatarSource.observeSnapshot(target.id).map { snapshot ->
                    Avatar(
                        target = target,
                        changedAtEpochMilliseconds = snapshot.changedAtEpochMilliseconds,
                        bytes = snapshot.bytes
                    )
                }
        }
}
