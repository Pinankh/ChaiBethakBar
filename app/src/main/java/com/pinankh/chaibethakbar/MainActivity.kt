package com.pinankh.chaibethakbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.pinankh.chaibethakbar.ui.admin.AdminViewModel
import com.pinankh.chaibethakbar.ui.admin.MenuAndTablesScreen
import com.pinankh.chaibethakbar.ui.admin.OutletSettingsScreen
import com.pinankh.chaibethakbar.ui.auth.OutletLoginScreen
import com.pinankh.chaibethakbar.ui.home.HomeViewModel
import com.pinankh.chaibethakbar.ui.home.PosHomeScreen
import com.pinankh.chaibethakbar.ui.inventory.InventoryManagerScreen
import com.pinankh.chaibethakbar.ui.inventory.InventoryViewModel
import com.pinankh.chaibethakbar.ui.supplier.SupplierManagerScreen
import com.pinankh.chaibethakbar.ui.supplier.SupplierViewModel
import com.pinankh.chaibethakbar.ui.order.OrderWorkspaceScreen
import com.pinankh.chaibethakbar.ui.reports.DailyReportsScreen
import com.pinankh.chaibethakbar.ui.reports.ReportsViewModel
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme

sealed class Screen {
    object Login : Screen()
    object Home : Screen()
    data class OrderWorkspace(val orderId: String) : Screen()
    object Reports : Screen()
    object MenuTables : Screen()
    object Settings : Screen()
    object InventoryManager : Screen()
    object SupplierManager : Screen()
}

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModels()
    private val adminViewModel: AdminViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()
    private val inventoryViewModel: InventoryViewModel by viewModels()
    private val supplierViewModel: SupplierViewModel by viewModels()

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
                                },
                                onNavigateInventory = {
                                    currentScreen = Screen.InventoryManager
                                },
                                onNavigateSuppliers = {
                                    currentScreen = Screen.SupplierManager
                                }
                            )
                        }
                        is Screen.OrderWorkspace -> {
                            OrderWorkspaceScreen(
                                orderId = screen.orderId,
                                onBackToHome = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Reports -> {
                            DailyReportsScreen(
                                viewModel = reportsViewModel,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.MenuTables -> {
                            MenuAndTablesScreen(
                                viewModel = adminViewModel,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.Settings -> {
                            OutletSettingsScreen(
                                viewModel = adminViewModel,
                                onBack = { currentScreen = Screen.Home },
                                onSignOutSuccess = {
                                    currentScreen = Screen.Login
                                }
                            )
                        }
                        is Screen.InventoryManager -> {
                            InventoryManagerScreen(
                                viewModel = inventoryViewModel,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.SupplierManager -> {
                            SupplierManagerScreen(
                                viewModel = supplierViewModel,
                                onNavigateBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}
