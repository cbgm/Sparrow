package com.cbgm.sparrow.feature.identity.device

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberIdentityShareLauncher(
    encodedIdentity: String,
    shareTitle: String
): () -> Unit {
    val context = LocalContext.current

    val currentEncodedIdentity = rememberUpdatedState(newValue = encodedIdentity)

    val currentShareTitle = rememberUpdatedState(newValue = shareTitle)

    return remember(context) {
        {
            val payload = currentEncodedIdentity.value.trim()

            if (payload.isNotEmpty()) {
                val sendIntent =
                    Intent(Intent.ACTION_SEND)
                        .apply {
                            type = "text/plain"

                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "Sparrow identity"
                            )

                            putExtra(
                                Intent.EXTRA_TEXT,
                                payload
                            )
                        }

                val chooserIntent =
                    Intent
                        .createChooser(
                            sendIntent,
                            currentShareTitle.value
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }

                context.startActivity(chooserIntent)
            }
        }
    }
}
