package com.example.boulderdownloader

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.net.URL
import java.util.regex.Pattern

class ShareActivity : AppCompatActivity() {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                handleSharedContent(sharedText)
            } ?: finish()
        } else {
            finish()
        }
    }

    private fun handleSharedContent(text: String) {
        val urlPattern = Pattern.compile("https?://[\\w./?-]+")
        val matcher = urlPattern.matcher(text)
        val targetUrl = if (matcher.find()) matcher.group() else text

        coroutineScope.launch {
            try {
                val imageUrl = resolveImageUrl(targetUrl)
                downloadAndSaveImage(imageUrl)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareActivity, "Obrázek úspěšně uložen!", Toast.LENGTH_LONG).show()
                    finish()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ShareActivity, "Chyba při stahování: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    private fun resolveImageUrl(pageUrl: String): String {
        if (pageUrl.endsWith(".jpg", true) || pageUrl.endsWith(".png", true)) {
            return pageUrl
        }
        val connection = URL(pageUrl).openConnection()
        connection.connectTimeout = 5000
        val html = connection.getInputStream().bufferedReader().use { it.readText() }

        val ogImagePattern = Pattern.compile("<meta property=\"og:image\" content=\"([^\"]+)\"")
        val matcher = ogImagePattern.matcher(html)

        if (matcher.find()) {
            return matcher.group(1) ?: pageUrl
        }
        return pageUrl
    }

    private fun downloadAndSaveImage(imageUrl: String) {
        val inputStream = URL(imageUrl).openStream()
        val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        val targetDir = File(picturesDir, "Obrysky")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val file = File(targetDir, "boulder_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { output ->
            inputStream.copyTo(output)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
