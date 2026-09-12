package com.safespend.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Categories store an icon *key*, not a drawable id, so the database stays free of
 * resource references that would break the moment an icon is renamed.
 */
object CategoryIcons {

    private val byKey: Map<String, ImageVector> = mapOf(
        "restaurant" to Icons.Outlined.Restaurant,
        "groceries" to Icons.Outlined.ShoppingCart,
        "transport" to Icons.Outlined.DirectionsBus,
        "bills" to Icons.Outlined.ReceiptLong,
        "rent" to Icons.Outlined.Home,
        "health" to Icons.Outlined.LocalHospital,
        "education" to Icons.Outlined.School,
        "shopping" to Icons.Outlined.ShoppingBag,
        "entertainment" to Icons.Outlined.Movie,
        "family" to Icons.Outlined.Groups,
        "salary" to Icons.Outlined.Payments,
        "freelance" to Icons.Outlined.Work,
        "business" to Icons.Outlined.Storefront,
        "gift" to Icons.Outlined.CardGiftcard,
        "other" to Icons.Outlined.MoreHoriz,
    )

    /** Every key a user can pick from when creating a category. */
    val pickable: List<String> = byKey.keys.toList()

    operator fun get(key: String): ImageVector = byKey[key] ?: Icons.Outlined.MoreHoriz
}

/**
 * Category colours are stored as an index into the theme's chart ramp instead of a
 * hex string. A category therefore cannot introduce an off-palette colour, and the
 * whole app re-tints correctly in dark mode.
 */
fun List<Color>.atIndex(index: Int): Color = this[((index % size) + size) % size]
