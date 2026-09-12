package com.safespend.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safespend.app.data.model.TransactionWithCategory
import com.safespend.app.data.model.TxSource
import com.safespend.app.data.model.TxType
import com.safespend.app.ui.theme.moneyColors
import com.safespend.app.util.Money
import com.safespend.app.util.relativeLabel

/**
 * One line of the ledger. Deliberately identical everywhere it appears — Home's
 * recent list and the full ledger use the same row, so nothing has to be re-learnt
 * between screens.
 */
@Composable
fun TransactionRow(
    item: TransactionWithCategory,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    showDate: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = moneyColors
    val palette = colors.chartSeries
    val isIncome = item.transaction.type == TxType.INCOME
    val amountColor = if (isIncome) colors.income else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            iconKey = item.categoryIcon,
            tint = palette.atIndex(item.categoryColor),
            contentDescription = item.categoryName,
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.transaction.merchant?.takeIf { it.isNotBlank() } ?: item.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (item.transaction.source == TxSource.SMS_IMPORT) {
                    Spacer(Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Captured from a bank message",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            val subtitle = buildList {
                if (item.transaction.merchant?.isNotBlank() == true) add(item.categoryName)
                if (item.transaction.note.isNotBlank()) add(item.transaction.note)
                if (showDate) add(item.transaction.date.relativeLabel())
            }.joinToString(" · ")

            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            Text(
                // The sign carries the meaning for anyone who can't rely on the colour.
                text = (if (isIncome) "+" else "−") +
                    Money.format(item.transaction.amountMinor, currencySymbol).removePrefix("-"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                maxLines = 1,
            )
        }
    }
}
