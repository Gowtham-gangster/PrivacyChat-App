package com.privychat.app.core.util

object PhoneNumberHelper {

    /**
     * Normalizes a user-entered phone number into standard international E.164 format (^\+[1-9]\d{1,14}$).
     *
     * Handles:
     * - Stripping whitespace, hyphens, parentheses, and dots.
     * - Replacing international exit code '00' with '+'.
     * - 10-digit Indian mobile numbers (e.g., 9705067505) -> auto-prefix '+91'.
     * - 11-digit numbers with leading zero (e.g., 09705067505) -> remove '0' and prefix '+91'.
     * - 12-digit Indian numbers without plus (e.g., 919705067505) -> prefix '+'.
     * - 11-digit North American numbers without plus (e.g., 12025550143) -> prefix '+'.
     * - Already formatted E.164 numbers (e.g., +919705067505, +12025550143) -> preserved.
     */
    fun formatToE164(raw: String): String {
        val clean = raw.trim().replace(Regex("[\\s\\-\\(\\)\\.]"), "")
        if (clean.isEmpty()) return ""

        // If already starts with '+', return as-is
        if (clean.startsWith("+")) return clean

        // If user typed international exit code '00'
        if (clean.startsWith("00")) return "+" + clean.substring(2)

        // If user typed leading 0 followed by 10-digit Indian mobile (starts with 6, 7, 8, 9)
        if (clean.startsWith("0") && clean.length == 11 && clean.substring(1).matches(Regex("^[6-9]\\d{9}$"))) {
            return "+91" + clean.substring(1)
        }

        // 10-digit Indian mobile number (standard 6-9 leading digit)
        if (clean.length == 10 && clean.matches(Regex("^[6-9]\\d{9}$"))) {
            return "+91$clean"
        }

        // 12-digit Indian number with '91' country code but no '+'
        if (clean.length == 12 && clean.startsWith("91") && clean.substring(2).matches(Regex("^[6-9]\\d{9}$"))) {
            return "+$clean"
        }

        // 11-digit US/Canada number with '1' country code but no '+'
        if (clean.length == 11 && clean.startsWith("1")) {
            return "+$clean"
        }

        // Generic fallback: if 7 to 15 numeric digits, prepend '+'
        if (clean.matches(Regex("^\\d{7,15}$"))) {
            return "+$clean"
        }

        return clean
    }

    /**
     * Checks if the phone number satisfies strict E.164 requirements:
     * Starts with '+', followed by country code (1-9) and 1 to 14 subscriber digits.
     */
    fun isValidE164(phone: String): Boolean {
        return phone.matches(Regex("^\\+[1-9]\\d{1,14}$"))
    }
}

