package com.sabihismail.SmartMirror.app

import com.sabihismail.SmartMirror.app.instances.MusicPlayer
import com.sabihismail.SmartMirror.mirror.MirrorException
import com.sabihismail.SmartMirror.settings.GeneralConstants
import javafx.beans.binding.Bindings
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable

/**
 * The [App] class is the base template for all [App] implementations for the SmartMirror.
 *
 * @date: August 11, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
abstract class App protected constructor(val properties: Properties, val scene: Scene) {
    /**
     * Each [App] is expected to have a JavaFX layout manager. This [Region] should have custom components relevant
     * to the functionality of the [App].
     *
     * For example, [MusicPlayer] will be expected to have a layout with information about a song, a pause/play button,
     * and a next/previous track button.
     */
    abstract val pane: Region

    /**
     * Ensures that the [App] implementation has a folder in the 'app/' folder located in the root directory. If the
     * [App] does not, a folder will be created and will throw an exception stating that a thumbnail image of the folder
     * cannot be found.
     */
    init {
        val directory = File(properties.saveFolder)
        if (!directory.exists()) {
            throw MirrorException.Exceptions.StartupException("The resources folder for '${properties.uniqueId}' " +
                    "does not exist! You must have an App folder at '${properties.saveFolder}' with a relevant App " +
                    "thumbnail image saved under the name '${properties.uniqueId}.png'.")
        }
    }

    /**
     * Returns absolute path of any file name relative to the [App]'s save directory.
     *
     * @param filename Desired filename.
     * @return Returns absolute path relative to [App]'s save directory.
     */
    protected fun getAbsolutePath(filename: String): String {
        checkFileName(filename)

        return File("${properties.saveFolder}/$filename").absolutePath
    }

    /**
     * Creates a file in the 'app/$uniqueId/' folder. All [App] implementations should save all relevant files in their
     * folders.
     *
     * @param filename The desired file's name.
     */
    protected fun createFileIfNotExists(filename: String, overwrite: Boolean) {
        checkFileName(filename)

        val file = File(getAbsolutePath(filename))
        if (!file.parentFile.mkdirs()) {
            throw MirrorException.Exceptions.IllegalCodeExecutionException("Folder creation failed. Please ensure " +
                    "that '${file.absolutePath}' is a valid directory.")
        }

        try {
            if (file.exists()) {
                if (overwrite) {
                    file.delete()
                    file.createNewFile()
                }
            } else {
                file.createNewFile()
            }
        } catch (e: IOException) {
            throw MirrorException.Exceptions.IllegalCodeExecutionException("File creation failed.", e)
        }
    }

    /**
     * Loads an image from the [App]'s designated image directory.
     *
     * @param filename The name of the desired image.
     * @return The loaded image.
     */
    protected fun loadImage(filename: String, x: Double, y: Double): Image {
        checkFileName(filename)

        val file = File("${properties.imagesFolder}/$filename")
        val exists = file.exists()
        if (exists) {
            return Image("${file.toURI()}", x, y, true, false)
        } else {
            throw MirrorException.Exceptions.MissingDataException("'${file.absolutePath}' does not exist!", true)
        }
    }

    /**
     * Checks if [filename] is valid.
     * Will throw [MirrorException.Exceptions.IllegalCodeExecutionException] if directory transversal detected.
     *
     * @param filename The filename that will be checked.
     */
    private fun checkFileName(filename: String) {
        if (filename.contains("../")) {
            throw MirrorException.Exceptions.IllegalCodeExecutionException("Do not attempt directory transversal. " +
                    "All files that your App creates must be located in it's respectable folder at " +
                    "'${properties.saveFolder}'. Your App is permitted to read from files in other locations " +
                    "but not write to them.")
        }
    }

    /**
     * This class allows for ease of transferring variables. It also allows for adding/removing variables easily.
     *
     * @param uniqueId The unique id of the [App] implementation.
     * @param displayName The official displayed name of the [App] implementation.
     */
    data class Properties(val uniqueId: String,
                          val displayName: String) {
        val saveFolder = "app/$uniqueId"
        val imagesFolder = "$saveFolder/images"
    }

    /**
     * This class displays the [App] implementation's thumbnail image directly above the [Properties.displayName] as a
     * [VBox].
     *
     * @param properties The [Properties] object that contains the unique id and the display name of the implementation.
     * @param scene The [Scene] object that allows for the [Thumbnail] to have components relative to the [Scene.width]
     *              and [Scene.height].
     */
    class Thumbnail(properties: Properties, scene: Scene) : VBox() {
        /**
         * All constants for the [Thumbnail].
         */
        companion object {
            /**
             * The file name of the thumbnail image. This image must exist in the [Properties.saveFolder] directory.
             */
            const val THUMBNAIL_FILE_NAME = "thumbnail.png"

            /**
             * The width and height of the thumbnail. The image will be resized if not already this size.
             */
            const val THUMBNAIL_SIZE = 256.0

            /**
             * The min/max scale values for the [Thumbnail] when clicked.
             */
            const val THUMBNAIL_MAX_SCALE = 1.0
            const val THUMBNAIL_MIN_SCALE = 0.9
        }

        /**
         * The location of the thumbnail image file. If the image is not found, an exception will be thrown and the
         * program will shut down automatically.
         */
        private val thumbnailLoc = "${properties.saveFolder}/" + THUMBNAIL_FILE_NAME

        val nameLabel = Label(properties.displayName)

        init {
            val thumbnailFile = File(thumbnailLoc)
            thumbnailFile.parentFile.mkdirs()

            try {
                var thumbnailImage = Image("file:${thumbnailFile.absolutePath}")
                if (thumbnailImage.width != thumbnailImage.height) {
                    throw MirrorException.Exceptions.SuggestedImprovementException("The thumbnail provided should " +
                            "have the same width/height for expected results. The image will automatically be fit " +
                            "to ${THUMBNAIL_SIZE}x${THUMBNAIL_SIZE} .")
                }
                thumbnailImage = Image("file:${thumbnailFile.absolutePath}", THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true)
                val thumbnail = ImageView(thumbnailImage)
                thumbnail.fitWidthProperty().bind(thumbnail.fitHeightProperty())
                thumbnail.fitHeightProperty().bind(Bindings.createDoubleBinding(Callable<Double> {
                    scene.heightProperty().get() / 6
                }, scene.heightProperty()))

                nameLabel.textAlignment = TextAlignment.CENTER
                nameLabel.textFill = GeneralConstants.GLOBAL_WIDGET_COLOUR
                nameLabel.font = Font.font(GeneralConstants.GLOBAL_FONT, 18.0)

                this.scaleYProperty().bind(this.scaleXProperty())

                this.alignment = Pos.CENTER
                this.spacing = 6.0
                this.children.addAll(thumbnail, nameLabel)
            } catch (e: IllegalArgumentException) {
                throw MirrorException.Exceptions.StartupException("'$thumbnailLoc' does not exist! Please add a " +
                        "basic image thumbnail with the format PNG with the name '${THUMBNAIL_FILE_NAME}'", e)
            }
        }
    }
}
