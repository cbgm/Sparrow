package com.cbgm.sparrow.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp)
    )

@Immutable
class MessageBubbleShapes internal constructor(
    val cornerRadius: Dp,
    val tailWidth: Dp,
    val tailHeight: Dp,
    val tailReturnOffset: Dp
)

@Immutable
class MessageInputShapes internal constructor(
    val field: Shape,
    val buttonNotchRadius: Dp,
    val buttonRightCornerRadius: Dp
)

@Immutable
class ContactsScreenShapes internal constructor(
    val dragHandle: Shape
)

@Immutable
class ContactDetailsScreenShapes internal constructor(
    val phoneNumber: Shape
)

@Immutable
class ScanIdentityScreenShapes internal constructor(
    val frameCornerRadius: Dp
)

private val badgeShape = RoundedCornerShape(6.dp)

private val modalShape =
    RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

private val messageBubbleShapeValues =
    MessageBubbleShapes(
        cornerRadius = 18.dp,
        tailWidth = 10.dp,
        tailHeight = 14.dp,
        tailReturnOffset = 1.dp
    )

private val messageInputShapeValues =
    MessageInputShapes(
        field = RoundedCornerShape(20.dp),
        buttonNotchRadius = 14.dp,
        buttonRightCornerRadius = 28.dp
    )

private val contactsScreenShapeValues =
    ContactsScreenShapes(
        dragHandle = RoundedCornerShape(2.dp)
    )

private val contactDetailsScreenShapeValues =
    ContactDetailsScreenShapes(
        phoneNumber = RoundedCornerShape(6.dp)
    )

private val scanIdentityScreenShapeValues =
    ScanIdentityScreenShapes(
        frameCornerRadius = 24.dp
    )

val Shapes.circle: Shape
    get() = CircleShape

val Shapes.rectangle: Shape
    get() = RectangleShape

val Shapes.badge: Shape
    get() = badgeShape

val Shapes.modal: Shape
    get() = modalShape

val Shapes.messageBubble: MessageBubbleShapes
    get() = messageBubbleShapeValues

val Shapes.messageInput: MessageInputShapes
    get() = messageInputShapeValues

val Shapes.contactsScreen: ContactsScreenShapes
    get() = contactsScreenShapeValues

val Shapes.contactDetailsScreen: ContactDetailsScreenShapes
    get() = contactDetailsScreenShapeValues

val Shapes.scanIdentityScreen: ScanIdentityScreenShapes
    get() = scanIdentityScreenShapeValues
