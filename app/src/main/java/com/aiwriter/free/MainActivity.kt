package com.aiwriter.free

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var aiEngine: LocalAIEngine
    
    private lateinit var statusText: TextView
    private lateinit var downloadButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    
    private var isDownloading = false
                }
                
                override fun onComplete(success: Boolean) {
                    runOnUiThread {
                        if (success) {
                            scope.launch {
                                aiEngine.initialize()
                                statusText.text = "✓ Download Complete!\n\nAI Writer is ready to use."
                                downloadButton.visibility = View.GONE
                                progressBar.visibility = View.GONE
                                progressText.visibility = View.GONE
                            }
                        } else {
                            statusText.text = "Download failed. Please try again."
                            downloadButton.isEnabled = true
                            progressBar.visibility = View.GONE
                            progressText.visibility = View.GONE
                        }
                    }

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        aiEngine = (application as AIWriterApplication).aiEngine
        
        statusText = findViewById(R.id.statusText)
        downloadButton = findViewById(R.id.downloadButton)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        
        downloadButton.setOnClickListener {
            downloadModel()
        }
        
        updateUI()
    }
    

    
    private fun updateUI() {
        if (aiEngine.isModelDownloaded()) {
            statusText.text = "✓ AI Model Ready\n\nSelect text anywhere and choose 'AI Writer' from the menu."
            downloadButton.visibility = View.GONE
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
        } else {
            statusText.text = "AI Writer needs to download a 1.5GB model file.\n\nThis is a one-time download."
            downloadButton.visibility = View.VISIBLE
            downloadButton.text = "Download Model (1.5GB)"
            progressBar.visibility = View.GONE
            progressText.visibility = View.GONE
        }
    }
    
    private fun downloadModel() {
        if (isDownloading) return
        
        isDownloading = true
        downloadButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressText.visibility = View.VISIBLE
        statusText.text = "Downloading AI model..."
        
        scope.launch {
            try {
                val modelFile = File(filesDir, "qwen2.5-1.5b-q4.onnx")
                val tempFile = File(filesDir, "qwen2.5-1.5b-q4.onnx.tmp")
                
                val url = URL("https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-ONNX/resolve/main/qwen2.5-1.5b-instruct-q4.onnx")
                
                withContext(Dispatchers.IO) {
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()
                    
                    val fileLength = connection.contentLength
                    val input = connection.inputStream
                    val output = FileOutputStream(tempFile)
                    
                    val buffer = ByteArray(8192)
                    var downloaded = 0L
                    var read: Int
                    
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        
                        val percent = (downloaded * 100 / fileLength).toInt()
                        withContext(Dispatchers.Main) {
                            progressBar.progress = percent
                            progressText.text = "${downloaded / 1024 / 1024} MB / ${fileLength / 1024 / 1024} MB"
                        }
                    }
                    
                    output.flush()
                    output.close()
                    input.close()
                    
                    tempFile.renameTo(modelFile)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Download complete! Initializing...", Toast.LENGTH_SHORT).show()
                    updateUI()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Error: ${e.message}\n\nPlease try again."
                    downloadButton.isEnabled = true
                    progressBar.visibility = View.GONE
                    progressText.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isDownloading = false
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
