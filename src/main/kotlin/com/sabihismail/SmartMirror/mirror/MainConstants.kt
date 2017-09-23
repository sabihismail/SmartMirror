package com.sabihismail.SmartMirror.mirror

import com.sabihismail.SmartMirror.widget.Widget
import javafx.geometry.Insets
import javafx.scene.input.KeyCode
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.paint.Color

/**
 * General constants that are accessed from multiple classes. These constants are also grouped to allow for ease of
 * editing to match the preferences of any other developer.
 *
 * @date: July 10, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
object MainConstants {
    /**
     * Combination to exit fullscreen. Currently set to: CTRL + ESC.
     */
    @JvmField val KEYCODE_EXIT_FULLSCREEN = KeyCode.ESCAPE

    /**
     * Colour of background of scene.
     */
    @JvmField val BACKGROUND_SCENE: Color = Color.BLACK

    /**
     * Colour of background of application. Currently set to: [BACKGROUND_SCENE].
     */
    @JvmField val BACKGROUND_APPLICATION = Background(BackgroundFill(BACKGROUND_SCENE, CornerRadii.EMPTY, Insets.EMPTY))

    /**
     * Time in milliseconds to update all [Widget] objects.
     */
    const val TIMER_UPDATE_WIDGET = 100

    /**
     * Path to all image files pertaining to [Main].
     */
    private const val PATH_TO_IMAGES = "/general/images/"

    /**
     * Path to image for [Main.overlayRemove].
     */
    const val OVERLAY_RESIZE_HIGHER = PATH_TO_IMAGES + "overlay-resize-higher.png"

    /**
     * Path to image for [Main.overlayRemove].
     */
    const val OVERLAY_RESIZE_LOWER = PATH_TO_IMAGES + "overlay-resize-lower.png"

    /**
     * Path to image for [Main.overlayRemove].
     */
    const val OVERLAY_REMOVE_IMAGE = PATH_TO_IMAGES + "overlay-remove.png"

    /**
     * Path to image for [Main.overlayConfirm].
     */
    const val OVERLAY_CONFIRM_IMAGE = PATH_TO_IMAGES + "overlay-confirm.png"

    /**
     * Path to image for [Main.overlayCancel].
     */
    const val OVERLAY_CANCEL_IMAGE = PATH_TO_IMAGES + "overlay-cancel.png"
}