package com.sabihismail.SmartMirror.widget.instances

import com.sabihismail.SmartMirror.database.Database
import com.sabihismail.SmartMirror.settings.GeneralConstants
import com.sabihismail.SmartMirror.widget.Widget
import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Point2D
import javafx.scene.layout.BorderPane
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.text.TextBoundsType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * This [Widget] implementation is a visible date format matching that of:
 *
 * {Day-of-week}
 * {Month} {Day-of-month}
 *
 * Touching the [Date] will toggle the alignment of the text from left align, center, right-align, and justify.
 *
 * @date: July 25, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class Date(enabled: Boolean, coords: Point2D) : Widget(Properties(enabled, coords, UNIQUE_ID)) {
    companion object {
        const val UNIQUE_ID = "date"

        /**
         * The font size difference to increase/decrease upon resizing the [Clock]
         */
        const val FONT_RESIZE_DIFFERENCE = 4
    }

    override val pane = BorderPane()

    private val label = Text()
    private var labelAlignment = 0
    private val date = SimpleStringProperty()
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE\nMMMM d")

    init {
        selectLabelAlignment()

        label.fill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        label.boundsType = TextBoundsType.VISUAL
        label.font = Font.font(GeneralConstants.GLOBAL_FONT, 22.0)
        label.textProperty().bind(date)

        pane.right = label
    }

    /**
     * Loads the saved [label] format from the [Database] if it exists.
     *
     * Also loads the font size of the [label].
     */
    override fun parameters(map: Map<String, String>) {
        map.forEach {
            when (it.key) {
                "labelAlignment" -> labelAlignment = it.value.toInt()
                "labelFontSize" -> label.font = Font.font(it.value.toDouble())
            }
        }

        selectLabelAlignment()
    }

    /**
     * Updates the value of the [dateFormat] to match the current date.
     */
    override fun update() = Platform.runLater { date.value = dateFormat.format(LocalDate.now()) }

    /**
     * Changes the alignment of the [label].
     */
    override fun onClick() {
        labelAlignment++
        selectLabelAlignment()

        Database.INSTANCE.addOrUpdateProperty(UNIQUE_ID, Pair("labelAlignment", labelAlignment.toString()))
    }

    /**
     * Upon confirming resize changes, saves the label font size data into the [Database].
     */
    override fun confirmChanges() {
        Database.INSTANCE.addOrUpdateProperty(UNIQUE_ID, Pair("labelFontSize", label.font.size.toString()))
    }

    /**
     * Upon cancelling resize changes, reloads the [label] font size data from [Database].
     */
    override fun cancelChanges() {
        val map = Database.INSTANCE.selectProperties(UNIQUE_ID)

        label.font = Font.font(map.getValue("labelFontSize").toDouble())
    }

    /**
     * Changes the alignment of the [label] to match the current active format identified by [labelAlignment].
     */
    private fun selectLabelAlignment() {
        when (labelAlignment) {
            0 -> label.textAlignment = TextAlignment.LEFT
            1 -> label.textAlignment = TextAlignment.CENTER
            2 -> label.textAlignment = TextAlignment.RIGHT
            3 -> label.textAlignment = TextAlignment.JUSTIFY
            else -> {
                labelAlignment = 0
                selectLabelAlignment()
            }
        }
    }

    /**
     * Resizes the [Date] by either increasing or decreasing the font size of [label] depending on if [increase] is
     * true.
     */
    override fun resize(increase: Boolean) {
        Platform.runLater {
            if (increase) {
                label.font = Font.font(label.font.size + FONT_RESIZE_DIFFERENCE)
            } else {
                label.font = Font.font(label.font.size - FONT_RESIZE_DIFFERENCE)
            }
        }
    }
}