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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hasanege.materialtv.R
import com.hasanege.materialtv.ui.theme.ExpressiveShapes
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
        val circleRadius = viewSizePx / 2.5f
        val circleDiameter = circleRadius * 2f
        
        val destSize = 512
        val cropped = Bitmap.createBitmap(destSize, destSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(cropped)
        
        val srcWidth = sourceBitmap.width.toFloat()
        val srcHeight = sourceBitmap.height.toFloat()
        
        // Base scale using ContentScale.Crop logic (fills viewSizePx)
        val baseScale = Math.max(viewSizePx / srcWidth, viewSizePx / srcHeight)
        val totalScale = baseScale * scale
        
        val matrix = android.graphics.Matrix()
        matrix.postTranslate(-srcWidth / 2f, -srcHeight / 2f)
        matrix.postScale(totalScale, totalScale)
        matrix.postTranslate(viewSizePx / 2f + offset.x, viewSizePx / 2f + offset.y)
        
        val cropLeft = (viewSizePx - circleDiameter) / 2f
        val cropTop = (viewSizePx - circleDiameter) / 2f
        matrix.postTranslate(-cropLeft, -cropTop)
        
        val scaleToDest = destSize / circleDiameter
        matrix.postScale(scaleToDest, scaleToDest)
        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropSuccess: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            bitmap = decodeUriToBitmap(context, imageUri)
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            if (bitmap == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.crop_profile_image_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val currentBitmap = bitmap!!
                var scale by remember { mutableFloatStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                val density = LocalContext.current.resources.displayMetrics.density
                val viewSizePx = 320f * density
                val circleRadiusPx = viewSizePx / 2.5f
                val circleDiameterPx = circleRadiusPx * 2f

                val srcW = currentBitmap.width.toFloat()
                val srcH = currentBitmap.height.toFloat()
                val baseScale = Math.max(viewSizePx / srcW, viewSizePx / srcH)

                fun clampOffset(proposedOffset: Offset, currentScale: Float): Offset {
                    val scaledW = srcW * baseScale * currentScale
                    val scaledH = srcH * baseScale * currentScale
                    val maxDx = ((scaledW - circleDiameterPx) / 2f).coerceAtLeast(0f)
                    val maxDy = ((scaledH - circleDiameterPx) / 2f).coerceAtLeast(0f)
                    return Offset(
                        proposedOffset.x.coerceIn(-maxDx, maxDx),
                        proposedOffset.y.coerceIn(-maxDy, maxDy)
                    )
                }

                fun setScaleAndClamp(newScale: Float) {
                    val clampedScale = newScale.coerceIn(1f, 5f)
                    scale = clampedScale
                    offset = clampOffset(offset, clampedScale)
                }

                val primaryColor = MaterialTheme.colorScheme.primary

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // ──────────────────────────────────────────────────────────
                    // Top Bar — Expressive Header
                    // ──────────────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(R.string.crop_profile_image_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Reset button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(42.dp)
                        ) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scale = 1f
                                offset = Offset.Zero
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Center Viewport Box
                    // ──────────────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .size(320.dp),
                            shape = ExpressiveShapes.ExtraLarge,
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color.Black
                            ),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                                            scale = newScale
                                            offset = clampOffset(offset + pan, newScale)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                    contentScale = ContentScale.Crop
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
                                    drawPath(resultPath, Color.Black.copy(alpha = 0.70f))
                                    
                                    // Outer glowing ring
                                    drawCircle(
                                        color = primaryColor,
                                        radius = circleRadius + 2.dp.toPx(),
                                        center = circleCenter,
                                        style = Stroke(width = 3.dp.toPx())
                                    )

                                    // Inner white ring
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.8f),
                                        radius = circleRadius,
                                        center = circleCenter,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    // ──────────────────────────────────────────────────────────
                    // Control Deck — Zoom Slider & Scale Presets
                    // ──────────────────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Zoom Slider Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ExpressiveShapes.Medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    setScaleAndClamp(scale - 0.25f)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Zoom Out",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Slider(
                                value = scale,
                                onValueChange = { setScaleAndClamp(it) },
                                valueRange = 1f..4f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )

                            IconButton(
                                onClick = {
                                    setScaleAndClamp(scale + 0.25f)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Zoom In",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Scale Preset Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(1.0f to "1.0x", 1.5f to "1.5x", 2.0f to "2.0x", 3.0f to "3.0x").forEach { (presetValue, label) ->
                                val isSelected = Math.abs(scale - presetValue) < 0.1f
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        setScaleAndClamp(presetValue)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = ExpressiveShapes.Small,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Text(
                            text = stringResource(R.string.crop_profile_image_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // ──────────────────────────────────────────────────────────
                    // Bottom Action Deck
                    // ──────────────────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = ExpressiveShapes.Medium
                        ) {
                            Text(
                                text = stringResource(R.string.profile_cancel),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val croppedBitmap = cropBitmap(context, bitmap!!, scale, offset, 320)
                                if (croppedBitmap != null) {
                                    onCropSuccess(croppedBitmap)
                                }
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp),
                            shape = ExpressiveShapes.Medium,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp).size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.crop_and_save),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
