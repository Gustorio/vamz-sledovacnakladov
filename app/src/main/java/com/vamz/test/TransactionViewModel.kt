package com.vamz.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vamz.test.data.AppDatabase
import com.vamz.test.data.TransactionEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Rozšírenie pre mapovanie [TransactionEntity] na UI model [Transaction].
 */
fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(
        id = this.id,
        description = this.description,
        amount = this.amount,
        type = this.type,
        timestamp = this.timestamp,
        category = this.category,
        note = this.note
    )
}

/**
 * Rozšírenie pre mapovanie UI modelu [Transaction] na databázovú entitu [TransactionEntity].
 */
fun Transaction.toEntity(): TransactionEntity {
    return TransactionEntity(
        id = this.id,
        description = this.description,
        amount = this.amount,
        type = this.type,
        timestamp = this.timestamp,
        category = this.category,
        note = this.note
    )
}

/**
 * ViewModel pre správu dát a stavu pre obrazovky transakcií a štatistík.
 * Získava dáta z databázy cez [TransactionDao] a poskytuje ich ako [StateFlow] pre UI.
 * Obsahuje logiku pre pridávanie, mazanie transakcií a výpočet štatistík.
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    // region Stav formulára pre pridanie/úpravu transakcie
    // Tieto StateFlowy uchovávajú stav formulára pre pridanie novej transakcie a prežívajú konfiguračné zmeny.

    private val _description = MutableStateFlow("")
    /** Aktuálna hodnota popisu vo formulári pre transakciu. */
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amountText = MutableStateFlow("")
    /** Aktuálna hodnota sumy (ako text) vo formulári pre transakciu. */
    val amountText: StateFlow<String> = _amountText.asStateFlow()

    private val _transactionType = MutableStateFlow(TransactionType.EXPENSE)
    /** Aktuálne vybraný typ transakcie ([TransactionType]) vo formulári. */
    val transactionType: StateFlow<TransactionType> = _transactionType.asStateFlow()

    private val _selectedCategory = MutableStateFlow("")
    /** Aktuálne vybraná kategória vo formulári pre transakciu. */
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _note = MutableStateFlow<String?>(null)
    /** Aktuálna hodnota poznámky vo formulári pre transakciu. Môže byť null. */
    val note: StateFlow<String?> = _note.asStateFlow()

    private val _selectedDateTimestamp = MutableStateFlow(System.currentTimeMillis())
    /** Aktuálne vybraná časová pečiatka dátumu vo formulári pre transakciu. */
    val selectedDateTimestamp: StateFlow<Long> = _selectedDateTimestamp.asStateFlow()

    // endregion

    // region Dátové toky transakcií a zostatkov

    /**
     * Flow všetkých transakcií z databázy, mapovaných na UI model [Transaction].
     * Emituje aktualizovaný zoznam pri každej zmene v tabuľke transakcií.
     */
    val allTransactions: StateFlow<List<Transaction>> =
        transactionDao.getAllTransactions()
            .map { entities -> entities.map { it.toTransaction() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flow celkového zostatku všetkých transakcií.
     * Emituje aktualizovaný zostatok pri každej zmene v zozname transakcií.
     */
    val totalBalance: StateFlow<Double> =
        allTransactions.map { transactions ->
            transactions.sumOf { it.amount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /** Definuje možné časové obdobia pre zobrazenie štatistík. */
    enum class StatisticsPeriod {
        LAST_MONTH, LAST_6_MONTHS, LAST_YEAR
    }

    private val _statisticsPeriod = MutableStateFlow(StatisticsPeriod.LAST_MONTH)
    /** Aktuálne vybrané časové obdobie pre štatistiky. */
    val statisticsPeriod: StateFlow<StatisticsPeriod> = _statisticsPeriod.asStateFlow()

    /**
     * Nastaví aktuálne vybrané časové obdobie pre štatistiky.
     * Spustí opätovné načítanie transakcií a prepočítanie súhrnov pre nové obdobie.
     *
     * @param period Nové časové obdobie ([StatisticsPeriod]).
     */
    fun setStatisticsPeriod(period: StatisticsPeriod) {
        _statisticsPeriod.value = period
    }

    /**
     * Flow transakcií spadajúcich do aktuálne vybraného časového obdobia pre štatistiky.
     * Emituje aktualizovaný zoznam pri zmene obdobia alebo zmene transakcií v rámci tohto obdobia.
     */
    val transactionsForStatistics: StateFlow<List<Transaction>> =
        statisticsPeriod.flatMapLatest { period ->
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis

            val startTime = when (period) {
                StatisticsPeriod.LAST_MONTH -> {
                    calendar.add(Calendar.MONTH, -1)
                    calendar.timeInMillis
                }
                StatisticsPeriod.LAST_6_MONTHS -> {
                    calendar.add(Calendar.MONTH, -6)
                    calendar.timeInMillis
                }
                StatisticsPeriod.LAST_YEAR -> {
                    calendar.add(Calendar.YEAR, -1)
                    calendar.timeInMillis
                }
            }
            transactionDao.getTransactionsBetweenTimestamps(startTime, endTime)
        }.map { entities -> entities.map { it.toTransaction() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flow celkového príjmu za aktuálne vybrané časové obdobie pre štatistiky.
     * Emituje aktualizovanú sumu pri zmene obdobia alebo zmene transakcií.
     */
    val totalIncomeForPeriod: StateFlow<Double> =
        transactionsForStatistics.map { transactions ->
            transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Flow celkových výdavkov za aktuálne vybrané časové obdobie pre štatistiky.
     * Emituje aktualizovanú sumu pri zmene obdobia alebo zmene transakcií.
     */
    val totalExpenseForPeriod: StateFlow<Double> =
        transactionsForStatistics.map { transactions ->
            transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    /**
     * Aktualizuje hodnotu popisu vo formulári pre pridanie transakcie.
     * @param desc Nový text popisu.
     */
    fun setDescription(desc: String) {
        _description.value = desc
    }

    /**
     * Aktualizuje hodnotu textu sumy vo formulári pre pridanie transakcie.
     * @param amount Nový text sumy.
     */
    fun setAmountText(amount: String) {
        _amountText.value = amount
    }

    /**
     * Nastaví typ transakcie vo formulári. Pri zmene typu resetuje vybranú kategóriu.
     * @param type Nový typ transakcie ([TransactionType]).
     */
    fun setTransactionType(type: TransactionType) {
        _transactionType.value = type
        setSelectedCategory("")
    }

    /**
     * Nastaví vybranú kategóriu vo formulári pre pridanie transakcie.
     * @param category Nová kategória.
     */
    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    /**
     * Aktualizuje hodnotu poznámky vo formulári. Ak je text prázdny, uloží sa ako null.
     * @param note Nový text poznámky.
     */
    fun setNote(note: String) {
        _note.value = note.ifBlank { null }
    }

    /**
     * Nastaví vybranú časovú pečiatku dátumu vo formulári pre transakciu.
     * @param timestamp Nová časová pečiatka v milisekundách.
     */
    fun setSelectedDateTimestamp(timestamp: Long) {
        _selectedDateTimestamp.value = timestamp
    }

    /**
     * Vyčistí všetky polia formulára pre pridanie transakcie na ich počiatočné hodnoty.
     */
    fun clearTransactionForm() {
        _description.value = ""
        _amountText.value = ""
        _transactionType.value = TransactionType.EXPENSE
        _selectedCategory.value = ""
        _note.value = null
        _selectedDateTimestamp.value = System.currentTimeMillis()
    }

    /**
     * Pridá novú transakciu do databázy na základe aktuálnych hodnôt vo formulári ViewModelu.
     * Pred vložením vykoná základnú validáciu. Po úspešnom pridaní vyčistí formulár.
     * Validáciu by bolo možné robustnejšie riešiť s odozvou pre UI.
     */
    fun addTransactionFromForm() {
        val description = _description.value
        val amount = _amountText.value.toDoubleOrNull()
        val type = _transactionType.value
        val timestamp = _selectedDateTimestamp.value
        val category = _selectedCategory.value
        val note = _note.value

        // Základná validácia
        if (description.isBlank() || amount == null || category.isBlank()) {
            // V produkčnej app zvážte emitovanie chyby (Event) pre UI.
            println("DEBUG: Validácia formulára zlyhala")
            return
        }

        viewModelScope.launch {
            val finalAmount = if (type == TransactionType.EXPENSE && amount > 0) -amount else amount
            val newTransactionEntity = TransactionEntity(
                description = description,
                amount = finalAmount,
                type = type,
                timestamp = timestamp,
                category = category,
                note = note
            )
            transactionDao.insertTransaction(newTransactionEntity)
            clearTransactionForm() // Vyčisti formulár po úspechu
        }
    }

    /**
     * Zmaže všetky transakcie z databázy.
     */
    fun deleteAllTransactions() {
        viewModelScope.launch {
            transactionDao.deleteAllTransactions()
        }
    }

    /**
     * Zmaže špecifickú transakciu z databázy.
     *
     * @param transaction Transakcia (UI model), ktorá sa má zmazať.
     */
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            // Prevedieme UI model na databázovú entitu pred zmazaním
            transactionDao.deleteTransaction(transaction.toEntity())
        }
    }

    // endregion

    init {
        // Tento debug log nie je v produkcii potrebný, slúži na diagnostiku počas vývoja.
        //println("DEBUG: TransactionViewModel initialized - attempting database access")
    }
}