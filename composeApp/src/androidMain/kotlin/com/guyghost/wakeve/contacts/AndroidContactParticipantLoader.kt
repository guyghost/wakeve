package com.guyghost.wakeve.contacts

import android.content.Context
import android.provider.ContactsContract
import com.guyghost.wakeve.contacts.ContactParticipantCandidate

fun loadAndroidContactParticipantCandidates(context: Context): List<ContactParticipantCandidate> {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
        ContactsContract.CommonDataKinds.Email.ADDRESS
    )
    val sortOrder = "${ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"

    return context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY)
        val emailIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val candidates = mutableListOf<ContactParticipantCandidate>()

        while (cursor.moveToNext()) {
            val email = cursor.getString(emailIndex)?.trim().orEmpty()
            if (email.isBlank()) continue

            val displayName = cursor.getString(nameIndex)?.trim().orEmpty()
            candidates += ContactParticipantCandidate(
                displayName = displayName.ifBlank { email },
                email = email
            )
        }

        candidates
    }.orEmpty()
}
