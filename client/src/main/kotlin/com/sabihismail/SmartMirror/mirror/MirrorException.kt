package com.sabihismail.SmartMirror.mirror

import kotlin.system.exitProcess

/**
 * Contains all possible custom [Exception] implementations for this application.
 *
 * If [exit] is true, the program will then exit immediately after throwing the exception.
 *
 * @date: August 14, 2017
 * @author: Sabih Ismail
 * @since 1.0
 */
open class MirrorException(private val reason: String, private val exceptionType: Int, exit: Boolean, e: Exception?) : Exception(e) {
    companion object Exceptions {
        class StartupException(reason: String, e: Exception?) : MirrorException(reason, 1, true, e) {
            constructor(reason: String) : this(reason, null)
        }

        class IllegalCodeExecutionException(reason: String, e: Exception?) : MirrorException(reason, 2, true, e) {
            constructor(reason: String) : this(reason, null)
        }

        class MissingDataException(reason: String, exit: Boolean, e: Exception?) : MirrorException(reason, 3, exit, e) {
            constructor(reason: String, exit: Boolean) : this(reason, exit, null)
        }

        class InvalidDataException(reason: String) : MirrorException(reason, 4, true, null)
        class SuggestedImprovementException(reason: String) : MirrorException(reason, 5, false, null)
    }

    override val message: String?
        get() = "Exception $exceptionType: $reason"

    init {
        if (exit) {
            super.printStackTrace()

            exitProcess(exceptionType)
        }
    }
}