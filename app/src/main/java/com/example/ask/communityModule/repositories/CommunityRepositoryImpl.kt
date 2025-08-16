package com.example.ask.communityModule.repositories

import android.util.Log
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommunityRepositoryImpl(private val firestore: FirebaseFirestore) : CommunityRepository {
    private var communityListener: ListenerRegistration? = null
    override fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String,
        result: (UiState<CommunityModels>) -> Unit
    ) {
        // 1️⃣ INITIATE PROCESS

        result.invoke(UiState.Loading)

        // 2️⃣ CREATE COMMUNITY DOCUMENT


        val communityDocRef = firestore.collection(Constant.COMMUNITIES).document()
        val communityId = communityDocRef.id
        model.communityId = communityId

        // 3️⃣ SAVE COMMUNITY IN GLOBAL COLLECTION


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
                // 5️⃣ SAVE UNDER USER'S "MY_COMMUNITIES" SUBCOLLECTION

                firestore.collection(Constant.USERS).document(userId)
                    .collection(Constant.MY_COMMUNITIES).document(communityId).set(userCommunity)
                    .addOnSuccessListener {
                        result.invoke(
                            UiState.Success(model)
                        )
                    }
                    .addOnFailureListener {
                        result.invoke(
                            UiState.Failure(
                                it.localizedMessage
                            )
                        )
                    }


            }
            .addOnFailureListener {
                result.invoke(
                    UiState.Failure(
                        it.localizedMessage
                    )
                )
            }


    }

    override fun getUserCommunity(
        userId: String,
        models: CommunityModels,
        role: String,
        result: (UiState<List<CommunityModels>>) -> Unit
    ) {
        result.invoke(UiState.Loading)

        val userCommunitiesRef = firestore.collection(Constant.USERS).document(userId).collection(
            Constant.MY_COMMUNITIES
        ).orderBy("joinedAt", Query.Direction.DESCENDING)

        communityListener = userCommunitiesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("GetUserCommunities", "Firestore error", error)
                result.invoke(UiState.Failure(error.localizedMessage ?: "unknown error"))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val communities = snapshot.toObjects(CommunityModels::class.java)
                    result.invoke(UiState.Success(communities))
                    Log.d("GetUserCommunities", "Retrieved ${communities.size} communities")

                }catch (e: Exception){
                    Log.e("GetUserCommunities", "Error parsing data", e)
                    result.invoke(UiState.Failure("Parsing error: ${e.localizedMessage}"))
                }
            } else {
                result.invoke(UiState.Success(emptyList()))
            }
        }

    }
    override fun removeCommunityListener() {
        communityListener?.remove()
        communityListener = null
    }
}