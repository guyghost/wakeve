package com.guyghost.wakeve.file

import com.guyghost.wakeve.models.DocumentPickerConfig
import com.guyghost.wakeve.models.DocumentType
import com.guyghost.wakeve.models.DocumentBatchResult
import com.guyghost.wakeve.models.PickedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.contentTypeIdentifier
import platform.Photos.PHAsset
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegate
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.dispatching
import platform.mobileCore.MCError
import platform.uniformTypeIdentifiers.UTType
import platform.uniformTypeIdentifiers.UTTypeIdentifiers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * iOS-specific implementation of [DocumentPickerService] using UIDocumentPickerViewController.
 *
 * This class handles all iOS-specific document picking operations including:
 * - Document library access via UIDocumentPickerViewController (iOS 14+)
 * - Multiple document selection
 * - MIME type filtering based on document types
 * - Permission handling for document access
 *
 * ## FC&IS Architecture
 *
 * This class belongs to the **Imperative Shell** layer as it handles:
 * - Platform-specific I/O operations (document library access)
 * - UI presentation (UIDocumentPickerViewController presentation)
 * - Permission handling
 *
 * It uses models from the **Functional Core** layer:
 * - [PickedDocument] - Pure data class for picked document metadata
 * - [DocumentType] - Document type enumeration
 * - [DocumentPickerConfig] - Configuration for picking behavior
 *
 * ## Usage
 *
 * ```kotlin
 * val pickerService: DocumentPickerService = IosDocumentPickerService()
 *
 * // Check authorization
 * if (!pickerService.isDocumentPickerAvailable()) {
 *     // Document picker not available
 * }
 *
 * // Pick a single document
 * val result = pickerService.pickDocument()
 * result.fold(
 *     onSuccess = { document ->
 *         uploadDocument(document.uri)
 *     },
 *     onFailure = { error ->
 *         showError("Failed to pick document: ${error.message}")
 *     }
 * )
 * ```
 *
 * @see DocumentPickerService
 * @see UIDocumentPickerViewController
 */
class IosDocumentPickerService : DocumentPickerService {

    private companion object {
        private const val TAG = "IosDocumentPickerService"
    }

    private var currentPicker: UIDocumentPickerViewController? = null
    private var cachedDocument: PickedDocument? = null

    /**
     * Picks a single document from the document library.
     *
     * Launches UIDocumentPickerViewController in single selection mode and waits
     * for the user to select a document.
     *
     * @return [Result] containing the picked document on success,
     *         or an exception on failure (cancellation, permission denied, etc.)
     */
    override suspend fun pickDocument(): Result<PickedDocument> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Create configuration for single document selection
                val configuration = UIDocumentPickerConfiguration().apply {
                    allowsMultipleSelection = false
                    // Importing is needed to get access to the file
                    allowsPickingMultipleItems = false
                }

