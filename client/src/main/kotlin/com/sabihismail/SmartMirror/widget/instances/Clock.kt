package com.sabihismail.SmartMirror.widget.instances

import com.sabihismail.SmartMirror.database.Database
import com.sabihismail.SmartMirror.settings.GeneralConstants
import com.sabihismail.SmartMirror.widget.Widget
import com.sabihismail.SmartMirror.widget.instances.Clock.Companion.CLOCK_FORMAT_1
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Point2D
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextBoundsType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * This [Widget] implementation is a digital clock.
 *
 * Clicking the [Clock] will change the format of the text. View the [CLOCK_FORMAT_1] section for more information.
 *
 * @date: July 10, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class Clock(enabled: Boolean, coords: Point2D) : Widget(Properties(enabled, coords, UNIQUE_ID)) {
    companion object {
        const val UNIQUE_ID = "clock"

        /**
         * The font size difference to increase/decrease upon resizing the [Clock]
         */
        const val FONT_RESIZE_DIFFERENCE = 4

        /**
         * All supported formats for the [Clock.timeLabel] text
         */
        const val CLOCK_FORMAT_1 = "hh:mm:ss a"
        const val CLOCK_FORMAT_2 = "kk:mm:ss"
        const val CLOCK_FORMAT_3 = "hh:mm a"
        const val CLOCK_FORMAT_4 = "hh:mm"
        const val CLOCK_FORMAT_5 = "kk:mm"
    }

    override val pane = BorderPane()

    private var dateFormatSelection = 0
    private var dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_1)
    private val timeLabel = Text()
    private val time = SimpleStringProperty()

    init {
        selectDateFormat()

        timeLabel.fill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        timeLabel.boundsType = TextBoundsType.VISUAL
        timeLabel.font = Font.font(GeneralConstants.GLOBAL_FONT, 46.0)
        timeLabel.textProperty().bind(time)

        pane.center = timeLabel
    }

    /**
     * Loads the saved [timeLabel] format from the [Database] if it exists.
     *
     * Also loads the font size of the [timeLabel].
     */
    override fun parameters(map: Map<String, String>) {
        map.forEach {
            when (it.key) {
                "dateFormatSelection" -> dateFormatSelection = it.value.toInt()
                "timeLabelFontSize" -> timeLabel.font = Font.font(it.value.toDouble())
            }
        }

        selectDateFormat()
    }

    /**
     * Updates the time value that is visible on the [timeLabel].
     */
    override fun update() = Platform.runLater { time.value = dateFormat.format(LocalTime.now()) }

    /**
     * Changes the format of the [timeLabel] text.
     */
    override fun onClick() {
        dateFormatSelection++
        selectDateFormat()

        Database.INSTANCE.addOrUpdateProperty(UNIQUE_ID, Pair("dateFormatSelection", dateFormatSelection.toString()))
    }

    /**
     * Upon confirming resize changes, saves the [timeLabel] font data into the [Database].
     */
    override fun confirmChanges() {
        Database.INSTANCE.addOrUpdateProperty(UNIQUE_ID, Pair("timeLabelFontSize", timeLabel.font.size.toString()))
    }

    /**
     * Upon cancelling resize changes, reloads the [timeLabel] font size data from [Database].
     */
    override fun cancelChanges() {
        val map = Database.INSTANCE.selectProperties(UNIQUE_ID)

        timeLabel.font = Font.font(map.getValue("timeLabelFontSize").toDouble())
    }

    /**
     * Changes the format of the [timeLabel] to match the current active format identified by [dateFormatSelection].
     */
    private fun selectDateFormat() {
        when (dateFormatSelection) {
            0 -> dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_1)
            1 -> dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_2)
            2 -> dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_3)
            3 -> dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_4)
            4 -> dateFormat = DateTimeFormatter.ofPattern(CLOCK_FORMAT_5)
            else -> {
                dateFormatSelection = 0
                selectDateFormat()
            }
        }
    }

    /**
     * Resizes the [Clock] by either increasing or decreasing the font size of [timeLabel] depending on if [increase] is
     * true.
     */
    override fun resize(increase: Boolean) {
        Platform.runLater {
            if (increase) {
                timeLabel.font = Font.font(timeLabel.font.size + FONT_RESIZE_DIFFERENCE)
            } else {
                timeLabel.font = Font.font(timeLabel.font.size - FONT_RESIZE_DIFFERENCE)
            }
        }
    }
}
