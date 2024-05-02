package com.example.strippayment

// Import necessary packages
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class Signup : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signupButton: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        // Initialize views
        nameEditText = findViewById(R.id.name)
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        signupButton = findViewById(R.id.buttonLogin)


        signupButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            // Validate input fields here if needed

            // Create user in Firebase Authentication
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // User created successfully, now add user info to Realtime Database
                        val firebaseUser = FirebaseAuth.getInstance().currentUser
                        val uid = firebaseUser?.uid ?: ""

                        val database = FirebaseDatabase.getInstance()
                        val usersRef = database.getReference("users")
                        val userMap = HashMap<String, Any>()
                        userMap["name"] = name
                        // Add other user information as needed

                        // Save user info under UID in Realtime Database
                        usersRef.child(uid).setValue(userMap)

                        // Navigate to next activity or perform other actions
                        Toast.makeText(this, "Signup successful!", Toast.LENGTH_SHORT).show()
                        // Example: navigate to HomeActivity
                         val intent = Intent(this, MainActivity::class.java)
                         startActivity(intent)
                    } else {
                        // Handle signup failure
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}
