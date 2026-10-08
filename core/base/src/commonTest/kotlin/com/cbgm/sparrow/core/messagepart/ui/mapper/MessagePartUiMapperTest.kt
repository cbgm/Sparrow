package com.cbgm.sparrow.core.messagepart.ui.mapper

import com.cbgm.sparrow.core.messagepart.domain.model.Image
import com.cbgm.sparrow.core.messagepart.domain.model.MessagePartSource
import com.cbgm.sparrow.core.messagepart.domain.model.Poll
import com.cbgm.sparrow.core.messagepart.domain.model.PollOption
import com.cbgm.sparrow.core.messagepart.domain.model.Voice
import com.cbgm.sparrow.core.messagepart.ui.model.MessagePartSourceUi
import com.cbgm.sparrow.core.messagepart.ui.model.PollUi
import com.cbgm.sparrow.core.messagepart.ui.model.VoiceUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessagePartUiMapperTest {
    @Test
    fun pollMapsSourceImagesAndOptionsToUi() {
        val poll =
            Poll(
                id = "poll-1",
                question = "Question?",
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
        val source = MessagePartSource.GroupPin(groupId = "group-1")

        val ui = assertIs<PollUi>(poll.toMessagePartUi(source))

        assertEquals(MessagePartSourceUi.GroupPin(groupId = "group-1"), ui.source)
        assertEquals("Option", ui.options.single().text)
        assertEquals("image-1", ui.images.single().id)
        assertEquals(MessagePartSourceUi.GroupPin(groupId = "group-1"), ui.images.single().source)
    }

    @Test
    fun voiceMapsSourceToUi() {
        val voice =
            Voice(
                id = "voice-1",
                mimeType = "audio/wav",
                byteSize = 512L,
                durationMilliseconds = 2_000L
            )

        val ui = assertIs<VoiceUi>(voice.toMessagePartUi())

        assertEquals(2_000L, ui.durationMilliseconds)
        assertEquals(MessagePartSourceUi.Message, ui.source)
    }
}
