package com.example.dpa

import android.graphics.PointF
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class DrowsyCheck {
    // 눈 주변의 랜드마크 인덱스
    private val RIGHT_EYE_IDX = arrayOf(33, 160, 158, 133, 153, 144)
    private val LEFT_EYE_IDX = arrayOf(263, 387, 385, 362, 380, 373)

    // EAR(눈 가로세로 비율) 계산
    fun drowsyCheck(resultBundle: FaceLandmarkerHelper.ResultBundle): Double {
        val result = resultBundle.result ?: return 0.0
        val faceLandmarksList = result.faceLandmarks()
        val h = resultBundle.inputImageHeight
        val w = resultBundle.inputImageWidth

        if (faceLandmarksList.isNullOrEmpty()) return 0.0

        val landmarks = faceLandmarksList[0] // 첫 번째 얼굴만 사용
        val right = calculateEAR(landmarks, RIGHT_EYE_IDX, h, w)
        val left = calculateEAR(landmarks, LEFT_EYE_IDX, h, w)

        return (left + right) / 2.0 // 양쪽 눈의 평균 EAR 값 반환
    }

    private fun calculateEAR(landmarks: List<NormalizedLandmark>, eyeIndices: Array<Int>, imageHeight: Int, imageWidth: Int): Double {
        val points = eyeIndices.map { index ->
            val x = landmarks[index].x() * imageWidth
            val y = landmarks[index].y() * imageHeight
            PointF(x, y)
        }
        val a = distance(points[1], points[5])
        val b = distance(points[2], points[4])
        val c = distance(points[0], points[3])

        return (a + b) / (2.0 * c)
    }

    private fun distance(p1: PointF, p2: PointF): Double {
        return Math.hypot((p1.x - p2.x).toDouble(), (p1.y - p2.y).toDouble())
    }
}