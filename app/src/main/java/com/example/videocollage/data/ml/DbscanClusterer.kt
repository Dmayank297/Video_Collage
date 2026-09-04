package com.example.videocollage.data.ml

import android.util.Log
import com.example.videocollage.utils.SimilarityUtils

class DbscanClusterer(
    private val seedEps: Float = 0.15f,
    private val assignEps: Float = 0.35f,
    private val mergeEps: Float = 0.22f,
    private val minPoints: Int = 2,
    //private val minPairEps: Float = 0.38f
) {

    fun cluster(embeddings: List<FloatArray>): IntArray {
        val n = embeddings.size
        if (n == 0) return IntArray(0)

        val labels = IntArray(n) { UNCLASSIFIED }
        var nextClusterId = 0

        for (i in 0 until n) {
            if (labels[i] != UNCLASSIFIED) continue
            val neighbors = regionQuery(embeddings, i, seedEps)
            if (neighbors.size < minPoints) {
                labels[i] = NOISE
                continue
            }
            expandCluster(embeddings, labels, i, neighbors, nextClusterId, seedEps)
            nextClusterId++
        }

        val seedClusterCount = nextClusterId
        Log.d(TAG, "Stage 1 (seed eps=$seedEps): $seedClusterCount clusters, " +
                "${labels.count { it == NOISE }} noise points")
        for (cId in 0 until seedClusterCount) {
            val members = (0 until n).filter { labels[it] == cId }
            Log.d(TAG, "Stage 1 seed cluster $cId members (${members.size}): $members")
        }

        val centroids = computeCentroids(embeddings, labels, seedClusterCount)

        val noiseIndices = (0 until n).filter { labels[it] == NOISE }
        val stillNoise = mutableListOf<Int>()

        for (idx in noiseIndices) {
            var bestCluster = -1
            var bestDist = Float.MAX_VALUE
            for (cId in 0 until seedClusterCount) {
                val dist = SimilarityUtils.cosineDistance(embeddings[idx], centroids[cId])
                if (dist < bestDist) {
                    bestDist = dist
                    bestCluster = cId
                }
            }
            if (bestDist <= assignEps) {
                labels[idx] = bestCluster
                Log.d(TAG, "Stage 2: noise point $idx → cluster $bestCluster (dist=$bestDist)")
            } else {
                stillNoise.add(idx)
                Log.d(TAG, "Stage 2: noise point $idx remains noise (nearest=$bestDist)")
            }
        }

        if (stillNoise.size >= minPoints) {
            val subLabels = IntArray(stillNoise.size) { UNCLASSIFIED }
            var subClusterId = 0
            val subEmbeddings = stillNoise.map { embeddings[it] }

            for (i in subEmbeddings.indices) {
                if (subLabels[i] != UNCLASSIFIED) continue
                val neighbors = regionQuery(subEmbeddings, i, assignEps)
                if (neighbors.size < minPoints) {
                    subLabels[i] = NOISE
                    continue
                }
                expandCluster(subEmbeddings, subLabels, i, neighbors, subClusterId, assignEps)
                subClusterId++
            }

            for (i in stillNoise.indices) {
                if (subLabels[i] >= 0) {
                    labels[stillNoise[i]] = nextClusterId + subLabels[i]
                    Log.d(TAG, "Stage 2b: noise point ${stillNoise[i]} → new cluster ${nextClusterId + subLabels[i]}")
                }
            }
            nextClusterId += subClusterId
        }

        Log.d(TAG, "After Stage 2: $nextClusterId clusters, " +
                "${labels.count { it == NOISE }} remaining noise")

        val clusterMembers = MutableList(nextClusterId) { cId ->
            (0 until n).filter { labels[it] == cId }.toMutableList()
        }
        val centroidList = MutableList(nextClusterId) { cId ->
            computeSingleCentroid(clusterMembers[cId].map { embeddings[it] })
        }
        val alive = BooleanArray(nextClusterId) { true }
        val adaptiveMergeEps = computeAdaptiveMergeEps(centroidList, alive)
            .coerceIn(0.20f, 0.38f)

        while (true) {
            var bestA = -1
            var bestB = -1
            var bestScore = Float.MAX_VALUE
            for (a in 0 until nextClusterId) {
                if (!alive[a]) continue
                for (b in a + 1 until nextClusterId) {
                    if (!alive[b]) continue
                    val centroidDist = SimilarityUtils.cosineDistance(centroidList[a], centroidList[b])
                    val minPair = minPairwiseDistance(embeddings, clusterMembers[a], clusterMembers[b])
                    Log.d(TAG, "Stage 3 scan: cluster $a↔$b centroidDist=$centroidDist minPair=$minPair " +
                            "(sizes ${clusterMembers[a].size}/${clusterMembers[b].size})")
                    if (centroidDist <= adaptiveMergeEps && centroidDist < bestScore) {
                        bestScore = centroidDist; bestA = a; bestB = b
                    }
                }
            }
            if (bestA == -1) break

            Log.d(TAG, "Stage 3: merging cluster $bestB into $bestA (minPair=$bestScore)")
            clusterMembers[bestA].addAll(clusterMembers[bestB])
            clusterMembers[bestB].clear()
            alive[bestB] = false
            centroidList[bestA] = computeSingleCentroid(clusterMembers[bestA].map { embeddings[it] })
        }

        val canonicalIds = mutableMapOf<Int, Int>()
        var finalId = 0
        for (cId in 0 until nextClusterId) {
            if (!alive[cId]) continue
            canonicalIds[cId] = finalId++
            for (idx in clusterMembers[cId]) labels[idx] = canonicalIds[cId]!!
        }

        Log.d(TAG, "Final: $finalId clusters, ${labels.count { it == NOISE }} noise")
        for (cId in 0 until finalId) {
            val members = (0 until n).filter { labels[it] == cId }
            Log.d(TAG, "  Final cluster $cId (${members.size} embeddings): indices=$members")
        }

        return labels
    }

    private fun expandCluster(
        embeddings: List<FloatArray>,
        labels: IntArray,
        pointIdx: Int,
        neighbors: MutableList<Int>,
        clusterId: Int,
        eps: Float
    ) {
        labels[pointIdx] = clusterId
        var i = 0
        while (i < neighbors.size) {
            val current = neighbors[i]
            if (labels[current] == NOISE) {
                labels[current] = clusterId
            }
            if (labels[current] == UNCLASSIFIED) {
                labels[current] = clusterId
                val currentNeighbors = regionQuery(embeddings, current, eps)
                if (currentNeighbors.size >= minPoints) {
                    for (n in currentNeighbors) {
                        if (n !in neighbors) neighbors.add(n)
                    }
                }
            }
            i++
        }
    }

    private fun regionQuery(embeddings: List<FloatArray>, pointIdx: Int, eps: Float): MutableList<Int> {
        val neighbors = mutableListOf<Int>()
        for (j in embeddings.indices) {
            if (j == pointIdx) continue
            val distance = SimilarityUtils.cosineDistance(embeddings[pointIdx], embeddings[j])
            if (distance <= eps) neighbors.add(j)
        }
        return neighbors
    }

    private fun computeCentroids(
        embeddings: List<FloatArray>,
        labels: IntArray,
        clusterCount: Int
    ): Array<FloatArray> {
        return Array(clusterCount) { cId ->
            val members = embeddings.indices.filter { labels[it] == cId }
            computeSingleCentroid(members.map { embeddings[it] })
        }
    }

    private fun computeSingleCentroid(points: List<FloatArray>): FloatArray {
        val dim = if (points.isNotEmpty()) points[0].size else 0
        if (points.isEmpty()) return FloatArray(dim)
        val sum = FloatArray(dim)
        for (p in points) {
            for (d in p.indices) sum[d] += p[d]
        }
        for (d in sum.indices) sum[d] /= points.size
        var normSq = 0f
        for (v in sum) normSq += v * v
        val norm = kotlin.math.sqrt(normSq).coerceAtLeast(1e-10f)
        return FloatArray(dim) { sum[it] / norm }
    }

    private fun minPairwiseDistance(
        embeddings: List<FloatArray>,
        membersA: List<Int>,
        membersB: List<Int>
    ): Float {
        var best = Float.MAX_VALUE
        for (a in membersA) {
            for (b in membersB) {
                val d = SimilarityUtils.cosineDistance(embeddings[a], embeddings[b])
                if (d < best) best = d
            }
        }
        return best
    }
    private fun computeAdaptiveMergeEps(
        centroidList: List<FloatArray>,
        alive: BooleanArray,
        fallback: Float = 0.25f,
        ceiling: Float = 0.5f
    ): Float {
        val distances = mutableListOf<Float>()
        for (a in centroidList.indices) {
            if (!alive[a]) continue
            for (b in a + 1 until centroidList.size) {
                if (!alive[b]) continue
                val d = SimilarityUtils.cosineDistance(centroidList[a], centroidList[b])
                if (d <= ceiling) distances.add(d)
            }
        }
        if (distances.size < 2) return fallback
        distances.sort()

        var bestGap = 0f
        var gapMidpoint = fallback
        for (i in 0 until distances.size - 1) {
            val gap = distances[i + 1] - distances[i]
            if (gap > bestGap) {
                bestGap = gap
                gapMidpoint = (distances[i] + distances[i + 1]) / 2f
            }
        }
        Log.d(TAG, "Adaptive mergeEps: sorted=$distances chosenGap=$bestGap threshold=$gapMidpoint")
        return gapMidpoint
    }

    companion object {
        private const val TAG = "DbscanClusterer"
        private const val UNCLASSIFIED = -2
        private const val NOISE = -1
    }
}