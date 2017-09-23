package com.sabihismail.SmartMirror.guitools

import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.DoubleProperty
import javafx.util.Duration

/**
 * This class implements a transition for any given property.
 *
 * This transition also includes a reversed transition that can be used for reverse transition operations. This will be
 * created if the [reverse] boolean is true.
 *
 * @date: August 19, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class SinglePropertyTransition(property: DoubleProperty, initial: Double, final: Double, time: Int, reverse: Boolean) {
    val timeline = Timeline()
    val reversedTimeline = Timeline()

    init {
        val kv1 = KeyValue(property, initial)
        val kf1 = KeyFrame(Duration.millis(0.0), kv1)

        val kv2 = KeyValue(property, final)
        val kf2 = KeyFrame(Duration.millis(time.toDouble()), kv2)

        timeline.keyFrames.addAll(kf1, kf2)

        if (reverse) {
            val kv1Reversed = KeyValue(property, final)
            val kf1Reversed = KeyFrame(Duration.millis(0.0), kv1Reversed)

            val kv2Reversed = KeyValue(property, initial)
            val kf2Reversed = KeyFrame(Duration.millis(time.toDouble()), kv2Reversed)

            reversedTimeline.keyFrames.addAll(kf1Reversed, kf2Reversed)
        }
    }
}