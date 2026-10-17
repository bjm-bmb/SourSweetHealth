package com.soursweethealth.ui

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soursweethealth.SourSweetApp
import com.soursweethealth.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.net.URI

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = (application as SourSweetApp).database
    private val userDao = db.userDao()
    private val recordDao = db.healthRecordDao()
    val settings = SettingsManager(application)
    private val llmService = LlmService()

    val users = userDao.getAllUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentUserId = settings.currentUserId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)
    val aiPrivacyAccepted = settings.aiPrivacyAccepted.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentUser: StateFlow<User?> = combine(users, currentUserId) { userList, id ->
        userList.find { it.id == id } ?: userList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch { settings.migrateLegacyApiKey() }
        viewModelScope.launch {
            combine(users, currentUserId) { userList, id ->
                if (userList.isNotEmpty() && userList.none { it.id == id }) {
                    settings.setCurrentUserId(userList.first().id)
                }
            }.collect()
        }
    }

    fun latestBloodSugar(userId: Long) = recordDao.getLatestRecordByType(userId, "blood_sugar")
    fun latestUricAcid(userId: Long) = recordDao.getLatestRecordByType(userId, "uric_acid")
    fun latestRecord(userId: Long) = recordDao.getLatestRecord(userId)

    fun getRecordsByDateRange(userId: Long, type: String, startDate: String, endDate: String) =
        recordDao.getRecordsByDateRange(userId, type, startDate, endDate)

    fun getRecordsByType(userId: Long, type: String) =
        recordDao.getRecordsByUserAndType(userId, type)

    fun switchUser(userId: Long) {
        viewModelScope.launch { settings.setCurrentUserId(userId) }
    }

    fun addUser(name: String, gender: String, birthYear: Int, birthMonth: Int, avatarColor: Int, onDone: (Long) -> Unit = {}) {
        if (HealthValidation.validateUser(name, birthYear, birthMonth) != null) return
        viewModelScope.launch {
            val id = userDao.insert(User(name = name.trim(), gender = gender, birthYear = birthYear, birthMonth = birthMonth, avatarColor = avatarColor))
            settings.setCurrentUserId(id)
            onDone(id)
        }
    }

    fun updateUser(user: User) {
        if (HealthValidation.validateUser(user.name, user.birthYear, user.birthMonth) != null) return
        viewModelScope.launch {
            userDao.update(user.copy(name = user.name.trim()))
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            userDao.delete(user)
            val remaining = users.value.filter { it.id != user.id }
            if (remaining.isNotEmpty()) {
                settings.setCurrentUserId(remaining.first().id)
            } else {
                settings.setCurrentUserId(-1L)
            }
        }
    }

    fun addRecord(
        userId: Long,
        type: String,
        value: Double?,
        date: String,
        time: String,
        measureTime: String,
        onResult: (String?) -> Unit = {}
    ) {
        val error = HealthValidation.validateRecord(type, value, date, time)
        if (error != null) {
            onResult(error)
            return
        }
        val measuredAt = HealthValidation.toEpochMillis(date, time)
        if (measuredAt == null || value == null) {
            onResult("请输入有效的日期和时间")
            return
        }
        viewModelScope.launch {
            recordDao.insert(HealthRecord(userId = userId, type = type, value = value, date = date, measureTime = measureTime, measuredAt = measuredAt))
            onResult(null)
        }
    }

    fun updateRecord(record: HealthRecord, onResult: (String?) -> Unit = {}) {
        val localDateTime = Instant.ofEpochMilli(record.measuredAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val date = localDateTime.toLocalDate().toString()
        val time = localDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
        val error = HealthValidation.validateRecord(record.type, record.value, date, time)
        if (error != null) {
            onResult(error)
            return
        }
        viewModelScope.launch {
            recordDao.update(record.copy(date = date))
            onResult(null)
        }
    }

    fun deleteRecord(record: HealthRecord) {
        viewModelScope.launch {
            recordDao.delete(record)
        }
    }

    fun saveApiConfig(url: String, key: String, model: String, onResult: (String?) -> Unit = {}) {
        val normalizedUrl = sanitizeUrl(url.trim())
        val validUrl = runCatching {
            val uri = URI(normalizedUrl)
            uri.scheme == "https" && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
        val error = when {
            !validUrl -> "请输入有效的 HTTPS API 地址"
            key.isBlank() -> "请输入 API Key"
            model.isBlank() -> "请输入模型名称"
            else -> null
        }
        if (error != null) {
            onResult(error)
            return
        }
        viewModelScope.launch {
            settings.setApiConfig(normalizedUrl, key, model.trim())
            onResult(null)
        }
    }

    fun setAiPrivacyAccepted(accepted: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            settings.setAiPrivacyAccepted(accepted)
            onDone()
        }
    }

    val apiUrl = settings.apiUrl.stateIn(viewModelScope, SharingStarted.Eagerly, "https://api.siliconflow.cn/v1/chat/completions")
    val apiKey = settings.apiKey.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val modelName = settings.modelName.stateIn(viewModelScope, SharingStarted.Eagerly, "Pro/zai-org/GLM-4.7")

    /** Sanitise user-typed URL: replace commas in domain with periods */
    private fun sanitizeUrl(url: String): String {
        if (url.isBlank()) return url
        // Split scheme from rest, fix commas in host part
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return url
        val scheme = url.substring(0, schemeEnd + 3)
        val rest = url.substring(schemeEnd + 3)
        val slashIdx = rest.indexOf('/')
        val host = if (slashIdx < 0) rest else rest.substring(0, slashIdx)
        val path = if (slashIdx < 0) "" else rest.substring(slashIdx)
        return scheme + host.replace(',', '.') + path
    }

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    fun analyzeHealth(user: User) {
        if (_isAnalyzing.value) return
        // Set flag immediately so UI shows spinner without delay
        _isAnalyzing.value = true
        _analysisResult.value = null

        viewModelScope.launch {
            try {
                // Read from already-loaded StateFlow (no DataStore round-trip delay)
                val key = apiKey.value
                val url = sanitizeUrl(apiUrl.value)
                val model = modelName.value

                if (key.isBlank()) {
                    _analysisResult.value = "请先在左上角设置大模型API配置"
                    return@launch
                }
                if (!aiPrivacyAccepted.value) {
                    _analysisResult.value = "请先阅读并同意 AI 数据处理说明"
                    return@launch
                }

                // Fetch only the 20 most recent records directly — much faster than loading all + takeLast(20)
                val records = recordDao.getLatestRecordsForUser(user.id, 20).reversed()
                if (records.isEmpty()) {
                    _analysisResult.value = "暂无记录数据，请先添加健康记录"
                    return@launch
                }

                val age = HealthValidation.age(user.birthYear, user.birthMonth)
                val gender = user.gender

                val sb = StringBuilder()
                sb.append("这位是${gender}性、${age}岁的朋友近期的健康数据，请分三段回复（每段开头用**《趋势分析》**、**《生活建议》**、**温馨提示**），语气像老朋友聊天，可加1-2个表情，200字以内：\n")
                // Only send the most recent 20 records to keep prompt short
                records.forEach { r ->
                    val typeName = HealthUtils.getTypeName(r.type)
                    val unit = HealthUtils.getUnit(r.type)
                    val measuredTime = Instant.ofEpochMilli(r.measuredAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    sb.append("$measuredTime ${r.measureTime} $typeName ${r.value}${unit}\n")
                }
                sb.append("\n请开心地告诉他/她：1）数据整体趋势如何；2）最近变化如何；3）最重要的一条生活建议；4）如有异常请用**加粗**温和提醒。")

                val accumulated = StringBuilder()
                var hasError = false
                llmService.chatStream(url, key, model, sb.toString(), maxTokens = 4096).collect { result ->
                    result.onSuccess { token ->
                        accumulated.append(token)
                        _analysisResult.value = accumulated.toString()
                    }.onFailure { e ->
                        // If we already have partial content, keep it — don't overwrite with connection error
                        if (accumulated.isNotEmpty()) {
                            // Partial result is still useful; just stop collecting
                            hasError = true
                        } else {
                            val msg = e.message ?: ""
                            _analysisResult.value = if (msg.contains("abort", ignoreCase = true) || msg.contains("reset", ignoreCase = true) || msg.contains("closed", ignoreCase = true)) {
                                "网络连接中断，请稍后重试"
                            } else {
                                "分析失败：$msg"
                            }
                            hasError = true
                        }
                    }
                }
                if (!hasError && accumulated.isEmpty()) {
                    _analysisResult.value = "未收到分析结果，请检查网络或API配置"
                }
            } catch (e: Exception) {
                // If we already showed partial/full content, don't overwrite with a socket error
                val alreadyHasContent = _analysisResult.value?.isNotBlank() == true
                if (!alreadyHasContent) {
                    val msg = e.message ?: ""
                    _analysisResult.value = if (msg.contains("abort", ignoreCase = true) || msg.contains("reset", ignoreCase = true) || msg.contains("closed", ignoreCase = true)) {
                        "网络连接中断，请稍后重试"
                    } else {
                        "分析出错：$msg"
                    }
                }
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearAnalysis() {
        _analysisResult.value = null
    }

    // Quick advice for abnormal values
    private val _quickAdviceResult = MutableStateFlow<String?>(null)
    val quickAdviceResult: StateFlow<String?> = _quickAdviceResult
    private val _isQuickAdvising = MutableStateFlow(false)
    val isQuickAdvising: StateFlow<Boolean> = _isQuickAdvising

    fun getQuickAdvice(type: String, value: Double, gender: String, age: Int, measureTime: String = "空腹（起床后）") {
        if (_isQuickAdvising.value) return
        // Set flag immediately so spinner appears without delay
        _isQuickAdvising.value = true
        _quickAdviceResult.value = null

        // Build prompt here (on calling thread) before launching coroutine — no DB fetch needed, data comes from screen
        val typeName = HealthUtils.getTypeName(type)
        val unit = HealthUtils.getUnit(type)
        val level = HealthUtils.getLevel(type, value, gender, measureTime)
        val levelText = when (level) {
            HealthLevel.VERY_LOW -> "明显偏低"
            HealthLevel.LOW -> "偏低"
            HealthLevel.HIGH -> "偏高"
            HealthLevel.VERY_HIGH -> "明显偏高"
            HealthLevel.NORMAL -> "在当前参考范围内"
        }
        val timeHint = if (type == "blood_sugar") "，${measureTime}测量" else ""
        val prompt = "${gender}性${age}岁，${typeName}${String.format("%.1f", value)}${unit}${timeHint}，${levelText}。请两段简要当面说明：第一段说数值意义；第二段给**最重要一条**建议。温暖鼓励，可加表情，80字以内。"

        viewModelScope.launch {
            try {
                val key = apiKey.value
                val url = sanitizeUrl(apiUrl.value)
                val model = modelName.value

                if (key.isBlank()) {
                    _quickAdviceResult.value = "请先配置大模型API"
                    return@launch
                }
                if (!aiPrivacyAccepted.value) {
                    _quickAdviceResult.value = "请先阅读并同意 AI 数据处理说明"
                    return@launch
                }

                val accumulated = StringBuilder()
                var hasError = false
                llmService.chatStream(url, key, model, prompt, maxTokens = 2048).collect { result ->
                    result.onSuccess { token ->
                        accumulated.append(token)
                        _quickAdviceResult.value = accumulated.toString()
                    }.onFailure { e ->
                        if (accumulated.isNotEmpty()) {
                            hasError = true // keep partial content, just stop
                        } else {
                            val msg = e.message ?: ""
                            _quickAdviceResult.value = if (msg.contains("abort", ignoreCase = true) || msg.contains("reset", ignoreCase = true) || msg.contains("closed", ignoreCase = true)) {
                                "网络连接中断，请稍后重试"
                            } else {
                                "获取建议失败：$msg"
                            }
                            hasError = true
                        }
                    }
                }
                if (!hasError && accumulated.isEmpty()) {
                    _quickAdviceResult.value = "未收到建议，请检查网络或API配置"
                }
            } catch (e: Exception) {
                val alreadyHasContent = _quickAdviceResult.value?.isNotBlank() == true
                if (!alreadyHasContent) {
                    val msg = e.message ?: ""
                    _quickAdviceResult.value = if (msg.contains("abort", ignoreCase = true) || msg.contains("reset", ignoreCase = true) || msg.contains("closed", ignoreCase = true)) {
                        "网络连接中断，请稍后重试"
                    } else {
                        "获取建议出错：$msg"
                    }
                }
            } finally {
                _isQuickAdvising.value = false
            }
        }
    }

    fun clearQuickAdvice() {
        _quickAdviceResult.value = null
    }

    fun exportCsv(user: User, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val rows = recordDao.getAllRecordsForUser(user.id)
            val csv = buildString {
                append("用户,性别,测量时间,类别,测量时段,数值,单位\n")
                rows.forEach { record ->
                    val measured = Instant.ofEpochMilli(record.measuredAt).atZone(ZoneId.systemDefault()).format(formatter)
                    val cells = listOf(
                        user.name,
                        user.gender,
                        measured,
                        HealthUtils.getTypeName(record.type),
                        record.measureTime,
                        record.value.toString(),
                        HealthUtils.getUnit(record.type)
                    )
                    append(cells.joinToString(",") { csvCell(it) }).append('\n')
                }
            }
            onReady(csv)
        }
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
