package com.soursweethealth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.components.*
import com.soursweethealth.ui.theme.Spacing

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
    val aiPrivacyAccepted by viewModel.aiPrivacyAccepted.collectAsState()
    var keyVisible by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }

    // Provider dropdown state
    var providerExpanded by remember { mutableStateOf(false) }
    var selectedProvider by remember {
        val matched = providers.indexOfFirst { it.apiUrl.isNotBlank() && apiUrl.contains(it.apiUrl.removePrefix("https://").substringBefore("/")) }
        mutableIntStateOf(if (matched >= 0) matched else providers.lastIndex)
    }
    LaunchedEffect(apiUrl) {
        val matched = providers.indexOfFirst { it.apiUrl.isNotBlank() && apiUrl.contains(it.apiUrl.removePrefix("https://").substringBefore("/")) }
        selectedProvider = if (matched >= 0) matched else providers.lastIndex
    }

    // Model dropdown state
    var modelExpanded by remember { mutableStateOf(false) }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = { Text("关于酸甜知己", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text("酸甜知己 v1.0.1", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "「酸」是尿酸，「甜」是血糖，「知己」是懂你健康的贴心伴侣。专为关注血糖和尿酸的朋友设计，界面简洁、操作方便，让记录数据和了解健康趋势变得轻松愉快。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "主要功能：\n• 📊 血糖与尿酸数据随手记\n• 📈 趋势图表一目了然\n• 🤖 AI 医生朋友帮您分析（需配置大模型）\n• 👨‍👩‍👧‍👦 支持家庭多人管理",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "AI 分析说明：支持任何兼容 OpenAI 接口的大模型，包括硅基流动、DeepSeek、小米等，选择提供商后会自动填充 API 地址。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                AppTextButton(text = "知道了", onClick = { showAbout = false })
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "大模型设置",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.pagePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Text("大模型 API 配置", style = MaterialTheme.typography.titleMedium)

            // Provider selector
            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it }
            ) {
                AppTextField(
                    value = providers[selectedProvider].name,
                    onValueChange = {},
                    readOnly = true,
                    label = "模型提供商",
                    modifier = Modifier.menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    providers.forEachIndexed { index, provider ->
                        DropdownMenuItem(
                            text = { Text(provider.name, style = MaterialTheme.typography.bodyLarge) },
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
            AppTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = "API 地址",
                placeholder = "https://api.deepseek.com",
                supportingText = "可手动修改，系统会自动补全路径"
            )

            // API Key
            AppTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = "API Key",
                placeholder = "sk-...",
                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (keyVisible) "隐藏 API Key" else "显示 API Key"
                        )
                    }
                }
            )

            // Model name with dropdown suggestions
            val currentModels = providers[selectedProvider].models
            if (currentModels.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    AppTextField(
                        value = modelText,
                        onValueChange = { modelText = it },
                        label = "模型名称",
                        modifier = Modifier.menuAnchor(),
                        placeholder = "选择或输入模型名称",
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded) },
                        supportingText = "可从列表选择，也可手动输入"
                    )
                    ExposedDropdownMenu(
                        expanded = modelExpanded,
                        onDismissRequest = { modelExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        currentModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    modelText = model
                                    modelExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                AppTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = "模型名称",
                    placeholder = "输入模型名称"
                )
            }

            PrimaryButton(
                text = "保存",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.saveApiConfig(urlText, keyText, modelText) { error ->
                        if (error == null) onBack() else formError = error
                    }
                }
            )

            formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            if (aiPrivacyAccepted) {
                AppTextButton(text = "撤回 AI 数据处理同意", onClick = { viewModel.setAiPrivacyAccepted(false) })
            }

            OutlinedButton(
                onClick = { showAbout = true },
                modifier = Modifier.fillMaxWidth().height(Spacing.buttonHeight),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Spacing.sm))
                Text("关于酸甜知己", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
