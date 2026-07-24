package com.hasanege.materialtv.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Material 3 Expressive Shape System
 * All shapes from the M3 Expressive design guidelines
 */

// Standard M3 Expressive Rounded Shapes
object ExpressiveShapes {
    val ExtraSmall = RoundedCornerShape(12.dp)
    val Small = RoundedCornerShape(16.dp)
    val Medium = RoundedCornerShape(20.dp)
    val Large = RoundedCornerShape(24.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val Full = RoundedCornerShape(50)
    val Hourglass = RoundedCornerShape(topStartPercent = 48, topEndPercent = 12, bottomEndPercent = 48, bottomStartPercent = 12)
}

// ===== WAVY SHAPES =====

/** Wavy Circle - 8 petals/waves */
class WavyCircle8 : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val outerR = minOf(size.width, size.height) / 2
            val innerR = outerR * 0.85f
            val waves = 8
            val points = waves * 2
            
            for (i in 0 until points) {
                val angle = 2 * PI * i / points - PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Wavy Circle - 6 petals/waves */
class WavyCircle6 : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val outerR = minOf(size.width, size.height) / 2
            val innerR = outerR * 0.8f
            val waves = 6
            val points = waves * 2
            
            for (i in 0 until points) {
                val angle = 2 * PI * i / points - PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Scalloped Circle - Many small waves */
class ScallopedCircle : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val outerR = minOf(size.width, size.height) / 2
            val innerR = outerR * 0.9f
            val waves = 12
            val points = waves * 2
            
            for (i in 0 until points) {
                val angle = 2 * PI * i / points - PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== CLOVER/FLOWER SHAPES =====

/** Clover - 4 leaf clover shape */
class CloverShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2
            
            // Create 4-leaf clover using bezier curves
            val leafR = r * 0.7f
            
            // Top leaf
            moveTo(cx, cy - r * 0.3f)
            cubicTo(cx - leafR * 0.5f, cy - r, cx + leafR * 0.5f, cy - r, cx, cy - r * 0.3f)
            
            // Right leaf  
            moveTo(cx + r * 0.3f, cy)
            cubicTo(cx + r, cy - leafR * 0.5f, cx + r, cy + leafR * 0.5f, cx + r * 0.3f, cy)
            
            // Bottom leaf
            moveTo(cx, cy + r * 0.3f)
            cubicTo(cx + leafR * 0.5f, cy + r, cx - leafR * 0.5f, cy + r, cx, cy + r * 0.3f)
            
            // Left leaf
            moveTo(cx - r * 0.3f, cy)
            cubicTo(cx - r, cy + leafR * 0.5f, cx - r, cy - leafR * 0.5f, cx - r * 0.3f, cy)
            
            close()
        }
        return Outline.Generic(path)
    }
}

/** Four Petal Flower - Smooth 4 petal shape */
class FourPetalFlower : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2
            
            moveTo(cx, 0f)
            cubicTo(cx + r * 0.6f, cy * 0.4f, cx + r * 0.6f, cy * 1.6f, cx, size.height)
            cubicTo(cx - r * 0.6f, cy * 1.6f, cx - r * 0.6f, cy * 0.4f, cx, 0f)
            
            moveTo(0f, cy)
            cubicTo(cx * 0.4f, cy - r * 0.6f, cx * 1.6f, cy - r * 0.6f, size.width, cy)
            cubicTo(cx * 1.6f, cy + r * 0.6f, cx * 0.4f, cy + r * 0.6f, 0f, cy)
            
            close()
        }
        return Outline.Generic(path)
    }
}

