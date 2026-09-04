package com.example.videocollage.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.unit.IntSize

object CollageRenderer {
    private const val EXPORT_W = 1080
    private const val EXPORT_H = 1920
    private const val GAP = 6f
    private const val RADIUS = 24f

    fun render(
        slots: List<CollageSlot>,
        rects: List<RectF>,
        previewContainerSize: IntSize
    ): Bitmap {
        val out = Bitmap.createBitmap(EXPORT_W, EXPORT_H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.parseColor("#101010"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val scaleRatio = if (previewContainerSize.width > 0)
            EXPORT_W / previewContainerSize.width.toFloat()
        else 1f

        for (i in slots.indices) {
            val slot = slots[i]
            val r = rects[i]
            val bitmap = slot.bitmap

            val cellRect = RectF(
                r.left * EXPORT_W + GAP,
                r.top * EXPORT_H + GAP,
                r.right * EXPORT_W - GAP,
                r.bottom * EXPORT_H - GAP
            )

            val clipPath = Path().apply {
                addRoundRect(cellRect, RADIUS, RADIUS, Path.Direction.CW)
            }

            canvas.save()
            canvas.clipPath(clipPath)

            val scaleToFill = maxOf(
                cellRect.width() / bitmap.width,
                cellRect.height() / bitmap.height
            )
            val scaledW = bitmap.width * scaleToFill
            val scaledH = bitmap.height * scaleToFill
            val dx = cellRect.left + (cellRect.width() - scaledW) / 2f
            val dy = cellRect.top + (cellRect.height() - scaledH) / 2f

            val matrix = Matrix()
            matrix.setScale(scaleToFill, scaleToFill)
            matrix.postTranslate(dx, dy)

            val cellCenterX = cellRect.centerX()
            val cellCenterY = cellRect.centerY()
            matrix.postScale(slot.scale, slot.scale, cellCenterX, cellCenterY)
            matrix.postTranslate(
                slot.offset.x * scaleRatio,
                slot.offset.y * scaleRatio
            )

            canvas.drawBitmap(bitmap, matrix, paint)
            canvas.restore()
        }

        return out
    }
}
