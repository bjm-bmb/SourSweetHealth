package com.soursweethealth

import android.app.Application
import android.content.Intent
import android.util.Log
import com.soursweethealth.data.AppDatabase
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class SourSweetApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                val trace = sw.toString()
                Log.e("SourSweetCrash", "Uncaught on ${t.name}:\n$trace")
                runCatching {
                    File(filesDir, "crash.log").writeText(
                        "Time: ${System.currentTimeMillis()}\nThread: ${t.name}\n$trace"
                    )
                }
                runCatching {
                    val intent = Intent(this, CrashReportActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra("trace", trace)
                    }
                    startActivity(intent)
                }
            } catch (_: Throwable) { }
            previous?.uncaughtException(t, e)
            exitProcess(2)
        }
    }
}
