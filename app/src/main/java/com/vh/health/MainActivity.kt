package com.vh.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vh.health.ui.VhHealthRoot
import com.vh.health.ui.theme.VhHealthTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as VhHealthApp).container
        setContent {
            VhHealthTheme {
                VhHealthRoot(container)
            }
        }
    }
}
