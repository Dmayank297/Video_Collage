package com.example.videocollage.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt
import androidx.core.graphics.scale

class MobileFaceNetEmbedder(context: Context) {

    private val interpreter: Interpreter = Interpreter(loadModelFile(context, MODEL_FILE))

    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        val squared = squarePad(faceBitmap)
        val resized = squared.scale(INPUT_SIZE, INPUT_SIZE)
        val inputBuffer = bitmapToByteBuffer(resized)

        val output = Array(BATCH_SIZE) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(inputBuffer, output)

        return l2Normalize(output[0])
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(BATCH_SIZE * INPUT_SIZE * INPUT_SIZE * CHANNELS * 4)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        repeat(BATCH_SIZE) {
            for (pixel in pixels) {
                val r = (pixel shr 16 and 0xFF)
                val g = (pixel shr 8 and 0xFF)
                val b = (pixel and 0xFF)
                buffer.putFloat((r - 127.5f) / 128.0f)
                buffer.putFloat((g - 127.5f) / 128.0f)
                buffer.putFloat((b - 127.5f) / 128.0f)
            }
        }
        buffer.rewind()
        return buffer
    }

    private fun l2Normalize(embedding: FloatArray): FloatArray {
        var sumSq = 0f
        for (v in embedding) sumSq += v * v
        val norm = sqrt(sumSq).coerceAtLeast(1e-10f)
        return FloatArray(embedding.size) { embedding[it] / norm }
    }

    private fun squarePad(src: Bitmap): Bitmap {
        val side = maxOf(src.width, src.height)
        if (src.width == src.height) return src
        val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(square)
        canvas.drawColor(Color.BLACK)
        val left = (side - src.width) / 2f
        val top  = (side - src.height) / 2f
        canvas.drawBitmap(src, left, top, null)
        return square
    }

    fun close() = interpreter.close()

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        val afd = context.assets.openFd(fileName)
        val inputStream = afd.createInputStream()
        val channel = inputStream.channel
        return channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    companion object {
        private const val MODEL_FILE = "MobileFaceNet.tflite"
        private const val INPUT_SIZE = 112
        private const val CHANNELS = 3
        private const val BATCH_SIZE = 2
        private const val EMBEDDING_SIZE = 192
    }
}