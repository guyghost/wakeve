package com.guyghost.wakeve.models

import platform.Foundation.NSDate

/**
 * Implémentation iOS pour getCurrentTimeMillis
 */
actual fun getCurrentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
