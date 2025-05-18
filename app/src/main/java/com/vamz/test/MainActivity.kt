package com.vamz.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vamz.test.ui.theme.TestTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.saveable.rememberSaveable

// Importy pre graf a dátumy (TransactionBarChart)
import android.graphics.Color as AndroidColor
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlin.math.abs
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale


/**
 * Definície navigačných ciest (routes) pre aplikáciu.
 */
object AppDestinations {
    const val HOME_ROUTE = "home"
    const val STATISTICS_ROUTE = "statistics"
    const val ADD_TRANSACTION_ROUTE = "add_transaction"
}

/**
 * Hlavná aktivita aplikácie, nastavuje obsah pomocou Jetpack Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TestTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}

/**
 * Hlavný navigačný host pre aplikáciu, spravuje prechody medzi obrazovkami.
 *
 * @param navController Navigačný kontrolér pre aplikáciu.
 * @param transactionViewModel ViewModel pre správu dát transakcií.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    NavHost(navController = navController, startDestination = AppDestinations.HOME_ROUTE) {
        composable(AppDestinations.HOME_ROUTE) {
            MainScreen(
                transactionViewModel = transactionViewModel,
                onNavigateToStatistics = { navController.navigate(AppDestinations.STATISTICS_ROUTE) },
                onNavigateToAddTransaction = { navController.navigate(AppDestinations.ADD_TRANSACTION_ROUTE) }
            )
        }
        composable(AppDestinations.STATISTICS_ROUTE) {
            val selectedPeriod by transactionViewModel.statisticsPeriod.collectAsState()
            val totalIncome by transactionViewModel.totalIncomeForPeriod.collectAsState()
            val totalExpense by transactionViewModel.totalExpenseForPeriod.collectAsState()
            val transactionsForPeriod by transactionViewModel.transactionsForStatistics.collectAsState()

            StatisticsScreen(
                selectedPeriod = selectedPeriod,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                transactionsForPeriod = transactionsForPeriod,
                onPeriodSelected = { period -> transactionViewModel.setStatisticsPeriod(period) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(AppDestinations.ADD_TRANSACTION_ROUTE) {
            AddTransactionScreen(
                transactionViewModel = transactionViewModel,
                onTransactionAdded = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Hlavná obrazovka aplikácie zobrazujúca celkový zostatok a zoznam transakcií.
 * Poskytuje navigáciu na obrazovku štatistík a pridanie novej transakcie,
 * a tiež možnosť zmazať všetky transakcie.
 *
 * @param modifier Modifikátor pre Composable.
 * @param transactionViewModel ViewModel pre správu dát transakcií.
 * @param onNavigateToStatistics Lambda funkcia pre navigáciu na obrazovku štatistík.
 * @param onNavigateToAddTransaction Lambda funkcia pre navigáciu na obrazovku pridania transakcie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    transactionViewModel: TransactionViewModel = viewModel(),
    onNavigateToStatistics: () -> Unit,
    onNavigateToAddTransaction: () -> Unit
) {
    val transactions by transactionViewModel.allTransactions.collectAsState()
    val totalBalance by transactionViewModel.totalBalance.collectAsState()

    var showClearAllDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Prehľad financií") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Default.Info, contentDescription = "Štatistiky")
                    }
                    IconButton(onClick = { showClearAllDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Vyčistiť všetky transakcie")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddTransaction) {
                Icon(Icons.Filled.Add, "Pridať transakciu")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aktuálny zostatok", style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
                Text(
                    text = "${"%.2f".format(totalBalance)} €",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (totalBalance >= 0) Color(0xFF008000) else Color.Red
                )
            }

            /**
             * Zoznam transakcií s možnosťou zmazania jednotlivých položiek.
             * Zobrazuje buď zoznam transakcií alebo text, ak je zoznam prázdny.
             */
            TransactionList(
                transactions = transactions,
                onDeleteTransaction = { transaction ->
                    transactionViewModel.deleteTransaction(transaction)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    /**
     * Potvrdzovací dialóg pre zmazanie všetkých transakcií.
     * Zobrazí sa, keď je premenná [showClearAllDialog] true.
     */
    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Upozornenie", tint = Color.Yellow)
                    Spacer(Modifier.width(8.dp))
                    Text("Vyčistiť všetky transakcie?")
                }
            },
            text = {
                Text("Naozaj chcete zmazať všetky transakcie? Túto akciu nie je možné vrátiť späť.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        transactionViewModel.deleteAllTransactions()
                        showClearAllDialog = false
                    }
                ) {
                    Text("Áno, zmazať všetko")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearAllDialog = false }
                ) {
                    Text("Zrušiť")
                }
            }
        )
    }
}

