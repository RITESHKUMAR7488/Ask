package com.example.ask.addModule.repositories

import androidx.lifecycle.MutableLiveData
import com.example.ask.addModule.interfaces.ImageUploadApi
import com.example.ask.addModule.models.ImageUploadResponse
import com.example.ask.addModule.models.QueryModel
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
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

    override fun uploadImage(
        imageFile: File,
        apiKey: String,
        data: MutableLiveData<ImageUploadResponse>,
        error: MutableLiveData<Throwable>
    ) {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("source", imageFile.name, requestFile)

        imageUploadApi.uploadImage(apiKey, "upload", body).enqueue(object : Callback<ImageUploadResponse> {
            override fun onResponse(call: Call<ImageUploadResponse>, response: Response<ImageUploadResponse>) {
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
        val queryId = database.collection(Constant.POSTS).document().id
        queryModel.queryId = queryId
        queryModel.timestamp = System.currentTimeMillis()

        database.collection(Constant.POSTS)
            .document(queryId)
            .set(queryModel)
            .addOnSuccessListener {
                result.invoke(UiState.Success("Query added successfully"))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to add query"))
            }
    }

    override fun getUserQueries(userId: String, result: (UiState<List<QueryModel>>) -> Unit) {
        database.collection(Constant.POSTS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val queries = documents.toObjects(QueryModel::class.java)
                result.invoke(UiState.Success(queries))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to fetch queries"))
            }
    }

    override fun getAllQueries(result: (UiState<List<QueryModel>>) -> Unit) {
        database.collection(Constant.POSTS)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val queries = documents.toObjects(QueryModel::class.java)
                result.invoke(UiState.Success(queries))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to fetch queries"))
            }
    }

    // ✅ NEW: Get queries only from communities the user has joined
    override fun getQueriesFromUserCommunities(userId: String, result: (UiState<List<QueryModel>>) -> Unit) {
        // First, get the user's joined communities
        database.collection(Constant.USERS)
            .document(userId)
            .collection(Constant.MY_COMMUNITIES)
            .get()
            .addOnSuccessListener { communityDocs ->
                // Extract community IDs from user's joined communities
                val communityIds = communityDocs.documents.mapNotNull { doc ->
                    doc.getString("communityId")
                }.filter { it.isNotEmpty() }

                if (communityIds.isEmpty()) {
                    // User hasn't joined any communities
                    result.invoke(UiState.Success(emptyList()))
                    return@addOnSuccessListener
                }

                // Now get queries from these communities
                database.collection(Constant.POSTS)
                    .whereIn("communityId", communityIds)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { queryDocs ->
                        val queries = queryDocs.toObjects(QueryModel::class.java)
                        result.invoke(UiState.Success(queries))
                    }
                    .addOnFailureListener { exception ->
                        result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to fetch community queries"))
                    }
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to fetch user communities"))
            }
    }

    override fun updateQueryStatus(queryId: String, status: String, result: (UiState<String>) -> Unit) {
        database.collection(Constant.POSTS)
            .document(queryId)
            .update("status", status)
            .addOnSuccessListener {
                result.invoke(UiState.Success("Status updated successfully"))
            }
            .addOnFailureListener { exception ->
                result.invoke(UiState.Failure(exception.localizedMessage ?: "Failed to update status"))
            }
    }
}