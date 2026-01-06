package com.guyghost.wakeve

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.di.appModule
import com.guyghost.wakeve.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

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

        // Initialize Koin with Android context BEFORE setContent
        initializeKoinWithContext()

        setContent {
            Log.d("MainActivity", "setContent called")
            App()
        }
    }

    /**
     * Initialize Koin with Android context for platform-specific dependencies.
     *
     * This enables:
     * - SQLite database via AndroidSqliteDriver
     * - DatabaseEventRepository for persistent storage
     * - State Machine connected to real data
     */
    private fun initializeKoinWithContext() {
        // Check if Koin is already started (e.g., from a previous Activity instance)
        if (GlobalContext.getOrNull() != null) {
            Log.d("MainActivity", "Koin already initialized, skipping")
            return
        }

        try {
            startKoin {
                // Android logger for debugging
                androidLogger(Level.INFO)
                
                // Provide Android context for platform dependencies
                androidContext(this@MainActivity)
                
                // Load modules: common + platform-specific
                modules(appModule, platformModule())
            }
            Log.d("MainActivity", "Koin initialized successfully with database repository")
        } catch (e: Exception) {
            Log.e("MainActivity", "Koin initialization failed", e)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}