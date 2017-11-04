# SmartMirror
The SmartMirror is a modular standalone application that supports
the addition of many components. The target hardware for this is
the Raspberry PI 3.

A SmartMirror is used to display an overlay above a regular mirror,
allowing for interaction with the program through a touchscreen
display.

## Getting Started

1. **Ensure that you have Java 8 installed.**

    Otherwise, type these commands in order:

    **Linux:**<br>
    To identify if java is installed, open up a Terminal instance and
    type `javac -version`. If a value similar to `javac 1.8.0_1XX` is
    returned, Java is installed.

    If a value is not returned, follow these steps:

        i. sudo apt-get install openjdk-8-jdk
        ii. apt-cache search jdk
        iii. export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
        iv. export PATH=$PATH:$JAVA_HOME/bin
        v. javac -version

    You should now see something along the lines of `javac 1.8.0_1XX`
    and you can move onto the next step.

    **Windows:**<br>
    To identify if java is installed, open up a command prompt instance
    and type `java -version`. If a value is returned (something like
    `java version: "1.8.0_1XX"`), Java is installed.

    If a value is not returned, download JDK 8 from
    [Oracle's site](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
    You will then be able to skip the next step as well.
2. **Ensure that you have JavaFX installed.**

    **Linux:**<br>
    Type `sudo apt-get install openjfx` and if you see
    `openjfx is already the newest version (8uXX...)`, JavaFX is
    installed.

    **The program will also throw an error and exit immediately if
    JavaFX is not installed on the machine.**
3. **Download the compiled jar located in the 
[releases section](https://github.com/sabihismail/SmartMirror/releases/latest)
and run it.**

## Custom Widgets and Apps

For an in-depth explanation, please view the `boilerplate` folder for
an example `Widget` and example `App` implementation. You may also
view active implementations such as the `Music Player` `App` and the
`Clock` `Widget`.

It is also encouraged to view the source code for the `Widget` and
`App` classes.

## TODO (in no particular order):
* App search functionality
* Implement a Kotlin file compiler to allow for files to be stored in
their designated folder instead of in a package
* Voice control functionality
* Widgets:
    1. Weather
    2. News
* Apps:
    1. Web Browser
    2. YouTube player

## Built With
* [Kotlin](https://kotlinlang.org/) - A statistically typed language which runs alongside Java in the JVM
* [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) - Database connection driver
* [Org.JSON](https://github.com/stleary/JSON-java) - JSON Parser and Creator
* [Apache HTTP Components](https://hc.apache.org/) - HTTP Client Connections
* [Org.Reflections](https://github.com/ronmamo/reflections) - Java Metadata Reflections
