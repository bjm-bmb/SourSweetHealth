package com.soursweethealth.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soursweethealth.ui.MainViewModel

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
                        "AI 分析说明：支持任何兼容 OpenAI 接口的大模型，包括硅基流动、DeepSeek、通义千问、OpenAI 等，在下方填入您的 API 地址、Key 和模型名称即可启用。",
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

            Text(
                "支持任何兼容 OpenAI 接口规范的大模型服务，如硅基流动、DeepSeek、通义千问等，填入对应的 API 地址、Key 和模型名称即可。",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                label = { Text("API 地址") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://api.siliconflow.cn/v1/chat/completions") }
            )

            OutlinedTextField(
                value = keyText,
                onValueChange = { keyText = it },
                label = { Text("API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("sk-...") }
            )

            OutlinedTextField(
                value = modelText,
                onValueChange = { modelText = it },
                label = { Text("模型名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pro/THUDM/glm-4-9b-chat") }
            )

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
