package com.example.videocollage.domain

import android.graphics.RectF

object CollageTemplates {
    data class Template(val name: String, val rects: List<RectF>)

    fun variantsFor(n: Int): List<Template> = when (n.coerceIn(1, 8)) {
        1 -> listOf(
            Template("Full", listOf(RectF(0f, 0f, 1f, 1f)))
        )
        2 -> listOf(
            Template("Split V", listOf(RectF(0f, 0f, 1f, 0.5f), RectF(0f, 0.5f, 1f, 1f))),
            Template("Split H", listOf(RectF(0f, 0f, 0.5f, 1f), RectF(0.5f, 0f, 1f, 1f))),
            Template("Diagonal", listOf(RectF(0f, 0f, 0.6f, 0.6f), RectF(0.4f, 0.4f, 1f, 1f)))
        )
        3 -> listOf(
            Template("Story", listOf(RectF(0f, 0f, 1f, 0.45f), RectF(0f, 0.45f, 0.5f, 1f), RectF(0.5f, 0.45f, 1f, 1f))),
            Template("Columns", listOf(RectF(0f, 0f, 0.33f, 1f), RectF(0.33f, 0f, 0.66f, 1f), RectF(0.66f, 0f, 1f, 1f))),
            Template("Featured", listOf(RectF(0f, 0f, 0.6f, 1f), RectF(0.6f, 0f, 1f, 0.5f), RectF(0.6f, 0.5f, 1f, 1f)))
        )
        4 -> listOf(
            Template("Grid", listOf(RectF(0f, 0f, 0.5f, 0.5f), RectF(0.5f, 0f, 1f, 0.5f), RectF(0f, 0.5f, 0.5f, 1f), RectF(0.5f, 0.5f, 1f, 1f))),
            Template("Header", listOf(RectF(0f, 0f, 1f, 0.4f), RectF(0f, 0.4f, 0.33f, 1f), RectF(0.33f, 0.4f, 0.66f, 1f), RectF(0.66f, 0.4f, 1f, 1f))),
            Template("Sidebar", listOf(RectF(0f, 0f, 0.6f, 1f), RectF(0.6f, 0f, 1f, 0.33f), RectF(0.6f, 0.33f, 1f, 0.66f), RectF(0.6f, 0.66f, 1f, 1f))),
            Template("Strips", listOf(RectF(0f, 0f, 1f, 0.25f), RectF(0f, 0.25f, 1f, 0.5f), RectF(0f, 0.5f, 1f, 0.75f), RectF(0f, 0.75f, 1f, 1f)))
        )
        5 -> listOf(
            Template("Center", listOf(
                RectF(0f, 0f, 0.5f, 0.5f),
                RectF(0.5f, 0f, 1f, 0.5f),
                RectF(0f, 0.5f, 0.5f, 1f),
                RectF(0.5f, 0.5f, 1f, 1f),
                RectF(0.25f, 0.25f, 0.75f, 0.75f)
            )),
            Template("Story", listOf(
                RectF(0f, 0f, 1f, 0.4f), RectF(0f, 0.4f, 0.33f, 0.7f),
                RectF(0.33f, 0.4f, 0.66f, 0.7f), RectF(0.66f, 0.4f, 1f, 0.7f),
                RectF(0f, 0.7f, 1f, 1f)
            )),
            Template("Grid", listOf(
                RectF(0f, 0f, 0.5f, 0.33f), RectF(0.5f, 0f, 1f, 0.33f),
                RectF(0f, 0.33f, 0.5f, 0.66f), RectF(0.5f, 0.33f, 1f, 0.66f),
                RectF(0f, 0.66f, 1f, 1f)
            )),
            Template("Spotlight", listOf(
                RectF(0.2f, 0f, 0.8f, 0.45f),
                RectF(0f, 0.45f, 0.5f, 0.72f), RectF(0.5f, 0.45f, 1f, 0.72f),
                RectF(0f, 0.72f, 0.5f, 1f), RectF(0.5f, 0.72f, 1f, 1f)
            )),
            Template("Banner", listOf(
                RectF(0f, 0f, 0.5f, 1f),
                RectF(0.5f, 0f, 1f, 0.25f), RectF(0.5f, 0.25f, 1f, 0.5f),
                RectF(0.5f, 0.5f, 1f, 0.75f), RectF(0.5f, 0.75f, 1f, 1f)
            ))
        )
        6 -> listOf(
            Template("Grid 2x3", listOf(
                RectF(0f, 0f, 0.5f, 0.33f), RectF(0.5f, 0f, 1f, 0.33f),
                RectF(0f, 0.33f, 0.5f, 0.66f), RectF(0.5f, 0.33f, 1f, 0.66f),
                RectF(0f, 0.66f, 0.5f, 1f), RectF(0.5f, 0.66f, 1f, 1f)
            )),
            Template("Grid 3x2", listOf(
                RectF(0f, 0f, 0.33f, 0.5f), RectF(0.33f, 0f, 0.66f, 0.5f), RectF(0.66f, 0f, 1f, 0.5f),
                RectF(0f, 0.5f, 0.33f, 1f), RectF(0.33f, 0.5f, 0.66f, 1f), RectF(0.66f, 0.5f, 1f, 1f)
            )),
            Template("Mosaic", listOf(
                RectF(0f, 0f, 0.5f, 0.4f), RectF(0.5f, 0f, 1f, 0.4f),
                RectF(0f, 0.4f, 0.25f, 1f), RectF(0.25f, 0.4f, 0.5f, 1f),
                RectF(0.5f, 0.4f, 0.75f, 1f), RectF(0.75f, 0.4f, 1f, 1f)
            ))
        )
        7 -> listOf(
            Template("Story", listOf(
                RectF(0f, 0f, 1f, 0.28f),
                RectF(0f, 0.28f, 0.5f, 0.52f), RectF(0.5f, 0.28f, 1f, 0.52f),
                RectF(0f, 0.52f, 0.33f, 0.76f), RectF(0.33f, 0.52f, 0.66f, 0.76f), RectF(0.66f, 0.52f, 1f, 0.76f),
                RectF(0f, 0.76f, 1f, 1f)
            )),
            Template("Mosaic", listOf(
                RectF(0f, 0f, 0.6f, 0.4f), RectF(0.6f, 0f, 1f, 0.4f),
                RectF(0f, 0.4f, 0.4f, 0.7f), RectF(0.4f, 0.4f, 0.7f, 0.7f), RectF(0.7f, 0.4f, 1f, 0.7f),
                RectF(0f, 0.7f, 0.5f, 1f), RectF(0.5f, 0.7f, 1f, 1f)
            )),
            Template("Columns", listOf(
                RectF(0f, 0f, 0.5f, 0.25f), RectF(0.5f, 0f, 1f, 0.25f),
                RectF(0f, 0.25f, 0.5f, 0.5f), RectF(0.5f, 0.25f, 1f, 0.5f),
                RectF(0f, 0.5f, 0.33f, 1f), RectF(0.33f, 0.5f, 0.66f, 1f), RectF(0.66f, 0.5f, 1f, 1f)
            ))
        )
        else -> listOf(
            Template("Grid", CollageTemplates8x1()),
            Template("Mosaic", listOf(
                RectF(0f, 0f, 0.5f, 0.33f), RectF(0.5f, 0f, 1f, 0.33f),
                RectF(0f, 0.33f, 0.33f, 0.66f), RectF(0.33f, 0.33f, 0.66f, 0.66f), RectF(0.66f, 0.33f, 1f, 0.66f),
                RectF(0f, 0.66f, 0.33f, 1f), RectF(0.33f, 0.66f, 0.66f, 1f), RectF(0.66f, 0.66f, 1f, 1f)
            ))
        )
    }

    private fun CollageTemplates8x1() = listOf(
        RectF(0f, 0f, 0.5f, 0.25f), RectF(0.5f, 0f, 1f, 0.25f),
        RectF(0f, 0.25f, 0.5f, 0.5f), RectF(0.5f, 0.25f, 1f, 0.5f),
        RectF(0f, 0.5f, 0.5f, 0.75f), RectF(0.5f, 0.5f, 1f, 0.75f),
        RectF(0f, 0.75f, 0.5f, 1f), RectF(0.5f, 0.75f, 1f, 1f)
    )
}
