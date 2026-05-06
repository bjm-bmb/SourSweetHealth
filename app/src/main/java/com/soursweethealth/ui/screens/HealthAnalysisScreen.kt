package com.soursweethealth.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soursweethealth.ui.MainViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthAnalysisScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val currentUser by viewModel.currentUser.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisResult by viewModel.analysisResult.collectAsState()

    // Auto-start analysis once when screen opens
    LaunchedEffect(Unit) {
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
            TopAppBar(
                title = { Text("健康分析", fontSize = 20.sp) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentUser == null) {
                Text("请先添加用户", color = MaterialTheme.colorScheme.error, fontSize = 16.sp)
            } else if (apiKey.isBlank()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("⚠️ 未配置大模型", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("请先在主页左上角设置中配置大模型API，才能使用健康分析功能。", fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            } else if (analysisResult != null) {
                // Streaming: show result as it arrives; spinner inline while still analyzing
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (isAnalyzing) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Text("正在输出...", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(parseBoldText(analysisResult ?: ""), lineHeight = 28.sp, fontSize = 17.sp)
                    }
                }
                if (!isAnalyzing) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { currentUser?.let { viewModel.analyzeHealth(it) } },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("重新分析", fontSize = 15.sp)
                    }
                }
            } else if (isAnalyzing) {
                Spacer(Modifier.height(60.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("正在分析您的健康数据...", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            } else {
                Spacer(Modifier.height(60.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("正在准备分析...", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            }
        }
    }
}
