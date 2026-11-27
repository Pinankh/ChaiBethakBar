package com.pinankh.chaibethakbar.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pinankh.chaibethakbar.model.Supplier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SupplierViewModel = viewModel()
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showActiveOnly by viewModel.showActiveOnly.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }
    var viewingSupplier by remember { mutableStateOf<Supplier?>(null) }

    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
        successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    val filteredSuppliers = suppliers.filter {
        if (searchQuery.isBlank()) true
        else {
            val q = searchQuery.lowercase()
            it.name.lowercase().contains(q) ||
                    it.contactPerson.lowercase().contains(q) ||
                    it.phone.contains(q) ||
                    it.gstin.lowercase().contains(q)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Supplier Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add Supplier")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Search Suppliers") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = showActiveOnly,
                        onCheckedChange = { viewModel.setShowActiveOnly(it) }
                    )
                    Text("Active Only")
                }
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading && suppliers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredSuppliers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No suppliers found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredSuppliers) { supplier ->
                        SupplierCard(
                            supplier = supplier,
                            onEdit = { editingSupplier = supplier },
                            onToggleActive = { viewModel.toggleSupplierActive(supplier.supplierId, supplier.isActive) },
                            onViewDetails = { viewingSupplier = supplier }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || editingSupplier != null) {
        SupplierFormDialog(
            supplier = editingSupplier,
            onDismiss = {
                showAddDialog = false
                editingSupplier = null
            },
            onSave = { savedSupplier ->
                viewModel.saveSupplier(savedSupplier, editingSupplier != null)
                showAddDialog = false
                editingSupplier = null
            },
            isLoading = isLoading
        )
    }

    if (viewingSupplier != null) {
        SupplierDetailsDialog(
            supplier = viewingSupplier!!,
            onDismiss = { viewingSupplier = null }
        )
    }
}

@Composable
fun SupplierCard(
    supplier: Supplier,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (supplier.isActive) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            contentColor = if (supplier.isActive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = supplier.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (!supplier.isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "INACTIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (supplier.contactPerson.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(supplier.contactPerson, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(supplier.phone, style = MaterialTheme.typography.bodyMedium)
            }

            if (supplier.city.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(supplier.city, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (supplier.gstin.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("GSTIN: ${supplier.gstin}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewDetails) {
                    Text("View")
                }
                TextButton(onClick = onToggleActive) {
                    Text(if (supplier.isActive) "Deactivate" else "Activate")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit")
                }
            }
        }
    }
}

@Composable
fun SupplierDetailsDialog(
    supplier: Supplier,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Supplier Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Business Name: ${supplier.name}", fontWeight = FontWeight.Bold)
                if (supplier.contactPerson.isNotBlank()) Text("Contact Person: ${supplier.contactPerson}")
                Text("Status: ${if (supplier.isActive) "Active" else "Inactive"}")

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                Text("Phone: ${supplier.phone}")
                if (supplier.alternatePhone.isNotBlank()) Text("Alt Phone: ${supplier.alternatePhone}")
                if (supplier.email.isNotBlank()) Text("Email: ${supplier.email}")

                if (supplier.address.isNotBlank() || supplier.city.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Address:", fontWeight = FontWeight.SemiBold)
                    if (supplier.address.isNotBlank()) Text(supplier.address)
                    if (supplier.city.isNotBlank()) Text("City: ${supplier.city}")
                    if (supplier.state.isNotBlank()) Text("State: ${supplier.state}")
                    if (supplier.pincode.isNotBlank()) Text("Pincode: ${supplier.pincode}")
                }

                if (supplier.gstin.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("GSTIN: ${supplier.gstin}")
                }

                if (supplier.notes.isNotBlank()) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text("Notes:", fontWeight = FontWeight.SemiBold)
                    Text(supplier.notes)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SupplierFormDialog(
    supplier: Supplier?,
    onDismiss: () -> Unit,
    onSave: (Supplier) -> Unit,
    isLoading: Boolean
) {
    val isEdit = supplier != null
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var contactPerson by remember { mutableStateOf(supplier?.contactPerson ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var alternatePhone by remember { mutableStateOf(supplier?.alternatePhone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var city by remember { mutableStateOf(supplier?.city ?: "") }
    var state by remember { mutableStateOf(supplier?.state ?: "") }
    var pincode by remember { mutableStateOf(supplier?.pincode ?: "") }
    var gstin by remember { mutableStateOf(supplier?.gstin ?: "") }
    var notes by remember { mutableStateOf(supplier?.notes ?: "") }
    var isActive by remember { mutableStateOf(supplier?.isActive ?: true) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var gstinError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(if (isEdit) "Edit Supplier" else "Add Supplier") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Business Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = if (it.trim().isEmpty()) "Name is required" else null
                    },
                    label = { Text("Supplier/Business Name *") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = { Text("Contact Person") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gstin,
                    onValueChange = {
                        gstin = it.uppercase()
                        gstinError = if (it.isNotBlank() && !it.matches(Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"))) {
                            "Invalid GSTIN format"
                        } else null
                    },
                    label = { Text("GSTIN") },
                    isError = gstinError != null,
                    supportingText = { gstinError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                Text("Contact Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        label = { Text("Alternate Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        emailError = if (it.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                            "Invalid email format"
                        } else null
                    },
                    label = { Text("Email") },
                    isError = emailError != null,
                    supportingText = { emailError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                Text("Address Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Full Address") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                OutlinedTextField(
                    value = pincode,
                    onValueChange = { pincode = it },
                    label = { Text("Pincode") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))
                Text("Additional Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (isEdit) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                        Text("Active Supplier")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    nameError = if (name.trim().isEmpty()) "Name is required" else null
                    if (nameError == null && emailError == null && gstinError == null) {
                        val newSupplier = supplier?.copy(
                            name = name,
                            contactPerson = contactPerson,
                            phone = phone,
                            alternatePhone = alternatePhone,
                            email = email,
                            address = address,
                            city = city,
                            state = state,
                            pincode = pincode,
                            gstin = gstin,
                            notes = notes,
                            isActive = isActive
                        ) ?: Supplier(
                            name = name,
                            contactPerson = contactPerson,
                            phone = phone,
                            alternatePhone = alternatePhone,
                            email = email,
                            address = address,
                            city = city,
                            state = state,
                            pincode = pincode,
                            gstin = gstin,
                            notes = notes,
                            isActive = isActive
                        )
                        onSave(newSupplier)
                    }
                },
                enabled = !isLoading && name.isNotBlank() && emailError == null && gstinError == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        }
    )
}
