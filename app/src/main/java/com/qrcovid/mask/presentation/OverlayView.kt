package com.qrcovid.mask.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import java.util.*

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val callbacks: MutableList<DrawCallback> = LinkedList<DrawCallback>()

    fun addCallback(callback: DrawCallback) {
        callbacks.add(callback)
    }

    @Synchronized
    @SuppressLint("MissingSuperCall")
    override fun draw(canvas: Canvas?) {
        for (callback in callbacks) {
            callback.drawCallback(canvas)
        }
    }


    interface DrawCallback {
        fun drawCallback(canvas: Canvas?)
    }
}