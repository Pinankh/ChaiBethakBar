package com.pinankh.chaibethakbar.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.TableModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()

    val menuItems: StateFlow<List<MenuItem>> = repository.getMenuFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tables: StateFlow<List<TableModel>> = repository.getTablesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outletConfig: StateFlow<OutletConfig?> = repository.getOutletConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // ------------------------------------------------------------------
    // Menu Operations
    // ------------------------------------------------------------------

    fun saveMenuItem(item: MenuItem, isEdit: Boolean) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (isEdit) {
                    repository.updateMenuItem(item)
                    _successMessage.value = "Menu item '${item.name}' updated."
                } else {
                    repository.addMenuItem(item)
                    _successMessage.value = "Menu item '${item.name}' added."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save menu item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleStock(itemId: String, currentInStock: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateMenuItemStock(itemId, !currentInStock)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update stock status"
            }
        }
    }

    // ------------------------------------------------------------------
    // Table Operations
    // ------------------------------------------------------------------

    fun saveTable(table: TableModel, isEdit: Boolean) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (isEdit) {
                    repository.updateTable(table)
                    _successMessage.value = "Table '${table.label}' updated."
                } else {
                    repository.addTable(table)
                    _successMessage.value = "Table '${table.label}' added."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save table"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTable(table: TableModel) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                repository.deleteTable(table.id)
                _successMessage.value = "Table '${table.label}' deleted."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to delete table"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ------------------------------------------------------------------
    // Outlet Settings Operations
    // ------------------------------------------------------------------

    fun saveOutletConfig(config: OutletConfig) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                repository.updateOutletConfig(config)
                _successMessage.value = "Outlet settings saved successfully."
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save outlet settings"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signOut()
                onSignedOut()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to sign out"
            }
        }
    }
}
