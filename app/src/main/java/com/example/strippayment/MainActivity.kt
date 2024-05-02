package com.example.strippayment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.strippayment.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.CustomerSession
import com.stripe.android.PaymentConfiguration
import com.stripe.android.PaymentSession
import com.stripe.android.PaymentSessionConfig
import com.stripe.android.PaymentSessionData
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.view.BillingAddressFields

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var usersRef: DatabaseReference
    private lateinit var stripe: Stripe
    private lateinit var paymentSession: PaymentSession
    private lateinit var selectPayment: TextView
    private lateinit var db: FirebaseFirestore // Specify the type explicitly
    private var selectedPaymentMethod: PaymentMethod? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPaymentSession()
        stripe = Stripe(applicationContext, PaymentConfiguration.getInstance(applicationContext).publishableKey)

        selectPayment = findViewById(R.id.selectPayment)
        db = FirebaseFirestore.getInstance()

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        usersRef = FirebaseDatabase.getInstance().reference.child("users")

        // Initialize Stripe

        showUserName()

        selectPayment.setOnClickListener {
            paymentSession.presentPaymentMethodSelection()
        }

        binding.paybtn.setOnClickListener {
            if (selectedPaymentMethod != null) {
                confirmPayment(selectedPaymentMethod!!.id!!)
            } else {
                Toast.makeText(applicationContext, "Please select a payment method", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUserName() {
        val currentUser = auth.currentUser
        val uid = currentUser?.uid
        if (currentUser != null && uid != null) {
            // Fetch user's name from Firebase Realtime Database
            usersRef.child(uid).child("name").get().addOnSuccessListener { dataSnapshot ->
                val name = dataSnapshot.value as String?
                name?.let {
                    // Update TextView with user's name
                    binding.name.text = "Welcome, $name!"
                }
            }.addOnFailureListener { exception ->
                // Handle failure to fetch user's name
                binding.name.text = "Welcome!"
            }
        } else {
            // Handle if user is not signed in
            binding.name.text = "Welcome!"
        }
    }
    private fun setupPaymentSession() {
        PaymentConfiguration.init(applicationContext, "your_key")
        CustomerSession.initCustomerSession(this, ExampleEphemeralKeyProvider())

        paymentSession = PaymentSession(this, PaymentSessionConfig.Builder()
            .setShippingInfoRequired(false)
            .setShippingMethodsRequired(false)
            .setBillingAddressFields(BillingAddressFields.None)
            .setShouldShowGooglePay(true)
            .build())

        paymentSession.init(
            object : PaymentSession.PaymentSessionListener {
                override fun onCommunicatingStateChanged(isCommunicating: Boolean) {
                    Log.e("check", "Communication Error")
                }
                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.e("PaymentSession", "Error $errorCode: $errorMessage")
                    // Handle errors if needed
                    Toast.makeText(applicationContext, "Payment Session Error: $errorMessage", Toast.LENGTH_SHORT).show()
                }
                override fun onPaymentSessionDataChanged(data: PaymentSessionData) {
                    val paymentMethod: PaymentMethod? = data.paymentMethod
                    if (paymentMethod != null) {
                        selectPayment.text = "${paymentMethod.card?.brand} card ends with ${paymentMethod.card?.last4}"
                    }
                            selectedPaymentMethod = paymentMethod
                            Log.d("PaymentSession", "Payment method ID updated: $selectedPaymentMethod")
                    }
                }
        )

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null) {
            paymentSession.handlePaymentData(requestCode, resultCode, data)
        }
    }
    private fun confirmPayment(paymentMethodId: String) {
        if (paymentMethodId.isEmpty()) {
            // Show an error message to the user indicating that a payment method must be selected
            Toast.makeText(applicationContext, "Please select a payment method", Toast.LENGTH_SHORT).show()
            return // Exit the function without proceeding with payment
        }
        val currentUser = auth.currentUser
        currentUser?.uid?.let { uid ->
            val paymentCollection = db.collection("stripe_customers").document(uid).collection("payments")
            paymentCollection.add(hashMapOf("amount" to 95, "currency" to "USD"))
                .addOnSuccessListener { documentReference ->
                    documentReference.addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.w("Payment", "Listener failed", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null && snapshot.exists()) {
                            val clientSecret = snapshot.getString("client_secret")
                            Log.d("Payment", "Client Secret: $clientSecret")
                            clientSecret?.let {
                                stripe.confirmPayment(this,
                                    ConfirmPaymentIntentParams.createWithPaymentMethodId(paymentMethodId, it))
                                Toast.makeText(applicationContext, "Payment Done", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Log.e("Payment", "Current payment intent: null")
                            // Handle if payment intent is null
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Payment", "Error adding document", e)
                    // Handle failure to add document
                }
        }
    }
}


