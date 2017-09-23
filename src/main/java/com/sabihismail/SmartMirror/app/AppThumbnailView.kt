package com.sabihismail.SmartMirror.app

import com.sabihismail.SmartMirror.guitools.SinglePropertyTransition
import com.sabihismail.SmartMirror.mirror.MirrorException
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.effect.GaussianBlur
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import org.reflections.Reflections
import java.util.*
import java.util.concurrent.Callable
import kotlin.reflect.full.primaryConstructor


/**
 * This class shows the overlay for the [App] selection page. All correctly implemented [App] implementations are
 * automatically included in the overlay.
 *
 * @date: August 19, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class AppThumbnailView(private val currentScene: Scene,
                       widgetPane: Pane,
                       private val apps: MutableList<App>) : BorderPane() {
    companion object {
        /**
         * Percentage cutoff of [Region.maxWidth] when in App view.
         */
        const val PERCENT_CUTOFF_X = 0.2

        /**
         * Percentage cutoff of [Region.maxHeight] when in App view.
         */
        const val PERCENT_CUTOFF_Y = 0.1

        /**
         * Time in milliseconds to increase the radius of blur on the [GaussianBlur] for the background.
         */
        const val TRANSITION_TIME = 500

        /**
         * Gap between each [App] thumbnail in [App] View mode.
         *
         * The gap is two times the value for [FlowPane.hgap] but is exactly the pixel distance for [FlowPane.vgap].
         */
        const val APP_VIEW_GAP = 20.0
    }

    private val thumbnails: MutableList<App.Thumbnail> = ArrayList()
    private val thumbnailPane = FlowPane(APP_VIEW_GAP * 3, APP_VIEW_GAP)

    private var selectedAppIndex = -1
    var selectedApp: App? = null
    var ready = false
    var completed = false
    var exists = false

    private val effect = GaussianBlur(0.0)

    val widgetPaneTransition = SinglePropertyTransition(widgetPane.opacityProperty(),
            1.0, 0.5, TRANSITION_TIME, true)
    private val blurTransition = SinglePropertyTransition(effect.radiusProperty(),
            0.0, 17.0, TRANSITION_TIME, true)
    private val thumbnailPaneTransition = SinglePropertyTransition(thumbnailPane.opacityProperty(),
            0.0, 1.0, TRANSITION_TIME / 4, true)

    /**
     * Creates a GaussianBlur over the background and shows all [App.Thumbnail] objects for each [App] implementation
     * which are all tiled.
     */
    init {
        widgetPane.effect = effect

        thumbnailPane.isPickOnBounds = false

        thumbnailPane.alignment = Pos.CENTER
        thumbnailPane.minWidthProperty().bind(thumbnailPane.maxWidthProperty())
        thumbnailPane.minHeightProperty().bind(thumbnailPane.maxHeightProperty())
        thumbnailPane.maxWidthProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            currentScene.widthProperty().get() - (currentScene.widthProperty().get() * PERCENT_CUTOFF_X)
        }, currentScene.widthProperty()))
        thumbnailPane.maxHeightProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
            currentScene.heightProperty().get() -
                    (currentScene.heightProperty().get() * PERCENT_CUTOFF_Y)
        }, currentScene.heightProperty()))

        loadImages()

        this.minWidthProperty().bind(currentScene.widthProperty())
        this.minHeightProperty().bind(currentScene.heightProperty())
        this.center = thumbnailPane
        this.isPickOnBounds = false
    }

    /**
     * Resets all the values back to their original values. This is done before the [AppThumbnailView] is called as
     * the last transition may still have elements that match the expected data at the end of their transition.
     */
    fun resetPane() {
        thumbnails.forEach { it.opacity = 1.0 }

        ready = false
        thumbnailPane.opacity = 0.0
        selectedApp = null
        selectedAppIndex = -1

        blurTransition.timeline.play()
        widgetPaneTransition.timeline.play()
        thumbnailPaneTransition.timeline.play()

        widgetPaneTransition.timeline.setOnFinished {
            ready = true
        }
    }

    /**
     * Loads all thumbnail images and prepares an [App.Thumbnail] object which will be tiled in this overlay.
     *
     * Upon selection of the [App], the [App] will be loaded asynchronously. The [Main] class will then load the [App]
     * implementation if the [App] does not already exist.
     */
    private fun loadImages() {
        /*
        val dir = File("app/class/").listFiles(FileFilter { it.name.endsWith(".kt") })
        val allURLs: Array<URL> = Arrays.stream(dir).map { it.toURI().toURL() }.toList().toTypedArray()

        println(File("app/class/").toURI().toURL())
        val classLoader = URLClassLoader(arrayOf(File("app/class/").toURI().toURL()))
        val allApps = mutableListOf<Class<*>>()

        dir.forEach {
            val cls = classLoader.loadClass(it.name.split(".").first())

            allApps.add(cls)
        }
         */

        val allApps = Reflections().getSubTypesOf(App::class.java)
        allApps.forEach {
            try {
                val id = it.getDeclaredField("UNIQUE_ID").get(String) as String
                val name = it.getDeclaredField("APP_NAME").get(String) as String

                val thumbnailView = App.Thumbnail(App.Properties(id, name), currentScene)
                thumbnails.add(thumbnailView)

                Platform.runLater { thumbnailPane.children.add(thumbnailView) }
            } catch (e: NoSuchFieldException) {
                throw MirrorException.Exceptions.MissingDataException("You must have a String " +
                        "constant 'UNIQUE_ID' and 'APP_NAME' in your App. Please view the " +
                        "example class for more information.", true)
            }
        }

        thumbnails.forEachIndexed { index, thumbnail ->
            thumbnail.setOnTouchPressed {
                if (selectedAppIndex == -1 && ready) {
                    selectedAppIndex = index

                    thumbnail.scaleX = App.Thumbnail.THUMBNAIL_MIN_SCALE
                }
            }
            thumbnail.setOnTouchReleased {
                if (selectedAppIndex > -1) {
                    val thumbnailScaleTransition = SinglePropertyTransition(thumbnail.scaleXProperty(),
                            App.Thumbnail.THUMBNAIL_MIN_SCALE, App.Thumbnail.THUMBNAIL_MAX_SCALE,
                            TRANSITION_TIME / 4, false)
                    thumbnailScaleTransition.timeline.play()

                    blurTransition.reversedTimeline.play()
                    widgetPaneTransition.reversedTimeline.play()
                    thumbnailPaneTransition.reversedTimeline.play()

                    thumbnails.forEachIndexed { index, thumbnail ->
                        if (index != selectedAppIndex) {
                            val thumbnailOpacityTransition = SinglePropertyTransition(thumbnail
                                    .opacityProperty(), 1.0, -0.2, TRANSITION_TIME / 2, false)
                            thumbnailOpacityTransition.timeline.play()
                        }
                    }

                    thumbnailScaleTransition.timeline.setOnFinished { _ ->
                        val selectedAppClass = allApps.stream().filter {
                            it.getDeclaredField("APP_NAME").get(String) == thumbnail.nameLabel.text
                        }.findFirst().get()

                        exists = apps.stream().anyMatch {
                            it.properties.uniqueId == selectedAppClass.getDeclaredField("UNIQUE_ID").get(String)
                        }

                        if (!exists) {
                            Thread({
                                selectedApp = selectedAppClass.kotlin.primaryConstructor!!.call(currentScene)

                                currentScene.onTouchReleased.handle(it)

                                completed = true
                            }).start()
                        }
                    }
                }
            }
        }
    }

    /**
     * Repeatedly sleeps thread for 100 ms while the function waits until [selectedApp] is not null.
     */
    fun waitSelectedApp() {
        while (!completed) {
            Thread.sleep(100)
        }
    }
}