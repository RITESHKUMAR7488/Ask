package com.example.ask.search.repositories

import android.util.Log
import com.example.ask.addModule.models.QueryModel
import com.example.ask.communityModule.models.CommunityModels
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.search.model.SearchResult
import com.example.ask.search.model.SearchResultType
import com.example.ask.utilities.Constant
import com.example.ask.utilities.UiState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SearchRepository {

    companion object {
        private const val TAG = "SearchRepositoryImpl"
        private const val SEARCH_LIMIT = 10L // Limit results per category
    }

    // --- Coroutine Usage ---
    // This function returns a Flow, a coroutine primitive for async data streams.
    // `flow { ... }` builds the flow.
    // `await()` is a suspend function used on Firestore Tasks, pausing the coroutine (not the thread) until data is fetched.
    // `searchQueries`, `searchCommunities`, `searchUsers` are suspend functions performing I/O.
    // `.flowOn(Dispatchers.IO)` moves this work to a background thread pool, keeping the UI thread free.
    // This makes the search efficient and non-blocking.
    @OptIn(FlowPreview::class)
    override fun search(query: String): Flow<UiState<List<SearchResult>>> = flow {
        emit(UiState.Loading)
        Log.d(TAG, "Starting search for query: '$query'")

        // No need to check for blank, the ViewModel will handle that

        val queryResults = searchQueries(query)
        val communityResults = searchCommunities(query)
        val userResults = searchUsers(query)

        val combinedResults = (queryResults + communityResults + userResults)
            .sortedByDescending { it.timestamp ?: 0 } // Sort results

        Log.d(TAG, "Search completed. Found ${combinedResults.size} results.")
        emit(UiState.Success(combinedResults))

    }.debounce(300).catch { e ->
        Log.e(TAG, "Search error for query '$query'", e)
        emit(UiState.Failure(e.localizedMessage ?: "Failed to perform search"))
    }.flowOn(Dispatchers.IO) // Perform all operations on the IO dispatcher

    // --- Coroutine Usage ---
    // `suspend fun` marks this as a function that can be paused by coroutines.
    // `.await()` suspends the coroutine until Firestore returns data, preventing thread blocking.
    private suspend fun searchQueries(query: String): List<SearchResult> {
        return try {
            val snapshot = firestore.collection(Constant.POSTS)
                .orderBy("title")
                .startAt(query)
                .endAt(query + '\uf8ff') // Firestore prefix search
                .limit(SEARCH_LIMIT)
                .get()
                .await() // Suspend until data is fetched

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(QueryModel::class.java)?.let { model ->
                    SearchResult(
                        id = model.queryId ?: doc.id,
                        type = SearchResultType.QUERY,
                        title = model.title ?: "No Title",
                        subtitle = model.description?.take(100), // Snippet
                        imageUrl = model.imageUrl,
                        timestamp = model.timestamp
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching queries", e)
            emptyList()
        }
    }

    // --- Coroutine Usage ---
    // `suspend fun` with `.await()` for non-blocking database access.
    private suspend fun searchCommunities(query: String): List<SearchResult> {
        return try {
            val snapshot = firestore.collection(Constant.COMMUNITIES)
                .orderBy("communityName")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .limit(SEARCH_LIMIT)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(CommunityModels::class.java)?.let { model ->
                    SearchResult(
                        id = model.communityId ?: doc.id,
                        type = SearchResultType.COMMUNITY,
                        title = model.communityName ?: "No Name",
                        subtitle = "Community",
                        imageUrl = null,
                        timestamp = model.joinedAt
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching communities", e)
            emptyList()
        }
    }

    // --- Coroutine Usage ---
    // `suspend fun` with `.await()` for non-blocking database access.
    private suspend fun searchUsers(query: String): List<SearchResult> {
        return try {
            val snapshot = firestore.collection(Constant.USERS)
                .orderBy("fullName")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .limit(SEARCH_LIMIT)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserModel::class.java)?.let { model ->
                    SearchResult(
                        id = model.uid ?: doc.id,
                        type = SearchResultType.USER,
                        title = model.fullName ?: "No Name",
                        subtitle = model.email,
                        imageUrl = model.imageUrl
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            emptyList()
        }
    }
}