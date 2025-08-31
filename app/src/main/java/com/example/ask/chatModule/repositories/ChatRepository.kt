package com.example.ask.chatModule.repositories

import com.example.ask.utilities.UiState
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.api.models.QueryChannelRequest
import io.getstream.chat.android.models.User
import io.getstream.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatClient: ChatClient
) {

    fun connectUser(
        userId: String,
        userName: String,
        userEmail: String?,
        userImageUrl: String?,
        token: String,
        result: (UiState<String>) -> Unit
    ) {
        val user = User(
            id = userId,
            name = userName,
            image = userImageUrl ?: "",
            extraData = mutableMapOf(
                "email" to (userEmail ?: "")
            )
        )

        chatClient.connectUser(user, token).enqueue { connectionResult ->
            when (connectionResult) {
                is Result.Success -> {
                    result(UiState.Success("User connected successfully"))
                }
                is Result.Failure -> {
                    result(UiState.Failure(connectionResult.value.message ?: "Connection failed"))
                }
            }
        }
    }

    fun disconnectUser() {
        chatClient.disconnect(flushPersistence = false).enqueue()
    }

    fun createOrGetChannel(
        queryId: String,
        queryTitle: String,
        queryOwnerIds: List<String>,
        helperIds: List<String>,
        result: (UiState<String>) -> Unit
    ) {
        val channelId = "query_$queryId"
        val memberIds = (queryOwnerIds + helperIds).distinct()

        val extraData = mutableMapOf<String, Any>(
            "name" to "Query: $queryTitle",
            "query_id" to queryId,
            "query_title" to queryTitle
        )

        // Simple channel creation/query without the problematic request parameter
        chatClient.queryChannel(
            channelType = "messaging",
            channelId = channelId,
            request = QueryChannelRequest().apply {
                data = extraData
            }
        ).enqueue { channelResult ->
            when (channelResult) {
                is Result.Success -> {
                    val channel = channelResult.value
                    // Add members to the channel if they're not already in it
                    channel.addMembers(memberIds).enqueue { addMembersResult ->
                        when (addMembersResult) {
                            is Result.Success -> {
                                result(UiState.Success(channelId))
                            }
                            is Result.Failure -> {
                                // Channel was created but adding members failed
                                result(UiState.Success(channelId)) // Still return success
                            }
                        }
                    }
                }
                is Result.Failure -> {
                    result(UiState.Failure(channelResult.value.message ?: "Failed to create/get channel"))
                }
            }
        }
    }
}