class WidgetBoilerplate(enabled: Boolean, coords: Point2D) : Widget(Properties(enabled, coords, UNIQUE_ID)) {
    companion object {
        /**
         * This is the unique identifier for this [Widget] implementation specifically.
         *
         * All [UNIQUE_ID] must be different from other implementations.
         */
        val UNIQUE_ID = "widget-boilerplate"
    }

    /**
     * This is the layout manager that is used when adding a [Widget] to the application.
     */
    override val pane: Region
        get() = Region()

    /**
     * These are the Key/Value pairs that are passed to this function. Each implementation handles the data differently.
     *
     * All implementations are not required to make use of this method.
     */
    override fun parameters(map: Map<String, String>) {}

    /**
     * This is the method that will constantly be updated every 100 milliseconds by default.
     *
     * All updates to the [Widget] implementation will be done here.
     */
    override fun update() {}

    /**
     * Upon user clicking/touching the [Widget], actions can be performed.
     *
     * All implementations are not required to make use of this method.
     */
    override fun onClick() {}

    /**
     * Upon user resizing the [Widget], components on the implementation should change depending on the [Widget].
     *
     * An example of this would be if a [javafx.scene.Node] on the implementation was a [javafx.scene.text.Text]
     * element. When resizing, the font can be changed allowing for the [pane] to also change.
     */
    override fun resize(increase: Boolean) {}

    /**
     * When resizing components, the user can confirm the changes made. This method should be used to save to the
     * [com.sabihismail.SmartMirror.database.Database].
     */
    override fun confirmChanges() {}

    /**
     * When resizing components, the user can cancel the changes made if they've had a change of mind. This method
     * should either read from the [com.sabihismail.SmartMirror.database.Database] and change values according to that,
     * or it should reset the values to some default values.
     */
    override fun cancelChanges() {}
}