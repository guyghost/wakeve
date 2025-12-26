package com.guyghost.wakeve

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        try {
            enableEdgeToEdge()
            Log.d("MainActivity", "enableEdgeToEdge completed")
        } catch (e: Exception) {
            Log.e("MainActivity", "enableEdgeToEdge failed", e)
        }

        setContent {
            Log.d("MainActivity", "setContent called")
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}