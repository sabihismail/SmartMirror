package com.sabihismail.SmartMirror.tools

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * This class contains static tools used by multiple classes such as reading from a URL.
 *
 * @date: July 24, 2017
 * @author: Sabih Ismail
 * @since SmartMirror 1.0
 */
object Tools {
    /**
     * Reads any given [url] and returns the text on that webpage.
     */
    @Throws(IOException::class)
    @JvmStatic
    fun readURL(url: String): String {
        val httpClient = HttpClients.createMinimal()
        val httpResponse = httpClient.execute(HttpGet(url))

        val response = EntityUtils.toString(httpResponse.entity, StandardCharsets.UTF_8)
        EntityUtils.consume(httpResponse.entity)

        return response
    }
}