package com.soursweethealth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.soursweethealth.ui.components.AppCard
import com.soursweethealth.ui.components.PrimaryButton
import com.soursweethealth.ui.components.SecondaryButton
import com.soursweethealth.ui.theme.SourSweetHealthTheme
import com.soursweethealth.ui.theme.Spacing

class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra("trace") ?: "no trace"
        setContent {
            SourSweetHealthTheme {
                CrashScreen(trace) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(trace: String, onRestart: () -> Unit) {
    val ctx = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(Spacing.pagePadding)
                .verticalScroll(rememberScrollState())
        ) {
            Text("应用崩溃报告", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(Spacing.sm))
            Text("请将以下错误信息截图反馈：", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(Spacing.sm))
            AppCard {
                Text(trace, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(text = "复制", onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                })
                PrimaryButton(text = "重启应用", onClick = onRestart)
            }
        }
    }
}