/**
 * Obrazovka pre pridanie novej transakcie.
 * Obsahuje formulár na zadanie detailov transakcie a ukladá stav formulára vo ViewModel.
 *
 * @param transactionViewModel ViewModel pre správu dát transakcií.
 * @param onTransactionAdded Lambda funkcia, ktorá sa zavolá po úspešnom pridaní transakcie a navigácii späť.
 * @param onNavigateBack Lambda funkcia pre navigáciu späť na predchádzajúcu obrazovku.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    transactionViewModel: TransactionViewModel = viewModel(),
    onTransactionAdded: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val description by transactionViewModel.description.collectAsState()
    val amountText by transactionViewModel.amountText.collectAsState()
    val transactionType by transactionViewModel.transactionType.collectAsState()
    val selectedCategory by transactionViewModel.selectedCategory.collectAsState()
    val note by transactionViewModel.note.collectAsState()
    val selectedDateTimestamp by transactionViewModel.selectedDateTimestamp.collectAsState()

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var expandedCategoryMenu by rememberSaveable { mutableStateOf(false) }

    var descriptionError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    val expenseCategories = listOf("Potraviny", "Doprava", "Bývanie", "Zábava", "Ostatné")
    val incomeCategories = listOf("Mzda", "Dar", "Ostatné")
    val availableCategories = if (transactionType == TransactionType.INCOME) incomeCategories else expenseCategories

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTimestamp
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        transactionViewModel.setSelectedDateTimestamp(it)
                        showDatePicker = false
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Zrušiť")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pridať novú transakciu") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zrušiť")
                    }
                },
                actions = {
                    IconButton(onClick = {

                        descriptionError = if (description.isBlank()) "Popis nemôže byť prázdny" else null
                        val amount = amountText.toDoubleOrNull()
                        amountError = if (amount == null || amount <= 0) "Zadajte platnú kladnú sumu" else null
                        categoryError = if (selectedCategory.isBlank()) "Vyberte kategóriu" else null

                        if (descriptionError == null && amountError == null && categoryError == null && amount != null && amount > 0) {
                            transactionViewModel.addTransactionFromForm()
                            onTransactionAdded()
                        }
                    }) {
                        Icon(Icons.Default.Create , contentDescription = "Uložiť transakciu")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { transactionViewModel.setDescription(it); descriptionError = null },
                label = { Text("Popis (povinné)") },
                modifier = Modifier.fillMaxWidth(),
                isError = descriptionError != null,
                singleLine = true
            )
            if (descriptionError != null) {
                Text(descriptionError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { transactionViewModel.setAmountText(it); amountError = null },
                label = { Text("Suma (€) (povinné)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = amountError != null,
                singleLine = true
            )
            if (amountError != null) {
                Text(amountError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box {
                val interactionSource = remember { MutableInteractionSource() }
                val isClicked by interactionSource.collectIsPressedAsState()

                OutlinedTextField(
                    value = selectedCategory.ifEmpty { "Vyberte kategóriu" },
                    onValueChange = { /* Not used for direct input */ },
                    label = { Text("Kategória (povinné)") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = interactionSource, indication = null) { /* Handled by LaunchedEffect */ },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Rozbaliť kategórie")
                    },
                    isError = categoryError != null,
                    interactionSource = interactionSource
                )

                LaunchedEffect(isClicked) {
                    if (isClicked) {
                        expandedCategoryMenu = true
                    }
                }

                DropdownMenu(
                    expanded = expandedCategoryMenu,
                    onDismissRequest = { expandedCategoryMenu = false }
                ) {
                    availableCategories.forEach { categoryOption ->
                        DropdownMenuItem(
                            text = { Text(categoryOption) },
                            onClick = {
                                transactionViewModel.setSelectedCategory(categoryOption)
                                expandedCategoryMenu = false
                                categoryError = null
                            }
                        )
                    }
                }
            }
            if (categoryError != null) {
                Text(categoryError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dátum: ${dateFormatter.format(java.util.Date(selectedDateTimestamp))}", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Default.DateRange, contentDescription = "Vybrať dátum")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note ?: "",
                onValueChange = { transactionViewModel.setNote(it) },
                label = { Text("Poznámka (voliteľné)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Typ: ", style = MaterialTheme.typography.bodyMedium)
                RadioButton(
                    selected = transactionType == TransactionType.INCOME,
                    onClick = {
                        transactionViewModel.setTransactionType(TransactionType.INCOME)
                        categoryError = null
                    }
                )
                Text("Príjem", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = transactionType == TransactionType.EXPENSE,
                    onClick = {
                        transactionViewModel.setTransactionType(TransactionType.EXPENSE)
                        categoryError = null
                    }
                )
                Text("Výdaj", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * Obrazovka zobrazujúca štatistiky transakcií.
 * Obsahuje graf a súhrn príjmov/výdavkov za zvolené obdobie.
 *
 * @param selectedPeriod Aktuálne vybrané časové obdobie pre štatistiky.
 * @param totalIncome Celkový príjem za zvolené obdobie.
 * @param totalExpense Celkové výdavky za zvolené obdobie.
 * @param transactionsForPeriod Zoznam transakcií v rámci zvoleného obdobia.
 * @param onPeriodSelected Lambda funkcia na zmenu zvoleného obdobia.
 * @param onNavigateBack Lambda funkcia pre navigáciu späť.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    selectedPeriod: TransactionViewModel.StatisticsPeriod,
    totalIncome: Double,
    totalExpense: Double,
    transactionsForPeriod: List<Transaction>,
    onPeriodSelected: (TransactionViewModel.StatisticsPeriod) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Štatistiky") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Späť")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Graf transakcií", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(16.dp))

            /**
             * Composable pre zobrazenie stĺpcového grafu transakcií.
             * Potrebuje transakcie a vybrané obdobie na správne vykreslenie dát.
             */
            TransactionBarChart(
                transactions = transactionsForPeriod,
                selectedPeriod = selectedPeriod,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onPeriodSelected(TransactionViewModel.StatisticsPeriod.LAST_MONTH) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPeriod == TransactionViewModel.StatisticsPeriod.LAST_MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("1 Mesiac")
                    }
                    Button(
                        onClick = { onPeriodSelected(TransactionViewModel.StatisticsPeriod.LAST_6_MONTHS) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPeriod == TransactionViewModel.StatisticsPeriod.LAST_6_MONTHS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("6 Mesiacov")
                    }
                    Button(
                        onClick = { onPeriodSelected(TransactionViewModel.StatisticsPeriod.LAST_YEAR) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedPeriod == TransactionViewModel.StatisticsPeriod.LAST_YEAR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("1 Rok")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Súhrn za zvolené obdobie:", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Celkové príjmy:", style = MaterialTheme.typography.bodyLarge)
                        Text("${"%.2f".format(totalIncome)} €", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF008000))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Celkové výdavky:", style = MaterialTheme.typography.bodyLarge)
                        // Zobrazujeme absolútnu hodnotu výdavkov pre súhrn
                        Text("${"%.2f".format(abs(totalExpense))} €", style = MaterialTheme.typography.bodyLarge, color = Color.Red)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Zostatok za obdobie:", style = MaterialTheme.typography.bodyLarge)
                        val periodBalance = totalIncome + totalExpense
                        Text(
                            text = "${"%.2f".format(periodBalance)} €",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (periodBalance >= 0) Color(0xFF008000) else Color.Red
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Transakcie v období", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(8.dp))
            /**
             * Zobrazuje zoznam transakcií pre aktuálne vybrané obdobie.
             * Mazanie jednotlivých položiek nie je na tejto obrazovke povolené.
             */
            TransactionList(
                transactions = transactionsForPeriod,
                onDeleteTransaction = { },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Composable funkcia na vykreslenie stĺpcového grafu príjmov a výdavkov.
 * Používa externú knižnicu MPAndroidChart. Funkcionalita bola programovaná podľa inštrukcií
 * z dokumentácie MPAndroidChart s využitím AI kvôli technickejšej zložitosti a aj
 * pri debugovaní z dôvodu neočakávaného správania.
 *
 * @param transactions Zoznam transakcií na vykreslenie.
 * @param selectedPeriod Vybrané obdobie, ovplyvňuje formátovanie osi X.
 * @param modifier Modifikátor pre Composable.
 */
@Composable
fun TransactionBarChart(
    transactions: List<Transaction>,
    selectedPeriod: TransactionViewModel.StatisticsPeriod,
    modifier: Modifier = Modifier
) {
    val numberOfMonths = when (selectedPeriod) {
        TransactionViewModel.StatisticsPeriod.LAST_MONTH -> 1
        TransactionViewModel.StatisticsPeriod.LAST_6_MONTHS -> 6
        TransactionViewModel.StatisticsPeriod.LAST_YEAR -> 12
    }

    val calendar = Calendar.getInstance()
    val endTime = calendar.timeInMillis

    val monthlyData = mutableMapOf<String, Pair<Float, Float>>()
    val monthLabels = mutableListOf<String>()
    val monthKeysInOrder = mutableListOf<String>()

    val monthYearKeyFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    val monthLabelFormat = SimpleDateFormat("MMM yy", Locale.getDefault())

    val startOfPeriodCalendar = Calendar.getInstance()
    startOfPeriodCalendar.timeInMillis = endTime
    startOfPeriodCalendar.set(Calendar.DAY_OF_MONTH, 1)
    startOfPeriodCalendar.add(Calendar.MONTH, -(numberOfMonths - 1))

    val iterCalendar = startOfPeriodCalendar.clone() as Calendar
    repeat(numberOfMonths) {
        val monthKey = monthYearKeyFormat.format(iterCalendar.time)
        monthKeysInOrder.add(monthKey)
        monthLabels.add(monthLabelFormat.format(iterCalendar.time))
        monthlyData[monthKey] = Pair(0f, 0f)
        iterCalendar.add(Calendar.MONTH, 1)
    }

    val startTime = startOfPeriodCalendar.timeInMillis
    val endTimeCorrected = Calendar.getInstance().timeInMillis // Používame aktuálny čas konca

    val relevantTransactions = transactions.filter {
        it.timestamp in startTime..endTimeCorrected
    }

    relevantTransactions.forEach { transaction ->
        calendar.timeInMillis = transaction.timestamp
        val transactionMonthKey = monthYearKeyFormat.format(calendar.time)
        if (monthlyData.containsKey(transactionMonthKey)) {
            val (currentIncome, currentExpense) = monthlyData[transactionMonthKey]!!
            if (transaction.type == TransactionType.INCOME) {
                monthlyData[transactionMonthKey] = Pair(currentIncome + transaction.amount.toFloat(), currentExpense)
            } else {
                // Ukladáme absolútnu hodnotu výdavkov pre zobrazenie na grafe
                monthlyData[transactionMonthKey] = Pair(currentIncome, currentExpense + abs(transaction.amount.toFloat()))
            }
        }
    }

    val incomeEntries = mutableListOf<BarEntry>()
    val expenseEntries = mutableListOf<BarEntry>()
    monthKeysInOrder.forEachIndexed { index, monthKey ->
        val (income, expense) = monthlyData[monthKey]!!
        incomeEntries.add(BarEntry(index.toFloat(), income))
        expenseEntries.add(BarEntry(index.toFloat(), expense))
    }

    val groupCount = monthKeysInOrder.size
    val barWidth = 0.35f
    val barSpace = 0.05f
    val groupSpace = 1f - (barWidth + barSpace) * 2

    AndroidView(
        modifier = modifier,
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setPinchZoom(false)
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)
                setFitBars(false)
                extraBottomOffset = 10f

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    labelCount = groupCount
                    setCenterAxisLabels(true)
                    setDrawLabels(true)
                    textColor = AndroidColor.BLACK
                    labelRotationAngle = if (numberOfMonths > 6) 45f else 0f
                    setAvoidFirstLastClipping(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < monthLabels.size) {
                                monthLabels[index]
                            } else {
                                ""
                            }
                        }
                    }
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    granularity = 1f
                    axisMinimum = 0f
                    textColor = AndroidColor.BLACK
                    axisLineColor = AndroidColor.BLACK
                    zeroLineColor = AndroidColor.GRAY
                    setDrawZeroLine(true)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return "%.2f €".format(value)
                        }
                    }
                }
                axisRight.isEnabled = false
                legend.apply {
                    verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.LEFT
                    orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                    form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
                    textSize = 12f
                    textColor = AndroidColor.BLACK
                }
                animateY(1000)
            }
        },
        update = { barChart ->
            if (incomeEntries.isNotEmpty() || expenseEntries.isNotEmpty()) {
                val incomeDataSet = BarDataSet(incomeEntries, "Príjmy").apply {
                    color = AndroidColor.GREEN
                    valueTextColor = AndroidColor.BLACK
                    valueTextSize = 10f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            // Nezobrazovať nulové hodnoty na grafe
                            return if (kotlin.math.abs(value) < 0.01f) "" else "%.0f €".format(value)
                        }
                    }
                }
                val expenseDataSet = BarDataSet(expenseEntries, "Výdavky").apply {
                    color = AndroidColor.RED
                    valueTextColor = AndroidColor.BLACK
                    valueTextSize = 10f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            // Nezobrazovať nulové hodnoty na grafe
                            return if (kotlin.math.abs(value) < 0.01f) "" else "%.0f €".format(value)
                        }
                    }
                }
                val barData = BarData(incomeDataSet, expenseDataSet)
                barData.barWidth = barWidth

                barData.groupBars(0f, groupSpace, barSpace)

                barChart.xAxis.axisMinimum = 0f
                barChart.xAxis.axisMaximum = 0f + barData.getGroupWidth(groupSpace, barSpace) * groupCount
                barChart.xAxis.labelCount = groupCount

                barChart.data = barData

                barChart.setVisibleXRangeMaximum(groupCount.toFloat())
                barChart.moveViewToX(0f)

                barChart.notifyDataSetChanged()
                barChart.invalidate()
            } else {
                barChart.clear()
                barChart.setNoDataText("Žiadne dáta pre graf za zvolené obdobie")
                barChart.invalidate()
            }
        }
    )
}

