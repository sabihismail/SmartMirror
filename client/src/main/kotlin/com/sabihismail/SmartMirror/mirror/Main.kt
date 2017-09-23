package com.sabihismail.SmartMirror.mirror

import com.sabihismail.SmartMirror.app.App
import com.sabihismail.SmartMirror.app.AppThumbnailView
import com.sabihismail.SmartMirror.database.Database
import com.sabihismail.SmartMirror.guitools.Dialog
import com.sabihismail.SmartMirror.mirror.Main.Companion.TIME_TO_HOLD
import com.sabihismail.SmartMirror.widget.Widget
import javafx.application.Application
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Point2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.TouchEvent
import javafx.scene.layout.Background
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * This is the main GUI for the SmartMirror application.
 *
 * This class contains all of the multiple GUI layers from least Z-index to greatest Z-index:
 * [widgetPane], [appPane], [overlayPane].
 *
 * [Widget]
 * Widgets can be held down for [TIME_TO_HOLD] milliseconds and then be moved around or removed from the [widgetPane].
 *
 * All [Widget] [Point2D] data and whether the [Widget] is enabled is saved in the data class [Widget.Properties]. This
 * data is stored in the main application database [Database].
 *
 * [App]
 * Applications work similarly. Holding the background of the pane shows an overlay [AppThumbnailView] which allows for
 * the user to select an [App] to load.
 *
 * @date: July 10, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class Main : Application() {
    companion object {
        /**
         * Time to wait in milliseconds until program allows for a [Widget] to be dragged. Currently set to: 1.25
         * seconds.
         */
        const val TIME_TO_HOLD = 1250

        /**
         * Maximum pixel distance to enable snapping of [Widget] edges to any other [Widget] or the sides of the
         * [Scene].
         */
        private const val WIDGET_SNAP_DISTANCE = 24

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                Class.forName("javafx.embed.swing.JFXPanel")
            } catch (e: ClassNotFoundException) {
                throw MirrorException.Exceptions.StartupException("JavaFX was not found. If on linux, please use the " +
                        "command 'sudo apt-get install openjfx' in Terminal to install JavaFX dependencies.")
            }

            Application.launch(Main::class.java)
        }
    }

    /**
     * Layouts
     */
    private val all = StackPane()
    private val widgetPane = Pane()
    private val overlayPane = Pane()
    private val appPane = Pane()

    /**
     * Active [Widget] and [App] lists
     */
    private val widgets: MutableList<Widget> = Database.INSTANCE.selectAllWidgets()
    private val apps: MutableList<App> = ArrayList()

    private val scene = Scene(all, 1400.0, 700.0, Color.TRANSPARENT)
    private val appThumbnailView = AppThumbnailView(scene, widgetPane, apps)

    /**
     * Timers dedicated to the waiting of [TIME_TO_HOLD] milliseconds upon the touch press event being triggered
     */
    private var transitionTimer = Executors.newSingleThreadScheduledExecutor()
    private var transitionFuture = transitionTimer.schedule({}, 0, TimeUnit.MILLISECONDS)

    /**
     * All images for the [Widget] overlays
     */
    private val overlayRemove = ImageView(Image(javaClass.getResourceAsStream(MainConstants.OVERLAY_REMOVE_IMAGE)))
    private val overlayResizeHigher = ImageView(Image(
            javaClass.getResourceAsStream(MainConstants.OVERLAY_RESIZE_HIGHER)))
    private val overlayResizeLower = ImageView(Image(javaClass.getResourceAsStream(MainConstants.OVERLAY_RESIZE_LOWER)))
    private val overlayConfirm = ImageView(Image(javaClass.getResourceAsStream(MainConstants.OVERLAY_CONFIRM_IMAGE)))
    private val overlayCancel = ImageView(Image(javaClass.getResourceAsStream(MainConstants.OVERLAY_CANCEL_IMAGE)))

    private var mouseClicked = false
    private var mouseStart = Point2D(0.0, 0.0)
    private var originalPosition = Point2D(0.0, 0.0)

    private var widgetDrag = false
    private var widgetSelected = SimpleObjectProperty<Widget>(widgets[0])
    private var widgetOutline = Rectangle()

    private var appDrag = false
    private var appSelected = SimpleObjectProperty<App>()

    @Throws(Exception::class)
    override fun start(stage: Stage) {
        scene.fill = MainConstants.BACKGROUND_SCENE
        widgetPane.background = Background.EMPTY
        appPane.background = Background.EMPTY
        all.background = MainConstants.BACKGROUND_APPLICATION

        overlayPane.isPickOnBounds = false
        appPane.isPickOnBounds = false
        widgetPane.isPickOnBounds = false
        all.isPickOnBounds = false

        widgetPane.minWidthProperty().bind(scene.widthProperty())
        appPane.minWidthProperty().bind(scene.widthProperty())
        overlayPane.minWidthProperty().bind(scene.widthProperty())
        widgetPane.minHeightProperty().bind(scene.heightProperty())
        appPane.minHeightProperty().bind(scene.heightProperty())
        overlayPane.minHeightProperty().bind(scene.heightProperty())
        all.children.addAll(widgetPane, appPane, overlayPane)

        scene.setOnKeyPressed { e -> if (e.code == MainConstants.KEYCODE_EXIT_FULLSCREEN) close() }
        scene.setOnTouchPressed { e -> onTouchPressed(e) }
        scene.setOnTouchMoved { e -> onTouchMoved(e) }
        scene.setOnTouchReleased { onTouchReleased() }

        overlayResizeHigher.setOnTouchReleased { widgetSelected.get().resize(true) }
        overlayResizeLower.setOnTouchReleased { widgetSelected.get().resize(false) }
        overlayRemove.setOnTouchReleased {
            val node = widgetSelected.get().pane

            widgetPane.children.remove(node)
            widgets.remove(widgetSelected.get())

            Database.INSTANCE.addOrUpdateWidget(node.id, false, Point2D(node.translateX, node.translateY))

            overlayConfirm.onTouchReleased.handle(it)
        }
        overlayCancel.setOnTouchReleased {
            widgetSelected.get().pane.translateX = originalPosition.x
            widgetSelected.get().pane.translateY = originalPosition.y

            widgetSelected.get().cancelChanges()

            overlayConfirm.onTouchReleased.handle(it)
        }
        overlayConfirm.setOnTouchReleased {
            Platform.runLater {
                overlayPane.children.removeAll(widgetOutline, overlayCancel, overlayConfirm, overlayRemove,
                        overlayResizeHigher, overlayResizeLower)
            }

            widgetSelected.get().confirmChanges()

            val node = widgetSelected.get().pane

            Database.INSTANCE.addOrUpdateWidget(node.id, true, Point2D(node.translateX, node.translateY))

            scene.onTouchReleased.handle(it)
        }

        calculateOverlayPositions()
        addWidgets()

        //checkIfFirstRun(stage, scene)
        stage.scene = scene

        stage.title = "SmartMirror"
        stage.setOnCloseRequest { close() }
        stage.isFullScreen = false
        stage.fullScreenExitKeyCombination = KeyCodeCombination.NO_MATCH
        stage.show()
    }

    /**
     * Resets all variables related to the touch interactions
     */
    private fun onTouchReleased() {
        if (mouseClicked) {
            transitionFuture.cancel(true)
        }
        mouseStart = Point2D.ZERO
        widgetDrag = false
        appDrag = false
        mouseClicked = false
    }

    /**
     * Checks if either a [Widget] or an [App] is being dragged. If either is true, then the active object will be
     * set to the co-ordinates set by [TouchEvent.touchPoint].
     *
     * For [Widget], if the [TouchEvent.touchPoint] attempts to go outside of the bounds of the screen or in the bounds
     * of another [Widget], the selected [Widget] will instead snap to that edge.
     *
     * For [App], it only snaps to the edges of the screen.
     */
    private fun onTouchMoved(event: TouchEvent) {
        if (widgetDrag && !appDrag) {
            val snapSide = SnapSide(scene, widgetSelected.get().pane, event)

            if (snapSide.isValidLocation()) {
                val edgeCollision = snapSide.isCollidingSide(0.0, scene.width, scene.height, 0.0)

                if (!edgeCollision) {
                    for (widget in widgets) {
                        val node = widget.pane
                        if (node == widgetSelected.get()) {
                            continue
                        }

                        updateOverlay()

                        val collide = snapSide.isCollidingWidget(node.translateY + node.layoutBounds.height,
                                node.translateX, node.translateY, node.translateX + node.layoutBounds.width)
                        if (collide) {
                            break
                        }
                    }
                }
            }
        } else if (!widgetDrag && appDrag) {
            val pane = appSelected.get().pane
            val bounds = pane.layoutBounds
            val tp = event.touchPoint

            if (tp.x > 0 && tp.x + bounds.width < scene.width) {
                Platform.runLater { pane.translateX = event.touchPoint.x }
            }

            if (tp.y > 0 && tp.y + bounds.height < scene.height) {
                Platform.runLater { pane.translateY = event.touchPoint.y }
            }
        }
    }

    /**
     * Depending on the [TouchEvent.touchPoint] location, the user may be touching an [App], a [Widget], or neither.
     *
     * This will call additional functions that will change [transitionFuture].
     */
    private fun onTouchPressed(event: TouchEvent) {
        if (!mouseClicked && overlayPane.children.size == 0) {
            mouseClicked = true
            mouseStart = Point2D(event.touchPoint.x, event.touchPoint.y)

            val touchesWidget = widgetPane.children.any { it.boundsInParent.contains(mouseStart) }
            val touchesApp = appPane.children.any { it.boundsInParent.contains(mouseStart) }

            if (!touchesWidget && !touchesApp) {
                setTransitionFuture(launchAppView())
            } else if (touchesWidget && !touchesApp) {
                setTransitionFuture(launchWidgetEditor())
            } else if (!touchesWidget && touchesApp) {
                setTransitionFuture(launchAppDrag())
            }
        }
    }

    /**
     * Prints that the program is closing and then exits the program.
     */
    private fun close() {
        println("Exiting program...")
        System.exit(0)
    }

    /**
     * Sets the [transitionFuture] to execute a different [Runnable].
     */
    private fun setTransitionFuture(runnable: Runnable) {
        transitionFuture = transitionTimer.schedule(runnable, TIME_TO_HOLD.toLong(), TimeUnit.MILLISECONDS)
    }

    /**
     * Allows for the selected [App] to be dragged around the [scene].
     */
    private fun launchAppDrag(): Runnable {
        return Runnable {
            loop@ for (i in 0 until appPane.children.size) {
                val child = appPane.children[i]

                if (child.boundsInParent.contains(mouseStart)) {
                    appSelected.set(apps.stream().filter { it.pane == child }.findFirst().get())

                    appDrag = true

                    break@loop
                }
            }
        }
    }

    /**
     * Allows for the selected [Widget] to be dragged around the [scene].
     *
     * Also allows for removal and resizing of [Widget] implementations.
     */
    private fun launchWidgetEditor(): Runnable {
        return Runnable {
            loop@ for (i in 0 until widgetPane.children.size) {
                val child = widgetPane.children[i]

                if (child.boundsInParent.contains(mouseStart)) {
                    widgetSelected.set(widgets.stream().filter { it.pane == child }.findFirst().get())

                    originalPosition = Point2D(child.translateX, child.translateY)

                    Platform.runLater {
                        overlayPane.children.addAll(widgetOutline, overlayRemove, overlayResizeHigher,
                                overlayResizeLower, overlayCancel, overlayConfirm)
                    }

                    widgetDrag = true

                    break@loop
                }
            }
        }
    }

    /**
     * Launches the [App] chooser menu.
     *
     * Will only load an [App] if an instance of that [App] does not already exist in [apps]. If it does exist, a
     * [Dialog] will be shown.
     */
    private fun launchAppView(): Runnable {
        return Runnable {
            appThumbnailView.resetPane()

            appThumbnailView.setOnTouchPressed {
                if (appThumbnailView.ready) {
                    appThumbnailView.widgetPaneTransition.reversedTimeline.setOnFinished { _ ->
                        appThumbnailView.waitSelectedApp()

                        if (appThumbnailView.exists) {
                            Dialog("This app is already open!", scene)

                            Platform.runLater { overlayPane.children.remove(appThumbnailView) }
                        } else {
                            apps.add(appThumbnailView.selectedApp!!)

                            Platform.runLater {
                                overlayPane.children.remove(appThumbnailView)
                                appPane.children.add(appThumbnailView.selectedApp!!.pane)
                            }
                        }

                        scene.onTouchReleased.handle(it)
                    }
                }
            }

            Platform.runLater { overlayPane.children.add(appThumbnailView) }
        }
    }

    /**
     * Initialize and bind the positions of all the [Widget] overlay positions to the other elements visible on the
     * [scene].
     */
    private fun calculateOverlayPositions() {
        overlayConfirm.preserveRatioProperty().bind(overlayCancel.preserveRatioProperty())
        overlayConfirm.opacityProperty().bind(overlayCancel.opacityProperty())
        overlayConfirm.translateYProperty().bind(overlayCancel.translateYProperty())
        overlayConfirm.fitWidthProperty().bind(overlayCancel.fitWidthProperty())
        overlayConfirm.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            (scene.widthProperty().get() / 3) * 2 - overlayConfirm.layoutBoundsProperty().get().width / 2
        }, scene.widthProperty(), overlayConfirm.layoutBoundsProperty()))

        overlayCancel.isPreserveRatio = true
        overlayCancel.opacity = 0.3
        overlayCancel.fitWidthProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            scene.widthProperty().get() / 16
        }, scene.widthProperty()))
        overlayCancel.translateYProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            3 * (scene.heightProperty().get() / 4) - overlayCancel.layoutBoundsProperty().get().height / 2
        }, scene.heightProperty(), overlayCancel.layoutBoundsProperty()))
        overlayCancel.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            (scene.widthProperty().get() / 3) - overlayCancel.layoutBoundsProperty().get().width / 2
        }, scene.widthProperty(), overlayCancel.layoutBoundsProperty()))

        widgetOutline.fill = Color.GREEN
        widgetOutline.opacity = 0.5

        updateOverlay()
    }

    /**
     * Updates certain overlay components that cannot be binded to another element properly. This is updated every
     * time [Platform.runLater] is called while a [Widget] is being moved.
     */
    private fun updateOverlay() {
        val node = widgetSelected.get().pane

        widgetOutline.xProperty().bind(node.translateXProperty())
        widgetOutline.yProperty().bind(node.translateYProperty())
        widgetOutline.widthProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            node.boundsInParentProperty().get().width
        }, node.boundsInParentProperty()))
        widgetOutline.heightProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            node.boundsInParentProperty().get().height
        }, node.boundsInParentProperty()))

        overlayRemove.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().maxX -
                    overlayRemove.imageProperty().get().width / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayRemove.imageProperty()))
        overlayRemove.translateYProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().minY -
                    overlayRemove.imageProperty().get().height / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayRemove.imageProperty()))

        overlayResizeHigher.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().maxX -
                    overlayResizeHigher.imageProperty().get().width / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayResizeHigher.imageProperty()))
        overlayResizeHigher.translateYProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().maxY -
                    overlayResizeHigher.imageProperty().get().height / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayResizeHigher.imageProperty()))

        overlayResizeLower.translateXProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().minX -
                    overlayResizeLower.imageProperty().get().width / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayResizeLower.imageProperty()))
        overlayResizeLower.translateYProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            widgetSelected.get().pane.boundsInParentProperty().get().maxY -
                    overlayResizeLower.imageProperty().get().height / 2
        }, widgetSelected, widgetSelected.get().pane.boundsInParentProperty(), overlayResizeLower.imageProperty()))
    }

    /**
     * Adds all active [Widget] implementations to the [widgetPane].
     */
    private fun addWidgets() {
        widgets.forEach { widgetPane.children.add(it.pane) }

        startWidgetTimer()
    }

    /**
     * Starts a timer that automatically updates all active [Widget] objects in [widgets] every
     * [MainConstants.TIMER_UPDATE_WIDGET] milliseconds.
     */
    private fun startWidgetTimer() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                widgets.forEach { it.update() }
            }
        }, 0, MainConstants.TIMER_UPDATE_WIDGET.toLong())
    }

    /**
     * This class manages checking the collision of the selected [Widget] and the sides of the [scene] or other
     * [Widget] objects on the [scene].
     *
     * If the collision should occur, the selected [Widget] will instead snap to the other [Widget] or the side of the
     * [scene].
     */
    class SnapSide(private val scene: Scene, private val widget: Node, event: TouchEvent) {
        /**
         * Get touch point location and calculate edge location based on half of the width/height of the [Widget]. The
         * width and height of the [Widget] is identified using [Node.getBoundsInParent].
         */
        private val bounds = widget.boundsInParent
        private val top = event.touchPoint.y - bounds.height / 2
        private val right = event.touchPoint.x + bounds.width / 2
        private val bottom = event.touchPoint.y + bounds.height / 2
        private val left = event.touchPoint.x - bounds.width / 2

        private var snapped: Boolean = false

        /**
         * Returns true if, based on [TouchEvent.touchPoint], the [Widget] boundaries are within the [scene] boundaries.
         */
        fun isValidLocation(): Boolean {
            if (top >= 0 && right < scene.width && bottom < scene.height && left >= 0) {
                return true
            }

            return false
        }

        /**
         * Returns true if the selected [Widget] is colliding with any edges of the [scene].
         */
        fun isCollidingSide(top2: Double, right2: Double, bottom2: Double, left2: Double): Boolean {
            val topCollision = shouldSnap(top, top2)
            val rightCollision = shouldSnap(right, right2)
            val bottomCollision = shouldSnap(bottom, bottom2)
            val leftCollision = shouldSnap(left, left2)

            when {
                topCollision -> {
                    Platform.runLater { widget.translateY = top2 }
                    snapped = true
                }
                rightCollision -> {
                    Platform.runLater { widget.translateX = right2 - bounds.width }
                    snapped = true
                }
                bottomCollision -> {
                    Platform.runLater { widget.translateY = bottom2 - bounds.height }
                    snapped = true
                }
                leftCollision -> {
                    Platform.runLater { widget.translateX = left2 }
                    snapped = true
                }
            }

            if (!rightCollision && !leftCollision) {
                Platform.runLater { widget.translateX = left }
            }

            if (!topCollision && !bottomCollision) {
                Platform.runLater { widget.translateY = top }
            }

            return snapped
        }

        /**
         * Returns true if the selected [Widget] is colliding with another [Widget].
         */
        fun isCollidingWidget(bottom2: Double, left2: Double, top2: Double, right2: Double): Boolean {
            val topCollision = shouldSnap(top, bottom2)
            val rightCollision = shouldSnap(right, left2)
            val bottomCollision = shouldSnap(bottom, top2)
            val leftCollision = shouldSnap(left, right2)

            if (topCollision) {
                if (right > left2 && left < right2) {
                    Platform.runLater { widget.translateY = bottom2 }
                    return true
                }
            } else if (rightCollision) {
                if (bottom > top2 && top < bottom2) {
                    Platform.runLater { widget.translateX = left2 - bounds.width }
                    return true
                }
            } else if (bottomCollision) {
                if (right > left2 && left < right2) {
                    Platform.runLater { widget.translateY = top2 - bounds.height }
                    return true
                }
            } else if (leftCollision) {
                if (bottom > top2 && top < bottom2) {
                    Platform.runLater { widget.translateX = right2 }
                    return true
                }
            }

            if (!topCollision && !bottomCollision) {
                Platform.runLater { widget.translateY = top }
            }

            if (!rightCollision && !leftCollision) {
                Platform.runLater { widget.translateX = left }
            }

            return false
        }

        /**
         * Returns true if the object should snap based on the proximity of one side to another side.
         */
        private fun shouldSnap(side1: Double, side2: Double): Boolean {
            return Math.abs(side1 - side2) in 1..WIDGET_SNAP_DISTANCE
        }
    }
}
