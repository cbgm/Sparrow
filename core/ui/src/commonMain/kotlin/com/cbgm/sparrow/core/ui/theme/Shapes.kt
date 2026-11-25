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
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp)
    )

@Immutable
class MessageBubbleShapes internal constructor(
    val mine: Shape,
    val theirs: Shape
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

private val dialogShape = RoundedCornerShape(22.dp)

private val modalShape =
    RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )

private val messageBubbleShapeValues =
    MessageBubbleShapes(
        mine = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 4.dp, bottomStart = 16.dp),
        theirs = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
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

val Shapes.dialog: Shape
    get() = dialogShape

val Shapes.messageBubble: MessageBubbleShapes
    get() = messageBubbleShapeValues

val Shapes.contactsScreen: ContactsScreenShapes
    get() = contactsScreenShapeValues

val Shapes.contactDetailsScreen: ContactDetailsScreenShapes
    get() = contactDetailsScreenShapeValues

val Shapes.scanIdentityScreen: ScanIdentityScreenShapes
    get() = scanIdentityScreenShapeValues
