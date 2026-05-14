package com.soursweethealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soursweethealth.ui.MainViewModel

// Provider presets
private data class ProviderPreset(
    val name: String,
    val apiUrl: String,
    val models: List<String>
)

private val providers = listOf(
    ProviderPreset(
        name = "硅基流动 (SiliconFlow)",
        apiUrl = "https://api.siliconflow.cn/v1/chat/completions",
        models = listOf("deepseek-ai/DeepSeek-V4-Flash", "Pro/moonshotai/Kimi-K2.6", "Pro/zai-org/GLM-5.1")
    ),
    ProviderPreset(
        name = "DeepSeek",
        apiUrl = "https://api.deepseek.com/chat/completions",
        models = listOf("deepseek-v4-flash", "deepseek-v4-pro")
    ),
    ProviderPreset(
        name = "小米 MiMo (mimo-v2)",
        apiUrl = "https://api.mimo-v2.com/v1",
        models = listOf("mimo-v2.5-pro", "mimo-v2.5")
    ),
    ProviderPreset(
        name = "小米 MiMo (token-plan)",
        apiUrl = "https://token-plan-cn.xiaomimimo.com/v1",
        models = listOf("mimo-v2.5-pro", "mimo-v2.5")
    ),
    ProviderPreset(
        name = "其他 (自定义)",
        apiUrl = "",
        models = emptyList()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmSettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val apiUrl by viewModel.apiUrl.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val modelName by viewModel.modelName.collectAsState()

    var urlText by remember(apiUrl) { mutableStateOf(apiUrl) }
    var keyText by remember(apiKey) { mutableStateOf(apiKey) }
    var modelText by remember(modelName) { mutableStateOf(modelName) }
    var showAbout by remember { mutableStateOf(false) }

    // Provider dropdown state
    var providerExpanded by remember { mutableStateOf(false) }
    var selectedProvider by remember {
        val matched = providers.indexOfFirst { it.apiUrl.isNotBlank() && apiUrl.contains(it.apiUrl.removePrefix("https://").substringBefore("/")) }
        mutableIntStateOf(if (matched >= 0) matched else providers.lastIndex)
    }

    // Model dropdown state
    var modelExpanded by remember { mutableStateOf(false) }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("关于酸甜知己", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("酸甜知己 v1.0", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Text(
                        "「酸」是尿酸，「甜」是血糖，「知己」是懂你健康的贴心伴侣。专为关注血糖和尿酸的朋友设计，界面简洁、操作方便，让记录数据和了解健康趋势变得轻松愉快。",
                        fontSize = 14.sp, lineHeight = 22.sp
                    )
                    Text(
                        "主要功能：\n• 📊 血糖与尿酸数据随手记\n• 📈 趋势图表一目了然\n• 🤖 AI 医生朋友帮您分析（需配置大模型）\n• 👨‍👩‍👧‍👦 支持家庭多人管理",
                        fontSize = 14.sp, lineHeight = 22.sp
                    )
                    Text(
                        "AI 分析说明：支持任何兼容 OpenAI 接口的大模型，包括硅基流动、DeepSeek、小米等，选择提供商后会自动填充 API 地址。",
                        fontSize = 14.sp, lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("知道了") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("大模型设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("大模型 API 配置", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            // Provider selector
            Text("模型提供商", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it }
            ) {
                OutlinedTextField(
                    value = providers[selectedProvider].name,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    providers.forEachIndexed { index, provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name, fontSize = 15.sp) },
                            onClick = {
                                selectedProvider = index
                                providerExpanded = false
                                // Auto-fill URL
                                if (provider.apiUrl.isNotBlank()) {
                                    urlText = provider.apiUrl
                                }
                                // Auto-fill first model if available
                                if (provider.models.isNotEmpty()) {
                                    modelText = provider.models.first()
                                }
                            }
                        )
                    }
                }
            }

            // API URL
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("API 地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.deepseek.com") },
                supportingText = { Text("可手动修改，系统会自动补全路径", fontSize = 12.sp) }
            )

            // API Key
            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...") }
            )

            // Model name with dropdown suggestions
            val currentModels = providers[selectedProvider].models
            Text("模型名称", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            if (currentModels.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        placeholder = { Text("选择或输入模型名称") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        supportingText = { Text("可从列表选择，也可手动输入", fontSize = 12.sp) }
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        currentModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model, fontSize = 14.sp) },
                                onClick = {
                                    modelText = model
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入模型名称") }
                )
            }

            Button(
                onClick = {
                    viewModel.saveApiConfig(urlText, keyText, modelText)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("保存", fontSize = 16.sp)
            }

            OutlinedButton(
                onClick = { showAbout = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("关于酸甜知己", fontSize = 16.sp)
            }
        }
    }
}
