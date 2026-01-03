package com.guyghost.wakeve.models

import kotlinx.serialization.Serializable

/**
 * Document types supported by file picker.
 */
@Serializable
enum class DocumentType(
    val displayName: String,
    val mimeType: String
) {
    PDF("PDF Document", "application/pdf"),
    DOC("Word Document", "application/msword"),
    DOCX("Word Document", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    SPREADSHEET("Spreadsheet", "application/vnd.ms-excel"),
    PRESENTATION("Presentation", "application/vnd.ms-powerpoint"),
    IMAGE("Image", "image/*"),
    VIDEO("Video", "video/*"),
    AUDIO("Audio", "audio/*"),
    ARCHIVE("Archive", "application/zip"),
    OTHER("Other File", "*/*");

    companion object {
        fun fromMimeType(mimeType: String?): DocumentType {
            return when {
                mimeType == null -> OTHER
                mimeType == "application/pdf" -> PDF
                mimeType == "application/msword" -> DOC
                mimeType.startsWith("application/vnd.openxmlformats-officedocument") -> DOCX
                mimeType.startsWith("image/") -> IMAGE
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                mimeType.startsWith("application/") -> OTHER
                else -> OTHER
            }
        }
    }
}

@Serializable
data class PickedDocument(
    val uri: String,
    val displayName: String,
    val type: DocumentType,
    val sizeBytes: Long,
    val lastModified: Long?,
    val mimeType: String?
) {
    val isImage: Boolean get() = type == DocumentType.IMAGE
    val isVideo: Boolean get() = type == DocumentType.VIDEO
    val isAudio: Boolean get() = type == DocumentType.AUDIO
    val isLargeFile: Boolean get() = sizeBytes > 10 * 1024 * 1024

    companion object {
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }

        fun create(
            uri: String,
            displayName: String,
            sizeBytes: Long,
            lastModified: Long?,
            mimeType: String?
        ): PickedDocument {
            return PickedDocument(
                uri = uri,
                displayName = displayName,
                type = DocumentType.fromMimeType(mimeType),
                sizeBytes = sizeBytes,
                lastModified = lastModified,
                mimeType = mimeType
            )
        }
    }
}

@Serializable
data class DocumentPickerConfig(
    val maxSelectionLimit: Int = 1,
    val allowedTypes: List<DocumentType> = emptyList(),
    val allowMultipleSelection: Boolean = false
) {
    fun getMimeTypeFilter(): String {
        return when {
            allowedTypes.isEmpty() -> "*/*"
            allowedTypes.size == 1 -> allowedTypes.first().mimeType
            else -> allowedTypes.firstOrNull()?.mimeType ?: "*/*"
        }
    }

    companion object {
        val singleDocument = DocumentPickerConfig(1, emptyList(), false)
        val multipleDocuments = DocumentPickerConfig(10, emptyList(), true)
        val pdfOnly = DocumentPickerConfig(1, listOf(DocumentType.PDF), false)
        val imagesOnly = DocumentPickerConfig(5, listOf(DocumentType.IMAGE), true)
        val mediaOnly = DocumentPickerConfig(
            5,
            listOf(DocumentType.IMAGE, DocumentType.VIDEO, DocumentType.AUDIO),
            true
        )
    }
}

@Serializable
data class DocumentBatchResult(
    val documents: List<PickedDocument>,
    val config: DocumentPickerConfig,
    val totalSizeBytes: Long = documents.sumOf { it.sizeBytes }
) {
    val isEmpty: Boolean get() = documents.isEmpty()
    val count: Int get() = documents.size
    val totalSize: Long get() = totalSizeBytes
}

sealed class FilePickerResult {
    data class Success(val document: PickedDocument) : FilePickerResult()
    data class MultipleSuccess(val documents: List<PickedDocument>) : FilePickerResult()
    data class Failure(val error: String) : FilePickerResult()
    val isSuccess: Boolean get() = this is Success || this is MultipleSuccess
    val isFailure: Boolean get() = this is Failure
    fun getOrNull(): PickedDocument? = when (this) { is Success -> document; else -> null }
    fun getOrNullList(): List<PickedDocument>? = when (this) {
        is Success -> listOf(document)
        is MultipleSuccess -> documents
        else -> null
    }
    fun errorOrNull(): String? = when (this) { is Failure -> error; else -> null }
}