/** Eight Petal Flower - Star-like flower */
class EightPetalFlower : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val outerR = minOf(size.width, size.height) / 2
            val innerR = outerR * 0.6f
            val petals = 8
            val points = petals * 2
            
            for (i in 0 until points) {
                val angle = 2 * PI * i / points - PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Cookie Shape - Flower with many rounded petals */
class CookieFlower : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2
            val petals = 10
            
            for (i in 0 until petals) {
                val angle = 2 * PI * i / petals - PI / 2
                val nextAngle = 2 * PI * (i + 1) / petals - PI / 2
                
                val x1 = cx + (r * cos(angle)).toFloat()
                val y1 = cy + (r * sin(angle)).toFloat()
                val x2 = cx + (r * cos(nextAngle)).toFloat()
                val y2 = cy + (r * sin(nextAngle)).toFloat()
                
                val midAngle = (angle + nextAngle) / 2
                val inwardR = r * 0.75f
                val cx1 = cx + (inwardR * cos(midAngle)).toFloat()
                val cy1 = cy + (inwardR * sin(midAngle)).toFloat()
                
                if (i == 0) moveTo(x1, y1)
                quadraticBezierTo(cx1, cy1, x2, y2)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== POLYGON SHAPES =====

/** Rounded Triangle */
class RoundedTriangle : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) * 0.15f
            
            // Top vertex
            moveTo(w * 0.5f, r)
            // Right edge to bottom right
            lineTo(w - r, h - r)
            quadraticBezierTo(w - r * 0.5f, h, w * 0.5f + r, h)
            // Bottom edge
            lineTo(r, h)
            quadraticBezierTo(r * 0.5f, h, r, h - r)
            // Left edge back to top
            lineTo(w * 0.5f - r * 0.5f, r * 1.5f)
            quadraticBezierTo(w * 0.5f, 0f, w * 0.5f + r * 0.5f, r * 1.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Pentagon - 5 sided rounded polygon */
class PentagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2
            val sides = 5
            
            for (i in 0 until sides) {
                val angle = 2 * PI * i / sides - PI / 2
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Hexagon - 6 sided polygon */
class HexagonShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val r = minOf(size.width, size.height) / 2
            val sides = 6
            
            for (i in 0 until sides) {
                val angle = 2 * PI * i / sides - PI / 2
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== STAR SHAPES =====

/** Starburst - Sharp pointed star */
class StarburstShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val cx = size.width / 2
            val cy = size.height / 2
            val outerR = minOf(size.width, size.height) / 2
            val innerR = outerR * 0.5f
            val points = 12
            val totalPoints = points * 2
            
            for (i in 0 until totalPoints) {
                val angle = 2 * PI * i / totalPoints - PI / 2
                val r = if (i % 2 == 0) outerR else innerR
                val x = cx + (r * cos(angle)).toFloat()
                val y = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== ROUNDED RECTANGLE VARIANTS =====

/** Tombstone/Arch - Rounded top, flat bottom */
class TombstoneShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = w / 2
            
            moveTo(0f, h)
            lineTo(0f, r)
            // Top arc
            cubicTo(0f, r * 0.4f, r * 0.4f, 0f, r, 0f)
            cubicTo(w - r * 0.4f, 0f, w, r * 0.4f, w, r)
            lineTo(w, h)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Squircle - iOS-like superellipse */
class SquircleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val r = minOf(w, h) * 0.3f
            
            moveTo(r, 0f)
            lineTo(w - r, 0f)
            cubicTo(w - r * 0.45f, 0f, w, r * 0.45f, w, r)
            lineTo(w, h - r)
            cubicTo(w, h - r * 0.45f, w - r * 0.45f, h, w - r, h)
            lineTo(r, h)
            cubicTo(r * 0.45f, h, 0f, h - r * 0.45f, 0f, h - r)
            lineTo(0f, r)
            cubicTo(0f, r * 0.45f, r * 0.45f, 0f, r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== ORGANIC/BLOB SHAPES =====

/** Blob Shape - Organic asymmetric blob */
class BlobShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.8f, h * 0.1f, w, h * 0.35f, w * 0.9f, h * 0.5f)
            cubicTo(w, h * 0.7f, w * 0.7f, h, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.25f, h, 0f, h * 0.7f, w * 0.1f, h * 0.5f)
            cubicTo(0f, h * 0.3f, w * 0.2f, 0f, w * 0.5f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Quatrefoil - 4-lobed decorative shape */
class QuatrefoilShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val cx = w / 2
            val cy = h / 2
            val lobeR = minOf(w, h) * 0.35f
            
            // Top lobe
            moveTo(cx, cy - lobeR * 0.3f)
            cubicTo(cx - lobeR, cy - lobeR * 0.3f, cx - lobeR, cy - lobeR * 1.7f, cx, cy - lobeR * 1.7f)
            cubicTo(cx + lobeR, cy - lobeR * 1.7f, cx + lobeR, cy - lobeR * 0.3f, cx, cy - lobeR * 0.3f)
            
            // Right lobe
            moveTo(cx + lobeR * 0.3f, cy)
            cubicTo(cx + lobeR * 0.3f, cy - lobeR, cx + lobeR * 1.7f, cy - lobeR, cx + lobeR * 1.7f, cy)
            cubicTo(cx + lobeR * 1.7f, cy + lobeR, cx + lobeR * 0.3f, cy + lobeR, cx + lobeR * 0.3f, cy)
            
            // Bottom lobe
            moveTo(cx, cy + lobeR * 0.3f)
            cubicTo(cx + lobeR, cy + lobeR * 0.3f, cx + lobeR, cy + lobeR * 1.7f, cx, cy + lobeR * 1.7f)
            cubicTo(cx - lobeR, cy + lobeR * 1.7f, cx - lobeR, cy + lobeR * 0.3f, cx, cy + lobeR * 0.3f)
            
            // Left lobe
            moveTo(cx - lobeR * 0.3f, cy)
            cubicTo(cx - lobeR * 0.3f, cy + lobeR, cx - lobeR * 1.7f, cy + lobeR, cx - lobeR * 1.7f, cy)
            cubicTo(cx - lobeR * 1.7f, cy - lobeR, cx - lobeR * 0.3f, cy - lobeR, cx - lobeR * 0.3f, cy)
            
            close()
        }
        return Outline.Generic(path)
    }
}

// ===== PRE-CONFIGURED INSTANCES =====
// All shapes ready to use

val M3WavyCircle8 = WavyCircle8()
val M3WavyCircle6 = WavyCircle6()
val M3ScallopedCircle = ScallopedCircle()
val M3Clover = CloverShape()
val M3FourPetal = FourPetalFlower()
val M3EightPetal = EightPetalFlower()
val M3CookieFlower = CookieFlower()
val M3RoundedTriangle = RoundedTriangle()
val M3Pentagon = PentagonShape()
val M3Hexagon = HexagonShape()
val M3Starburst = StarburstShape()
val M3Tombstone = TombstoneShape()
val M3Squircle = SquircleShape()
val M3Blob = BlobShape()
val M3Quatrefoil = QuatrefoilShape()

// List of all shapes for random selection
val AllExpressiveShapes: List<Shape> = listOf(
    M3WavyCircle8,
    M3WavyCircle6,
    M3ScallopedCircle,
    M3Clover,
    M3FourPetal,
    M3EightPetal,
    M3CookieFlower,
    M3RoundedTriangle,
    M3Pentagon,
    M3Hexagon,
    M3Starburst,
    M3Tombstone,
    M3Squircle,
    M3Blob,
    M3Quatrefoil
)
