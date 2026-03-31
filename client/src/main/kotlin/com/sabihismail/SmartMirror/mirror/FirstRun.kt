package com.sabihismail.SmartMirror.mirror

import com.sabihismail.SmartMirror.database.Database
import javafx.scene.Scene
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.stage.Stage
import java.sql.SQLException

/**
 *
 *
 * @date: July 20, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class FirstRun(private val stage: Stage) {
    private fun checkIfFirstRun() {
        try {
            val generalProperties = Database.INSTANCE.selectProperties("general")

            if (generalProperties.isEmpty() || generalProperties["first_run"] == null) {
                val welcome = Label("Welcome")
                welcome.font = Font("Calibri", 32.0)
                val setup = Label("Let's get you set up")
                setup.font = Font("Calibri", 20.0)

                val top = VBox()
                top.spacing = 6.0
                top.children.addAll(welcome, setup)



                val country = ComboBox<String>()

                val center = VBox()

                val pane = BorderPane()
                pane.top = top

                val firstRunScene = Scene(pane)
                stage.scene = firstRunScene
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        }
    }

    /**
     * Returns the IP address of the user
     */
    private fun getIp(): String {
        return ""
    }
}