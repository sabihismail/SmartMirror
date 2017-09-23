/**
 * @author: Sabih Ismail
 */
package com.sabihismail.SmartMirror.guitools

import com.sabihismail.SmartMirror.settings.GeneralConstants
import javafx.animation.FadeTransition
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.effect.Bloom
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.util.Duration
import java.util.concurrent.Callable

/**
 * Creates a small dialog message in the center of the screen which is meant to alert the user of something.
 *
 * The developer can include a [Runnable] to run after the [Dialog] is no longer visible. The developer can also
 * force the [Dialog] to block until no longer visible.
 *
 * @date: August 19, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class Dialog(text: String, scene: Scene, runnable: Runnable?, wait: Boolean) {
    constructor(text: String, scene: Scene) : this(text, scene, null, false)
    constructor(text: String, scene: Scene, wait: Boolean) : this(text, scene, null, wait)
    constructor(text: String, scene: Scene, runnable: Runnable?) : this(text, scene, runnable, false)

    private var alive = true

    init {
        val stackpane = StackPane()

        val rectangle = Rectangle()
        rectangle.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            scene.widthProperty().get() / 2 - rectangle.widthProperty().get() / 2
        }, scene.widthProperty(), rectangle.widthProperty()))
        rectangle.translateYProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            scene.heightProperty().get() / 2 - rectangle.heightProperty().get() / 2
        }, scene.heightProperty(), rectangle.heightProperty()))

        val label = Label(text)
        label.effect = Bloom(1.0)
        label.textFill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        label.textAlignment = TextAlignment.CENTER
        label.font = Font.font(GeneralConstants.GLOBAL_FONT, 48.0)

        rectangle.widthProperty().bind(label.widthProperty())
        rectangle.heightProperty().bind(label.heightProperty())
        rectangle.arcHeightProperty().bind(rectangle.arcWidthProperty())
        rectangle.arcWidth = 1.0
        rectangle.fill = Color.RED
        rectangle.opacity = 0.3

        stackpane.children.addAll(rectangle, label)
        StackPane.setAlignment(rectangle, Pos.TOP_LEFT)

        val timeline = Timeline(KeyFrame(Duration.millis(2000.0), EventHandler<ActionEvent> {
            val transition = FadeTransition(Duration.millis(1000.0), stackpane)
            transition.fromValue = 1.0
            transition.toValue = 0.0
            transition.play()

            transition.setOnFinished {
                runnable?.run()

                alive = false

                (scene.root as StackPane).children.remove(stackpane)
            }
        }))
        timeline.play()

        (scene.root as StackPane).children.add(stackpane)

        if (wait) {
            while (alive) {
                Thread.sleep(100)
            }
        }
    }
}