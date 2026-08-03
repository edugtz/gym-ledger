package com.edu.gymledger.data.remote

import android.os.SystemClock

interface MonotonicTimeSource {
    fun nowMillis(): Long
}

object SystemMonotonicTimeSource : MonotonicTimeSource {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}
