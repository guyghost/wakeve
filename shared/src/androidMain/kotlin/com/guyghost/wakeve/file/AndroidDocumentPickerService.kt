package com.guyghost.wakeve.file

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.result.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.PickedDocument
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android-specific implementation of [DocumentPickerService] using ActivityResultContracts.
 *
 * Uses the GetContent API (Android 13+) for a modern, privacy-friendly
 * document selection experience without requiring READ_EXTERNAL_STORAGE permission.
 *
 * For older Android versions, falls back to the traditional intent-based picker.
 *
 * ## Architecture
 *
 * This class is part of the **Imperative Shell** - it handles:
 * - ActivityResultLauncher registration and management
 * - Content URI resolution and permission handling
 * - Document metadata extraction via ContentResolver
 * - Callback-to-suspend conversion via Channels
 *
 * ## Features
 *
 * - Single and multi-document selection
 * - MIME type filtering for document types
 * - Automatic metadata extraction (size, name, mime type)
 * - Works without READ_EXTERNAL_STORAGE on Android 13+
 * - Graceful fallback for older Android versions
 *
 * ## Usage
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     private lateinit var documentPicker: AndroidDocumentPickerService
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         documentPicker = AndroidDocumentPickerService(this)
 *     }
 *
 *     private fun onPickDocumentClicked() {
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
 * @property activity The AppCompatActivity used for ActivityResultLauncher registration
 * @property context Application context for content resolution and metadata queries
 */
class AndroidDocumentPickerService(
    private val activity: AppCompatActivity
) : DocumentPickerService {

    private val context: Context
        get() = activity.applicationContext

    // Channel for single document picking
    private val singleDocumentChannel = Channel<Result<PickedDocument>>(Channel.CONFLATED)

    // Channel for multiple document picking
    private val multipleDocumentChannel = Channel<Result<List<PickedDocument>>>(Channel.CONFLATED)

    // Cache for the last picked document
    @Volatile
    private var lastPickedDocument: PickedDocument? = null

    // ActivityResultLaunchers
    private val pickDocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val document = extractDocumentMetadata(it)
            lastPickedDocument = document
            singleDocumentChannel.trySend(Result.success(document))
        } ?: singleDocumentChannel.trySend(
            Result.failure(DocumentPickerCancelledException())
        )
    }

    private val pickMultipleDocumentsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val documents = uris.mapNotNull { uri ->
                try {
                    extractDocumentMetadata(uri)
                } catch (e: Exception) {
                    null // Skip invalid documents
                }
            }
            lastPickedDocument = documents.firstOrNull()
            multipleDocumentChannel.trySend(Result.success(documents))
        } else {
            multipleDocumentChannel.trySend(
                Result.failure(DocumentPickerCancelledException())
            )
        }
    }

    override suspend fun pickDocument(): Result<PickedDocument> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses GetContent which doesn't require permission
            suspendCancellableCoroutine { continuation ->
                pickDocumentLauncher.launch("*/*")

                continuation.invokeOnCancellation {
                    singleDocumentChannel.cancel()
                }
            }
        } else {
            // Fallback for older Android versions
            pickDocumentLegacy()
        }
    }

    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                // Create a new launcher for each call to handle the limit
                val launcher = activity.registerForActivityResult(
                    ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        val documents = uris
                            .mapNotNull { uri ->
                                try {
                                    extractDocumentMetadata(uri)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            .take(limit)
                        lastPickedDocument = documents.firstOrNull()
                        continuation.resume(Result.success(documents))
                    } else {
                        continuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                launcher.launch("*/*")

                continuation.invokeOnCancellation {
                    multipleDocumentChannel.cancel()
                }
            }
        } else {
            pickMultipleDocumentsLegacy(limit)
        }
    }

    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> {
        return suspendCancellableCoroutine { continuation ->
            val mimeType = type.mimeType

            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val document = extractDocumentMetadata(it)
                    lastPickedDocument = document
                    continuation.resume(Result.success(document))
                } ?: continuation.resume(
                    Result.failure(DocumentPickerCancelledException())
                )
            }

            launcher.launch(mimeType)

            continuation.invokeOnCancellation {
                singleDocumentChannel.cancel()
            }
        }
    }

    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        return when {
            config.allowMultipleSelection -> {
                pickDocuments(config.maxSelectionLimit).map { documents ->
                    DocumentBatchResult(
                        documents = documents.take(config.maxSelectionLimit),
                        config = config
                    )
                }
            }
            else -> {
                val mimeType = config.getMimeTypeFilter()
                pickDocumentWithMimeType(mimeType).map { document ->
                    DocumentBatchResult(
                        documents = listOf(document),
                        config = config
                    )
                }
            }
        }
    }

    private suspend fun pickDocumentWithMimeType(mimeType: String): Result<PickedDocument> {
        return suspendCancellableCoroutine { continuation ->
            val launcher = activity.registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                uri?.let {
                    val document = extractDocumentMetadata(it)
                    lastPickedDocument = document
                    continuation.resume(Result.success(document))
                } ?: continuation.resume(
                    Result.failure(DocumentPickerCancelledException())
                )
            }

            launcher.launch(mimeType)

            continuation.invokeOnCancellation {
                singleDocumentChannel.cancel()
            }
        }
    }

    override fun isDocumentPickerAvailable(): Boolean {
        return true // GetContent is available on all supported Android versions
    }

    override fun getLastPickedDocument(): PickedDocument? = lastPickedDocument

    override fun clearCache() {
        lastPickedDocument = null
    }

    // Legacy support for Android < 13
    private suspend fun pickDocumentLegacy(): Result<PickedDocument> {
        return if (hasReadStoragePermission()) {
            suspendCancellableCoroutine { continuation ->
                val legacyLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let {
                        val document = extractDocumentMetadata(it)
                        lastPickedDocument = document
                        continuation.resume(Result.success(document))
                    } ?: continuation.resume(
                        Result.failure(DocumentPickerCancelledException())
                    )
                }

                legacyLauncher.launch("*/*")

                continuation.invokeOnCancellation {
                    singleDocumentChannel.cancel()
                }
            }
        } else {
            Result.failure(
                DocumentPickerPermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    private suspend fun pickMultipleDocumentsLegacy(limit: Int): Result<List<PickedDocument>> {
        return if (hasReadStoragePermission()) {
            suspendCancellableCoroutine { continuation ->
                val legacyLauncher = activity.registerForActivityResult(
                    ActivityResultContracts.GetMultipleContents()
                ) { uris ->
                    if (uris.isNotEmpty()) {
                        val documents = uris
                            .mapNotNull { uri ->
                                try {
                                    extractDocumentMetadata(uri)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            .take(limit)
                        lastPickedDocument = documents.firstOrNull()
                        continuation.resume(Result.success(documents))
                    } else {
                        continuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                legacyLauncher.launch("*/*")

                continuation.invokeOnCancellation {
                    multipleDocumentChannel.cancel()
                }
            }
        } else {
            Result.failure(
                DocumentPickerPermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            )
        }
    }

    private fun hasReadStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // GetContent doesn't require permission on Android 13+
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Extract document metadata from a content URI.
     *
     * @param uri The content URI of the document
     * @return A PickedDocument with extracted metadata
     */
    private fun extractDocumentMetadata(uri: Uri): PickedDocument {
        val contentResolver: ContentResolver = context.contentResolver

        // Query for document metadata
        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
            OpenableColumns.CONTENT_TYPE
        )

        var displayName = "Unknown"
        var sizeBytes = 0L
        var mimeType: String? = null

        contentResolver.query(uri, projection, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                val typeIndex = cursor.getColumnIndex(OpenableColumns.CONTENT_TYPE)

                if (nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex) ?: "Unknown"
                }
                if (sizeIndex >= 0) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
                if (typeIndex >= 0) {
                    mimeType = cursor.getString(typeIndex)
                }
            }
        }

        // Try to get MIME type from content resolver if not found
        if (mimeType == null) {
            mimeType = contentResolver.getType(uri)
        }

        // Determine document type from MIME type
        val documentType = DocumentType.fromMimeType(mimeType)

        return PickedDocument(
            uri = uri.toString(),
            displayName = displayName,
            type = documentType,
            sizeBytes = sizeBytes,
            lastModified = null, // OpenableColumns doesn't provide last modified
            mimeType = mimeType
        )
    }

    companion object {
        private const val MAX_MULTIPLE_DOCUMENTS = 20
    }
}
