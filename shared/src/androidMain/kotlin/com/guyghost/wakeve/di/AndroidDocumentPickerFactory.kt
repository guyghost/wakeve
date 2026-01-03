package com.guyghost.wakeve.di

import androidx.appcompat.app.AppCompatActivity
import com.guyghost.wakeve.file.DocumentPickerService
import com.guyghost.wakeve.file.AndroidDocumentPickerService

/**
 * Android-specific implementation of DocumentPickerFactory.
 *
 * Creates [AndroidDocumentPickerService] instances for document picking
 * functionality on Android devices.
 *
 * ## Architecture
 *
 * This is part of the **Imperative Shell** - it handles:
 * - Activity binding for ActivityResultLauncher registration
 * - Service lifecycle management
 * - Platform-specific initialization
 *
 * ## Usage
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private lateinit var documentPicker: DocumentPickerService
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Get the factory and create the service
 *         val factory = DocumentPickerFactory.getInstance()
 *         documentPicker = factory.createPickerService()
 *     }
 *
 *     private fun onPickDocument() {
 *         lifecycleScope.launch {
 *             val result = documentPicker.pickDocument()
 *             result.fold(
 *                 onSuccess = { document ->
 *                     // Use the picked document
 *                     processDocument(document.uri)
 *                 },
 *                 onFailure = { error ->
 *                     // Handle error
 *                     showError(error.message)
 *                 }
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## Lifecycle Considerations
 *
 * The DocumentPickerService should be created when the Activity is created
 * and will remain valid until the Activity is destroyed. The service
 * uses Activity.registerForActivityResult() which requires an active
 * Activity lifecycle.
 *
 * ## Thread Safety
 *
 * This class is thread-safe. The factory methods can be called from
 * any thread, but the resulting service must be used from within
 * a coroutine context on the main thread for ActivityResultLauncher
 * operations.
 */
actual class DocumentPickerFactory(
    private val activity: AppCompatActivity
) {
    /**
     * Create an Android-specific document picker service.
     *
     * The returned service is configured to use the provided Activity
     * for all document-related operations.
     *
     * @return An AndroidDocumentPickerService instance
     */
    actual fun createPickerService(): DocumentPickerService {
        return AndroidDocumentPickerService(activity)
    }

    companion object {
        /**
         * Get the singleton factory instance for Android.
         *
         * Note: The activity must be provided when using this factory.
         * For most use cases, prefer creating the factory directly
         * with an Activity reference.
         *
         * @param activity The AppCompatActivity to use for document picking
         * @return A new DocumentPickerFactory instance configured for Android
         */
        fun getInstance(activity: AppCompatActivity): DocumentPickerFactory {
            return DocumentPickerFactory(activity)
        }
    }
}

/**
 * Extension function to get a DocumentPickerService from an Activity.
 *
 * This provides a convenient way to obtain a document picker service
 * from within an Activity or Fragment.
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private val documentPicker: DocumentPickerService by lazy {
 *         this.getDocumentPickerService()
 *     }
 * }
 * ```
 *
 * @return A DocumentPickerService instance bound to this Activity
 */
fun AppCompatActivity.getDocumentPickerService(): DocumentPickerService {
    return DocumentPickerFactory.getInstance(this).createPickerService()
}

/**
 * Extension function to get a DocumentPickerFactory from an Activity.
 *
 * This provides a convenient way to obtain the document picker factory
 * from within an Activity or Fragment.
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private val factory: DocumentPickerFactory by lazy {
 *         this.getDocumentPickerFactory()
 *     }
 * }
 * ```
 *
 * @return A DocumentPickerFactory instance bound to this Activity
 */
fun AppCompatActivity.getDocumentPickerFactory(): DocumentPickerFactory {
    return DocumentPickerFactory.getInstance(this)
}
