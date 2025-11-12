package com.guyghost.wakeve.models

/**
 * Implémentation JVM pour getCurrentTimeMillis
 */
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()
