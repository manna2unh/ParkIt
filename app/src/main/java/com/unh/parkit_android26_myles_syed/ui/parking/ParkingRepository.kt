package com.unh.parkit_android26_myles_syed.ui.parking

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FirebaseFirestore
import com.unh.parkit_android26_myles_syed.ui.screens.Lot
import com.unh.parkit_android26_myles_syed.ui.screens.lots

data class ParkingSession(val lot: Lot, val spot: Int, val startMillis: Long)

object ParkingRepository {
    private const val TAG = "ParkingRepository"
    private const val SPOTS_PER_ROW = 8

    private val db get() = FirebaseFirestore.getInstance()
    private val auth get() = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    // lot name -> taken spot numbers. Filled live from Firestore.
    private val takenSpots = mutableStateMapOf<String, Set<Int>>().also { map ->
        lots.forEach { map[it.name] = emptySet() }
    }

    var session by mutableStateOf<ParkingSession?>(null)
        private set

    private fun docRef(lot: Lot, spot: Int) =
        db.collection("parking").document("${tokenFor(lot)}_$spot")

    /**
     * Starts the live Firestore sync for the signed-in account. Safe to call repeatedly:
     * MainActivity calls it on launch (returning users) and the login flow calls it after
     * sign-in / sign-up. Does nothing until a real (non-anonymous) account is signed in.
     */
    //had gemini fix our issue
    fun init() {
        val user = auth.currentUser
        if (user == null || user.isAnonymous) return
        listen()
    }

    /** Call on sign-out: stops listening and clears this account's local parking state. */
    fun reset() {
        listener?.remove()
        listener = null
        session = null
        lots.forEach { takenSpots[it.name] = emptySet() }
    }

    private fun listen() {
        val uid = auth.currentUser?.uid ?: return
        listener?.remove()
        listener = db.collection("parking").addSnapshotListener { snap, err ->
            if (err != null) {
                Log.e(TAG, "Snapshot listener error", err)
                return@addSnapshotListener
            }
            snap ?: return@addSnapshotListener

            // Someone else's session ended (their spot doc was deleted): tell the user if the
            // lot is near them. Skip our own deletes (pending local writes / our uid).
            if (!snap.metadata.hasPendingWrites()) {
                snap.documentChanges
                    .filter { it.type == DocumentChange.Type.REMOVED }
                    .forEach { change ->
                        val d = change.document
                        if (d.get("takenBy") as? String == uid) return@forEach
                        val lot = (d.get("lot") as? String)?.let { lotForToken(it) } ?: return@forEach
                        val spot = (d.get("spot") as? Number)?.toInt() ?: return@forEach
                        ParkingNotifier.onSpotFreed(lot, spot)
                    }
            }

            // Safe casts: malformed documents are skipped instead of crashing the listener
            val byLot = snap.documents.mapNotNull { d ->
                val lot = d.get("lot") as? String ?: return@mapNotNull null
                val spot = (d.get("spot") as? Number)?.toInt() ?: return@mapNotNull null
                lot to spot
            }.groupBy({ it.first }, { it.second })

            lots.forEach { lot ->
                takenSpots[lot.name] = byLot[tokenFor(lot)].orEmpty().toSet()
            }

            // Restore this user's session after an app restart
            if (session == null) {
                val mine = snap.documents.firstOrNull { it.get("takenBy") as? String == uid }
                val lot = (mine?.get("lot") as? String)?.let { lotForToken(it) }
                val spot = (mine?.get("spot") as? Number)?.toInt()
                if (lot != null && spot != null) {
                    val started = mine.getTimestamp("startedAt")?.toDate()?.time
                        ?: System.currentTimeMillis()
                    session = ParkingSession(lot, spot, started)
                }
            }
        }
    }

    /** Token encoded in each lot's barcode, e.g. "UNH-01". */
    fun tokenFor(lot: Lot): String = "UNH-%02d".format(lots.indexOf(lot) + 1)

    fun lotForToken(token: String): Lot? {
        val t = token.trim().uppercase()
        return lots.firstOrNull { tokenFor(it) == t }
    }

    fun isTaken(lot: Lot, spot: Int) = spot in (takenSpots[lot.name] ?: emptySet())

    fun openCount(lot: Lot) = lot.spots - (takenSpots[lot.name]?.size ?: 0)

    /** Row letters run A..Z, then AA, AB, ... so big lots never run out of letters. */
    fun rowLabel(spot: Int): String {
        var n = (spot - 1) / SPOTS_PER_ROW
        val sb = StringBuilder()
        do {
            sb.insert(0, 'A' + n % 26)
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    fun spotLabel(spot: Int) = "${rowLabel(spot)}-$spot"

    fun startSession(lot: Lot, spot: Int): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        if (session != null || isTaken(lot, spot)) return false

        // Optimistic local update so the UI responds instantly
        takenSpots[lot.name] = (takenSpots[lot.name] ?: emptySet()) + spot
        session = ParkingSession(lot, spot, System.currentTimeMillis())

        val ref = docRef(lot, spot)
        db.runTransaction { tx ->
            if (tx.get(ref).exists()) {
                throw FirebaseFirestoreException("Spot taken", FirebaseFirestoreException.Code.ABORTED)
            }
            tx.set(
                ref, mapOf(
                    "lot" to tokenFor(lot),
                    "spot" to spot,
                    "takenBy" to uid,
                    "startedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.addOnFailureListener {
            Log.e(TAG, "startSession failed", it)
            // Someone else got it first (or no network): roll back
            session = null
            takenSpots[lot.name] = (takenSpots[lot.name] ?: emptySet()) - spot
        }
        return true
    }

    fun endSession() {
        val s = session ?: return
        takenSpots[s.lot.name] = (takenSpots[s.lot.name] ?: emptySet()) - s.spot
        session = null
        docRef(s.lot, s.spot).delete()
            .addOnFailureListener { Log.e(TAG, "endSession delete failed", it) }
    }
}