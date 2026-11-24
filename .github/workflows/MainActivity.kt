package com.example.ytdlpapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private val FORMATS = listOf("Best (MP4)", "Up to 1080p (MP4)", "Up to 720p (MP4)", "Audio only (MP3)")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = if (intent?.action == Intent.ACTION_SEND)
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: "" else ""
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { DownloaderScreen(shared) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloaderScreen(initialUrl: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val app = ctx.applicationContext as App
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf(initialUrl) }
    var fmt by remember { mutableIntStateOf(0) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var playlist by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var status by remember { mutableStateOf("Starting engine...") }
    var log by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (!app.ready && app.initError == null) delay(200)
        status = app.initError?.let { "Engine failed: $it" } ?: "Ready"
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("yt-dlp Downloader", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = url, onValueChange = { url = it },
            label = { Text("Video URL") }, modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenuBox(expanded = menu, onExpandedChange = { menu = it }) {
            OutlinedTextField(
                value = FORMATS[fmt], onValueChange = {}, readOnly = true,
                label = { Text("Format") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menu) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                FORMATS.forEachIndexed { i, name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { fmt = i; menu = false })
                }
            }
        }

        Text("Clip a section (optional): SS, MM:SS or HH:MM:SS", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(start, { start = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(end, { end = it }, label = { Text("End") }, modifier = Modifier.weight(1f), singleLine = true)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(playlist, { playlist = it })
            Text("Download whole playlist")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = !busy && app.ready && url.isNotBlank(),
                onClick = {
                    busy = true; progress = 0f; status = "Downloading..."; log = ""
                    scope.launch {
                        val result = withContext(Dispatchers.IO) {
                            runDownload(ctx, url.trim(), fmt, start.trim(), end.trim(), playlist) { p, line ->
                                progress = p / 100f
                                if (line.isNotBlank()) log = (log + line.trim() + "\n").takeLast(4000)
                            }
                        }
                        status = result
                        busy = false
                    }
                }
            ) { Text("Download") }

            OutlinedButton(
                enabled = !busy && app.ready,
                onClick = {
                    busy = true; status = "Updating yt-dlp..."
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            try {
                                val s = YoutubeDL.getInstance().updateYoutubeDL(ctx, YoutubeDL.UpdateChannel.STABLE)
                                "Update: $s"
                            } catch (e: Exception) { "Update failed: ${e.message}" }
                        }
                        busy = false
                    }
                }
            ) { Text("Update yt-dlp") }
        }

        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(status)
        Text(log, style = MaterialTheme.typography.bodySmall)
    }
}

private fun runDownload(
    ctx: Context, url: String, fmt: Int, start: String, end: String, playlist: Boolean,
    onProgress: (Float, String) -> Unit
): String {
    val tmp = File(ctx.getExternalFilesDir(null), "dl-" + UUID.randomUUID()).apply { mkdirs() }
    return try {
        val req = YoutubeDLRequest(url)
        req.addOption("-o", tmp.absolutePath + "/%(title).150B [%(id)s].%(ext)s")
        req.addOption("--windows-filenames")
        if (!playlist) req.addOption("--no-playlist")

        when (fmt) {
            0 -> { req.addOption("-f", "bv*+ba/b"); req.addOption("--merge-output-format", "mp4") }
            1 -> { req.addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]"); req.addOption("--merge-output-format", "mp4") }
            2 -> { req.addOption("-f", "bv*[height<=720]+ba/b[height<=720]"); req.addOption("--merge-output-format", "mp4") }
            else -> { req.addOption("-x"); req.addOption("--audio-format", "mp3") }
        }

        if (start.isNotEmpty() || end.isNotEmpty()) {
            req.addOption("--download-sections", "*${start.ifEmpty { "0" }}-${end.ifEmpty { "inf" }}")
            req.addOption("--force-keyframes-at-cuts")
        }

        YoutubeDL.getInstance().execute(req, UUID.randomUUID().toString()) { p, _, line ->
            onProgress(p, line)
        }
        val n = publish(ctx, tmp)
        if (n == 0) "Finished, but no file was produced" else "Saved $n file(s) to Downloads/YtDlpApp"
    } catch (e: Exception) {
        "Error: ${e.message}"
    } finally {
        tmp.deleteRecursively()
    }
}

/** Moves finished files into the public Downloads/YtDlpApp folder via MediaStore. */
private fun publish(ctx: Context, dir: File): Int {
    var count = 0
    dir.listFiles()?.forEach { f ->
        if (f.extension in setOf("part", "ytdl", "tmp")) return@forEach
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, f.name)
            put(MediaStore.Downloads.MIME_TYPE,
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension) ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/YtDlpApp")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@forEach
        ctx.contentResolver.openOutputStream(uri)?.use { out -> f.inputStream().use { it.copyTo(out) } }
        count++
    }
    return count
}
