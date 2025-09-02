package com.example.ask.chatModule.viewModels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.ask.chatModule.models.ChatRoomModel
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "ChatListViewModel"
        private const val CHAT_ROOMS = "chatRooms"
    }

    private val _chatRooms = MutableLiveData<UiState<List<ChatRoomModel>>>()
    val chatRooms: LiveData<UiState<List<ChatRoomModel>>> = _chatRooms

    fun getUserChatRooms(userId: String) {
        Log.d(TAG, "getUserChatRooms called for userId: $userId")
        _chatRooms.value = UiState.Loading

        // First, let's check if the collection exists and debug
        firestore.collection(CHAT_ROOMS)
            .get()
            .addOnSuccessListener { allChats ->
                Log.d(TAG, "Total chat rooms in database: ${allChats.size()}")

                // Log all chat rooms for debugging
                allChats.documents.forEach { doc ->
                    val participants = doc.get("participants") as? List<*>
                    val queryTitle = doc.getString("queryTitle")
                    val isActive = doc.getBoolean("isActive")
                    Log.d(TAG, "Chat room: ${doc.id}, participants: $participants, query: $queryTitle, active: $isActive")
                }

                // Now get user's specific chat rooms (removed isActive filter temporarily)
                firestore.collection(CHAT_ROOMS)
                    .whereArrayContains("participants", userId)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        Log.d(TAG, "Found ${querySnapshot.size()} chat rooms for user: $userId")

                        val chatRooms = querySnapshot.documents.mapNotNull { doc ->
                            try {
                                val chatRoom = doc.toObject(ChatRoomModel::class.java)
                                Log.d(TAG, "Parsed chat room: ${chatRoom?.chatRoomId}, query: ${chatRoom?.queryTitle}, active: ${chatRoom?.isActive}")

                                // Include chat room if isActive is true OR null (for backwards compatibility)
                                if (chatRoom?.isActive != false) {
                                    chatRoom
                                } else {
                                    Log.d(TAG, "Excluding inactive chat room: ${chatRoom.chatRoomId}")
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing chat room: ${doc.id}", e)
                                null
                            }
                        }

                        Log.d(TAG, "Successfully parsed ${chatRooms.size} chat rooms")
                        _chatRooms.value = UiState.Success(chatRooms)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to get user's chat rooms", exception)
                        _chatRooms.value = UiState.Failure(
                            exception.message ?: "Failed to load chat rooms"
                        )
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to access chat rooms collection", exception)
                _chatRooms.value = UiState.Failure(
                    exception.message ?: "Failed to access chat rooms"
                )
            }
    }

    fun addRealtimeChatRoomsListener(userId: String, callback: (List<ChatRoomModel>) -> Unit) {
        firestore.collection(CHAT_ROOMS)
            .whereArrayContains("participants", userId)
            .whereEqualTo("isActive", true)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, exception ->
                if (exception != null) {
                    Log.e(TAG, "Chat rooms listener error", exception)
                    return@addSnapshotListener
                }

                if (querySnapshot != null) {
                    val chatRooms = querySnapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatRoomModel::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing chat room: ${doc.id}", e)
                            null
                        }
                    }

                    Log.d(TAG, "Real-time chat rooms update: ${chatRooms.size}")
                    callback(chatRooms)
                }
            }
    }
}