package ru.filantrop.firebaseexampleapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.filantrop.firebaseexampleapp.ui.AuthUiState
import ru.filantrop.firebaseexampleapp.ui.HomeScreen
import ru.filantrop.firebaseexampleapp.ui.LoginScreen
import ru.filantrop.firebaseexampleapp.ui.MainViewModel
import ru.filantrop.firebaseexampleapp.ui.theme.FirebaseExampleAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (MainViewModel::class.java.isAssignableFrom(modelClass)) {
                        @Suppress("UNCHECKED_CAST")
                        return MainViewModel() as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel: $modelClass")
                }
            }
        ).get(MainViewModel::class.java)
        enableEdgeToEdge()
        setContent {
            FirebaseExampleAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        when (val authState = state.authState) {
                            AuthUiState.Unknown -> LoadingScreen(
                                modifier = Modifier.fillMaxSize(),
                            )

                            AuthUiState.SignedOut -> LoginScreen(
                                state = state,
                                onEmailLogin = viewModel::loginWithEmail,
                                onEmailRegister = viewModel::registerWithEmail,
                                modifier = Modifier.fillMaxSize(),
                            )

                            is AuthUiState.SignedIn -> HomeScreen(
                                state = state,
                                onSignOut = viewModel::signOut,
                                onRetrySecret = viewModel::retryLoadSecret,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
