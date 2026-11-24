package com.pinankh.chaibethakbar.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.model.TableModel
import com.pinankh.chaibethakbar.util.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()
    private val networkMonitor = NetworkMonitor(application)

    val outletConfig: StateFlow<OutletConfig?> = repository.getOutletConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tables: StateFlow<List<TableModel>> = repository.getTablesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val menuItems: StateFlow<List<MenuItem>> = repository.getMenuFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTakeawayOrders: StateFlow<List<PosOrder>> = repository.getActiveTakeawayOrdersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedZone = MutableStateFlow("All")
    val selectedZone: StateFlow<String> = _selectedZone.asStateFlow()

    fun setSelectedZone(zone: String) {
        _selectedZone.value = zone
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun seedDemoOutlet() {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                repository.seedDemoOutlet()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to seed demo data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createTakeawayOrder(customerName: String = "", onCreated: (String) -> Unit) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val parcelCharge = outletConfig.value?.defaultParcelChargePaise ?: 0L
                val orderId = repository.createTakeawayOrder(customerName, parcelCharge)
                onCreated(orderId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to create takeaway order"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openTable(tableId: String, tableLabel: String, onOpened: (String) -> Unit) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val orderId = repository.openTable(tableId, tableLabel)
                onOpened(orderId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to open table"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markParcelHandedOver(orderId: String) {
        viewModelScope.launch {
            try {
                repository.markParcelHandedOver(orderId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update parcel status"
            }
        }
    }

    fun shiftTable(fromTableId: String, toTableId: String, orderId: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.shiftTable(fromTableId, toTableId, orderId)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to shift table"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
