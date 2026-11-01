package com.privychat.app.core.network

import com.google.gson.Gson
import retrofit2.Response

object ApiErrorParser {

    private val gson = Gson()

    /**
     * Safely extracts and formats user-facing validation and error messages from backend responses.
     * Never exposes internal stack traces, database details, or credentials.
     */
    fun parseError(response: Response<*>, defaultMessage: String = "Request failed"): String {
        val errorJson = response.errorBody()?.string()
        if (errorJson.isNullOrBlank()) {
            return "$defaultMessage (${response.code()})"
        }

        return try {
            val map = gson.fromJson(errorJson, Map::class.java) as? Map<*, *>
                ?: return "$defaultMessage (${response.code()})"

            // 1. Check for Spring validation details map: "details": { "fieldName": "error message" }
            val details = map["details"] as? Map<*, *>
            if (!details.isNullOrEmpty()) {
                val messages = details.values.mapNotNull { it?.toString()?.trim() }.filter { it.isNotEmpty() }
                if (messages.isNotEmpty()) {
                    return messages.joinToString("; ")
                }
            }

            // 2. Check for top-level "message" field (e.g. from BadRequestException)
            val message = map["message"] as? String
            if (!message.isNullOrBlank()) {
                return message.trim()
            }

            // 3. Check for "error" field if informative
            val error = map["error"] as? String
            if (!error.isNullOrBlank() && !error.equals("Bad Request", ignoreCase = true)) {
                return error.trim()
            }

            "$defaultMessage (${response.code()})"
        } catch (_: Exception) {
            "$defaultMessage (${response.code()})"
        }
    }
}

