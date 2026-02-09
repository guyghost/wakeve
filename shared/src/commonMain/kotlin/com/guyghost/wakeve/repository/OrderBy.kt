package com.guyghost.wakeve.repository

/**
 * Enumeration for ordering options in pagination queries.
 */
enum class OrderBy {
    CREATED_AT_DESC,
    CREATED_AT_ASC,
    TITLE_ASC,
    TITLE_DESC,
    STATUS_ASC,
    STATUS_DESC
}
