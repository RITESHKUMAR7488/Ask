package com.example.ask.chatModule.uis

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException
import com.cometchat.chat.models.BaseMessage
import com.cometchat.chat.models.TextMessage
import com.cometchat.chat.models.User
import com.example.ask.R
import com.example.ask.chatModule.adapters.MessageAdapter

class SimpleChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<BaseMessage>()

    private var targetUserId: String? = null
    private var userName: String? = null
    private var queryTitle: String? = null

    companion object {
        private const val TAG = "SimpleChatFragment"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_QUERY_TITLE = "query_title"

        fun newInstance(userId: String, userName: String?, queryTitle: String?): SimpleChatFragment {
            val fragment = SimpleChatFragment()
            val args = Bundle().apply {
                putString(ARG_USER_ID, userId)
                putString(ARG_USER_NAME, userName)
                putString(ARG_QUERY_TITLE, queryTitle)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_USER_ID)
            userName = it.getString(ARG_USER_NAME)
            queryTitle = it.getString(ARG_QUERY_TITLE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_simple_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSendButton()
        loadMessages()
        setupMessageListener()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewMessages)
        messageInput = view.findViewById(R.id.etMessage)
        sendButton = view.findViewById(R.id.btnSend)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty() && !targetUserId.isNullOrEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val textMessage = TextMessage(
            targetUserId!!,
            messageText,
            CometChat.MESSAGE_TYPE_TEXT,
            CometChat.RECEIVER_TYPE_USER
        )

        CometChat.sendMessage(textMessage, object : CometChat.CallbackListener<TextMessage>() {
            override fun onSuccess(message: TextMessage) {
                Log.d(TAG, "Message sent successfully: ${message.text}")
                activity?.runOnUiThread {
                    messageInput.setText("")
                    messages.add(message)
                    messageAdapter.notifyItemInserted(messages.size - 1)
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Message sending failed: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun loadMessages() {
        if (targetUserId.isNullOrEmpty()) return

        val messagesRequest = CometChat.MessagesRequestBuilder()
            .setUID(targetUserId!!)
            .setLimit(50)
            .build()

        messagesRequest.fetchPrevious(object : CometChat.CallbackListener<List<BaseMessage>>() {
            override fun onSuccess(messageList: List<BaseMessage>) {
                Log.d(TAG, "Messages fetched successfully: ${messageList.size}")
                activity?.runOnUiThread {
                    messages.clear()
                    messages.addAll(messageList.reversed())
                    messageAdapter.notifyDataSetChanged()
                    if (messages.isNotEmpty()) {
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }

            override fun onError(e: CometChatException) {
                Log.e(TAG, "Message fetching failed: ${e.message}")
                activity?.runOnUiThread {
                    Toast.makeText(context, "Failed to load messages: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupMessageListener() {
        val listenerID = "SIMPLE_CHAT_LISTENER"

        CometChat.addMessageListener(listenerID, object : CometChat.MessageListener() {
            override fun onTextMessageReceived(message: TextMessage) {
                Log.d(TAG, "Text message received: ${message.text}")
                if (message.sender.uid == targetUserId) {
                    activity?.runOnUiThread {
                        messages.add(message)
                        messageAdapter.notifyItemInserted(messages.size - 1)
                        recyclerView.scrollToPosition(messages.size - 1)
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        CometChat.removeMessageListener("SIMPLE_CHAT_LISTENER")
    }
}