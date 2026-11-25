package com.pinankh.chaibethakbar.ui.order

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.MenuItem
import com.pinankh.chaibethakbar.model.OrderItem
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.model.TableModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()

    private val _orderId = MutableStateFlow("")
    val orderId: StateFlow<String> = _orderId.asStateFlow()

    fun setOrderId(id: String) {
        if (_orderId.value != id) {
            _orderId.value = id
        }
    }

    val activeOrder: StateFlow<PosOrder?> = _orderId.flatMapLatest { id ->
        if (id.isBlank()) flowOf(null)
        else repository.getActiveOrderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val menuItems: StateFlow<List<MenuItem>> = repository.getMenuFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outletConfig: StateFlow<OutletConfig?> = repository.getOutletConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val availableTables: StateFlow<List<TableModel>> = repository.getTablesFlow()
        .map { tables -> tables.filter { it.status == "AVAILABLE" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isVegOnly = MutableStateFlow(false)
    val isVegOnly: StateFlow<Boolean> = _isVegOnly.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setVegOnly(vegOnly: Boolean) {
        _isVegOnly.value = vegOnly
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // ------------------------------------------------------------------
    // Item Operations
    // ------------------------------------------------------------------

    fun addOrIncrementItem(menuItem: MenuItem, customNote: String? = null) {
        val currentOrder = activeOrder.value ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val currentItems = currentOrder.items.toMutableList()
                val existingIndex = currentItems.indexOfFirst { it.menuItemId == menuItem.id }

                if (existingIndex >= 0) {
                    val existing = currentItems[existingIndex]
                    val updatedNote = if (!customNote.isNullOrBlank()) customNote else existing.notes
                    currentItems[existingIndex] = existing.copy(
                        qty = existing.qty + 1,
                        notes = updatedNote
                    )
                } else {
                    currentItems.add(
                        OrderItem(
                            menuItemId = menuItem.id,
                            name = menuItem.name,
                            unitPricePaise = menuItem.pricePaise,
                            taxType = menuItem.taxType,
                            taxPercent = menuItem.taxPercent,
                            qty = 1,
                            sentQty = 0,
                            notes = customNote ?: ""
                        )
                    )
                }

                val updatedOrder = currentOrder.copy(items = currentItems)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update item"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun updateItemNote(index: Int, note: String) {
        val currentOrder = activeOrder.value ?: return
        if (index !in currentOrder.items.indices) return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val currentItems = currentOrder.items.toMutableList()
                currentItems[index] = currentItems[index].copy(notes = note)
                val updatedOrder = currentOrder.copy(items = currentItems)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update note"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun incrementItemQuantity(index: Int) {
        val currentOrder = activeOrder.value ?: return
        if (index !in currentOrder.items.indices) return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val currentItems = currentOrder.items.toMutableList()
                val existing = currentItems[index]
                currentItems[index] = existing.copy(qty = existing.qty + 1)
                val updatedOrder = currentOrder.copy(items = currentItems)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to increment quantity"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun decrementItemQuantity(index: Int) {
        val currentOrder = activeOrder.value ?: return
        if (index !in currentOrder.items.indices) return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val currentItems = currentOrder.items.toMutableList()
                val existing = currentItems[index]
                if (existing.qty > 1) {
                    currentItems[index] = existing.copy(qty = existing.qty - 1)
                } else {
                    currentItems.removeAt(index)
                }
                val updatedOrder = currentOrder.copy(items = currentItems)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to decrement quantity"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun removeItem(index: Int) {
        val currentOrder = activeOrder.value ?: return
        if (index !in currentOrder.items.indices) return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                val currentItems = currentOrder.items.toMutableList()
                currentItems.removeAt(index)
                val updatedOrder = currentOrder.copy(items = currentItems)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to remove item"
            } finally {
                _isSaving.value = false
            }
        }
    }

    // ------------------------------------------------------------------
    // Order-Level Operations
    // ------------------------------------------------------------------

    fun updateCustomerName(name: String) {
        val currentOrder = activeOrder.value ?: return
        viewModelScope.launch {
            try {
                val updatedOrder = currentOrder.copy(customerName = name)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update customer name"
            }
        }
    }

    fun setParcelCharge(chargePaise: Long) {
        val currentOrder = activeOrder.value ?: return
        val validCharge = maxOf(0L, chargePaise)
        viewModelScope.launch {
            try {
                val updatedOrder = currentOrder.copy(parcelChargePaise = validCharge)
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update parcel charge"
            }
        }
    }

    fun toggleParcelCharge(enabled: Boolean) {
        val defaultCharge = outletConfig.value?.defaultParcelChargePaise ?: 1000L
        val chargePaise = if (enabled) (if (defaultCharge > 0L) defaultCharge else 1000L) else 0L
        setParcelCharge(chargePaise)
    }

    fun applyDiscount(discountPaise: Long, reason: String) {
        val currentOrder = activeOrder.value ?: return
        val validDiscount = maxOf(0L, discountPaise)
        viewModelScope.launch {
            try {
                val updatedOrder = currentOrder.copy(
                    discountPaise = validDiscount,
                    discountReason = reason
                )
                repository.updateOrder(updatedOrder)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to apply discount"
            }
        }
    }

    fun markSentToKitchen() {
        val currentOrder = activeOrder.value ?: return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                repository.markSentToKitchen(currentOrder.id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to update kitchen status"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun markTableBilled() {
        val currentOrder = activeOrder.value ?: return
        if (currentOrder.tableId.isBlank()) return
        viewModelScope.launch {
            try {
                repository.markTableBilled(currentOrder.tableId, currentOrder.id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to mark table as billed"
            }
        }
    }

    fun shiftTable(targetTableId: String) {
        val currentOrder = activeOrder.value ?: return
        if (currentOrder.tableId.isBlank() || targetTableId.isBlank()) return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                repository.shiftTable(currentOrder.tableId, targetTableId, currentOrder.id)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to shift table"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun cancelOrder(reason: String, onCancelled: () -> Unit) {
        val currentOrder = activeOrder.value ?: return
        if (_isSaving.value) return
        _isSaving.value = true

        viewModelScope.launch {
            try {
                repository.cancelOrder(currentOrder.id, reason)
                onCancelled()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to cancel order"
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun settleOrder(paymentMode: String, isHandedOver: Boolean, onSuccess: () -> Unit) {
        val currentOrder = activeOrder.value ?: return
        if (_isSaving.value) return
        _isSaving.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                repository.settleOrder(
                    orderId = currentOrder.id,
                    paymentMode = paymentMode,
                    isHandedOver = isHandedOver
                )
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Failed to settle order"
            } finally {
                _isSaving.value = false
            }
        }
    }
}
