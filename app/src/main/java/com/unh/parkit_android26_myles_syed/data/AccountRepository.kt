package com.unh.parkit_android26_myles_syed.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

data class Vehicle(
    val make: String = "",
    val model: String = "",
    val plate: String = ""
) {
    val isEmpty: Boolean get() = make.isBlank() && model.isBlank() && plate.isBlank()
}

/**
 * Firebase Auth holds the login (email + password). Firestore holds one document per
 * account at users/{uid}:  { email, vehicle: { make, model, plate } }
 * Every callback receives null on success or a user-readable error message.
 */
object AccountRepository {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val users get() = FirebaseFirestore.getInstance().collection("users")

    val isSignedIn: Boolean get() = auth.currentUser?.isAnonymous == false
    val email: String get() = auth.currentUser?.email.orEmpty()

    fun signIn(email: String, password: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) null else friendlyError(task.exception, signUp = false))
        }
    }

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                onResult(friendlyError(task.exception, signUp = true))
                return@addOnCompleteListener
            }
            // Create the account's profile document; saveVehicle() will also create it if this misses.
            auth.currentUser?.uid?.let { uid ->
                users.document(uid).set(mapOf("email" to email), SetOptions.merge())
            }
            onResult(null)
        }
    }

    fun signOut() = auth.signOut()

    /** Streams the signed-in account's vehicle. Returns a function that stops listening. */
    fun listenVehicle(onChange: (Vehicle) -> Unit): () -> Unit {
        val uid = auth.currentUser?.uid ?: return {}
        val registration = users.document(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val map = snapshot.get("vehicle") as? Map<*, *>
            onChange(
                Vehicle(
                    make = map?.get("make") as? String ?: "",
                    model = map?.get("model") as? String ?: "",
                    plate = map?.get("plate") as? String ?: ""
                )
            )
        }
        return { registration.remove() }
    }

    /** Saves the vehicle to the signed-in account, replacing whatever was there before. */
    fun saveVehicle(vehicle: Vehicle, onResult: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult("You're signed out. Please log in again.")
            return
        }
        val data = mapOf(
            "email" to user.email,
            "vehicle" to mapOf(
                "make" to vehicle.make,
                "model" to vehicle.model,
                "plate" to vehicle.plate
            )
        )
        users.document(user.uid).set(data, SetOptions.merge()).addOnCompleteListener { task ->
            onResult(if (task.isSuccessful) null else "Couldn't save. Check your connection and try again.")
        }
    }

    private fun friendlyError(e: Exception?, signUp: Boolean): String = when (e) {
        is FirebaseAuthUserCollisionException -> "An account with that email already exists."
        is FirebaseAuthWeakPasswordException -> "Password needs at least 6 characters."
        is FirebaseAuthInvalidCredentialsException ->
            if (signUp) "That email address doesn't look valid." else "Wrong email or password."
        is FirebaseAuthInvalidUserException -> "Wrong email or password."
        is FirebaseNetworkException -> "No internet connection."
        else -> "Something went wrong: ${e?.message ?: "unknown error"}"
    }
}
