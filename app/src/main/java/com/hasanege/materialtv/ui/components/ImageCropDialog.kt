package com.hasanege.materialtv.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun cropBitmap(context: Context, sourceBitmap: Bitmap, scale: Float, offset: Offset, viewSizeDp: Int): Bitmap? {
    try {
        val density = context.resources.displayMetrics.density
        val viewSizePx = viewSizeDp * density
        
        val destSize = 512
        val cropped = Bitmap.createBitmap(destSize, destSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(cropped)
        
        val matrix = android.graphics.Matrix()
        
        val destToViewScale = destSize / viewSizePx
        
        val srcWidth = sourceBitmap.width
        val srcHeight = sourceBitmap.height
        val fitScale = Math.min(viewSizePx / srcWidth, viewSizePx / srcHeight)
        val fittedWidth = srcWidth * fitScale
        val fittedHeight = srcHeight * fitScale
        
        val startX = (viewSizePx - fittedWidth) / 2f
        val startY = (viewSizePx - fittedHeight) / 2f
        
        matrix.postTranslate(-srcWidth / 2f, -srcHeight / 2f)
        matrix.postScale(fitScale * scale, fitScale * scale)
        matrix.postTranslate(viewSizePx / 2f + offset.x, viewSizePx / 2f + offset.y)
        matrix.postScale(destToViewScale, destToViewScale)
        
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(sourceBitmap, matrix, paint)
        return cropped
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            bitmap = decodeUriToBitmap(context, imageUri)
        }
    }
    
    if (bitmap == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        )
        return
    }
    
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Crop Profile Image",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            ) 
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = offset + pan
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val circleRadius = size.minDimension / 2.5f
                        val circleCenter = Offset(size.width / 2f, size.height / 2f)
                        
                        val path = Path().apply {
                            addRect(Rect(Offset.Zero, size))
                        }
                        val circlePath = Path().apply {
                            addOval(Rect(circleCenter, circleRadius))
                        }
                        
                        val resultPath = Path.combine(PathOperation.Difference, path, circlePath)
                        drawPath(resultPath, Color.Black.copy(alpha = 0.6f))
                        
                        drawCircle(
                            color = Color.White,
                            radius = circleRadius,
                            center = circleCenter,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
                
                Text(
                    text = "Pinch to zoom and drag to pan inside the circle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val croppedBitmap = cropBitmap(context, bitmap!!, scale, offset, 260)
                    if (croppedBitmap != null) {
                        onCropSuccess(croppedBitmap)
                    }
                    onDismiss()
                }
            ) {
                Text("Crop & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
