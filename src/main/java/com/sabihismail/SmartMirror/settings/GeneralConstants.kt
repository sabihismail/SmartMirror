package com.sabihismail.SmartMirror.settings

import javafx.scene.Scene
import javafx.scene.paint.Color

/**
 * This class contains all constants that should be followed by every relevant class by default.
 *
 * For example, this object contains values for the default font and the default text colour. All GUI instances should
 * all refer to these values to maintain a consistent look on the [Scene].
 *
 * @date: July 26, 2017
 * @author: Sabih Ismail
 * @since 1.0
*/
object GeneralConstants {
    /**
     * The name of the font that will be used by all GUI instances
     */
    const val GLOBAL_FONT = "Calibri"

    /**
     * The [Color] of all GUI instances by default.
     */
    @JvmField val GLOBAL_WIDGET_COLOUR: Color = Color.WHITE!!
}