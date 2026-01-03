package com.guyghost.wakeve.di

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.guyghost.wakeve.image.AndroidImagePickerService
import com.guyghost.wakeve.image.ImagePickerService

/**
 * Android-specific implementation of ImagePickerFactory.
 * 
 * Creates [AndroidImagePickerService] instances for image picking
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
 *     private lateinit var imagePicker: ImagePickerService
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Get the factory and create the service
 *         val factory = ImagePickerFactory.getInstance()
 *         imagePicker = factory.createPickerService()
 *     }
 *     
 *     private fun onPickImage() {
 *         lifecycleScope.launch {
 *             val result = imagePicker.pickImage()
 *             result.fold(
 *                 onSuccess = { image ->
 *                     // Use the picked image
 *                     loadImage(image.uri)
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
 * The ImagePickerService should be created when the Activity is created
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
actual class ImagePickerFactory(
    private val activity: AppCompatActivity
) {
    /**
     * Create an Android-specific image picker service.
     * 
     * The returned service is configured to use the provided Activity
     * for all gallery-related operations.
     * 
     * @return An AndroidImagePickerService instance
     */
    actual fun createPickerService(): ImagePickerService {
        return AndroidImagePickerService(activity)
    }
    
    companion object {
        /**
         * Get the singleton factory instance for Android.
         * 
         * Note: The activity must be provided when using this factory.
         * For most use cases, prefer creating the factory directly
         * with an Activity reference.
         * 
         * @param activity The AppCompatActivity to use for image picking
         * @return A new ImagePickerFactory instance configured for Android
         */
        fun getInstance(activity: AppCompatActivity): ImagePickerFactory {
            return ImagePickerFactory(activity)
        }
    }
}

/**
 * Extension function to get an ImagePickerService from an Activity.
 * 
 * This provides a convenient way to obtain an image picker service
 * from within an Activity or Fragment.
 * 
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private val imagePicker: ImagePickerService by lazy {
 *         this.getImagePickerService()
 *     }
 * }
 * ```
 * 
 * @return An ImagePickerService instance bound to this Activity
 */
fun AppCompatActivity.getImagePickerService(): ImagePickerService {
    return ImagePickerFactory.getInstance(this).createPickerService()
}

/**
 * Extension function to get an ImagePickerFactory from an Activity.
 * 
 * This provides a convenient way to obtain the image picker factory
 * from within an Activity or Fragment.
 * 
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private val factory: ImagePickerFactory by lazy {
 *         this.getImagePickerFactory()
 *     }
 * }
 * ```
 * 
 * @return An ImagePickerFactory instance bound to this Activity
 */
fun AppCompatActivity.getImagePickerFactory(): ImagePickerFactory {
    return ImagePickerFactory.getInstance(this)
}