/**
 * Composable funkcia zobrazujúca zoznam transakcií v LazyColumn.
 *
 * @param transactions Zoznam transakcií na zobrazenie.
 * @param onDeleteTransaction Lambda funkcia volaná pri požiadavke na zmazanie transakcie.
 * @param modifier Modifikátor pre Composable.
 */
@Composable
fun TransactionList(
    transactions: List<Transaction>,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        Text(
            "Zatiaľ žiadne transakcie. Kliknite na '+' pre pridanie.",
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    } else {
        LazyColumn(modifier = modifier) {
            items(transactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onDeleteClick = { onDeleteTransaction(transaction) }
                )
                Divider()
            }
        }
    }
}

/**
 * Composable funkcia na zobrazenie jedného riadku transakcie v zozname.
 * Obsahuje detaily transakcie a tlačidlo na jej zmazanie.
 *
 * @param transaction Transakcia na zobrazenie.
 * @param onDeleteClick Lambda funkcia volaná pri kliknutí na ikonu zmazania.
 * @param modifier Modifikátor pre Composable.
 */
@Composable
fun TransactionRow(
    transaction: Transaction,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Text(transaction.description, style = MaterialTheme.typography.bodyLarge)
            Text("Kategória: ${transaction.category}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            val date = java.util.Date(transaction.timestamp)
            val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            Text("Dňa: ${format.format(date)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (!transaction.note.isNullOrBlank()) {
                Text("Pozn.: ${transaction.note}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Zmazať transakciu",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }

        Text(
            text = "${"%.2f".format(transaction.amount)} €",
            color = if (transaction.type == TransactionType.INCOME) Color(0xFF008000) else Color.Red,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


