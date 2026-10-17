package com.soursweethealth.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.soursweethealth.data.HealthUtils
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.components.*
import com.soursweethealth.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedType by remember { mutableStateOf("blood_sugar") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var timeText by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var valueText by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }
    var selectedMeasureTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val measureTimeOptions = HealthUtils.getMeasureTimeOptions(selectedType)

    LaunchedEffect(selectedType) {
        selectedMeasureTime = measureTimeOptions.firstOrNull() ?: ""
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        dateText = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            tonalElevation = 0.dp
        ) { DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "添加记录",
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = Spacing.pagePadding, vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Type selector
            Text("记录类别", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                TypeChip("血糖", selectedType == "blood_sugar") { selectedType = "blood_sugar" }
                TypeChip("尿酸", selectedType == "uric_acid") { selectedType = "uric_acid" }
            }

            // Date
            Text("日期", style = MaterialTheme.typography.titleMedium)
            AppTextField(
                value = dateText,
                onValueChange = { dateText = it; formError = null },
                placeholder = "yyyy-MM-dd",
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                    }
                }
            )

            Text("时间", style = MaterialTheme.typography.titleMedium)
            AppTextField(
                value = timeText,
                onValueChange = { input ->
                    timeText = input.filter { it.isDigit() || it == ':' }.take(5)
                    formError = null
                },
                placeholder = "HH:mm",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
            )

            // Value
            val typeName = HealthUtils.getTypeName(selectedType)
            val unit = HealthUtils.getUnit(selectedType)
            Text("${typeName}数值（$unit）", style = MaterialTheme.typography.titleMedium)
            AppTextField(
                value = valueText,
                onValueChange = { input ->
                    if (selectedType == "uric_acid") {
                        // Uric acid: integer only
                        val filtered = input.filter { it.isDigit() }
                        valueText = filtered
                    } else {
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            val parts = filtered.split(".")
                            if (parts.size <= 1 || parts[1].length <= 1) valueText = filtered
                        }
                    }
                    formError = null
                },
                placeholder = "请输入${typeName}数值",
                isError = formError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (selectedType == "uric_acid") KeyboardType.Number else KeyboardType.Decimal
                )
            )

            // Measure time
            Text("测量时间", style = MaterialTheme.typography.titleMedium)
            Column {
                measureTimeOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedMeasureTime = option }
                            .fillMaxWidth().height(Spacing.touchTargetMin)
                    ) {
                        RadioButton(selected = selectedMeasureTime == option, onClick = { selectedMeasureTime = option })
                        Spacer(Modifier.width(Spacing.xs))
                        Text(option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                SecondaryButton(text = "取消", onClick = onBack, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "保存",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val value = valueText.toDoubleOrNull()
                        val userId = currentUser?.id
                        if (userId == null) {
                            formError = "请先选择用户"
                        } else {
                            viewModel.addRecord(userId, selectedType, value, dateText, timeText, selectedMeasureTime) { error ->
                                if (error == null) onBack() else formError = error
                            }
                        }
                    }
                )
            }
        }
    }
}
