package com.pinankh.chaibethakbar.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.Category
import com.pinankh.chaibethakbar.model.InventoryItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()

    val inventoryItems: StateFlow<List<InventoryItem>> = repository.observeInventoryItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryCategories: StateFlow<List<Category>> = repository.observeInventoryCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedItemType = MutableStateFlow("All")
    val selectedItemType: StateFlow<String> = _selectedItemType.asStateFlow()

    private val _showActiveOnly = MutableStateFlow(true)
    val showActiveOnly: StateFlow<Boolean> = _showActiveOnly.asStateFlow()

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

    fun setSelectedItemType(type: String) {
        _selectedItemType.value = type
    }

    fun setShowActiveOnly(activeOnly: Boolean) {
        _showActiveOnly.value = activeOnly
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun saveInventoryItem(item: InventoryItem, isEdit: Boolean) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                if (isEdit) {
                    repository.updateInventoryItem(item)
                    _successMessage.value = "Inventory item '${item.name}' updated."
                } else {
                    repository.addInventoryItem(item)
                    _successMessage.value = "Inventory item '${item.name}' added."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to save inventory item"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleInventoryItemActive(itemId: String, currentActive: Boolean) {
        viewModelScope.launch {
            try {
                repository.setInventoryItemActive(itemId, !currentActive)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update item status"
            }
        }
    }
}
