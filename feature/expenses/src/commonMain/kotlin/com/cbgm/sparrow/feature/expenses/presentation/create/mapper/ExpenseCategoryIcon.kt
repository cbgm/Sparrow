package com.cbgm.sparrow.feature.expenses.presentation.create

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.cbgm.sparrow.feature.expenses.presentation.create.model.ExpenseCategoryUi
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_expenses_category_accommodation
import com.cbgm.sparrow.resources.feature_expenses_category_activity
import com.cbgm.sparrow.resources.feature_expenses_category_flight
import com.cbgm.sparrow.resources.feature_expenses_category_food
import com.cbgm.sparrow.resources.feature_expenses_category_other
import com.cbgm.sparrow.resources.feature_expenses_category_parking
import com.cbgm.sparrow.resources.feature_expenses_category_shopping
import com.cbgm.sparrow.resources.feature_expenses_category_transport
import org.jetbrains.compose.resources.stringResource

internal val ExpenseCategoryUi.icon: ImageVector
    get() = when (this) {
        ExpenseCategoryUi.FLIGHT -> Icons.Default.Flight
        ExpenseCategoryUi.FOOD -> Icons.Default.Restaurant
        ExpenseCategoryUi.ACTIVITY -> Icons.Default.LocalActivity
        ExpenseCategoryUi.PARKING -> Icons.Default.LocalParking
        ExpenseCategoryUi.ACCOMMODATION -> Icons.Default.Hotel
        ExpenseCategoryUi.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategoryUi.SHOPPING -> Icons.Default.ShoppingBag
        ExpenseCategoryUi.OTHER -> Icons.AutoMirrored.Filled.ReceiptLong
    }

@Composable
internal fun ExpenseCategoryUi.label(): String = stringResource(
    when (this) {
        ExpenseCategoryUi.FLIGHT -> Res.string.feature_expenses_category_flight
        ExpenseCategoryUi.FOOD -> Res.string.feature_expenses_category_food
        ExpenseCategoryUi.ACTIVITY -> Res.string.feature_expenses_category_activity
        ExpenseCategoryUi.PARKING -> Res.string.feature_expenses_category_parking
        ExpenseCategoryUi.ACCOMMODATION -> Res.string.feature_expenses_category_accommodation
        ExpenseCategoryUi.TRANSPORT -> Res.string.feature_expenses_category_transport
        ExpenseCategoryUi.SHOPPING -> Res.string.feature_expenses_category_shopping
        ExpenseCategoryUi.OTHER -> Res.string.feature_expenses_category_other
    }
)
