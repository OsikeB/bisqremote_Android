package com.joachimneumann.bisq.model

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.joachimneumann.bisq.database.BisqNotification
import com.joachimneumann.bisq.services.BISQ_MESSAGE_ANDROID_MAGIC
import com.joachimneumann.bisq.util.CryptoUtil
import com.joachimneumann.bisq.util.DateUtil
import java.util.*

class NotificationMessage(private var notification: String?) {

    private lateinit var magicValue: String
    private lateinit var initializationVector: String
    private lateinit var encryptedPayload: String
    private lateinit var decryptedPayload: String

    lateinit var bisqNotification: BisqNotification

    companion object {
        private const val TAG = "NotificationMessage"
    }

    init {
        parseNotification()
        decryptNotificationMessage()
        deserializeNotificationMessage()
    }

    private fun parseNotification() {
        try {
            val array = notification?.split("\\|".toRegex())?.dropLastWhile { it.isEmpty() }
                ?.toTypedArray()
            if (array == null || array.size != 3) {
                throw IllegalArgumentException("Unexpected format")
            }
            magicValue = array[0]
            initializationVector = array[1]
            encryptedPayload = array[2]
            if (magicValue != BISQ_MESSAGE_ANDROID_MAGIC) {
                throw IllegalArgumentException("Invalid magic value: $magicValue")
            }
            if (initializationVector.length != 16) {
                throw IllegalArgumentException(
                    "Invalid initialization vector: $initializationVector"
                )
            }
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Unable to parse notification; ${e.message}")
            throw IllegalArgumentException("Unable to parse notification; ${e.message}")
        }
    }

    private fun decryptNotificationMessage() {
        try {
            decryptedPayload = CryptoUtil(Device.instance.key!!).decrypt(
                encryptedPayload, initializationVector
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unable to decrypt notification payload: $encryptedPayload")
            throw Exception("Unable to decrypt notification payload")
        }
    }

    private fun deserializeNotificationMessage() {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(Date::class.java, DateUtil())
        val gson = gsonBuilder.create()
        try {
            bisqNotification = gson.fromJson(decryptedPayload, BisqNotification::class.java)
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Unable to deserialize notification message: $decryptedPayload")
            throw Exception("Unable to deserialize notification message")
        }
    }

}
