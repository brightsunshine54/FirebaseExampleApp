package ru.filantrop.firebaseexampleapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ru.filantrop.firebaseexampleapp.R

@Composable
fun HomeScreen(
    state: MainUiState,
    onSignOut: () -> Unit,
    onRetrySecret: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val user = (state.authState as? AuthUiState.SignedIn)?.user
    val identity = user?.email ?: user?.displayName ?: user?.uid ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.signed_in_as, identity),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(32.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.secret_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    state.isSecretLoading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                    }

                    state.secret != null -> {
                        SelectionContainer {
                            Text(
                                text = state.secret!!,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }

                    state.secretError != null -> {
                        Text(
                            text = state.secretError!!,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRetrySecret) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onSignOut) {
            Text(stringResource(R.string.sign_out))
        }
    }
}
