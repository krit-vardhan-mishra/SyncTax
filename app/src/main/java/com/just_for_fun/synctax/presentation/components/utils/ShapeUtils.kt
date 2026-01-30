@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.just_for_fun.synctax.presentation.components.utils

import android.graphics.Matrix
import android.graphics.RectF
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * A utility object to hold ready-to-use Compose Shapes derived from RoundedPolygons.
 * Usage: Modifier.clip(PolyShapes.Cookie9)
 */
@OptIn(ExperimentalMaterial3Api::class)
object PolyShapes {
    // Cookie Shapes
    val Cookie4: Shape = MaterialShapes.Cookie4Sided.toShape()
    val Cookie6: Shape = MaterialShapes.Cookie6Sided.toShape()
    val Cookie7: Shape = MaterialShapes.Cookie7Sided.toShape()
    val Cookie9: Shape = MaterialShapes.Cookie9Sided.toShape()
    val Cookie12: Shape = MaterialShapes.Cookie12Sided.toShape()

    // Burst and Boom Shapes
    val Burst: Shape = MaterialShapes.Burst.toShape()
    val Boom: Shape = MaterialShapes.Boom.toShape()
    val SoftBoom: Shape = MaterialShapes.SoftBoom.toShape()
    val SoftBurst: Shape = MaterialShapes.SoftBurst.toShape()

    // Basic Geometric Shapes
    val Circle: Shape = MaterialShapes.Circle.toShape()
    val Square: Shape = MaterialShapes.Square.toShape()
    val Triangle: Shape = MaterialShapes.Triangle.toShape()
    val Diamond: Shape = MaterialShapes.Diamond.toShape()
    val Pentagon: Shape = MaterialShapes.Pentagon.toShape()
    val Oval: Shape = MaterialShapes.Oval.toShape()
    val Pill: Shape = MaterialShapes.Pill.toShape()
    val SemiCircle: Shape = MaterialShapes.SemiCircle.toShape()
    val Slanted: Shape = MaterialShapes.Slanted.toShape()

    // Pixel Shapes
    val PixelCircle: Shape = MaterialShapes.PixelCircle.toShape()
    val PixelTriangle: Shape = MaterialShapes.PixelTriangle.toShape()

    // Nature and Organic Shapes
    val Heart: Shape = MaterialShapes.Heart.toShape()
    val Flower: Shape = MaterialShapes.Flower.toShape()
    val Clover4Leaf: Shape = MaterialShapes.Clover4Leaf.toShape()
    val Clover8Leaf: Shape = MaterialShapes.Clover8Leaf.toShape()
    val Fan: Shape = MaterialShapes.Fan.toShape()
    val Bun: Shape = MaterialShapes.Bun.toShape()
    val ClamShell: Shape = MaterialShapes.ClamShell.toShape()
    val Ghostish: Shape = MaterialShapes.Ghostish.toShape()

    // Abstract and Decorative Shapes
    val Arrow: Shape = MaterialShapes.Arrow.toShape()
    val Arch: Shape = MaterialShapes.Arch.toShape()
    val Gem: Shape = MaterialShapes.Gem.toShape()
    val Puffy: Shape = MaterialShapes.Puffy.toShape()
    val PuffyDiamond: Shape = MaterialShapes.PuffyDiamond.toShape()
    val Sunny: Shape = MaterialShapes.Sunny.toShape()
    val VerySunny: Shape = MaterialShapes.VerySunny.toShape()

    // Custom Shape Examples:
    val Star5Custom: Shape = RoundedPolygon.star(numVerticesPerRadius = 5).toShape()
    val Hexagon: Shape = RoundedPolygon(numVertices = 6).toShape()
    val RoundedStar: Shape = RoundedPolygon.star(
        numVerticesPerRadius = 6,
        rounding = CornerRounding(radius = 0.2f)
    ).toShape()
}

/**
 * Extension function to convert a generic [RoundedPolygon] into a Jetpack Compose [Shape].
 * This handles the scaling of the polygon to fit the UI component's size.
 */
fun RoundedPolygon.toShape(rotation: Float = 0f): Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // 1. Convert the Polygon to an Android Path
        val path = androidx.compose.ui.graphics.Path().asAndroidPath()
        this@toShape.toPath(path)

        // 2. Calculate the bounds of the original polygon path (usually unit size)
        val pathBounds = RectF()
        path.computeBounds(pathBounds, true)

        // 3. Create a Matrix to scale the path to fit the container's Size
        val matrix = Matrix()

        // Optional: Apply rotation if needed
        if (rotation != 0f) {
            matrix.postRotate(rotation, pathBounds.centerX(), pathBounds.centerY())
        }

        // Scale to fit the destination box
        matrix.setRectToRect(
            pathBounds,
            RectF(0f, 0f, size.width, size.height),
            Matrix.ScaleToFit.FILL
        )

        // 4. Apply the transformation
        path.transform(matrix)

        // 5. Return as a Compose Outline
        return Outline.Generic(path.asComposePath())
    }
}