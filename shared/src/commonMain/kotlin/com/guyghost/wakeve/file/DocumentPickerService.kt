package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.PickedDocument

/**
 * Service for picking documents from device storage.
 *
 * This interface defines the contract for document picking functionality
 * across all platforms. Platform-specific implementations handle
 * actual file system access, permissions, and document handling.
 *
 * ## Architecture
 *
 * This interface is part of the **Functional Core** - it defines the contract
 * without any I/O or side effects. The actual platform-specific implementations
 * (AndroidDocumentPickerService, IosDocumentPickerService) are in the
 * **Imperative Shell** and handle:
 * - File system access via platform APIs
 * - Runtime permissions
 * - Content URI resolution
 * - Document metadata extraction
 *
 * ## Usage
 *
 * ```kotlin
 * // Get the service from DI/factory
 * val documentPicker = getDocumentPickerService()
 *
 * // Pick a single document
 * val result = documentPicker.pickDocument()
 * result.fold(
 *     onSuccess = { document ->
 *         // Use the picked document
 *         uploadDocument(document.uri)
 *     },
 *     onFailure = { error ->
 *         // Handle error (permission denied, cancellation, etc.)
 *         showError("Failed to pick document: ${error.message}")
 *     }
 * )
 *
 * // Pick multiple documents
 * val batchResult = documentPicker.pickDocuments(limit = 3)
 * batchResult.fold(
 *     onSuccess = { batch ->
 *         batch.documents.forEach { document ->
 *             uploadDocument(document.uri)
 *         }
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick documents: ${error.message}")
 *     }
 * )
 *
 * // Pick a specific document type
 * val pdfResult = documentPicker.pickDocument(DocumentType.PDF)
 * pdfResult.fold(
 *     onSuccess = { pdf ->
 *         processPdf(pdf.uri)
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick PDF: ${error.message}")
 *     }
 * )
 * ```
 *
 * ## Error Handling
 *
 * All methods return a [Result] type to handle:
 * - Permission denial ([SecurityException])
 * - User cancellation
 * - Invalid URI or corrupted file
 * - I/O errors during file access
 * - Storage permission issues
 *
 * ## Thread Safety
 *
 * Implementations must be thread-safe as document picking operations
 * may be called from multiple coroutines simultaneously.
 */
interface DocumentPickerService {

    /**
     * Pick a single document from storage.
     *
     * Launches the platform's document picker and waits for the user
     * to select a document. Returns the selected document metadata.
     *
     * @return [Result] containing the picked document on success,
     *         or an exception on failure (cancellation, permission denied, etc.)
     */
    suspend fun pickDocument(): Result<PickedDocument>

    /**
     * Pick multiple documents from storage.
     *
     * Launches the platform's document picker in multi-select mode
     * and waits for the user to select multiple documents.
     *
     * @param limit Maximum number of documents that can be selected.
     *              If the picker returns more, they'll be truncated to this limit.
     * @return [Result] containing the list of picked documents on success,
     *         or an exception on failure
     */
    suspend fun pickDocuments(limit: Int = 5): Result<List<PickedDocument>>

    /**
     * Pick a document of a specific type.
     *
     * Launches the document picker filtered to only show documents
     * of the specified type.
     *
     * @param type The type of document to filter by
     * @return [Result] containing the picked document on success,
     *         or an exception on failure
     */
    suspend fun pickDocument(type: DocumentType): Result<PickedDocument>

    /**
     * Pick documents with a custom configuration.
     *
     * Provides more control over the document picking behavior through
     * a [DocumentPickerConfig] object.
     *
     * @param config Configuration for document picking behavior
     * @return [Result] containing the batch result on success,
     *         or an exception on failure
     */
    suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult>

    /**
     * Check if document picker is available on this platform.
     *
     * Some features may not be available on older OS versions.
     *
     * @return true if the document picker is available, false otherwise
     */
    fun isDocumentPickerAvailable(): Boolean

    /**
     * Get the last picked document from cache.
     *
     * Useful for retrieving the result after the picker closes.
     *
     * @return The last picked document, or null if no document has been picked
     */
    fun getLastPickedDocument(): PickedDocument?

    /**
     * Clear the cached picked document.
     *
     * Should be called after processing the picked document to free memory.
     */
    fun clearCache()
}

/**
 * Exception thrown when document picking is cancelled by the user.
 */
class DocumentPickerCancelledException : Exception("Document picking was cancelled by the user")

/**
 * Exception thrown when required permissions are not granted.
 */
class DocumentPickerPermissionDeniedException(
    val permission: String
) : Exception("Permission denied: $permission")

/**
 * Exception thrown when the picked document is invalid or corrupted.
 */
class DocumentPickerInvalidDocumentException(
    val reason: String
) : Exception("Invalid document: $reason")
