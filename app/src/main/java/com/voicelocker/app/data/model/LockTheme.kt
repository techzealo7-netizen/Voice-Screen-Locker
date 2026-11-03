package com.voicelocker.app.data.model

class LockTheme(val name: String, val c1: Long, val c2: Long)

/**
 * Wallpaper i uses drawable "theme{i+1}" if present, else this gradient.
 */
val lockThemes = listOf(
    LockTheme("Neon Wave", 0xFF0D023B, 0xFF0072FF),
    LockTheme("Luxury Gold", 0xFF0B0B0D, 0xFFE5A93C),
    LockTheme("Pastel Gradient", 0xFFE070D7, 0xFF6DB3F2),
    LockTheme("Purple Flame", 0xFF0A0214, 0xFFE52055),
    LockTheme("Cyber Voice", 0xFF031026, 0xFF00A2FF),
    LockTheme("Full Moon Lake", 0xFF051336, 0xFF1C6FD8),
    LockTheme("Blue Lily", 0xFF050B17, 0xFF1B64E8),
    LockTheme("Ocean Sunset", 0xFF1A1A3A, 0xFFFF7A18),
    LockTheme("Blue Moon Cabin", 0xFF061A44, 0xFF1D6FD8),
    LockTheme("Underwater", 0xFF0B3C5D, 0xFF3CC4B0),
)

