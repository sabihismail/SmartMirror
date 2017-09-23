class AppBoilerplate (scene: Scene) : App (Properties(UNIQUE_ID, DISPLAY_NAME), scene) {
    companion object {
        /**
         * This is the unique identifier for this [App] implementation specifically.
         *
         * All [UNIQUE_ID] must be different from other implementations.
         */
        const val UNIQUE_ID = "app-boilerplate"

        /**
         * This value is the value that will be visible in [AppThumbnailView] where the user can select an [App] to run.
         */
        const val DISPLAY_NAME = "Application Boilerplate"
    }

    /**
     * This is the layout manager that is used when adding a [App] to the application.
     */
    override val pane = BorderPane()
}