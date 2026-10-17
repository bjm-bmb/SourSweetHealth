package com.soursweethealth.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.soursweethealth.ui.components.AppTextButton

@Composable
fun AiPrivacyDialog(onAccept: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 数据处理说明", style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                "继续后，当前成员的年龄、性别、测量日期、时段和健康数值将发送到您配置的第三方模型服务商。" +
                    "数据处理受该服务商条款约束。AI 内容可能不准确，仅供健康信息参考，不能替代医生诊断；出现严重不适请及时就医。",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = { AppTextButton(text = "同意并继续", onClick = onAccept) },
        dismissButton = { AppTextButton(text = "取消", onClick = onDismiss) }
    )
}
