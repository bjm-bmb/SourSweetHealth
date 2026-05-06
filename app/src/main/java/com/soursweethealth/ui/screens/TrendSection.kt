package com.soursweethealth.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import com.soursweethealth.data.*
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.theme.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendSection(viewModel: MainViewModel, user: User) {
    var selectedType by remember { mutableStateOf("blood_sugar") }
    var selectedRange by remember { mutableStateOf("30d") }
    var showCustomPickers by remember { mutableStateOf(true) }

    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Custom date range state
    var customStartDate by remember { mutableStateOf(today.minusDays(6)) }
    var customEndDate by remember { mutableStateOf(today) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Edit record dialog state
    var editingRecord by remember { mutableStateOf<HealthRecord?>(null) }
    var editValueText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Edit record dialog
    editingRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { editingRecord = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text("修改${HealthUtils.getTypeName(record.type)}记录", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("日期：${record.date}  ${record.measureTime}", fontSize = 14.sp, color = Color(0xFF888888))
                    OutlinedTextField(
                        value = editValueText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) {
                                val parts = filtered.split(".")
                                if (parts.size <= 1 || parts[1].length <= 1) editValueText = filtered
                            }
                        },
                        label = { Text("数值（${HealthUtils.getUnit(record.type)}）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showDeleteConfirm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = HealthRed.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("确定要删除这条记录吗？", fontSize = 14.sp, color = HealthRed)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showDeleteConfirm = false },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("取消", fontSize = 14.sp) }
                                    Button(
                                        onClick = {
                                            viewModel.deleteRecord(record)
                                            showDeleteConfirm = false
                                            editingRecord = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("确认删除", fontSize = 14.sp) }
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = HealthRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("删除此记录", color = HealthRed, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newVal = editValueText.toDoubleOrNull()
                    if (newVal != null) {
                        viewModel.updateRecord(record.copy(value = newVal))
                        editingRecord = null
                    }
                }) { Text("保存", fontSize = 16.sp) }
            },
            dismissButton = {
                TextButton(onClick = { editingRecord = null }) { Text("取消", fontSize = 16.sp) }
            }
        )
    }

    // Date pickers for custom range
    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = customStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        customStartDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showStartPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("取消") } },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            tonalElevation = 0.dp
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.White)) }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = customEndDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        customEndDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showEndPicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("取消") } },
            colors = DatePickerDefaults.colors(containerColor = Color.White),
            tonalElevation = 0.dp
        ) { DatePicker(state = state, colors = DatePickerDefaults.colors(containerColor = Color.White)) }
    }

    val (startDate, endDate) = when (selectedRange) {
        "7d" -> today.minusDays(6) to today
        "30d" -> today.minusDays(29) to today
        "3m" -> today.minusMonths(3) to today
        "custom" -> customStartDate to customEndDate
        else -> today.minusDays(29) to today
    }

    val records by viewModel.getRecordsByDateRange(
        user.id, selectedType,
        startDate.format(dateFormatter),
        endDate.format(dateFormatter)
    ).collectAsState(initial = emptyList())

    val allRecords by viewModel.getRecordsByType(user.id, selectedType).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize()) {
        Text("趋势", color = Color(0xFF999999), fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)) {
                // Type selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TypeChip("血糖", selectedType == "blood_sugar") { selectedType = "blood_sugar" }
                    TypeChip("尿酸", selectedType == "uric_acid") { selectedType = "uric_acid" }
                }

                Spacer(Modifier.height(6.dp))

                // Range selector
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf("7d" to "7天", "30d" to "30天", "3m" to "3个月").forEach { (key, label) ->
                    RangeChip(label, selectedRange == key) {
                        selectedRange = key
                    }
                }
                RangeChip("自定义", selectedRange == "custom") {
                    if (selectedRange == "custom") {
                        showCustomPickers = !showCustomPickers
                    } else {
                        selectedRange = "custom"
                        showCustomPickers = true
                    }
                }
                RangeChip("查看全部", selectedRange == "all") {
                    selectedRange = "all"
                }
            }

            // Custom date range with calendar pickers
            if (selectedRange == "custom" && showCustomPickers) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(customStartDate.format(DateTimeFormatter.ofPattern("MM-dd")), fontSize = 13.sp)
                    }
                    Text("至", fontSize = 14.sp, color = Color(0xFF999999))
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(customEndDate.format(DateTimeFormatter.ofPattern("MM-dd")), fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // Chart area — use filtered records for range chips, allRecords for "查看全部"
            // getRecordsByDateRange returns ASC (oldest→newest) — already correct order for the chart
            // getRecordsByUserAndType returns DESC (newest→oldest) — needs reversing
            val chartRecords = if (selectedRange == "all") allRecords.reversed() else records
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                if (allRecords.isEmpty()) {
                    Text("暂无${HealthUtils.getTypeName(selectedType)}记录", color = Color(0xFFBBBBBB), fontSize = 15.sp)
                } else if (chartRecords.isEmpty()) {
                    Text("该时间段暂无${HealthUtils.getTypeName(selectedType)}记录", color = Color(0xFFBBBBBB), fontSize = 15.sp)
                } else {
                    TrendChart(
                        records = remember(chartRecords) { chartRecords },
                        type = selectedType,
                        gender = user.gender,
                        onEditRecord = { record ->
                            editValueText = if (selectedType == "uric_acid") String.format("%.0f", record.value) else String.format("%.1f", record.value)
                            showDeleteConfirm = false
                            editingRecord = record
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) LightBlue600 else Color(0xFFE0E0E0),
        contentColor = if (selected) Color.White else Color(0xFF222222)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) LightBlue100 else Color.Transparent,
        contentColor = if (selected) LightBlue700 else Color(0xFF444444)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun TrendChart(records: List<HealthRecord>, type: String, gender: String, onEditRecord: (HealthRecord) -> Unit = {}) {
    var selectedPoint by remember(records) { mutableStateOf<Int?>(null) }

    val tooltipRecord = selectedPoint?.let { records.getOrNull(it) }
    val (thresholdNormal, thresholdHigh) = HealthUtils.getThresholds(type, gender)

    // Minimum spacing between points in px
    val minPointSpacing = 60f

    // Track canvas width so we can auto-scroll to the rightmost (most recent) data
    var canvasWidth by remember { mutableStateOf(0f) }
    // scrollOffset is keyed ONLY on records — not on canvasWidth.
    // If canvasWidth were also a key, every onSizeChanged would create a NEW MutableFloatState
    // while the pointerInput coroutine (keyed on records) still holds a reference to the OLD one,
    // causing drag writes to go to the discarded state and appear to have no effect.
    var scrollOffset by remember(records) { mutableFloatStateOf(0f) }

    // Auto-scroll to the rightmost (newest) point once both records and canvas size are known.
    LaunchedEffect(records, canvasWidth) {
        if (canvasWidth > 0f && records.size > 1) {
            val paddingLeft = 52f; val paddingRight = 16f
            val chartWidth = canvasWidth - paddingLeft - paddingRight
            val naturalSpacing = chartWidth / (records.size - 1)
            val useSpacing = maxOf(naturalSpacing, minPointSpacing)
            val totalContentWidth = (records.size - 1) * useSpacing
            scrollOffset = (totalContentWidth - chartWidth).coerceAtLeast(0f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasWidth = it.width.toFloat() }
                .pointerInput(records) {
                    // Single gesture handler: distinguishes tap/longpress from drag
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPos = down.position
                        var isDragging = false
                        var dragStart = downPos
                        val longPressTimeout = 500L
                        val downTime = System.currentTimeMillis()
                        var lastPos = downPos

                        do {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull() ?: break
                            val elapsed = System.currentTimeMillis() - downTime

                            if (!isDragging) {
                                val delta = pointer.position - dragStart
                                // If moved more than 10px, it's a drag
                                if (abs(delta.x) > 10f || abs(delta.y) > 10f) {
                                    isDragging = true
                                }
                            }

                            if (isDragging) {
                                val drag = pointer.position - lastPos
                                pointer.consume()
                                val paddingLeft = 52f
                                val paddingRight = 16f
                                val chartWidth = size.width - paddingLeft - paddingRight
                                val pointCount = records.size
                                val naturalSpacing = if (pointCount <= 1) chartWidth else chartWidth / (pointCount - 1)
                                val useSpacing = maxOf(naturalSpacing, minPointSpacing)
                                val totalContentWidth = if (pointCount <= 1) chartWidth else (pointCount - 1) * useSpacing
                                val maxScroll = (totalContentWidth - chartWidth).coerceAtLeast(0f)
                                scrollOffset = (scrollOffset - drag.x).coerceIn(0f, maxScroll)
                            }

                            lastPos = pointer.position
                        } while (event.changes.any { it.pressed })

                        // Finger lifted — decide tap vs longpress
                        if (!isDragging) {
                            val elapsed = System.currentTimeMillis() - downTime
                            val idx = findNearestPointWithScroll(downPos.x, records.size, size.width.toFloat(), scrollOffset, minPointSpacing)
                            if (elapsed >= longPressTimeout) {
                                records.getOrNull(idx)?.let { onEditRecord(it) }
                            } else {
                                selectedPoint = if (selectedPoint == idx) null else idx
                            }
                        }
                    }
                }
        ) {
            if (records.isEmpty()) return@Canvas

            val paddingLeft = 52f
            val paddingRight = 16f
            val paddingTop = 30f
            val paddingBottom = 36f
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

            val values = records.map { it.value }
            val dataMin = values.minOrNull() ?: 0.0
            val dataMax = values.maxOrNull() ?: 10.0
            // Extend range to include thresholds for proper zone display
            val minVal = minOf(dataMin * 0.85, thresholdNormal * 0.85)
            val maxVal = maxOf(dataMax * 1.1, thresholdHigh * 1.15)
            val range = if (maxVal - minVal < 0.1) 1.0 else maxVal - minVal

            val pointCount = records.size

            // Calculate content width: use minPointSpacing if it's wider than chart
            val naturalSpacing = if (pointCount <= 1) chartWidth else chartWidth / (pointCount - 1)
            val useSpacing = maxOf(naturalSpacing, minPointSpacing)
            val totalContentWidth = if (pointCount <= 1) chartWidth else (pointCount - 1) * useSpacing

            fun xForIndex(i: Int): Float {
                return if (pointCount <= 1) paddingLeft + chartWidth / 2
                else paddingLeft + i * useSpacing - scrollOffset
            }
            fun yForValue(v: Double): Float = paddingTop + ((maxVal - v) / range * chartHeight).toFloat()

            // Draw colored zones (green = normal, yellow = high, red = very high)
            val yNormal = yForValue(thresholdNormal)
            val yHigh = yForValue(thresholdHigh)
            val chartBottom = paddingTop + chartHeight
            val chartTop = paddingTop

            // Clip chart content to chart area
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.clipRect(
                paddingLeft, chartTop, paddingLeft + chartWidth, chartBottom
            )

            // Green zone: from bottom to normal threshold
            if (yNormal < chartBottom) {
                drawRect(
                    color = HealthGreen.copy(alpha = 0.08f),
                    topLeft = Offset(paddingLeft, yNormal),
                    size = Size(chartWidth, chartBottom - yNormal)
                )
            }
            // Yellow zone: from normal to high threshold
            if (yHigh < yNormal) {
                drawRect(
                    color = HealthYellow.copy(alpha = 0.10f),
                    topLeft = Offset(paddingLeft, yHigh),
                    size = Size(chartWidth, yNormal - yHigh)
                )
            }
            // Red zone: from high threshold to top
            if (yHigh > chartTop) {
                drawRect(
                    color = HealthRed.copy(alpha = 0.08f),
                    topLeft = Offset(paddingLeft, chartTop),
                    size = Size(chartWidth, yHigh - chartTop)
                )
            }

            // Draw threshold lines
            val thinDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            drawLine(color = HealthGreen.copy(alpha = 0.5f), start = Offset(paddingLeft, yNormal), end = Offset(paddingLeft + chartWidth, yNormal), strokeWidth = 1f, pathEffect = thinDash)
            drawLine(color = HealthRed.copy(alpha = 0.5f), start = Offset(paddingLeft, yHigh), end = Offset(paddingLeft + chartWidth, yHigh), strokeWidth = 1f, pathEffect = thinDash)

            // Draw grid lines (light)
            val gridPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#EEEEEE"); strokeWidth = 1f }
            for (i in 0..4) {
                val gy = paddingTop + chartHeight * i / 4
                drawContext.canvas.nativeCanvas.drawLine(paddingLeft, gy, paddingLeft + chartWidth, gy, gridPaint)
            }

            // Draw data lines (solid blue)
            val blueLineColor = LightBlue600
            for (i in 0 until pointCount - 1) {
                val x1 = xForIndex(i); val y1 = yForValue(records[i].value)
                val x2 = xForIndex(i + 1); val y2 = yForValue(records[i + 1].value)
                drawLine(color = blueLineColor, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }

            // Restore clip before drawing points (so edge dots aren't clipped)
            drawContext.canvas.nativeCanvas.restore()

            // Draw points (outside clip to prevent edge clipping)
            records.forEachIndexed { i, record ->
                val x = xForIndex(i); val y = yForValue(record.value)
                // Only draw points that are roughly within visible area
                if (x >= paddingLeft - 14f && x <= paddingLeft + chartWidth + 14f) {
                    val level = HealthUtils.getLevel(type, record.value, gender)
                    val color = levelToColor(level)
                    drawCircle(color = color, radius = if (i == selectedPoint) 16f else 12f, center = Offset(x, y))
                    drawCircle(color = Color.White, radius = 5f, center = Offset(x, y))
                }
            }

            // Draw labels (outside clip)
            drawContext.canvas.nativeCanvas.apply {
                // Date labels - show first and last visible points (full date with year)
                val datePaint = android.graphics.Paint().apply { textSize = 36f; this.color = android.graphics.Color.parseColor("#222222"); isAntiAlias = true; isFakeBoldText = true }
                // Find first and last visible point
                val firstVisible = records.indices.firstOrNull { xForIndex(it) >= paddingLeft }
                val lastVisible = records.indices.lastOrNull { xForIndex(it) <= paddingLeft + chartWidth }
                if (firstVisible != null) {
                    datePaint.textAlign = android.graphics.Paint.Align.LEFT
                    drawText(records[firstVisible].date, paddingLeft, size.height - 2f, datePaint)
                }
                if (lastVisible != null && lastVisible != firstVisible) {
                    datePaint.textAlign = android.graphics.Paint.Align.RIGHT
                    drawText(records[lastVisible].date, paddingLeft + chartWidth, size.height - 2f, datePaint)
                }

                // Y-axis labels (bigger text)
                val yPaint = android.graphics.Paint().apply {
                    textSize = 36f; this.color = android.graphics.Color.parseColor("#222222"); isAntiAlias = true; isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
                for (i in 0..4) {
                    val v = minVal + range * (4 - i) / 4
                    val label = if (type == "blood_sugar") String.format("%.1f", v) else String.format("%.0f", v)
                    val gy = paddingTop + chartHeight * i / 4
                    drawText(label, paddingLeft - 6f, gy + 10f, yPaint)
                }
            }
        }

        // Tooltip
        tooltipRecord?.let { record ->
            val level = HealthUtils.getLevel(type, record.value, gender)
            val color = levelToColor(level)
            val unit = HealthUtils.getUnit(type)
            val warning = if (level == HealthLevel.VERY_HIGH) " ⚠️ 数值偏高" else ""

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${record.date} ", fontSize = 13.sp, color = Color(0xFF999999))
                    Text("${if (type == "uric_acid") String.format("%.0f", record.value) else String.format("%.1f", record.value)} $unit", fontWeight = FontWeight.Bold, color = color, fontSize = 15.sp)
                    if (warning.isNotEmpty()) Text(warning, fontSize = 13.sp, color = HealthRed)
                }
            }
        }
    }
}

private fun findNearestPointWithScroll(x: Float, count: Int, width: Float, scrollOffset: Float, minPointSpacing: Float): Int {
    if (count <= 0) return 0
    if (count == 1) return 0
    val paddingLeft = 52f
    val paddingRight = 16f
    val chartWidth = width - paddingLeft - paddingRight
    val naturalSpacing = chartWidth / (count - 1)
    val useSpacing = maxOf(naturalSpacing, minPointSpacing)
    val idx = ((x - paddingLeft + scrollOffset) / useSpacing).roundToInt().coerceIn(0, count - 1)
    return idx
}

private fun levelToColor(level: HealthLevel): Color {
    return when (level) {
        HealthLevel.NORMAL -> HealthGreen
        HealthLevel.HIGH -> HealthYellow
        HealthLevel.VERY_HIGH -> HealthRed
    }
}

/** Parse **bold** markers in [text] into an AnnotatedString with bold spans. */
fun parseBoldText(text: String): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val start = remaining.indexOf("**")
        if (start == -1) { append(remaining); break }
        if (start > 0) append(remaining.substring(0, start))
        remaining = remaining.substring(start + 2)
        val end = remaining.indexOf("**")
        if (end == -1) { append("**"); append(remaining); break }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(remaining.substring(0, end)) }
        remaining = remaining.substring(end + 2)
    }
}
