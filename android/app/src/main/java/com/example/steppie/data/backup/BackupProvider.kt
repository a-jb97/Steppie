package com.example.steppie.data.backup

import android.net.Uri

interface BackupProvider {
    suspend fun exportTo(uri: Uri)
    suspend fun previewImport(uri: Uri): BackupImportPreview
    suspend fun restoreReplace(uri: Uri)
}
