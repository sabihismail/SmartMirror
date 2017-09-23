package com.sabihismail.SmartMirror.database

import com.sabihismail.SmartMirror.mirror.MirrorException
import com.sabihismail.SmartMirror.widget.Widget
import javafx.geometry.Point2D
import javafx.scene.Scene
import org.jetbrains.annotations.NotNull
import org.reflections.Reflections
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.reflect.full.primaryConstructor

/**
 * Manages the main application data storage.
 *
 * This includes data about which [Widget] implementations are enabled and data about the properties for different
 * [Widget] implementations.
 *
 * @date: July 20, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class Database {
    /**
     * SQLite connection to database file located at [DATABASE_LOCATION]
     */
    private val connection = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_LOCATION)

    /**
     * Verify all tables exist in database. If they do not, create them.
     */
    init {
        createTableProperties()
        createTableWidget()
    }

    /**
     * Create properties table if it does not already exist. This table accepts 3 values, the [Widget.uniqueId], and a
     * key/value [Pair] object.
     */
    private fun createTableProperties() {
        val statement = connection.prepareStatement(String.format(
                "CREATE TABLE IF NOT EXISTS %s (id TEXT NOT NULL, key TEXT NOT NULL, value TEXT NOT NULL, " +
                        "CONSTRAINT ct_primarykey PRIMARY KEY(id, key))",
                TABLE_PROPERTIES))
        statement.executeUpdate()
        statement.close()
    }

    /**
     * Primarily, attempt to insert the values into the database. If an [SQLException] is caught, the entry must already
     * exist. Instead, update the current saved value for that key.
     *
     * @param id The [Widget.uniqueId] of which the property pertains to.
     * @param pair The property key and set value that is to be saved.
     */
    fun addOrUpdateProperty(@NotNull id: String, @NotNull pair: Pair<String, String>) {
        var sql = String.format("INSERT INTO %s VALUES ('%s', '%s', '%s')",
                TABLE_PROPERTIES, id, pair.first, pair.second)
        var statement = connection.prepareStatement(sql)

        try {
            statement.executeUpdate()
        } catch (e: SQLException) {
            sql = String.format("UPDATE %s SET value='%s' WHERE id='%s' AND key='%s'",
                    TABLE_PROPERTIES, pair.second, id, pair.first)
            statement = connection.prepareStatement(sql)
            statement.executeUpdate()
        }

        statement.close()
    }

    /**
     * Returns all properties saved for the desired [Widget.uniqueId].
     *
     * @param id The [Widget.uniqueId] of which to retrieve all related properties.
     * @return Returns a [Map] containing all key/value pair of properties pertaining to the specific [Widget.uniqueId].
     */
    fun selectProperties(@NotNull id: String): Map<String, String> {
        val statement = connection.prepareStatement(String.format(
                "SELECT * FROM %s WHERE id='%s'",
                TABLE_PROPERTIES, id))
        val resultSet = statement.executeQuery()

        val properties: MutableMap<String, String> = HashMap()
        while (resultSet.next()) {
            val key = resultSet.getString("key")
            val value = resultSet.getString("value")

            properties.put(key, value)
        }

        resultSet.close()
        statement.close()

        return properties
    }

    /**
     * Retrieve all key/value properties with id in the entire Properties table.
     *
     * This is just for testing purposes and <b>SHOULD NOT</b> be used in regular [Widget] development.
     */
    fun selectAllProperties() {
        val statement = connection.prepareStatement(String.format("SELECT * FROM %s",
                TABLE_PROPERTIES))
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val key = resultSet.getString("key")
            val value = resultSet.getString("value")

            println("$id $key - $value")
        }

        resultSet.close()
        statement.close()
    }

    /**
     * Create database table for [Widget] objects if it does not already exist. This table accepts 4 values, the
     * [Widget.uniqueId], whether the [Widget] is enabled in the project, and the x and y co-ordinates of the object
     * on the [Scene].
     */
    private fun createTableWidget() {
        val statement = connection.prepareStatement(String.format(
                "CREATE TABLE IF NOT EXISTS %s (id TEXT PRIMARY KEY, enabled BOOLEAN, x FLOAT, y FLOAT)",
                TABLE_WIDGET))
        statement.executeUpdate()
        statement.close()
    }

    /**
     * Primarily, attempt to insert the values into the database. If an [SQLException] is caught, the entry must already
     * exist. Instead, update the current properties of the [Widget] based on the [Widget.uniqueId].
     *
     * @param id The [Widget.uniqueId] of which the property pertains to.
     * @param enabled True if the [Widget] is enabled on the [Scene].
     * @param coord The x/y co-ordinate where the [Widget] is located on the [Scene].
     */
    fun addOrUpdateWidget(id: String, enabled: Boolean, coord: Point2D) {
        var boolToInt = 1
        if (!enabled) {
            boolToInt = 0
        }

        var sql = String.format("INSERT INTO %s VALUES ('%s', %d, %f, %f)",
                TABLE_WIDGET, id, boolToInt, coord.x, coord.y)
        var statement = connection!!.prepareStatement(sql)

        try {
            statement.executeUpdate()
        } catch (e: SQLException) {
            sql = String.format("UPDATE %s SET enabled=%d, x=%f, y=%f WHERE id='%s'",
                    TABLE_WIDGET, boolToInt, coord.x, coord.y, id)
            statement = connection.prepareStatement(sql)
            statement.executeUpdate()
        }

        statement.close()
    }

    /**
     * Returns all properties related to all [Widget] saved for the desired [Widget.uniqueId].
     *
     * @return Returns a [List] of [Widget] objects that are to be added to the [Scene].
     */
    fun selectAllWidgets(): MutableList<Widget> {
        val allWidgetProperties: MutableMap<String, Widget.Properties> = HashMap()

        val statement = connection.prepareStatement(String.format("SELECT * FROM %s", TABLE_WIDGET))
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val id = resultSet.getString("id")
            val enabled = resultSet.getBoolean("enabled")
            val x = resultSet.getDouble("x")
            val y = resultSet.getDouble("y")

            allWidgetProperties.put(id, Widget.Properties(enabled, Point2D(x, y), id))
        }

        resultSet.close()
        statement.close()

        val widgets: MutableList<Widget> = ArrayList()

        val allWidgets = Reflections().getSubTypesOf(Widget::class.java)
        allWidgets.forEach {
            val id = it.getDeclaredField("UNIQUE_ID").get(String) as String

            val widgetProperty = allWidgetProperties.getOrDefault(id, Widget.Properties(true, Point2D.ZERO, id))
            INSTANCE.addOrUpdateWidget(widgetProperty.uniqueId, widgetProperty.enabled, widgetProperty.coords)

            if (widgetProperty.enabled) {
                try {
                    val widget = it.kotlin.primaryConstructor!!.call(widgetProperty.enabled, widgetProperty.coords)
                    widget.parameters(selectProperties(id))

                    widgets.add(widget)
                } catch (e: Exception) {
                    MirrorException.Exceptions.MissingDataException("Missing primary constructor that accepts 2 " +
                            "parameters: A boolean for if the Widget is enabled, and the co-ordinates.", true, e)
                }
            }
        }

        return widgets
    }

    /**
     * All constants for the [Database]. Also includes the [Database] instance that all other classes utilize when
     * needed.
     */
    companion object {
        /**
         * The [Database] instance that all other classes, such as [Widget], refer to. This prevents having to pass
         * an instance of the [Database] through multiple objects.
         */
        @JvmStatic
        val INSTANCE = Database()

        /**
         * File name of database save file
         */
        private const val DATABASE_LOCATION = "smartmirror.sqlite"
        /**
         * Table name for general key/value properties
         */
        private const val TABLE_PROPERTIES = "properties"
        /**
         * Table name for properties relating to [Widget] objects
         */
        private const val TABLE_WIDGET = "widget"

        @JvmStatic
        fun main(args: Array<String>) {
            val d = Database()

            d.selectAllProperties()
        }
    }
}