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