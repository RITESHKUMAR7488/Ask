package com.example.ask.addModule.repositories

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.ask.addModule.interfaces.ImageUploadApi
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.addModule.models.QueryModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import javax.inject.Inject

class RepositoryQueryImpl @Inject constructor(
    private val database: FirebaseFirestore,
    private val imageUploadApi: ImageUploadApi
) : RepositoryQuery {

    companion object {
        private const val TAG = "RepositoryQueryImpl"
    }

    override fun uploadImage(
        imageFile: File,
        apiKey: String,
        data: MutableLiveData<ImageUploadResponse>,
        error: MutableLiveData<Throwable>
    ) {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("source", imageFile.name, requestFile)

        imageUploadApi.uploadImage(apiKey, "upload", body)
            .enqueue(object : Callback<ImageUploadResponse> {
                override fun onResponse(
                    call: Call<ImageUploadResponse>,
                    response: Response<ImageUploadResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        data.postValue(response.body())
                    } else {
                        error.postValue(Throwable("Image upload failed: ${response.message()}"))
                    }
                }

                override fun onFailure(call: Call<ImageUploadResponse>, t: Throwable) {
                    error.postValue(t)
                }
            })
    }

    override fun addQuery(queryModel: QueryModel, result: (UiState<String>) -> Unit) {
        Log.d(
            TAG,
            "addQuery: Starting with communityId=${queryModel.communityId}, communityName=${queryModel.communityName}"
        )

        val queryId = database.collection(Constant.POSTS).document().id
        queryModel.queryId = queryId
        queryModel.timestamp = System.currentTimeMillis()

        // ✅ VALIDATION: Ensure communityId is present before saving
        if (queryModel.communityId.isNullOrBlank()) {
            Log.e(TAG, "addQuery: Query rejected - no communityId")
            result.invoke(UiState.Failure("Query must be associated with a community"))
            return
        }

        database.collection(Constant.POSTS)
            .document(queryId)
            .set(queryModel)
            .addOnSuccessListener {
                Log.d(
                    TAG,
                    "addQuery: Successfully added query $queryId to community ${queryModel.communityName}"
                )
                result.invoke(UiState.Success("Query added successfully to ${queryModel.communityName}"))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "addQuery: Failed to add query", exception)
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to add query"))
            }
    }

    override fun getUserQueries(userId: String, result: (UiState<List<QueryModel>>) -> Unit) {
        Log.d(TAG, "getUserQueries: Starting for userId=$userId")

        database.collection(Constant.POSTS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val queries = documents.toObjects(QueryModel::class.java)
                Log.d(TAG, "getUserQueries: Found ${queries.size} queries for user $userId")
                result.invoke(UiState.Success(queries))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "getUserQueries: Failed", exception)
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to fetch queries"
                    )
                )
            }
    }

    override fun getAllQueries(result: (UiState<List<QueryModel>>) -> Unit) {
        Log.d(TAG, "getAllQueries: Starting")

        database.collection(Constant.POSTS)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val queries = documents.toObjects(QueryModel::class.java)
                Log.d(TAG, "getAllQueries: Found ${queries.size} total queries")
                result.invoke(UiState.Success(queries))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "getAllQueries: Failed", exception)
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to fetch queries"
                    )
                )
            }
    }

    // ✅ ENHANCED with detailed logging
    override fun getQueriesFromUserCommunities(
        userId: String,
        result: (UiState<List<QueryModel>>) -> Unit
    ) {
        Log.d(TAG, "getQueriesFromUserCommunities: Starting for userId=$userId")
        Log.d(
            TAG,
            "getQueriesFromUserCommunities: Looking in collection path: ${Constant.USERS}/$userId/${Constant.MY_COMMUNITIES}"
        )

        // First, get the user's joined communities
        database.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.MY_COMMUNITIES)
            .get()
            .addOnSuccessListener { communityDocs ->
                Log.d(
                    TAG,
                    "getQueriesFromUserCommunities: Found ${communityDocs.size()} community documents"
                )

                // Log each community document
                communityDocs.documents.forEachIndexed { index, doc ->
                    Log.d(
                        TAG,
                        "getQueriesFromUserCommunities: Community $index - ID: ${doc.id}, Data: ${doc.data}"
                    )
                }

                // Extract community IDs from user's joined communities
                val communityIds = communityDocs.documents.mapNotNull { doc ->
                    val communityId = doc.getString("communityId")
                    Log.d(
                        TAG,
                        "getQueriesFromUserCommunities: Extracted communityId: $communityId from document ${doc.id}"
                    )
                    communityId
                }.filter { it.isNotEmpty() }

                Log.d(TAG, "getQueriesFromUserCommunities: Final community IDs: $communityIds")

                if (communityIds.isEmpty()) {
                    Log.d(TAG, "getQueriesFromUserCommunities: User hasn't joined any communities")
                    result.invoke(UiState.Success(emptyList()))
                    return@addOnSuccessListener
                }

                // Now get queries from these communities
                Log.d(
                    TAG,
                    "getQueriesFromUserCommunities: Fetching queries from ${communityIds.size} communities"
                )
                fetchQueriesFromCommunities(communityIds, result)
            }
            .addOnFailureListener { exception ->
                Log.e(
                    TAG,
                    "getQueriesFromUserCommunities: Failed to fetch user communities",
                    exception
                )
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to fetch user communities"
                    )
                )
            }
    }

    // ✅ Enhanced with logging
    private fun fetchQueriesFromCommunities(
        communityIds: List<String>,
        result: (UiState<List<QueryModel>>) -> Unit
    ) {
        Log.d(
            TAG,
            "fetchQueriesFromCommunities: Searching for queries with communityIds: $communityIds"
        )

        database.collection(Constant.POSTS)
            .whereIn("communityId", communityIds)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { queryDocs ->
                Log.d(TAG, "fetchQueriesFromCommunities: Found ${queryDocs.size()} query documents")

                val queries = queryDocs.toObjects(QueryModel::class.java)
                Log.d(
                    TAG,
                    "fetchQueriesFromCommunities: Converted to ${queries.size} QueryModel objects"
                )

                // Log each query for debugging
                queries.forEachIndexed { index, query ->
                    Log.d(
                        TAG,
                        "fetchQueriesFromCommunities: Query $index - ID: ${query.queryId}, Title: ${query.title}, CommunityId: ${query.communityId}, CommunityName: ${query.communityName}"
                    )
                }

                val filteredQueries = queries.filter { !it.communityId.isNullOrBlank() }
                Log.d(
                    TAG,
                    "fetchQueriesFromCommunities: After filtering: ${filteredQueries.size} queries"
                )

                result.invoke(UiState.Success(filteredQueries))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "fetchQueriesFromCommunities: Failed to fetch queries", exception)
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to fetch community queries"
                    )
                )
            }
    }

    private fun fetchQueriesInChunks(
        communityIds: List<String>,
        result: (UiState<List<QueryModel>>) -> Unit
    ) {
        Log.d(TAG, "fetchQueriesInChunks: Handling ${communityIds.size} communities in chunks")

        val allQueries = mutableListOf<QueryModel>()
        val chunks = communityIds.chunked(10)
        var completedChunks = 0

        Log.d(TAG, "fetchQueriesInChunks: Split into ${chunks.size} chunks")

        chunks.forEach { chunk ->
            database.collection(Constant.POSTS)
                .whereIn("communityId", chunk)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { queryDocs ->
                    val queries = queryDocs.toObjects(QueryModel::class.java)
                        .filter { !it.communityId.isNullOrBlank() }

                    synchronized(allQueries) {
                        allQueries.addAll(queries)
                        completedChunks++

                        Log.d(
                            TAG,
                            "fetchQueriesInChunks: Completed chunk $completedChunks/${chunks.size}, found ${queries.size} queries"
                        )

                        if (completedChunks == chunks.size) {
                            val sortedQueries = allQueries.sortedByDescending { it.timestamp ?: 0 }
                            Log.d(
                                TAG,
                                "fetchQueriesInChunks: All chunks completed, returning ${sortedQueries.size} total queries"
                            )
                            result.invoke(UiState.Success(sortedQueries))
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "fetchQueriesInChunks: Failed on chunk", exception)
                    result.invoke(
                        UiState.Failure(
                            exception.localizedMessage ?: "Failed to fetch community queries"
                        )
                    )
                }
        }
    }

    override fun updateQueryStatus(
        queryId: String,
        status: String,
        result: (UiState<String>) -> Unit
    ) {
        database.collection(Constant.POSTS)
            .document(queryId)
            .update("status", status)
            .addOnSuccessListener {
                result.invoke(UiState.Success("Status updated successfully"))
            }
            .addOnFailureListener { exception ->
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to update status"
                    )
                )
            }
    }

    override fun deleteQuery(queryId: String, result: (UiState<String>) -> Unit) {
        Log.d(TAG, "deleteQuery: Deleting query with ID: $queryId")

        database.collection(Constant.POSTS)
            .document(queryId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "deleteQuery: Successfully deleted query $queryId")
                result.invoke(UiState.Success("Query deleted successfully"))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "deleteQuery: Failed to delete query $queryId", exception)
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to delete query"
                    )
                )

            }
    }

    /**
     * --- Coroutine Usage ---
     * `callbackFlow` is a coroutine builder that converts callback-based APIs (like your repository
     * function) into a cold Flow stream.
     * `trySend(UiState.Loading)` immediately emits the loading state to the collector.
     * `getQueriesFromUserCommunities(userId) { ... }` calls your existing function.
     * `trySend(result)` takes the final result (Success or Failure) from the callback and
     * emits it into the flow.
     * `close()` signals that the flow is complete, as this is a one-shot read, not a snapshot listener.
     * `awaitClose { ... }` is required by `callbackFlow` to handle cleanup or cancellation.
     * This allows the `HomeViewModel` to use modern, efficient Flow operators like `flatMapLatest`.
     */
    override fun getQueriesFromUserCommunitiesFlow(userId: String): Flow<UiState<List<QueryModel>>> = callbackFlow {
        // Send loading state immediately
        trySend(UiState.Loading)

        // Call your existing callback function
        getQueriesFromUserCommunities(userId) { result ->
            // When the callback returns, send the result (Success/Failure) to the flow
            trySend(result)
            // Since this is a one-shot operation, close the flow
            close()
        }

        // Keep the flow open until close() is called or the coroutine is cancelled
        awaitClose {
            Log.d(TAG, "getQueriesFromUserCommunitiesFlow flow was closed/cancelled.")
        }
    }

    // --- Coroutine Usage ---
// This function uses callbacks but performs an asynchronous Firestore query.
// The ViewModel calls this function within a coroutine scope.
    override fun getCommunityPosts(communityId: String, result: (UiState<List<QueryModel>>) -> Unit) {
        Log.d(TAG, "getCommunityPosts: Fetching posts for communityId=$communityId")
        result.invoke(UiState.Loading)

        database.collection(Constant.POSTS)
            .whereEqualTo("communityId", communityId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.toObjects(QueryModel::class.java)
                Log.d(TAG, "getCommunityPosts: Found ${posts.size} posts for community $communityId")
                result.invoke(UiState.Success(posts))
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "getCommunityPosts: Failed for community $communityId", exception)
                result.invoke(
                    UiState.Failure(
                        exception.localizedMessage ?: "Failed to fetch community posts"
                    )
                )
            }
    }
}