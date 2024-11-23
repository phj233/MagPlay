package top.phj233.magplay.ui.screens.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import top.phj233.magplay.entity.Calculator
import top.phj233.magplay.repository.DBUtil

class CalculatorViewModel : ViewModel() {
    private val calculatorDao = DBUtil.getCalculatorDao()

    val calculators: StateFlow<List<Calculator>> = calculatorDao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun insertCalculator(calculator: Calculator) {
        viewModelScope.launch {
            calculatorDao.insertAll(calculator)
        }
    }

    fun deleteCalculator(calculator: Calculator) {
        viewModelScope.launch {
            calculatorDao.delete(calculator)
        }
    }
}