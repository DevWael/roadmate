package com.roadmate.phone.ui.documents

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.roadmate.phone.ui.components.RoadMateScaffold

@Composable
fun DocumentListScreen(
    onDocumentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Document List",
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
fun DocumentDetailScreen(
    documentId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadMateScaffold(
        title = "Document Detail",
        onBack = onBack,
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Document Detail: $documentId",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
