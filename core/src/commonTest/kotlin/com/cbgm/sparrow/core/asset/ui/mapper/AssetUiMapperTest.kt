package com.cbgm.sparrow.core.asset.ui.mapper

import com.cbgm.sparrow.core.asset.domain.model.Image
import com.cbgm.sparrow.core.asset.domain.model.Poll
import com.cbgm.sparrow.core.asset.domain.model.PollOption
import com.cbgm.sparrow.core.asset.ui.model.AssetSourceUi
import com.cbgm.sparrow.core.asset.ui.model.PollUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AssetUiMapperTest {
    @Test
    fun pollMapsImagesOptionsAndSourceToUi() {
        val poll =
            Poll(
                id = "poll-1",
                question = "Question",
                options = listOf(PollOption(id = "option-1", text = "Option")),
                images =
                    listOf(
                        Image(
                            id = "image-1",
                            mimeType = "image/jpeg",
                            byteSize = 123L,
                            width = 10,
                            height = 20
                        )
                    )
            )
        val source = AssetSourceUi.GroupPin(groupId = "group-1")

        val ui = assertIs<PollUi>(poll.toAssetUi(source))

        assertEquals(source, ui.source)
        assertEquals("Option", ui.options.single().text)
        assertEquals("image-1", ui.images.single().id)
        assertEquals(source, ui.images.single().source)
    }
}
