package com.roadmate.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.DocumentType
import com.roadmate.core.ui.theme.RoadMateSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

fun daysUntilExpiry(expiryDate: Long): Long {
    val expiryLocalDate = Instant.ofEpochMilli(expiryDate)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return ChronoUnit.DAYS.between(LocalDate.now(), expiryLocalDate)
}

fun documentTypeIcon(type: DocumentType): ImageVector = when (type) {
    DocumentType.INSURANCE -> Icons.Filled.Shield
    DocumentType.LICENSE -> Icons.Filled.Badge
    DocumentType.REGISTRATION -> Icons.Filled.Description
    DocumentType.OTHER -> Icons.AutoMirrored.Filled.Article
}

enum class DocumentExpiryState {
    NORMAL,
    WARNING,
    EXPIRED,
}

fun documentExpiryState(daysUntil: Long, reminderDaysBefore: Int): DocumentExpiryState = when {
    daysUntil < 0 -> DocumentExpiryState.EXPIRED
    reminderDaysBefore > 0 && daysUntil <= reminderDaysBefore -> DocumentExpiryState.WARNING
    else -> DocumentExpiryState.NORMAL
}

@Composable
@ReadOnlyComposable
fun documentExpiryColor(state: DocumentExpiryState): Color = when (state) {
    DocumentExpiryState.EXPIRED -> MaterialTheme.colorScheme.error
    DocumentExpiryState.WARNING -> MaterialTheme.colorScheme.tertiary
    DocumentExpiryState.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun formatExpiryText(daysUntil: Long): String = when {
    daysUntil < 0 -> "Expired ${-daysUntil} day${if (-daysUntil != 1L) "s" else ""} ago"
    daysUntil == 0L -> "Expires today"
    else -> "${daysUntil} day${if (daysUntil != 1L) "s" else ""} remaining"
}

@Composable
fun DocumentCard(
    document: Document,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val daysUntil = daysUntilExpiry(document.expiryDate)
    val state = documentExpiryState(daysUntil, document.reminderDaysBefore)
    val color = documentExpiryColor(state)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 76.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RoadMateSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = documentTypeIcon(document.type),
                contentDescription = document.type.name,
                tint = color,
                modifier = Modifier.size(32.dp),
            )

            Spacer(modifier = Modifier.width(RoadMateSpacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(RoadMateSpacing.xs))
                Text(
                    text = formatDateForDisplay(document.expiryDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.width(RoadMateSpacing.md))

            Text(
                text = formatExpiryText(daysUntil),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun DocumentList(
    documents: List<Document>,
    onDocumentClick: (Document) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (documents.isEmpty()) {
        DocumentEmptyState(modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(RoadMateSpacing.sm),
        ) {
            items(documents, key = { it.id }) { document ->
                DocumentCard(
                    document = document,
                    onClick = { onDocumentClick(document) },
                )
            }
        }
    }
}

@Composable
fun DocumentEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = "Documents",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

        Text(
            text = "No documents.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(RoadMateSpacing.xs))

        Text(
            text = "Add your first vehicle document.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
