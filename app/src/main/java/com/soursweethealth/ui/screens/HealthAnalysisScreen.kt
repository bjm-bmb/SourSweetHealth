package com.soursweethealth.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.soursweethealth.ui.MainViewModel
import com.soursweethealth.ui.components.*
import com.soursweethealth.ui.theme.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAnalysisScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()
    val aiPrivacyAccepted by viewModel.aiPrivacyAccepted.collectAsState()
    var showPrivacyDialog by remember { mutableStateOf(false) }

    if (showPrivacyDialog) {
        AiPrivacyDialog(
            onAccept = {
                viewModel.setAiPrivacyAccepted(true)
                showPrivacyDialog = false
            },
            onDismiss = { showPrivacyDialog = false }
        )
    }

    // Auto-start analysis once when screen opens
    LaunchedEffect(aiPrivacyAccepted) {
        if (!aiPrivacyAccepted) return@LaunchedEffect
        try {
            val user = viewModel.currentUser.filterNotNull().first()
            if (viewModel.analysisResult.value == null && !viewModel.isAnalyzing.value) {
                viewModel.analyzeHealth(user)
            }
        } catch (_: Exception) { }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearAnalysis() }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "健康分析",
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser == null) {
                Text("请先添加用户", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            } else if (apiKey.isBlank()) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(Spacing.pagePadding)) {
                        Text("⚠️ 未配置大模型", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Text(
                            "请先在主页左上角设置中配置大模型API，才能使用健康分析功能。",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else if (!aiPrivacyAccepted) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(Spacing.pagePadding)) {
                        Text("分析前请确认数据处理方式", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(Spacing.sm))
                        Text("健康数据会发送至您配置的第三方模型服务商。", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Spacing.md))
                        PrimaryButton(text = "阅读并确认", onClick = { showPrivacyDialog = true })
                    }
                }
            } else if (analysisResult != null) {
                // Streaming: show result as it arrives; inline progress while still analyzing
                AppCard {
                    if (isAnalyzing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            AppLoadingBar(modifier = Modifier.weight(1f))
                            Text(
                                "正在输出...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(Spacing.sm))
                    }
                    Text(parseBoldText(analysisResult ?: ""), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        "AI 内容仅供健康信息参考，不能替代医生诊断。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isAnalyzing) {
                    Spacer(Modifier.height(Spacing.lg))
                    SecondaryButton(text = "重新分析", onClick = { currentUser?.let { viewModel.analyzeHealth(it) } })
                }
            } else if (isAnalyzing) {
                Spacer(Modifier.height(Spacing.xxxl))
                AppLoadingBar()
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "正在分析您的健康数据...",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Spacer(Modifier.height(Spacing.xxxl))
                AppLoadingBar()
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    "正在准备分析...",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
