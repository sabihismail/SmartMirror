package com.sabihismail.SmartMirror.widget

import com.sabihismail.SmartMirror.database.Database
import com.sabihismail.SmartMirror.mirror.Main
import com.sabihismail.SmartMirror.mirror.MainConstants
import com.sabihismail.SmartMirror.widget.instances.Clock
import javafx.application.Platform
import javafx.geometry.Point2D
import javafx.scene.layout.Region

/**
 * A [Widget] is defined as any object that remains active throughout the entire lifetime of the application as
 * long as the [Widget] itself is enabled by the user.
 *
 * An example of this would be a [Clock] which remains active even while the user is performing another task,
 * such as playing music. Even while the user is interacting with another feature, the [Clock] will continue to be
 * updated in the background.
 *
 * All [Widget] return the layout of the [Widget] itself through the [Widget.get] method. All [Widget] also
 * update after [Main.startWidgetTimer] milliseconds.
 *
 * @date: July 10, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
abstract class Widget protected constructor(private val properties: Properties) {
    /**
     * Each [Widget] is expected to have a JavaFX layout manager to allow for placement of components.
     */
    abstract val pane: Region

    private var pressedTime = 0L
    private var pressedPos = Point2D.ZERO

    /**
     * On initialization, translate the pane to the user's desired location based on information supplied by the
     * [Database]. The main constructor also accepts a [Properties] class object with various variables that pertain
     * to [Widget] information, such as location, and whether the [Widget] is enabled in the SmartMirror.
     *
     * In this initialization, pressing on a [Widget] implementation will store that [TouchPoint] location and on
     * release, will compare to the new location. If the location is the same, the [onClick] method will be called.
     */
    init {
        Platform.runLater {
            pane.translateX = properties.coords.x
            pane.translateY = properties.coords.y

            pane.id = properties.uniqueId

            pane.setOnTouchPressed {
                pressedTime = System.currentTimeMillis()
                pressedPos = Point2D(it.touchPoint.x, it.touchPoint.y)
            }
            pane.setOnTouchReleased {
                if (System.currentTimeMillis() - pressedTime < Main.TIME_TO_HOLD) {
                    val point = Point2D(it.touchPoint.x, it.touchPoint.y)
                    if (point == pressedPos) {
                        onClick()
                    }
                }
            }
        }
    }

    /**
     * Pass and handle any parameters supplied by the [Database.selectAllWidgets] method. Each implementation should
     * parse the parameters as needed.
     *
     * All values are supplied as [String] and should be converted to alternate formats as required.
     */
    abstract fun parameters(map: Map<String, String>)

    /**
     * All [Widget] that are added to the project will update every [MainConstants.TIMER_UPDATE_WIDGET] milliseconds by
     * the timer [Main.startWidgetTimer].
     */
    abstract fun update()

    /**
     * When the [Widget] is clicked once, execute code specific to every implementation. The implementation does not
     * have to execute code upon click.
     */
    abstract fun onClick()

    /**
     * When the [Widget] is selected and the user resizes the selected object, call this method. Each [Widget]
     * implementation will have a custom property that is resized upon this function being called.
     * @param increase True when the user attempts to increase the [Widget]'s size.
     */
    abstract fun resize(increase: Boolean)

    /**
     * When the user clicks [Main.overlayConfirm] this method should save all the data to the database.
     */
    abstract fun confirmChanges()

    /**
     * When the user clicks [Main.overlayCancel] this method should revert all the data back to the original data.
     */
    abstract fun cancelChanges()

    /**
     * This class only contains data for all properties. Eases the developer's ability to add/remove variables that need
     * to be supplied to the [Widget] super class from the [Widget] implementation.
     */
    data class Properties(val enabled: Boolean,
                          val coords: Point2D,
                          val uniqueId: String)
}