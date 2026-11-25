package com.pinankh.chaibethakbar.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.ui.theme.ChaiBethakBarTheme

fun isValidGstin(gstin: String): Boolean {
    if (gstin.isBlank()) return true
    val regex = Regex("^[0-9]{2}[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}[1-9A-Za-z]{1}[Zz][0-9A-Za-z]{1}$")
    return regex.matches(gstin.trim()) || gstin.trim().length == 15
}

fun isValidFssai(fssai: String): Boolean {
    if (fssai.isBlank()) return true
    val regex = Regex("^[0-9]{14}$")
    return regex.matches(fssai.trim())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletSettingsScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    onSignOutSuccess: () -> Unit
) {
    val outletConfig by viewModel.outletConfig.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    OutletSettingsScreenContent(
        outletConfig = outletConfig,
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage,
        onBack = onBack,
        onSaveConfig = { config -> viewModel.saveOutletConfig(config) },
        onSignOut = { viewModel.signOut(onSignOutSuccess) },
        onClearMessages = { viewModel.clearMessages() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutletSettingsScreenContent(
    outletConfig: OutletConfig?,
    isLoading: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onSaveConfig: (OutletConfig) -> Unit,
    onSignOut: () -> Unit,
    onClearMessages: () -> Unit
) {
    var outletNameInput by remember(outletConfig) { mutableStateOf(outletConfig?.outletName ?: "Chai Bathak Bar") }
    var addressInput by remember(outletConfig) { mutableStateOf(outletConfig?.addressLine1 ?: "") }
    var cityPincodeInput by remember(outletConfig) { mutableStateOf(outletConfig?.cityPincode ?: "") }
    var phoneInput by remember(outletConfig) { mutableStateOf(outletConfig?.phone ?: "") }
    var gstinInput by remember(outletConfig) { mutableStateOf(outletConfig?.gstin ?: "") }
    var fssaiInput by remember(outletConfig) { mutableStateOf(outletConfig?.fssaiNo ?: "") }
    var billPrefixInput by remember(outletConfig) { mutableStateOf(outletConfig?.billPrefix ?: "CBB") }
    var parcelChargeInput by remember(outletConfig) {
        mutableStateOf(outletConfig?.defaultParcelChargePaise?.toRupeesInputString() ?: "10")
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Outlet Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Error / Success Banners
            if (errorMessage != null || successMessage != null || validationError != null) {
                val isError = errorMessage != null || validationError != null
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = validationError ?: errorMessage ?: successMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF166534)
                        )
                        IconButton(onClick = {
                            validationError = null
                            onClearMessages()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Café Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = outletNameInput,
                                onValueChange = { outletNameInput = it },
                                label = { Text("Outlet Name *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("Contact Phone *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = { addressInput = it },
                                label = { Text("Address Line") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = cityPincodeInput,
                                onValueChange = { cityPincodeInput = it },
                                label = { Text("City & Pincode") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Tax, License & Invoice Billing Config",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = gstinInput,
                                onValueChange = { gstinInput = it.uppercase() },
                                label = { Text("GSTIN (Optional 15-char)") },
                                placeholder = { Text("24AAAAA0000A1Z5") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = fssaiInput,
                                onValueChange = { fssaiInput = it },
                                label = { Text("FSSAI License No. (Optional 14-digit)") },
                                placeholder = { Text("10020021000123") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = billPrefixInput,
                                onValueChange = { billPrefixInput = it.uppercase() },
                                label = { Text("Bill Invoice Prefix *") },
                                placeholder = { Text("CBB") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = parcelChargeInput,
                                onValueChange = { parcelChargeInput = it },
                                label = { Text("Default Parcel Packaging Charge (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out of POS")
                    }

                    Button(
                        onClick = {
                            val name = outletNameInput.trim()
                            if (name.isBlank()) {
                                validationError = "Outlet name cannot be blank"
                                return@Button
                            }

                            val gstin = gstinInput.trim()
                            if (gstin.isNotBlank() && !isValidGstin(gstin)) {
                                validationError = "Invalid GSTIN format (must be 15 alphanumeric characters)"
                                return@Button
                            }

                            val fssai = fssaiInput.trim()
                            if (fssai.isNotBlank() && !isValidFssai(fssai)) {
                                validationError = "Invalid FSSAI format (must be 14 digits)"
                                return@Button
                            }

                            val parcelChargePaise = parseRupeesToPaise(parcelChargeInput) ?: 0L
                            val prefix = billPrefixInput.trim().ifBlank { "CBB" }

                            validationError = null

                            val updatedConfig = (outletConfig ?: OutletConfig()).copy(
                                outletName = name,
                                addressLine1 = addressInput.trim(),
                                cityPincode = cityPincodeInput.trim(),
                                phone = phoneInput.trim(),
                                gstin = gstin,
                                fssaiNo = fssai,
                                billPrefix = prefix,
                                defaultParcelChargePaise = parcelChargePaise
                            )

                            onSaveConfig(updatedConfig)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Outlet Settings")
                        }
                    }
                }
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Confirm Sign Out") },
            text = { Text("Are you sure you want to sign out of Chai Bathak Bar POS? You will need to sign in with email and password again.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OutletSettingsScreenPreview() {
    ChaiBethakBarTheme {
        OutletSettingsScreenContent(
            outletConfig = OutletConfig(
                outletName = "Chai Bathak Bar",
                addressLine1 = "12 Main Street",
                cityPincode = "Ahmedabad, 380015",
                phone = "+91 98765 43210",
                gstin = "24AAAAA0000A1Z5",
                fssaiNo = "10020021000123",
                billPrefix = "CBB",
                defaultParcelChargePaise = 1000L
            ),
            isLoading = false,
            errorMessage = null,
            successMessage = null,
            onBack = {},
            onSaveConfig = {},
            onSignOut = {},
            onClearMessages = {}
        )
    }
}
