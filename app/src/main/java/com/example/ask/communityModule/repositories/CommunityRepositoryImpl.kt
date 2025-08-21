package com.example.ask.communityModule.repositories

import android.util.Log
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommunityRepositoryImpl(
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    private var communityListener: ListenerRegistration? = null

    override fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String,
        result: (UiState<CommunityModels>) -> Unit
    ) {
        result(UiState.Loading)

        val communityDocRef = firestore.collection(Constant.COMMUNITIES).document()
        val communityId = communityDocRef.id
        model.communityId = communityId

        communityDocRef.set(model)
            .addOnSuccessListener {
                val userCommunity = CommunityModels(
                    communityId = communityId,
                    communityName = model.communityName,
                    role = role,
                    userId = userId,
                    communityCode = model.communityCode,
                    joinedAt = System.currentTimeMillis()
                )

                firestore.collection(Constant.USERS).document(userId)
                    .collection(Constant.MY_COMMUNITIES)
                    .document(communityId)
                    .set(userCommunity)
                    .addOnSuccessListener { result(UiState.Success(model)) }
                    .addOnFailureListener { e ->
                        result(UiState.Failure(e.localizedMessage ?: "Failed to save user community"))
                    }
            }
            .addOnFailureListener { e ->
                result(UiState.Failure(e.localizedMessage ?: "Failed to create community"))
            }
    }

    override fun getUserCommunity(
        userId: String,
        result: (UiState<List<CommunityModels>>) -> Unit
    ) {
        result(UiState.Loading)

        val ref = firestore.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.MY_COMMUNITIES)
            .orderBy("joinedAt", Query.Direction.DESCENDING)

        communityListener?.remove()
        communityListener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GetUserCommunities", "Firestore error", error)
                result(UiState.Failure(error.localizedMessage ?: "Unknown error"))
                return@addSnapshotListener
            }

            val communities = snapshot?.documents?.mapNotNull { it.toObject(CommunityModels::class.java) }
                ?: emptyList()
            result(UiState.Success(communities))
        }
    }

    override fun joinCommunity(
        userId: String,
        communityCode: String,
        result: (UiState<CommunityModels>) -> Unit
    ) {
        result(UiState.Loading)

        firestore.collection(Constant.COMMUNITIES)
            .whereEqualTo("communityCode", communityCode)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    val community = query.documents[0].toObject(CommunityModels::class.java)
                    if (community != null) {
                        firestore.collection(Constant.USERS).document(userId)
                            .collection(Constant.MY_COMMUNITIES)
                            .document(community.communityId ?: "")
                            .set(community.copy(role = "member", userId = userId, joinedAt = System.currentTimeMillis()))
                            .addOnSuccessListener { result(UiState.Success(community)) }
                            .addOnFailureListener { e ->
                                result(UiState.Failure(e.localizedMessage ?: "Failed to join community"))
                            }
                    } else {
                        result(UiState.Failure("Community not found"))
                    }
                } else {
                    result(UiState.Failure("Invalid community code"))
                }
            }
            .addOnFailureListener { e ->
                result(UiState.Failure(e.localizedMessage ?: "Error while joining"))
            }
    }

    override fun removeCommunityListener() {
        communityListener?.remove()
        communityListener = null
    }
}
