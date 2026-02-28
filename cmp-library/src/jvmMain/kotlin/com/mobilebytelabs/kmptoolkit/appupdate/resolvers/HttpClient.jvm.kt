package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * JVM implementation of HTTP client using HttpURLConnection.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        // Set headers
        headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                val errorBody = connection.errorStream?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                } ?: ""
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}. $errorBody")
            }
        } finally {
            connection.disconnect()
        }
    }
}
