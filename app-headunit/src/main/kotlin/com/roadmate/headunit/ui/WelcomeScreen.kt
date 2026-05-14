package com.roadmate.headunit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roadmate.core.model.UiState
import com.roadmate.core.ui.theme.RoadMateSpacing
import com.roadmate.headunit.R
import com.roadmate.headunit.ui.components.CockpitPanel
import com.roadmate.headunit.ui.components.PanelDivider
import com.roadmate.headunit.viewmodel.WelcomeFormState
import com.roadmate.headunit.viewmodel.WelcomeViewModel

private val MinTouchTarget = 76.dp

@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WelcomeContent(
        uiState = uiState,
        onNameChange = viewModel::updateName,
        onOdometerChange = viewModel::updateOdometer,
        onStartTracking = viewModel::startTracking,
        onRetry = viewModel::resetToForm,
    )
}

@Composable
fun WelcomeContent(
    uiState: UiState<WelcomeFormState>,
    onNameChange: (String) -> Unit,
    onOdometerChange: (String) -> Unit,
    onStartTracking: () -> Unit,
    onRetry: () -> Unit = {},
) {
    when (uiState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Success -> {
            val form = uiState.data
            WelcomeForm(
                form = form,
                onNameChange = onNameChange,
                onOdometerChange = onOdometerChange,
                onStartTracking = onStartTracking,
            )
        }
        is UiState.Error -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.height(RoadMateSpacing.lg))
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.height(MinTouchTarget),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_try_again),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeForm(
    form: WelcomeFormState,
    onNameChange: (String) -> Unit,
    onOdometerChange: (String) -> Unit,
    onStartTracking: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(RoadMateSpacing.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CockpitPanel(
            modifier = Modifier
                .fillMaxWidth(0.7f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(RoadMateSpacing.xl),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

                PanelDivider()

                Spacer(modifier = Modifier.height(RoadMateSpacing.xxl))

                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(text = stringResource(R.string.welcome_vehicle_name)) },
                    isError = form.errors.containsKey("name"),
                    supportingText = form.errors["name"]?.let {
                        { Text(text = it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget + 24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(),
                )

                Spacer(modifier = Modifier.height(RoadMateSpacing.lg))

                OutlinedTextField(
                    value = form.odometer,
                    onValueChange = onOdometerChange,
                    label = { Text(text = stringResource(R.string.welcome_odometer_label)) },
                    isError = form.errors.containsKey("odometer"),
                    supportingText = form.errors["odometer"]?.let {
                        { Text(text = it, color = MaterialTheme.colorScheme.error) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget + 24.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(),
                )

                Spacer(modifier = Modifier.height(RoadMateSpacing.xxl))

                PanelDivider()

                Spacer(modifier = Modifier.height(RoadMateSpacing.xl))

                Button(
                    onClick = onStartTracking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(MinTouchTarget),
                    enabled = form.name.isNotBlank() && form.odometer.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.welcome_start_tracking),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
