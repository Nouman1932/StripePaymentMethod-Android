package com.example.strippayment

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.stripe.android.EphemeralKeyProvider
import com.stripe.android.EphemeralKeyUpdateListener

// Define a custom exception class that extends Exception and includes a code property
class FirebaseFunctionException(val code: Int, message: String) : Exception(message)

class ExampleEphemeralKeyProvider : EphemeralKeyProvider {
    private val firebaseFunctions = FirebaseFunctions.getInstance()
    override fun createEphemeralKey(
        apiVersion: String,
        keyUpdateListener: EphemeralKeyUpdateListener
    ) {
        val data = hashMapOf(
            "api_version" to apiVersion
        )

        firebaseFunctions
            .getHttpsCallable("createEphemeralKey")
            .call(data)
            .continueWith { task ->
                val keyResult = if (task.isSuccessful) {
                    task.result?.data.toString()
                } else {
                    val e = task.exception
                    if (e is FirebaseFunctionException) { // Check if it's your custom exception
                        val code = e.code
                        val message = e.message
                        Log.e("ephemeral_key", "Error: $code - $message")
                    } else {
                        Log.e("ephemeral_key", "Unknown error occurred")
                    }
                    null // Return null in the error case
                }

                keyResult?.let { keyUpdateListener.onKeyUpdate(it) }
            }
    }
}
