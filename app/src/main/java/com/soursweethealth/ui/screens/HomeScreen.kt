package com.soursweethealth.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.soursweethealth.data.*
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.components.*
import com.soursweethealth.ui.theme.*
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 头像色板：品牌绿与中性深色的组合（全部为深色，保证白字对比度 ≥ 4.5:1）
val avatarColors = listOf(
    Accent, AccentContainerDark, NeutralText, NeutralTextSecondary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavController) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()

    var showUserSwitchMenu by remember { mutableStateOf(false) }
    var showUserManageDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<User?>(null) }
    var pendingCsv by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = pendingCsv
        if (uri != null && csv != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                    writer.write('﻿'.toString())
                    writer.write(csv)
                } ?: error("无法打开文件")
            }.onSuccess {
                Toast.makeText(context, "记录已导出", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "导出失败：${it.message.orEmpty()}", Toast.LENGTH_LONG).show()
            }
        }
        pendingCsv = null
    }

    // Dialogs
    if (showUserManageDialog) {
        AlertDialog(
            onDismissRequest = { showUserManageDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("用户管理") },
            text = {
                Column {
                    TextButton(onClick = {
                        showUserManageDialog = false
                        showAddUserDialog = true
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Spacing.sm))
                        Text("新增用户")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                    users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                                    .background(avatarColors[user.avatarColor % avatarColors.size]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    user.name.firstOrNull()?.toString() ?: "?",
                                    color = OnAccent,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(Modifier.width(Spacing.sm))
                            Text(user.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = {
                                showUserManageDialog = false
                                showEditUserDialog = user
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = {
                                showUserManageDialog = false
                                showDeleteConfirm = user
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserManageDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showAddUserDialog) {
        UserFormDialog(
            title = "新增用户",
            onDismiss = { showAddUserDialog = false },
            onConfirm = { name, gender, year, month, colorIdx ->
                viewModel.addUser(name, gender, year, month, colorIdx)
                showAddUserDialog = false
            }
        )
    }

    showEditUserDialog?.let { user ->
        UserFormDialog(
            title = "编辑用户",
            initialName = user.name,
            initialGender = user.gender,
            initialBirthYear = user.birthYear.toString(),
            initialBirthMonth = user.birthMonth.toString(),
            initialColor = user.avatarColor,
            onDismiss = { showEditUserDialog = null },
            onConfirm = { name, gender, year, month, colorIdx ->
                viewModel.updateUser(user.copy(name = name, gender = gender, birthYear = year, birthMonth = month, avatarColor = colorIdx))
                showEditUserDialog = null
            }
        )
    }

    showDeleteConfirm?.let { user ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("确认删除") },
            text = { Text("确定要删除用户「${user.name}」吗？\n\n⚠️ 删除后所有数据将不可恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUser(user)
                    showDeleteConfirm = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentUser != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.pagePadding, vertical = Spacing.sm)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Button(
                                onClick = { navController.navigate("add_record") },
                                modifier = Modifier.weight(1f).height(Spacing.buttonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text("添加记录", style = MaterialTheme.typography.bodyLarge)
                            }
                            OutlinedButton(
                                onClick = { navController.navigate("health_analysis") },
                                modifier = Modifier.weight(1f).height(Spacing.buttonHeight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(Spacing.xs))
                                Text("健康分析", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.pagePadding, vertical = Spacing.xs)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = { navController.navigate("llm_settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "大模型设置", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    currentUser?.let { user ->
                        IconButton(onClick = {
                            viewModel.exportCsv(user) { csv ->
                                pendingCsv = csv
                                exportLauncher.launch("${user.name}-健康记录.csv")
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "导出健康记录", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showUserManageDialog = true }) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = "管理家庭成员", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                    val color = currentUser?.let { avatarColors[it.avatarColor % avatarColors.size] } ?: MaterialTheme.colorScheme.outline
                    val initial = currentUser?.name?.firstOrNull()?.toString() ?: "?"
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(color)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showUserSwitchMenu = true },
                                    onLongPress = { showUserManageDialog = true }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, color = OnAccent, style = MaterialTheme.typography.bodyLarge)
                    }
                    DropdownMenu(
                        expanded = showUserSwitchMenu,
                        onDismissRequest = { showUserSwitchMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                modifier = Modifier.height(48.dp),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                                .background(avatarColors[user.avatarColor % avatarColors.size]),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                user.name.firstOrNull()?.toString() ?: "?",
                                                color = OnAccent,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(Modifier.width(Spacing.sm))
                                        Text(user.name, style = MaterialTheme.typography.bodyLarge)
                                        if (user.id == currentUser?.id) {
                                            Spacer(Modifier.width(Spacing.xs))
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = { viewModel.switchUser(user.id); showUserSwitchMenu = false }
                            )
                        }
                    }
                }
                }
            }

            if (currentUser == null) {
                Spacer(Modifier.height(Spacing.xxxl))
                Text("欢迎使用酸甜知己", style = MaterialTheme.typography.headlineSmall)
                AppEmptyState(
                    message = "添加第一位家庭成员，开始记录血糖与尿酸。",
                    actionText = "添加用户",
                    onAction = { showAddUserDialog = true }
                )
            } else {
                val user = currentUser!!
                Text("你好，${user.name}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Spacing.xxl))
                LatestRecordsSection(viewModel, user)
                Spacer(Modifier.height(Spacing.xxl))
                Box(modifier = Modifier.weight(1f)) {
                    TrendSection(viewModel, user)
                }
            }
        }
    }
}

@Composable
fun LatestRecordsSection(viewModel: MainViewModel, user: User) {
    val latestBS by viewModel.latestBloodSugar(user.id).collectAsState(initial = null)
    val latestUA by viewModel.latestUricAcid(user.id).collectAsState(initial = null)
    val latestDate = listOfNotNull(latestBS?.date, latestUA?.date).maxOrNull()
    var showAdviceDialog by remember { mutableStateOf(false) }
    var adviceType by remember { mutableStateOf("") }

    val age = HealthValidation.age(user.birthYear, user.birthMonth)
    val aiPrivacyAccepted by viewModel.aiPrivacyAccepted.collectAsState()
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var pendingAdviceType by remember { mutableStateOf<String?>(null) }

    fun startAdvice(type: String) {
        adviceType = type
        showAdviceDialog = true
        val record = if (type == "blood_sugar") latestBS else latestUA
        record?.let { viewModel.getQuickAdvice(type, it.value, user.gender, age, it.measureTime) }
    }

    if (showPrivacyDialog) {
        AiPrivacyDialog(
            onAccept = {
                showPrivacyDialog = false
                viewModel.setAiPrivacyAccepted(true) {
                    pendingAdviceType?.let { startAdvice(it) }
                    pendingAdviceType = null
                }
            },
            onDismiss = { showPrivacyDialog = false; pendingAdviceType = null }
        )
    }

    // Check for abnormal values
    val bsWarning = latestBS?.let { HealthUtils.getWarningText("blood_sugar", it.value, user.gender, it.measureTime) }
    val uaWarning = latestUA?.let { HealthUtils.getWarningText("uric_acid", it.value, user.gender, it.measureTime) }

    if (showAdviceDialog) {
        val isAdvising by viewModel.isQuickAdvising.collectAsState()
        val adviceResult by viewModel.quickAdviceResult.collectAsState()

        AlertDialog(
            onDismissRequest = {
                showAdviceDialog = false
                viewModel.clearQuickAdvice()
            },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    if (adviceType == "blood_sugar") "💡 血糖建议" else "💡 尿酸建议",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                if (isAdvising && adviceResult == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("正在获取建议...", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Column {
                        Text(parseBoldText(adviceResult ?: "暂无建议"), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Spacing.md))
                        Text(
                            "AI 内容仅供健康信息参考，不能替代医生诊断。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAdviceDialog = false
                    viewModel.clearQuickAdvice()
                }) { Text("知道了") }
            }
        )
    }

    Text(
        if (latestDate != null) "最新记录：$latestDate" else "暂无记录",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(Spacing.sm))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        HealthCard(Modifier.weight(1f).fillMaxHeight(), "血糖", latestBS, "blood_sugar", user.gender, bsWarning) {
            if (aiPrivacyAccepted) startAdvice("blood_sugar") else {
                pendingAdviceType = "blood_sugar"; showPrivacyDialog = true
            }
        }
        HealthCard(Modifier.weight(1f).fillMaxHeight(), "尿酸", latestUA, "uric_acid", user.gender, uaWarning) {
            if (aiPrivacyAccepted) startAdvice("uric_acid") else {
                pendingAdviceType = "uric_acid"; showPrivacyDialog = true
            }
        }
    }
}

@Composable
fun HealthCard(modifier: Modifier, title: String, record: HealthRecord?, type: String, gender: String, warning: String? = null, onWarningClick: () -> Unit = {}) {
    val levels = LocalHealthLevels.current
    val level = record?.let { HealthUtils.getLevel(type, it.value, gender, it.measureTime) }
    val levelColor = when (level) {
        HealthLevel.VERY_LOW -> levels.critical; HealthLevel.LOW -> levels.low
        HealthLevel.NORMAL -> levels.normal; HealthLevel.HIGH -> levels.high
        HealthLevel.VERY_HIGH -> levels.critical; null -> MaterialTheme.colorScheme.outline
    }
    val levelText = when (level) {
        HealthLevel.VERY_LOW -> "过低⚠️"; HealthLevel.LOW -> "偏低"
        HealthLevel.NORMAL -> "正常"; HealthLevel.HIGH -> "偏高"; HealthLevel.VERY_HIGH -> "偏高⚠️"; null -> "--"
    }
    val icon = if (type == "blood_sugar") Icons.Default.WaterDrop else Icons.Default.Science

    AppCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row: icon + title + badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = levelColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(icon, contentDescription = null, tint = levelColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (warning != null) {
                    Surface(
                        onClick = onWarningClick,
                        shape = MaterialTheme.shapes.small,
                        color = levelColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                        ) {
                            Text(levelText, color = levelColor, style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = levelColor, modifier = Modifier.size(12.dp))
                        }
                    }
                } else {
                    Surface(shape = MaterialTheme.shapes.small, color = levelColor.copy(alpha = 0.12f)) {
                        Text(levelText, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs), color = levelColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            // Value row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    record?.value?.let { if (type == "uric_acid") String.format("%.0f", it) else String.format("%.1f", it) } ?: "--",
                    style = MaterialTheme.typography.displaySmall
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    HealthUtils.getUnit(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
            record?.let {
                val measured = Instant.ofEpochMilli(it.measuredAt).atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                Text("$measured · ${it.measureTime}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun UserFormDialog(
    title: String,
    initialName: String = "",
    initialGender: String = "男",
    initialBirthYear: String = LocalDate.now().year.toString(),
    initialBirthMonth: String = "1",
    initialColor: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, Int, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(initialGender) }
    var birthYear by remember { mutableStateOf(initialBirthYear) }
    var birthMonth by remember { mutableStateOf(initialBirthMonth) }
    var selectedColor by remember { mutableIntStateOf(initialColor) }
    var formError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("性别：")
                    listOf("男", "女").forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = g }.padding(horizontal = Spacing.sm)) {
                            RadioButton(selected = gender == g, onClick = { gender = g })
                            Text(g)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(value = birthYear, onValueChange = { birthYear = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("出生年") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = birthMonth, onValueChange = { birthMonth = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("月") }, singleLine = true, modifier = Modifier.weight(0.6f))
                }
                formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text("头像颜色：", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    avatarColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                                .then(if (idx == selectedColor) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { selectedColor = idx },
                            contentAlignment = Alignment.Center
                        ) { if (idx == selectedColor) Text("✓", color = OnAccent, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val year = birthYear.toIntOrNull()
                val month = birthMonth.toIntOrNull()
                val error = HealthValidation.validateUser(name, year, month)
                if (error == null) onConfirm(name.trim(), gender, year!!, month!!, selectedColor) else formError = error
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
