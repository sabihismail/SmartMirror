package com.sabihismail.SmartMirror.app.instances

import com.sabihismail.SmartMirror.app.App
import com.sabihismail.SmartMirror.guitools.Dialog
import com.sabihismail.SmartMirror.mirror.MirrorException
import com.sabihismail.SmartMirror.settings.GeneralConstants
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.MapChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.stage.DirectoryChooser
import org.jetbrains.annotations.NotNull
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import java.util.Map
import java.util.stream.Collectors

/**
 * This class is an implementation of a music player that currently supports local music files with extensions
 * [MusicPlayer.MUSIC_EXTENSIONS].
 *
 * Shuffling and single song repeat is currently supported.
 *
 * Music Logo (Name: Music) by Sandy Priyasa from the Noun Project. The image was resized and colours altered.
 * Link: https://thenounproject.com/term/music/1187007/
 * Repeat and Shuffle Controls (Name: Music) by Jardson Almeida from the Noun Project. The image was resized, and
 * colours altered.
 * Link: https://thenounproject.com/jardson/collection/music/?oq=music&cidx=40
 * Pause Control (Name: Pause) by AfterGrind from the Noun Project. The image was resized, colours altered, and
 * edited.
 * Link: https://thenounproject.com/aftergrindilabs/collection/music/?i=1072555
 * Play, Next, and Previous Controls (Name: Music Player Icons) by DesignBite from the Noun Project. The images were
 * resized and colours altered.
 * Link: https://thenounproject.com/designbite/collection/music-player-icons/?oq=music%20icons&cidx=6
 * Default Album Art (Name: Album) by StoneHub from the Noun Project. The images were resized and colours altered.
 * Link: https://thenounproject.com/term/album/1113118/
 *
 * All images above are licenced under Creative Commons Attribution 3.0 United States License:
 * https://creativecommons.org/licenses/by/3.0/us/.
 *
 * @date: August 11, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
class MusicPlayer(scene: Scene) : App(Properties(UNIQUE_ID, APP_NAME), scene) {
    companion object {
        const val UNIQUE_ID = "musicplayer"
        const val APP_NAME = "Music Player"

        @JvmStatic
        val MUSIC_EXTENSIONS = arrayOf("mp3", "wav")
        const val SAVE_FILE = "libraries.sqlite"

        const val ALBUM_ART_SIZE = 256.0
        const val CONTROL_SIZE = 32.0
    }

    override val pane = BorderPane()
    private val db = Database(super.getAbsolutePath(SAVE_FILE))

    private var library = Paths.get("")
    private var musicPaths: MutableList<Path> = ArrayList()
    private var songIndex = SimpleIntegerProperty(0)
    private var mediaPlayer = SimpleObjectProperty<MediaPlayer>()

    private var paused = true
    private var shuffle = false
    private var repeat = false

    /**
     * All the images and controls for the GUI
     */
    private val play = super.loadImage("play.png", 0.0, CONTROL_SIZE)
    private val pause = super.loadImage("pause.png", 0.0, CONTROL_SIZE)
    private val next = ImageView(super.loadImage("next.png", 0.0, CONTROL_SIZE))
    private val previous = ImageView(super.loadImage("previous.png", 0.0, CONTROL_SIZE))
    private val shuffleOff = super.loadImage("shuffle-off.png", 0.0, CONTROL_SIZE)
    private val shuffleOn = super.loadImage("shuffle-on.png", 0.0, CONTROL_SIZE)
    private val shuffleImage = ImageView(shuffleOff)
    private val repeatOff = super.loadImage("repeat-off.png", 0.0, CONTROL_SIZE)
    private val repeatOn = super.loadImage("repeat-on.png", 0.0, CONTROL_SIZE)
    private val repeatImage = ImageView(repeatOff)
    private val controlCenter = ImageView(play)

    private val stop = ImageView(super.loadImage("stop.png", 0.0, CONTROL_SIZE))
    private val volumeUp = ImageView(super.loadImage("volume-up.png", 0.0, CONTROL_SIZE))
    private val volumeDown = ImageView(super.loadImage("volume-down.png", 0.0, CONTROL_SIZE))
    private val mute = ImageView(super.loadImage("mute.png", 0.0, CONTROL_SIZE))

    private val defaultAlbumArt = super.loadImage("album-art.png", 0.0, ALBUM_ART_SIZE)

    private val title = SimpleStringProperty()
    private val artist = SimpleStringProperty()
    private val album = SimpleStringProperty()
    private val albumArt = ImageView(defaultAlbumArt)

    /**
     * Creates the GUI for the music player and prepares the songs that will be played.
     */
    init {
        checkIfLibraryInitialized()
        Collections.sort(musicPaths)

        loadSong()

        albumArt.isPreserveRatio = true
        controlCenter.isPreserveRatio = true
        shuffleImage.isPreserveRatio = true
        repeatImage.isPreserveRatio = true

        controlCenter.isPickOnBounds = true
        previous.isPickOnBounds = true
        next.isPickOnBounds = true
        shuffleImage.isPickOnBounds = true
        repeatImage.isPickOnBounds = true

        albumArt.fitWidth = ALBUM_ART_SIZE
        controlCenter.fitWidth = CONTROL_SIZE * 1.4
        shuffleImage.fitWidth = CONTROL_SIZE / 1.4
        repeatImage.fitWidth = CONTROL_SIZE / 1.4

        val titleText = Text()
        titleText.textProperty().bind(title)
        titleText.font = Font.font(24.0)
        titleText.fill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        titleText.textAlignment = TextAlignment.CENTER
        val artistText = Text()
        artistText.textProperty().bind(artist)
        artistText.font = Font(titleText.font.size - 4)
        artistText.fill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        artistText.textAlignment = TextAlignment.CENTER
        val albumText = Text()
        albumText.textProperty().bind(album)
        albumText.font = Font(titleText.font.size - 10)
        albumText.fill = GeneralConstants.GLOBAL_WIDGET_COLOUR
        albumText.textAlignment = TextAlignment.CENTER

        val metadataTitleAlbumPane = VBox()
        metadataTitleAlbumPane.spacing = 2.0
        metadataTitleAlbumPane.alignment = Pos.CENTER
        metadataTitleAlbumPane.children.addAll(titleText, albumText)

        val metadataText = VBox()
        metadataText.spacing = 4.0
        metadataText.alignment = Pos.CENTER
        metadataText.padding = Insets(12.0, 0.0, 0.0, 0.0)
        metadataText.children.addAll(metadataTitleAlbumPane, artistText)

        val controlsCenter = HBox()
        controlsCenter.alignment = Pos.CENTER
        controlsCenter.spacing = 12.0
        controlsCenter.padding = Insets(12.0, 0.0, 0.0, 0.0)
        controlsCenter.children.addAll(previous, controlCenter, next)

        val controls = BorderPane()
        controls.left = shuffleImage
        controls.center = controlsCenter
        controls.right = repeatImage
        BorderPane.setAlignment(shuffleImage, Pos.CENTER)
        BorderPane.setAlignment(repeatImage, Pos.CENTER)

        val all = BorderPane()
        all.top = metadataText
        all.center = controls

        pane.center = albumArt
        pane.bottom = all

        setUpTouchEvents()
    }

    /**
     * Sets up the touch events for the various controls in the GUI
     *
     * Includes logic for the music operations such as shuffling music and repeating a single song.
     */
    private fun setUpTouchEvents() {
        controlCenter.setOnTouchPressed {
            if (paused) {
                paused = false

                mediaPlayer.get().play()

                controlCenter.image = pause
            } else {
                paused = true

                mediaPlayer.get().pause()

                controlCenter.image = play
            }
        }
        previous.setOnTouchPressed {
            if (!repeat) {
                songIndex.set(songIndex.value - 1)

                if (songIndex.get() < 0) {
                    songIndex.set(musicPaths.size - 1)
                }
            }

            loadSong()
            mediaPlayer.get().play()

            controlCenter.image = pause
        }
        next.setOnTouchPressed {
            if (!repeat) {
                songIndex.set(songIndex.value + 1)

                if (songIndex.get() > musicPaths.size - 1) {
                    songIndex.set(0)
                }
            }

            loadSong()
            mediaPlayer.get().play()

            controlCenter.image = pause
        }
        shuffleImage.setOnTouchPressed {
            if (shuffle) {
                Collections.sort(musicPaths)

                shuffleImage.image = shuffleOff

                shuffle = false
            } else {
                val currentSong = musicPaths[songIndex.get()]

                Collections.shuffle(musicPaths)

                val currentSongIndex = musicPaths.indexOf(currentSong)

                Collections.swap(musicPaths, songIndex.get(), currentSongIndex)

                shuffleImage.image = shuffleOn

                shuffle = true
            }
        }
        repeatImage.setOnTouchPressed {
            if (repeat) {
                repeatImage.image = repeatOff

                repeat = false
            } else {
                repeatImage.image = repeatOn

                repeat = true
            }
        }
    }

    /**
     * Loads the current song specified by the index [songIndex] in the list [musicPaths].
     *
     * This includes loading and parsing the metadata that corresponds with the specified image.
     */
    private fun loadSong() {
        if (mediaPlayer.get() != null) {
            mediaPlayer.get().stop()

            title.set("")
            artist.set("")
            album.set("")
            albumArt.image = defaultAlbumArt
        }

        val media = Media(musicPaths[songIndex.get()].toUri().toString())
        media.tracks.forEach { it.metadata.forEach { t, u -> println("$t $u") } }
        media.metadata.addListener(MapChangeListener {
            if (it.wasAdded()) {
                parseMetadata(it)
            }
        })
        val player = MediaPlayer(media)
        player.setOnEndOfMedia {
            if (!repeat) {
                songIndex.set(songIndex.get() + 1)
            }
            loadSong()
            mediaPlayer.get().play()
        }

        mediaPlayer.set(player)
    }

    /**
     * Parses title name, artist name, album name, and album image when the metadata has loaded.
     *
     * @param metadata The metadata information.
     */
    private fun parseMetadata(metadata: MapChangeListener.Change<out String, out Any>) {
        when (metadata.key) {
            "title" -> title.set(metadata.valueAdded as String)
            "artist" -> artist.set(metadata.valueAdded as String)
            "album" -> album.set(metadata.valueAdded as String)
            "image" -> albumArt.image = metadata.valueAdded as Image
        }
    }

    /**
     * Retrieves all supported audio files located in the specified directory. This method also looks in all
     * sub-directories as well.
     *
     * @param dir The directory from which to load songs.
     * @return Returns a list of music file paths located in the folder.
     */
    private fun initializeLibrarySongs(dir: Path): MutableList<Path>? {
        return Files.walk(dir)
                .filter { Files.isRegularFile(it) && MUSIC_EXTENSIONS.any { ext -> it.toString().endsWith(ext) } }
                .collect(Collectors.toList())
    }

    /**
     * Asks user to select default music library path if it does not already exist. The library is stored in the
     * database under the key 'library'.
     *
     * When the library is identified, all supported music files in that path are loaded into [MusicPlayer.musicPaths].
     */
    private fun checkIfLibraryInitialized() {
        try {
            library = Paths.get(db.getValue("library"))
            musicPaths = initializeLibrarySongs(library)!!
        } catch (e: MirrorException.Exceptions.MissingDataException) {
            Dialog("You haven't set up your music library location!", scene, true)

            while (true) {
                var selectedDirectory: File? = null
                val directoryChooser = DirectoryChooser()

                while (selectedDirectory === null) {
                    selectedDirectory = directoryChooser.showDialog(scene.window)
                }

                val dir = Paths.get(selectedDirectory.absolutePath)
                val dirSongs = initializeLibrarySongs(dir)
                if (dirSongs != null && dirSongs.size > 0) {
                    db.addOrUpdateKey(Pair("library", dir.toString()))

                    library = dir
                    musicPaths = dirSongs

                    return
                }

                Dialog("Your library path is invalid!", scene, true)
            }
        }
    }

    /**
     * This class manages the SQLite database connection with a database file that is specific to this [App]
     * implementation.
     */
    class Database(path: String) {
        companion object {
            private const val TABLE_KEYS = "keys"
        }

        /**
         * The JDBC connection for the [MusicPlayer].
         */
        private val connection = DriverManager.getConnection("jdbc:sqlite:" + path)

        /**
         * On initialization, create all tables if they do not already exist.
         */
        init {
            createTableKeys()
        }

        /**
         * Create properties table if it does not already exist. This table accepts a [Pair] key/value object.
         */
        private fun createTableKeys() {
            val statement = connection.prepareStatement(String.format(
                    "CREATE TABLE IF NOT EXISTS %s (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)",
                    TABLE_KEYS))
            statement.executeUpdate()
            statement.close()
        }

        /**
         * Attempt to insert key/value [Pair] but if already exists, update the value instead.
         *
         * @param pair The property key and set value that is to be saved.
         */
        fun addOrUpdateKey(@NotNull pair: Pair<String, String>) {
            var sql = String.format("INSERT INTO %s VALUES ('%s', '%s')",
                    TABLE_KEYS, pair.first, pair.second)
            var statement = connection.prepareStatement(sql)

            try {
                statement.executeUpdate()
            } catch (e: SQLException) {
                sql = String.format("UPDATE %s SET value='%s' WHERE key='%s'",
                        TABLE_KEYS, pair.second, pair.first)
                statement = connection.prepareStatement(sql)
                statement.executeUpdate()
            }

            statement.close()
        }

        /**
         * Returns the value of a certain key if it exists or throw a [MirrorException.Exceptions.MissingDataException]
         * if the key does not exist in the database.
         *
         * @return Returns a [Map] containing all key/value pair of properties pertaining to the specific
         * [Widget.uniqueId].
         * @throws MirrorException.Exceptions.MissingDataException Throws exception of the key does not exist in the
         * database.
         */
        @Throws(MirrorException.Exceptions.MissingDataException::class)
        fun getValue(@NotNull key: String): String {
            val statement = connection.prepareStatement(String.format(
                    "SELECT * FROM %s WHERE key='%s'",
                    TABLE_KEYS, key))
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                val value = resultSet.getString("value")

                resultSet.close()
                statement.close()

                return value
            } else {
                throw MirrorException.Exceptions.MissingDataException("The key '$key' does not exist!", false)
            }
        }
    }
}