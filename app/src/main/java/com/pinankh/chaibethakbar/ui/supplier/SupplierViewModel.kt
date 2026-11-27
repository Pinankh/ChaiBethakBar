package com.pinankh.chaibethakbar.ui.supplier

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.Supplier
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SupplierViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showActiveOnly = MutableStateFlow(true)
    val showActiveOnly: StateFlow<Boolean> = _showActiveOnly.asStateFlow()

    val suppliers: StateFlow<List<Supplier>> = combine(
        repository.observeSuppliers(),
        _showActiveOnly
    ) { allSuppliers, activeOnly ->
        if (activeOnly) {
            allSuppliers.filter { it.isActive }
        } else {
            allSuppliers
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setShowActiveOnly(activeOnly: Boolean) {
        _showActiveOnly.value = activeOnly
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun saveSupplier(supplier: Supplier, isEdit: Boolean) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (isEdit) {
                    repository.updateSupplier(supplier)
                    _successMessage.value = "Supplier '${supplier.name}' updated."
                } else {
                    repository.addSupplier(supplier)
                    _successMessage.value = "Supplier '${supplier.name}' added."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save supplier"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleSupplierActive(supplierId: String, currentActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.setSupplierActive(supplierId, !currentActive)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update supplier status"
            }
        }
    }
}
