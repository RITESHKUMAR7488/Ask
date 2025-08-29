package com.example.ask.chatModule.uis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.Conversation
import com.example.ask.R
import com.example.ask.chatModule.adapters.ConversationAdapter

class ConversationListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var conversationAdapter: ConversationAdapter
    private val conversations = mutableListOf<Conversation>()

    companion object {
        private const val TAG = "ConversationListFragment"

        fun newInstance(): ConversationListFragment {
            return ConversationListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_conversation_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        loadConversations()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewConversations)
    }

    private fun setupRecyclerView() {
        conversationAdapter = ConversationAdapter(conversations) { conversation ->
            onConversationClick(conversation)
        }
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = conversationAdapter
        }
    }

    private fun loadConversations() {
        val conversationsRequest = CometChat.ConversationsRequestBuilder()
            .setLimit(50)
            .build()

        conversationsRequest.fetchNext(object : CometChat.CallbackListener<List<Conversation>>() {
            override fun onSuccess(conversationList: List<Conversation>) {
                Log.d(TAG, "Conversations fetched successfully: ${conversationList.size}")
                activity?.runOnUiThread {
                    conversations.clear()
                    conversations.addAll(conversationList)
                    conversationAdapter.notifyDataSetChanged()

                    if (conversations.isEmpty()) {
                        // Show empty state
                        showEmptyState()
                    }
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Conversation fetching failed: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to load conversations: ${e.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        })
    }

    private fun onConversationClick(conversation: Conversation) {
        val conversationWith = conversation.conversationWith

        if (conversationWith is com.cometchat.chat.models.User) {
            val user = conversationWith as com.cometchat.chat.models.User

            // Replace current fragment with chat fragment
            val chatFragment = SimpleChatFragment.newInstance(
                userId = user.uid,
                userName = user.name,
                queryTitle = null
            )

            parentFragmentManager.beginTransaction()
                .replace(R.id.chat_container, chatFragment)
                .addToBackStack(null)
                .commit()

            Log.d(TAG, "Opening chat with user: ${user.name}")
        }
    }

    private fun showEmptyState() {
        // You can add an empty state view here
        Toast.makeText(context, "No conversations yet. Start chatting to see conversations here!", Toast.LENGTH_LONG).show()
    }
}