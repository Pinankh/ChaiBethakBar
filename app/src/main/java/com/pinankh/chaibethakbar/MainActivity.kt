package com.pinankh.chaibethakbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.pinankh.chaibethakbar.ui.auth.OutletLoginScreen
import com.pinankh.chaibethakbar.ui.home.HomeViewModel
import com.pinankh.chaibethakbar.ui.home.PosHomeScreen
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme

sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class OrderWorkspace(val orderId: String) : Screen()
    object Reports : Screen()
    object MenuTables : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val currentUser = FirebaseAuth.getInstance().currentUser
        val initialScreen = if (currentUser != null) Screen.Home else Screen.Login

        setContent {
            ChaiBethakBarTheme {
                var currentScreen by remember { mutableStateOf<Screen>(initialScreen) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Login -> {
                            OutletLoginScreen(
                                onLoginSuccess = {
                                    currentScreen = Screen.Home
                                }
                            )
                        }
                        is Screen.Home -> {
                            PosHomeScreen(
                                viewModel = homeViewModel,
                                onOpenOrder = { orderId ->
                                    currentScreen = Screen.OrderWorkspace(orderId)
                                },
                                onNavigateReports = {
                                    currentScreen = Screen.Reports
                                },
                                onNavigateMenuTables = {
                                    currentScreen = Screen.MenuTables
                                },
                                onNavigateSettings = {
                                    currentScreen = Screen.Settings
                                }
                            )
                        }
                        is Screen.OrderWorkspace -> {
                            PlaceholderScreen(
                                title = "Order Workspace Placeholder",
                                subtitle = "Order ID: ${screen.orderId}",
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Reports -> {
                            PlaceholderScreen(
                                title = "Daily Sales Reports Placeholder",
                                subtitle = "Phase 6 Feature",
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.MenuTables -> {
                            PlaceholderScreen(
                                title = "Menu & Tables Admin Placeholder",
                                subtitle = "Phase 6 Feature",
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Settings -> {
                            SettingsPlaceholderScreen(
                                onBack = { currentScreen = Screen.Home },
                                onSignOut = {
                                    FirebaseAuth.getInstance().signOut()
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPlaceholderScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Outlet Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Outlet Settings & Management", style = MaterialTheme.typography.headlineMedium)
                Text(text = "Signed in as: ${runCatching { FirebaseAuth.getInstance().currentUser?.email }.getOrNull() ?: "Owner"}", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out of POS")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaceholderScreenPreview() {
    ChaiBethakBarTheme {
        PlaceholderScreen(
            title = "Order Workspace Placeholder",
            subtitle = "Order ID: ORD-101",
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPlaceholderScreenPreview() {
    ChaiBethakBarTheme {
        SettingsPlaceholderScreen(
            onBack = {},
            onSignOut = {}
        )
    }
}
