package me.plexs.music.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

private const val QR_PREFIX = "plexqr://swap?token="

/**
 * Live QR scanner that fires [onToken] once with a token from the desktop site's
 * `plexqr://swap?token=...` QR payload. Pairs the app with the desktop session.
 */
@Composable
fun QrScanner(
    onToken: (String) -> Unit,
    onError: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val handled = remember { mutableStateOf(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    AndroidView(factory = { previewView }, modifier = modifier)

    LaunchedEffect(Unit) {
        if (handled.value) return@LaunchedEffect
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        val executor = ContextCompat.getMainExecutor(context)
        analysis.setAnalyzer(executor) { image ->
            val media = image.image
            if (media != null && !handled.value) {
                val input = InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees)
                scanner.process(input)
                    .addOnSuccessListener { barcodes ->
                        for (b in barcodes) {
                            val raw = b.rawValue ?: continue
                            if (raw.startsWith(QR_PREFIX)) {
                                val token = raw.removePrefix(QR_PREFIX)
                                if (token.isNotEmpty() && token.length <= 64 && !handled.value) {
                                    handled.value = true
                                    onToken(token)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("PlexQr", "Barcode processing failed", e)
                    }
                    .addOnCompleteListener { image.close() }
            } else {
                image.close()
            }
        }
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                val provider = future.get()
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            } catch (e: Exception) {
                onError("Could not start camera")
                Log.w("PlexQr", "Camera bind failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
}
