package com.soursweethealth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.soursweethealth.ui.SourSweetHealthApp
import com.soursweethealth.ui.theme.SourSweetHealthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SourSweetHealthTheme {
                SourSweetHealthApp()
            }
        }
    }
}