                // Create picker
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = supportedUTTypes)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : UIDocumentPickerViewControllerDelegate {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentAt url: NSURL
                    ) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)

                        val document = loadDocument(from = url)
                        document?.let {
                            cachedDocument = it
                            pickerContinuation.resume(Result.success(it))
                        } ?: run {
                            pickerContinuation.resume(
                                Result.failure(DocumentPickerInvalidDocumentException("Failed to load document"))
                            )
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)
                        pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Picks multiple documents from the document library.
     *
     * Launches UIDocumentPickerViewController in multi-selection mode and waits
     * for the user to select multiple documents.
     *
     * @param limit Maximum number of documents that can be selected (default: 5)
     * @return [Result] containing the list of picked documents on success,
     *         or an exception on failure
     */
    override suspend fun pickDocuments(limit: Int): Result<List<PickedDocument>> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Create configuration for multiple document selection
                val configuration = UIDocumentPickerConfiguration().apply {
                    allowsMultipleSelection = true
                    allowsPickingMultipleItems = true
                }

                // Create picker
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = supportedUTTypes)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : UIDocumentPickerViewControllerDelegate {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentAt url: NSURL
                    ) {
                        // For multiple selection, we need to handle differently
                        // This callback is called for each selected document
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)
                        pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }

                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAt urls: List<NSURL>
                    ) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)

                        if (urls.isEmpty()) {
                            pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                            return
                        }

                        val documents = urls
                            .take(limit)
                            .mapNotNull { url -> loadDocument(from = url) }

                        if (documents.isEmpty()) {
                            pickerContinuation.resume(
                                Result.failure(DocumentPickerInvalidDocumentException("Failed to load any documents"))
                            )
                        } else {
                            cachedDocument = documents.firstOrNull()
                            pickerContinuation.resume(Result.success(documents))
                        }
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Picks a document of a specific type.
     *
     * Launches the document picker filtered to only show documents
     * of the specified type.
     *
     * @param type The type of document to filter by
     * @return [Result] containing the picked document on success,
     *         or an exception on failure
     */
    override suspend fun pickDocument(type: DocumentType): Result<PickedDocument> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                // Get UTType for the requested document type
                val utTypes = getUTTypesForDocumentType(type)

                // Create configuration with filtered types
                val configuration = UIDocumentPickerConfiguration().apply {
                    allowsMultipleSelection = false
                    allowsPickingMultipleItems = false
                }

                // Create picker with filtered types
                val picker = UIDocumentPickerViewController(forOpeningContentTypes = utTypes)
                currentPicker = picker

                // Store continuation for callback
                val pickerContinuation = continuation

                // Set delegate
                picker.delegate = object : UIDocumentPickerViewControllerDelegate {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentAt url: NSURL
                    ) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)

                        val document = loadDocument(from = url)
                        document?.let {
                            cachedDocument = it
                            pickerContinuation.resume(Result.success(it))
                        } ?: run {
                            pickerContinuation.resume(
                                Result.failure(DocumentPickerInvalidDocumentException("Failed to load document"))
                            )
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)
                        pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                // Present picker
                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                // Handle cancellation
                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Picks documents with a custom configuration.
     *
     * Provides more control over the document picking behavior through
     * a [DocumentPickerConfig] object.
     *
     * @param config Configuration for document picking behavior
     * @return [Result] containing the batch result on success,
     *         or an exception on failure
     */
    override suspend fun pickDocumentsWithConfig(config: DocumentPickerConfig): Result<DocumentBatchResult> {
        val effectiveLimit = config.maxSelectionLimit.coerceIn(1, 100)

        return if (config.allowedTypes.isEmpty()) {
            // No type filtering - use all supported types
            if (config.allowMultipleSelection) {
                pickDocuments(effectiveLimit).map { documents ->
                    DocumentBatchResult(
                        documents = documents.take(effectiveLimit),
                        config = config
                    )
                }
            } else {
                pickDocument().map { document ->
                    DocumentBatchResult(
                        documents = listOf(document),
                        config = config
                    )
                }
            }
        } else {
            // Filter by allowed types
            if (config.allowMultipleSelection) {
                pickMultipleDocumentsWithTypes(config.allowedTypes, effectiveLimit).map { documents ->
                    DocumentBatchResult(
                        documents = documents.take(effectiveLimit),
                        config = config
                    )
                }
            } else {
                pickDocumentWithTypes(config.allowedTypes).map { document ->
                    DocumentBatchResult(
                        documents = listOf(document),
                        config = config
                    )
                }
            }
        }
    }

    private suspend fun pickDocumentWithTypes(types: List<DocumentType>): Result<PickedDocument> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val utTypes = types.flatMap { getUTTypesForDocumentType(it) }

                val configuration = UIDocumentPickerConfiguration().apply {
                    allowsMultipleSelection = false
                    allowsPickingMultipleItems = false
                }

                val picker = UIDocumentPickerViewController(forOpeningContentTypes = utTypes)
                currentPicker = picker
                val pickerContinuation = continuation

                picker.delegate = object : UIDocumentPickerViewControllerDelegate {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentAt url: NSURL
                    ) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)

                        val document = loadDocument(from = url)
                        document?.let {
                            cachedDocument = it
                            pickerContinuation.resume(Result.success(it))
                        } ?: run {
                            pickerContinuation.resume(
                                Result.failure(DocumentPickerInvalidDocumentException("Failed to load document"))
                            )
                        }
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)
                        pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    private suspend fun pickMultipleDocumentsWithTypes(types: List<DocumentType>, limit: Int): Result<List<PickedDocument>> = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            try {
                val utTypes = types.flatMap { getUTTypesForDocumentType(it) }

                val configuration = UIDocumentPickerConfiguration().apply {
                    allowsMultipleSelection = true
                    allowsPickingMultipleItems = true
                }

                val picker = UIDocumentPickerViewController(forOpeningContentTypes = utTypes)
                currentPicker = picker
                val pickerContinuation = continuation

                picker.delegate = object : UIDocumentPickerViewControllerDelegate {
                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAt urls: List<NSURL>
                    ) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)

                        if (urls.isEmpty()) {
                            pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                            return
                        }

                        val documents = urls
                            .take(limit)
                            .mapNotNull { url -> loadDocument(from = url) }

                        cachedDocument = documents.firstOrNull()
                        pickerContinuation.resume(Result.success(documents))
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        currentPicker = null
                        controller.dismissViewControllerAnimated(true, completion = null)
                        pickerContinuation.resume(Result.failure(DocumentPickerCancelledException()))
                    }
                }

                val topViewController = getTopViewController()
                if (topViewController != null) {
                    topViewController.presentViewController(picker, animated = true, completion = null)
                } else {
                    continuation.resume(Result.failure(Exception("No view controller to present picker")))
                }

                continuation.invokeOnCancellation {
                    currentPicker?.dismissViewControllerAnimated(true, completion = null)
                    currentPicker = null
                }
            } catch (e: Exception) {
                currentPicker = null
                continuation.resume(Result.failure(e))
            }
        }
    }

    /**
     * Check if the document picker is available on this platform.
     *
     * UIDocumentPickerViewController is available on iOS 14+.
     *
     * @return true if the document picker is available, false otherwise
     */
    override fun isDocumentPickerAvailable(): Boolean = true

    /**
     * Get the last picked document from cache.
     *
     * Useful for retrieving the result after the picker closes.
     *
     * @return The last picked document, or null if no document has been picked
     */
    override fun getLastPickedDocument(): PickedDocument? = cachedDocument

    /**
     * Clear the cached picked document.
     *
     * Should be called after processing the picked document to free memory.
     */
    override fun clearCache() {
        cachedDocument = null
    }

    // MARK: - Private Helper Methods

    /**
     * Gets the top view controller for presenting the picker.
     */
    private fun getTopViewController(): UIViewController? {
        var rootViewController = UIApplication.sharedApplication?.keyWindow?.rootViewController

        while (rootViewController?.presentedViewController != null) {
            rootViewController = rootViewController.presentedViewController
        }

        return rootViewController
    }

    /**
     * Loads a document from a file URL and extracts its metadata.
     */
    private fun loadDocument(from url: NSURL): PickedDocument? {
        val accessing = url.startAccessingSecurityScopedResource()
        defer { if (accessing) url.stopAccessingSecurityScopedResource() }

        // Get file attributes
        val resourceValues = try {
            url.resourceValuesForKeys(
                listOf(
                    NSURLFileSizeKey,
                    NSURLContentTypeKey,
                    NSURLDisplayNameKey
                )
            )
        } catch (e: Exception) {
            null
        }

        val fileSize = resourceValues?.fileSize?.toLong() ?: 0L
        val contentType = resourceValues?.contentType
        val displayName = resourceValues?.displayName ?: url.lastPathComponent ?: "Unknown"
        val mimeType = contentType?.preferredMIMEType ?: "application/octet-stream"

        // Determine document type from content type
        val documentType = DocumentType.fromMimeType(mimeType)

        return PickedDocument(
            uri = url.absoluteString ?: url.path ?: "",
            displayName = displayName,
            type = documentType,
            sizeBytes = fileSize,
            lastModified = null, // We'd need additional keys for this
            mimeType = mimeType
        )
    }

    /**
     * Maps a DocumentType to corresponding UTType identifiers.
     */
    private fun getUTTypesForDocumentType(type: DocumentType): List<UTType> {
        return when (type) {
            DocumentType.PDF -> listOf(UTType.pdf)
            DocumentType.DOC, DocumentType.DOCX -> listOf(UTType.doc, UTType.docx)
            DocumentType.SPREADSHEET -> listOf(UTType.spreadsheet)
            DocumentType.PRESENTATION -> listOf(UTType.presentation)
            DocumentType.IMAGE -> listOf(UTType.image)
            DocumentType.VIDEO -> listOf(UTType.movie, UTType.video)
            DocumentType.AUDIO -> listOf(UTType.audio)
            DocumentType.ARCHIVE -> listOf(UTType.archive)
            DocumentType.OTHER -> supportedUTTypes
        }
    }

    /**
     * Supported UTTypes for document picking.
     */
    private val supportedUTTypes: List<UTType>
        get() = listOf(
            // Documents
            UTType.pdf,
            UTType.doc,
            UTType.docx,
            UTType.rtf,
            UTType.plainText,
            // Spreadsheets
            UTType.spreadsheet,
            // Presentations
            UTType.presentation,
            // Images
            UTType.image,
            // Video
            UTType.movie,
            UTType.video,
            // Audio
            UTType.audio,
            // Archives
            UTType.archive,
            // Data
            UTType.data
        )
}
