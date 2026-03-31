/**
 * @date: July 20, 2017
 * @author: Sabih Ismail
 *
 * NOTE: Weather icons downloaded from https://www.amcharts.com/free-animated-svg-weather-icons/ under CC rights
 */
package com.sabihismail.SmartMirror.widget.instances

/*
import com.arkazeen.SmartMirror.settings.APIKeys
import com.arkazeen.SmartMirror.tools.Tools
import com.arkazeen.SmartMirror.widget.Widget
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Point2D
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import org.json.JSONObject
import java.time.LocalTime

/**
 * @since SmartMirror 1.0
 */
class Weather(enabled: Boolean, coords: Point2D) : Widget(Properties(enabled, coords, UNIQUE_ID)) {
    companion object {
        const val UNIQUE_ID = "weather"

        private val TIME_RESET = 10
        private val ICONS_BASE_DIR = "widget/temperature/"
        private val API_ENDPOINT = "http://samples.openweathermap.org/data/2.5/weather"

        private const val WEATHER_API_KEY = "f51be86c3ab50521ffef79c2c3131656"
    }

    override val pane = BorderPane()

    private var id = 0
    private var celcius = true
    private val temperature = SimpleStringProperty()
    private val weatherIcon = SimpleObjectProperty<Image>()

    private var resetTime: LocalTime = LocalTime.now()

    init {
        val label = Label()
        label.textProperty().bind(temperature)

        val imageView = ImageView()
        imageView.imageProperty().bind(weatherIcon)

        pane.center = imageView
        pane.right = label
    }

    override fun parameters(map: Map<String, String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update() {
        val localTime = LocalTime.now()

        if (localTime.isAfter(resetTime)) {
            val weatherUpdate = WeatherUpdate(id, celcius)
            temperature.set(weatherUpdate.temperature)
            weatherIcon.set(weatherUpdate.image)

            resetTime = localTime.plusMinutes(TIME_RESET.toLong())
        }
    }

    override fun onClick() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun resize(increase: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private class WeatherUpdate internal constructor(id: Int, celcius: Boolean) {
        internal val temperature: String
        internal val image: Image? = null

        init {
            val urlRequest = API_ENDPOINT + "?id=" + id + "&appid=" + APIKeys.WEATHER_API_KEY

            var json = Tools.readURL(urlRequest)
            val obj = JSONObject(json)

            val main = obj.getJSONObject("weather").getString("main")
            val description = obj.getJSONObject("weather").getString("description")

            temperature = checkTemperature(obj.getJSONObject("main").getString("temp"), celcius)
        }

        private fun checkTemperature(temperature: String, celcius: Boolean): String {

            if (celcius) {

            } else {

            }

            return ""
        }
    }
}
*/