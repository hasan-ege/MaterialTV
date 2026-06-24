package com.hasanege.materialtv.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun ImageCropperDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onCrop: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(uri) {
        scope.launch {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false) // Needs to be software for pixel manipulation
                .build()
            
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val b = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
            if (b != null) {
                originalBitmap = b
                bitmap = b.asImageBitmap()
            }
        }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (bitmap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offset += pan
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            canvasSize = size
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            
                            val circleRadius = min(canvasWidth, canvasHeight) * 0.4f
                            val circleCenter = Offset(canvasWidth / 2, canvasHeight / 2)

                            // Draw the image
                            val imgWidth = bitmap!!.width * scale
                            val imgHeight = bitmap!!.height * scale
                            val imgX = circleCenter.x - (imgWidth / 2) + offset.x
                            val imgY = circleCenter.y - (imgHeight / 2) + offset.y

                            withTransform({
                                translate(left = imgX, top = imgY)
                                scale(scale, scale, pivot = Offset.Zero)
                            }) {
                                drawImage(bitmap!!)
                            }

                            // Draw the dark overlay with a circular cutout
                            val overlayPath = Path().apply {
                                addRect(Rect(0f, 0f, canvasWidth, canvasHeight))
                                addOval(Rect(
                                    circleCenter.x - circleRadius,
                                    circleCenter.y - circleRadius,
                                    circleCenter.x + circleRadius,
                                    circleCenter.y + circleRadius
                                ))
                                fillType = PathFillType.EvenOdd
                            }
                            
                            drawPath(
                                path = overlayPath,
                                color = Color.Black.copy(alpha = 0.7f)
                            )
                            
                            // Draw a white border around the circle
                            drawCircle(
                                color = Color.White,
                                radius = circleRadius,
                                center = circleCenter,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("İptal", color = Color.White)
                        }
                        Button(onClick = {
                            if (originalBitmap != null && canvasSize != Size.Zero) {
                                val canvasWidth = canvasSize.width
                                val canvasHeight = canvasSize.height
                                val circleRadius = min(canvasWidth, canvasHeight) * 0.4f
                                val circleCenter = Offset(canvasWidth / 2, canvasHeight / 2)
                                
                                val imgWidth = originalBitmap!!.width * scale
                                val imgHeight = originalBitmap!!.height * scale
                                val imgX = circleCenter.x - (imgWidth / 2) + offset.x
                                val imgY = circleCenter.y - (imgHeight / 2) + offset.y
                                
                                val cropSizeCanvas = circleRadius * 2
                                val mappedX = (circleCenter.x - circleRadius - imgX) / scale
                                val mappedY = (circleCenter.y - circleRadius - imgY) / scale
                                val mappedSize = cropSizeCanvas / scale
                                
                                val finalX = mappedX.toInt().coerceIn(0, originalBitmap!!.width - 1)
                                val finalY = mappedY.toInt().coerceIn(0, originalBitmap!!.height - 1)
                                val finalW = mappedSize.toInt().coerceAtMost(originalBitmap!!.width - finalX)
                                val finalH = mappedSize.toInt().coerceAtMost(originalBitmap!!.height - finalY)
                                
                                if (finalW > 0 && finalH > 0) {
                                    val cropped = Bitmap.createBitmap(originalBitmap!!, finalX, finalY, finalW, finalH)
                                    val circular = getCircularBitmap(cropped)
                                    onCrop(circular)
                                } else {
                                    val circular = getCircularBitmap(originalBitmap!!)
                                    onCrop(circular)
                                }
                            }
                        }) {
                            Text("Kaydet")
                        }
                    }
                }
            }
        }
    }
}

private fun getCircularBitmap(src: Bitmap): Bitmap {
    val size = min(src.width, src.height)
    val dst = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(dst)
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
    }
    canvas.drawARGB(0, 0, 0, 0)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    
    val left = (size - src.width) / 2f
    val top = (size - src.height) / 2f
    canvas.drawBitmap(src, left, top, paint)
    return dst
}
