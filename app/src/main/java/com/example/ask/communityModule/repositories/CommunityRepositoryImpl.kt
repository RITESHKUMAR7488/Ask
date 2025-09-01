package com.example.ask.communityModule.repositories

import android.util.Log
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import javax.inject.Inject

class CommunityRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CommunityRepository {

    private var communityListener: ListenerRegistration? = null

    override fun addCommunity(
        userId: String,
        model: CommunityModels,
        role: String,
        result: (UiState<CommunityModels>) -> Unit
    ) {
        // ✅ FIXED: Following OnBoardingRepositoryImpl pattern - set loading first
        result(UiState.Loading)

        val communityDocRef = firestore.collection(Constant.COMMUNITIES).document()
        val communityId = communityDocRef.id
        model.communityId = communityId

        // ✅ FIXED: Following OnBoardingRepositoryImpl pattern with nested operations
        communityDocRef.set(model)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
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
                        .addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful) {
                                result.invoke(UiState.Success(model))
                            } else {
                                result.invoke(UiState.Failure(userTask.exception?.localizedMessage ?: "Failed to save user community"))
                            }
                        }
                        .addOnFailureListener { exception ->
                            result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to save user community"))
                        }
                } else {
                    result.invoke(UiState.Failure(task.exception?.localizedMessage ?: "Failed to create community"))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to create community"))
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

            val communities = snapshot?.documents?.mapNotNull {
                try {
                    it.toObject(CommunityModels::class.java)
                } catch (e: Exception) {
                    Log.e("GetUserCommunities", "Error parsing document", e)
                    null
                }
            } ?: emptyList()
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
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val query = task.result
                    if (!query.isEmpty) {
                        val community = query.documents[0].toObject(CommunityModels::class.java)
                        if (community != null) {
                            // ✅ FIXED: Check if user already joined (following your pattern)
                            firestore.collection(Constant.USERS).document(userId)
                                .collection(Constant.MY_COMMUNITIES)
                                .document(community.communityId ?: "")
                                .get()
                                .addOnCompleteListener { checkTask ->
                                    if (checkTask.isSuccessful) {
                                        if (checkTask.result.exists()) {
                                            result.invoke(UiState.Failure("You are already a member of this community"))
                                        } else {
                                            // Join the community
                                            val userCommunityModel = community.copy(
                                                role = "member",
                                                userId = userId,
                                                joinedAt = System.currentTimeMillis()
                                            )

                                            firestore.collection(Constant.USERS).document(userId)
                                                .collection(Constant.MY_COMMUNITIES)
                                                .document(community.communityId ?: "")
                                                .set(userCommunityModel)
                                                .addOnCompleteListener { joinTask ->
                                                    if (joinTask.isSuccessful) {
                                                        result.invoke(UiState.Success(userCommunityModel))
                                                    } else {
                                                        result.invoke(UiState.Failure(joinTask.exception?.localizedMessage ?: "Failed to join community"))
                                                    }
                                                }
                                                .addOnFailureListener { exception ->
                                                    result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to join community"))
                                                }
                                        }
                                    } else {
                                        result.invoke(UiState.Failure(checkTask.exception?.localizedMessage ?: "Error checking membership"))
                                    }
                                }
                        } else {
                            result.invoke(UiState.Failure("Community not found"))
                        }
                    } else {
                        result.invoke(UiState.Failure("Invalid community code"))
                    }
                } else {
                    result.invoke(UiState.Failure(task.exception?.localizedMessage ?: "Error while joining"))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Error while joining"))
            }
    }

    override fun leaveCommunity(
        userId: String,
        communityId: String,
        result: (UiState<String>) -> Unit
    ) {
        result(UiState.Loading)

        // Remove user from their communities collection
        firestore.collection(Constant.USERS).document(userId)
            .collection(Constant.MY_COMMUNITIES)
            .document(communityId)
            .delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    result.invoke(UiState.Success("Left community successfully"))
                } else {
                    result.invoke(UiState.Failure(task.exception?.localizedMessage ?: "Failed to leave community"))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to leave community"))
            }
    }

    override fun deleteCommunity(
        userId: String,
        communityId: String,
        result: (UiState<String>) -> Unit
    ) {
        result(UiState.Loading)

        // First, verify that the user is an admin of this community
        firestore.collection(Constant.USERS).document(userId)
            .collection(Constant.MY_COMMUNITIES)
            .document(communityId)
            .get()
            .addOnCompleteListener { checkTask ->
                if (checkTask.isSuccessful && checkTask.result.exists()) {
                    val userCommunity = checkTask.result.toObject(CommunityModels::class.java)
                    if (userCommunity?.role?.lowercase() == "admin") {

                        // First delete the community from the main communities collection
                        firestore.collection(Constant.COMMUNITIES).document(communityId)
                            .delete()
                            .addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    // Then delete from user's communities
                                    firestore.collection(Constant.USERS).document(userId)
                                        .collection(Constant.MY_COMMUNITIES)
                                        .document(communityId)
                                        .delete()
                                        .addOnCompleteListener { userDeleteTask ->
                                            if (userDeleteTask.isSuccessful) {
                                                result.invoke(UiState.Success("Community deleted successfully"))
                                            } else {
                                                result.invoke(UiState.Failure("Community deleted but failed to update user data"))
                                            }
                                        }
                                } else {
                                    result.invoke(UiState.Failure(deleteTask.exception?.localizedMessage ?: "Failed to delete community"))
                                }
                            }
                            .addOnFailureListener { exception ->
                                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to delete community"))
                            }
                    } else {
                        result.invoke(UiState.Failure("Only admins can delete communities"))
                    }
                } else {
                    result.invoke(UiState.Failure("Community membership not found"))
                }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Error verifying permissions"))
            }
    }

    override fun removeCommunityListener() {
        communityListener?.remove()
        communityListener = null
    }
}