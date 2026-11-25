package com.pinankh.chaibethakbar.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pinankh.chaibethakbar.data.PosRepository
import com.pinankh.chaibethakbar.model.OutletConfig
import com.pinankh.chaibethakbar.model.PosOrder
import com.pinankh.chaibethakbar.util.DailyReportSummary
import com.pinankh.chaibethakbar.util.ReportCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PosRepository.getInstance()

    private val initialTodayIST = LocalDate.now(ZoneId.of("Asia/Kolkata"))
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    private val _selectedDate = MutableStateFlow(initialTodayIST)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _orderTypeFilter = MutableStateFlow("ALL") // ALL, DINE_IN, TAKEAWAY
    val orderTypeFilter: StateFlow<String> = _orderTypeFilter.asStateFlow()

    private val _paymentModeFilter = MutableStateFlow("ALL") // ALL, CASH, ONLINE
    val paymentModeFilter: StateFlow<String> = _paymentModeFilter.asStateFlow()

    val outletConfig: StateFlow<OutletConfig?> = repository.getOutletConfigFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dailyOrders: StateFlow<List<PosOrder>> = _selectedDate.flatMapLatest { dateStr ->
        repository.getDailyOrdersFlow(dateStr)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportSummary: StateFlow<DailyReportSummary> = dailyOrders.map { orders ->
        ReportCalculator.calculateDailyReport(orders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyReportSummary())

    val filteredHistoryOrders: StateFlow<List<PosOrder>> = combine(
        dailyOrders,
        orderTypeFilter,
        paymentModeFilter
    ) { orders, typeFilter, modeFilter ->
        orders.filter { order ->
            val matchType = when (typeFilter) {
                "DINE_IN" -> order.orderType.equals("DINE_IN", ignoreCase = true)
                "TAKEAWAY" -> order.orderType.equals("TAKEAWAY", ignoreCase = true)
                else -> true
            }
            val matchMode = when (modeFilter) {
                "CASH" -> order.paymentMode.equals("CASH", ignoreCase = true)
                "ONLINE" -> order.paymentMode.equals("ONLINE", ignoreCase = true)
                else -> true
            }
            matchType && matchMode
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedDate(dateStr: String) {
        if (dateStr.isNotBlank() && _selectedDate.value != dateStr) {
            _selectedDate.value = dateStr
        }
    }

    fun setOrderTypeFilter(filter: String) {
        _orderTypeFilter.value = filter
    }

    fun setPaymentModeFilter(filter: String) {
        _paymentModeFilter.value = filter
    }
}
