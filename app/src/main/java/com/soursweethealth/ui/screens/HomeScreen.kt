package com.soursweethealth.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.soursweethealth.data.*
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.theme.*
import java.time.LocalDate

val avatarColors = listOf(
    Color(0xFF29B6F6), Color(0xFF66BB6A), Color(0xFFFF7043),
    Color(0xFFAB47BC), Color(0xFFFFCA28), Color(0xFF26A69A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, navController: NavController) {
    val currentUser by viewModel.currentUser.collectAsState()
    val users by viewModel.users.collectAsState()

    var showUserSwitchMenu by remember { mutableStateOf(false) }
    var showUserManageDialog by remember { mutableStateOf(false) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf<User?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<User?>(null) }

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
                        Spacer(Modifier.width(8.dp))
                        Text("新增用户")
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    users.forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp).clip(CircleShape)
                                    .background(avatarColors[user.avatarColor % avatarColors.size]),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.name.first().toString(), color = Color.White, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(user.name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                            IconButton(onClick = {
                                showUserManageDialog = false
                                showEditUserDialog = user
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                            IconButton(onClick = {
                                showUserManageDialog = false
                                showDeleteConfirm = user
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = HealthRed, modifier = Modifier.size(22.dp))
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
                }) { Text("删除", color = HealthRed) }
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
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { navController.navigate("add_record") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加记录", fontSize = 16.sp)
                        }
                        OutlinedButton(
                            onClick = { navController.navigate("health_analysis") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("健康分析", fontSize = 16.sp)
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
                .padding(horizontal = 16.dp, vertical = 2.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.navigate("llm_settings") }) {
                    Icon(Icons.Default.Settings, contentDescription = "大模型设置", tint = MaterialTheme.colorScheme.primary)
                }
                Box {
                    val color = currentUser?.let { avatarColors[it.avatarColor % avatarColors.size] } ?: Color.Gray
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
                        Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    DropdownMenu(
                        expanded = showUserSwitchMenu,
                        onDismissRequest = { showUserSwitchMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        users.forEach { user ->
                            DropdownMenuItem(
                                modifier = Modifier.height(40.dp),
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(24.dp).clip(CircleShape)
                                                .background(avatarColors[user.avatarColor % avatarColors.size]),
                                            contentAlignment = Alignment.Center
                                        ) { Text(user.name.first().toString(), color = Color.White, fontSize = 11.sp) }
                                        Spacer(Modifier.width(8.dp))
                                        Text(user.name, fontSize = 18.sp)
                                        if (user.id == currentUser?.id) {
                                            Spacer(Modifier.width(4.dp))
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

            if (currentUser == null) {
                Spacer(Modifier.height(24.dp))
                Text("欢迎使用酸甜知己", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("请长按右上角头像添加第一个用户", color = Color.Gray, fontSize = 16.sp)
            } else {
                val user = currentUser!!
                Text("你好，${user.name}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Spacer(Modifier.height(6.dp))
                LatestRecordsSection(viewModel, user)
                Spacer(Modifier.height(6.dp))
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

    val age = LocalDate.now().year - user.birthYear

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
                    fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            },
            text = {
                if (isAdvising && adviceResult == null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("正在获取建议...", fontSize = 15.sp)
                    }
                } else {
                    Text(parseBoldText(adviceResult ?: "暂无建议"), fontSize = 17.sp, lineHeight = 26.sp)
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
        color = Color(0xFF999999), fontSize = 14.sp
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HealthCard(Modifier.weight(1f).fillMaxHeight(), "血糖", latestBS, "blood_sugar", user.gender, bsWarning) {
            adviceType = "blood_sugar"
            showAdviceDialog = true
            latestBS?.let { viewModel.getQuickAdvice("blood_sugar", it.value, user.gender, age, it.measureTime) }
        }
        HealthCard(Modifier.weight(1f).fillMaxHeight(), "尿酸", latestUA, "uric_acid", user.gender, uaWarning) {
            adviceType = "uric_acid"
            showAdviceDialog = true
            latestUA?.let { viewModel.getQuickAdvice("uric_acid", it.value, user.gender, age, it.measureTime) }
        }
    }
}

@Composable
fun HealthCard(modifier: Modifier, title: String, record: HealthRecord?, type: String, gender: String, warning: String? = null, onWarningClick: () -> Unit = {}) {
    val level = record?.let { HealthUtils.getLevel(type, it.value, gender, it.measureTime) }
    val levelColor = when (level) {
        HealthLevel.NORMAL -> HealthGreen; HealthLevel.HIGH -> HealthYellow; HealthLevel.VERY_HIGH -> HealthRed; null -> Color(0xFFCCCCCC)
    }
    val levelText = when (level) {
        HealthLevel.NORMAL -> "正常"; HealthLevel.HIGH -> "偏高"; HealthLevel.VERY_HIGH -> "偏高⚠️"; null -> "--"
    }
    val icon = if (type == "blood_sugar") Icons.Default.WaterDrop else Icons.Default.Science

    Card(
        modifier = modifier.shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row: icon + title + badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = levelColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(icon, contentDescription = null, tint = levelColor, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Color(0xFF333333))
                Spacer(Modifier.weight(1f))
                if (warning != null) {
                    Surface(
                        onClick = onWarningClick,
                        shape = RoundedCornerShape(10.dp),
                        color = levelColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(levelText, color = levelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = levelColor, modifier = Modifier.size(13.dp))
                        }
                    }
                } else {
                    Surface(shape = RoundedCornerShape(10.dp), color = levelColor.copy(alpha = 0.12f)) {
                        Text(levelText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = levelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Value row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    record?.value?.let { if (type == "uric_acid") String.format("%.0f", it) else String.format("%.1f", it) } ?: "--",
                    fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    HealthUtils.getUnit(type),
                    fontSize = 15.sp, color = Color(0xFFAAAAAA),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("姓名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("性别：")
                    listOf("男", "女").forEach { g ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { gender = g }.padding(horizontal = 8.dp)) {
                            RadioButton(selected = gender == g, onClick = { gender = g })
                            Text(g)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = birthYear, onValueChange = { birthYear = it.filter { c -> c.isDigit() }.take(4) }, label = { Text("出生年") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = birthMonth, onValueChange = { birthMonth = it.filter { c -> c.isDigit() }.take(2) }, label = { Text("月") }, singleLine = true, modifier = Modifier.weight(0.6f))
                }
                Text("头像颜色：", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    avatarColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                                .then(if (idx == selectedColor) Modifier.border(3.dp, Color.Black, CircleShape) else Modifier)
                                .clickable { selectedColor = idx },
                            contentAlignment = Alignment.Center
                        ) { if (idx == selectedColor) Text("✓", color = Color.White, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name, gender, birthYear.toIntOrNull() ?: 2000, birthMonth.toIntOrNull() ?: 1, selectedColor) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
