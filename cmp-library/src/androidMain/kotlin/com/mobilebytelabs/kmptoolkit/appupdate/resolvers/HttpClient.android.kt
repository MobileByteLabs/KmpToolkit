package com.mobilebytelabs.kmptoolkit.appupdate.resolvers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android implementation of HTTP client.
 * Uses HttpURLConnection for compatibility.
 */
internal actual object HttpClient {
    actual suspend fun get(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        headers.forEach { (key, value) ->
            connection.setRequestProperty(key, value)
        }

        try {
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
            }
        } finally {
            connection.disconnect()
        }
    }
}
