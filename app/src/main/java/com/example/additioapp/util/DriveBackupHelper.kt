package com.example.additioapp.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Helper class for Google Drive backup/restore operations.
 * Uses Drive REST v3 API with appDataFolder for private app backups.
 */
class DriveBackupHelper(private val context: Context) {

    companion object {
        private const val TAG = "DriveBackupHelper"
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val MAX_BACKUPS = 5
    }

    private val driveAppDataScope = Scope(DRIVE_SCOPE)
    
    /**
     * Data class representing a backup file in Drive
     */
    data class DriveBackupFile(
        val id: String,
        val name: String,
        val modifiedTime: String
    )

    /**
     * Check if authorization is needed and get authorization request
     */
    fun createAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(listOf(driveAppDataScope))
            .build()
    }

    /**
     * Authorize with Google Drive - returns access token or null if consent needed
     */
    suspend fun authorize(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): String? = suspendCancellableCoroutine { cont ->
        val request = createAuthorizationRequest()
        
        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    // Need user consent - launch the intent
                    try {
                        launcher.launch(
                            IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                        )
                        // Will be resumed via handleAuthorizationResult
                        cont.resume(null)
                    } catch (e: Exception) {
                        cont.resumeWithException(e)
                    }
                } else {
                    // Already authorized
                    cont.resume(result.accessToken)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Authorization failed", e)
                cont.resumeWithException(e)
            }
    }

    /**
     * Get access token from authorization result (after user consent)
     */
    fun getTokenFromResult(activity: Activity, result: androidx.activity.result.ActivityResult): String? {
        return try {
            val authResult = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(result.data)
            authResult.accessToken
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get token from result", e)
            null
        }
    }

    /**
     * Upload backup JSON to Drive appDataFolder
     */
    suspend fun uploadBackup(accessToken: String, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
            val fileName = "backup_$timestamp.json"
            
            val boundary = "====${System.currentTimeMillis()}===="
            val url = URL("$DRIVE_UPLOAD_URL?uploadType=multipart")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connection.doOutput = true

            val metadata = """{"name": "$fileName", "parents": ["appDataFolder"]}"""
            
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata)
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json\r\n\r\n")
                append(jsonContent)
                append("\r\n--$boundary--")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Upload response code: $responseCode")
            
            if (responseCode != 200) {
                // Read error response
                val errorBody = try {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error body"
                } catch (e: Exception) {
                    "Could not read error: ${e.message}"
                }
                Log.e(TAG, "Upload failed with code $responseCode: $errorBody")
            } else {
                // Delete old backups to keep only MAX_BACKUPS
                deleteOldBackups(accessToken)
            }
            
            responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            false
        }
    }

    /**
     * List backup files in appDataFolder
     */
    suspend fun listBackups(accessToken: String): List<DriveBackupFile> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DRIVE_FILES_URL?spaces=appDataFolder&orderBy=modifiedTime desc&fields=files(id,name,modifiedTime)")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "List backups failed: $responseCode")
                return@withContext emptyList()
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            parseBackupList(response)
        } catch (e: Exception) {
            Log.e(TAG, "List backups failed", e)
            emptyList()
        }
    }

    /**
     * Download backup file content
     */
    suspend fun downloadBackup(accessToken: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DRIVE_FILES_URL/$fileId?alt=media")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.e(TAG, "Download failed: $responseCode")
                return@withContext null
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            null
        }
    }

    /**
     * Delete old backups, keeping only MAX_BACKUPS
     */
    private suspend fun deleteOldBackups(accessToken: String) {
        withContext(Dispatchers.IO) {
            try {
                val backups = listBackups(accessToken)
                if (backups.size > MAX_BACKUPS) {
                    val toDelete = backups.drop(MAX_BACKUPS)
                    toDelete.forEach { backup ->
                        deleteBackupFile(accessToken, backup.id)
                    }
                    Log.d(TAG, "Deleted ${toDelete.size} old backups")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Delete old backups failed", e)
            }
            Unit
        }
    }

    /**
     * Delete a backup file from Drive
     */
    suspend fun deleteBackupFile(accessToken: String, fileId: String) = withContext(Dispatchers.IO) {
        try {
            val url = URL("$DRIVE_FILES_URL/$fileId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.responseCode // Execute the request
        } catch (e: Exception) {
            Log.e(TAG, "Delete file failed: $fileId", e)
        }
    }

    /**
     * Parse JSON response from files.list API
     */
    private fun parseBackupList(json: String): List<DriveBackupFile> {
        val files = mutableListOf<DriveBackupFile>()
        try {
            // Simple JSON parsing without adding Gson dependency just for this
            val filesMatch = Regex(""""files"\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(json)
            val filesContent = filesMatch?.groupValues?.get(1) ?: return files
            
            val filePattern = Regex(""""id"\s*:\s*"([^"]+)".*?"name"\s*:\s*"([^"]+)".*?"modifiedTime"\s*:\s*"([^"]+)"""", RegexOption.DOT_MATCHES_ALL)
            filePattern.findAll(filesContent).forEach { match ->
                files.add(DriveBackupFile(
                    id = match.groupValues[1],
                    name = match.groupValues[2],
                    modifiedTime = match.groupValues[3]
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse backup list failed", e)
        }
        return files
    }
}
