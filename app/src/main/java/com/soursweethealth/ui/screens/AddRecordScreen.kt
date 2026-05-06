package com.soursweethealth.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soursweethealth.data.HealthUtils
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedType by remember { mutableStateOf("blood_sugar") }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var valueText by remember { mutableStateOf("") }
    var valueError by remember { mutableStateOf(false) }
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
                        val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        dateText = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
            colors = DatePickerDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.White),
            tonalElevation = 0.dp
        ) { DatePicker(state = datePickerState, colors = DatePickerDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.White)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type selector
            Text("记录类别", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeChip("血糖", selectedType == "blood_sugar") { selectedType = "blood_sugar" }
                TypeChip("尿酸", selectedType == "uric_acid") { selectedType = "uric_acid" }
            }

            // Date
            Text("日期", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            OutlinedTextField(
                value = dateText, onValueChange = { dateText = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                    }
                },
                placeholder = { Text("yyyy-MM-dd") }
            )

            // Value
            val typeName = HealthUtils.getTypeName(selectedType)
            val unit = HealthUtils.getUnit(selectedType)
            Text("${typeName}数值（$unit）", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            OutlinedTextField(
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
                    valueError = false
                },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (selectedType == "uric_acid") KeyboardType.Number else KeyboardType.Decimal),
                placeholder = { Text("请输入${typeName}数值") },
                isError = valueError,
                supportingText = if (valueError) ({ Text("请输入${typeName}数值", color = MaterialTheme.colorScheme.error) }) else null
            )

            // Measure time
            Text("测量时间", fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                measureTimeOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedMeasureTime = option }.fillMaxWidth().height(40.dp)
                    ) {
                        RadioButton(selected = selectedMeasureTime == option, onClick = { selectedMeasureTime = option })
                        Spacer(Modifier.width(4.dp))
                        Text(option, fontSize = 14.sp)
                    }
                }
            }

            // Buttons
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("取消", fontSize = 17.sp) }
                Button(
                    onClick = {
                        val value = valueText.toDoubleOrNull()
                        val userId = currentUser?.id
                        if (value == null || valueText.isBlank()) {
                            valueError = true
                        } else if (userId != null && dateText.isNotBlank()) {
                            viewModel.addRecord(userId, selectedType, value, dateText, selectedMeasureTime)
                            onBack()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)
                ) { Text("保存", fontSize = 17.sp) }
            }
        }
    }
}
