package com.example.videocollage.ui.screens

import android.graphics.Bitmap
import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videocollage.domain.CollageRenderer
import com.example.videocollage.domain.CollageSlot
import com.example.videocollage.domain.CollageTemplates
import com.example.videocollage.utils.CollageExporter

@Composable
fun CollageEditorScreen(
    initialSlots: List<CollageSlot>,
    onBack: () -> Unit,
    onCancel: () -> Unit = {},
    onExport: () -> Unit
) {
    val context = LocalContext.current
    var slots by remember { mutableStateOf(initialSlots) }
    val templates = remember(slots.size) { CollageTemplates.variantsFor(slots.size) }
    var templateIndex by remember { mutableIntStateOf(0) }
    var swapSourceIndex by remember { mutableIntStateOf(-1) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val rects = templates[templateIndex].rects

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Text("Collage", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
            TextButton(onClick = onCancel) {
                Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A), modifier = Modifier.padding(end = 6.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101010))
                    .onSizeChanged { containerSize = it }
            ) {
                slots.forEachIndexed { i, slot ->
                    if (i < rects.size) {
                        CollageCell(
                            slot = slot,
                            rect = rects[i],
                            containerSize = containerSize,
                            isSwapSource = swapSourceIndex == i,
                            onTap = {
                                if (swapSourceIndex == -1) {
                                    swapSourceIndex = i
                                } else if (swapSourceIndex == i) {
                                    swapSourceIndex = -1
                                } else {
                                    val targetIdx = i
                                    val srcIdx = swapSourceIndex
                                    slots = slots.toMutableList().apply {
                                        val slotSrc = this[srcIdx]
                                        val slotDst = this[targetIdx]
                                        this[srcIdx] = slotSrc.copy(
                                            bitmap = slotDst.bitmap,
                                            personId = slotDst.personId,
                                            offset = Offset.Zero,
                                            scale = 1f
                                        )
                                        this[targetIdx] = slotDst.copy(
                                            bitmap = slotSrc.bitmap,
                                            personId = slotSrc.personId,
                                            offset = Offset.Zero,
                                            scale = 1f
                                        )
                                    }
                                    swapSourceIndex = -1
                                }
                            },
                            onTransform = { pan, zoom ->
                                slots = slots.toMutableList().apply {
                                    val current = this[i]
                                    val newScale = (current.scale * zoom).coerceIn(1f, 3f)
                                    val maxOffsetX = (containerSize.width * rects[i].width() * (newScale - 1f) / 2f)
                                    val maxOffsetY = (containerSize.height * rects[i].height() * (newScale - 1f) / 2f)
                                    val newOffset = Offset(
                                        x = (current.offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                        y = (current.offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                                    )
                                    this[i] = current.copy(offset = newOffset, scale = newScale)
                                }
                            }
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 10.dp, bottom = 20.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layouts", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                if (swapSourceIndex != -1) {
                    Text(
                        "Tap another photo to swap",
                        fontSize = 11.sp,
                        color = Color(0xFF7C4DFF),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            LazyRow(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(templates) { idx, t ->
                    TemplateThumb(
                        template = t,
                        selected = idx == templateIndex,
                        onClick = { templateIndex = idx }
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val bitmap = CollageRenderer.render(slots, rects, containerSize)
                        val uri = CollageExporter.saveToGallery(context, bitmap)
                        if (uri != null) {
                            CollageExporter.share(context, uri)
                        } else {
                            Toast.makeText(context, "Failed to share", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Share")
                }

                Button(
                    onClick = {
                        val bitmap = CollageRenderer.render(slots, rects, containerSize)
                        val uri = CollageExporter.saveToGallery(context, bitmap)
                        if (uri != null) {
                            Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                            onExport()
                        } else {
                            Toast.makeText(context, "Failed to save collage", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Text("Save to Gallery")
                }
            }
        }
    }
}

@Composable
fun CollageCell(
    slot: CollageSlot,
    rect: RectF,
    containerSize: IntSize,
    isSwapSource: Boolean,
    onTap: () -> Unit,
    onTransform: (Offset, Float) -> Unit
) {
    val density = LocalDensity.current
    val w = (rect.width() * containerSize.width)
    val h = (rect.height() * containerSize.height)

    Box(
        Modifier
            .offset { IntOffset((rect.left * containerSize.width).toInt(), (rect.top * containerSize.height).toInt()) }
            .size(with(density) { DpSize(w.toDp(), h.toDp()) })
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSwapSource) Modifier.border(3.dp, Color(0xFF7C4DFF), RoundedCornerShape(8.dp))
                else Modifier
            )
            .pointerInput(Unit) {
                detectTapGestures { onTap() }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    onTransform(pan, zoom)
                }
            }
    ) {
        Image(
            bitmap = slot.bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = slot.scale,
                    scaleY = slot.scale,
                    translationX = slot.offset.x,
                    translationY = slot.offset.y
                ),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun TemplateThumb(template: CollageTemplates.Template, selected: Boolean, onClick: () -> Unit) {
    val density = LocalDensity.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(44.dp, 70.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .border(
                    width = if (selected) 2.5.dp else 1.2.dp,
                    color = if (selected) Color(0xFF7C4DFF) else Color(0xFF222222),
                    shape = RoundedCornerShape(6.dp)
                )
        ) {
            template.rects.forEach { r ->
                Box(
                    Modifier
                        .offset { IntOffset(with(density) { (r.left * 44).dp.roundToPx() }, with(density) { (r.top * 70).dp.roundToPx() }) }
                        .size((r.width() * 44).dp, (r.height() * 70).dp)
                        .padding(0.8.dp)
                        .background(Color(0xFFE0E0E0))
                        .border(0.5.dp, Color(0xFF333333))
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            template.name,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFF7C4DFF) else Color(0xFF555555)
        )
    }
}