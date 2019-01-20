package samuelhaws.carecomms

import android.support.v4.app.Fragment
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import samuelhaws.carecomms.MainActivity
import java.security.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class Message(val senderUid: String, val messageText: String, val messageTimeStamp: String){
    constructor():this("","","")
}


class ChatDetailFragment:Fragment() {
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var myAdapter: ChatDetailAdapter

    companion object {
        fun newInstance(childUID: String): ChatDetailFragment {
            val args: Bundle = Bundle()
            args.putString("childUID", childUID)
            val fragment = ChatDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rview = view.findViewById<RecyclerView>(R.id.chat_detail_recyclerview)
        val layoutManager = LinearLayoutManager(view.context)
        //rview.hasFixedSize()
        rview.layoutManager = layoutManager
        mLayoutManager = layoutManager
        //var mUserUID = FirebaseAuth.getInstance().currentUser!!.uid
        //var other = arguments!!.getString("otherUserUID")
        var chatRef = FirebaseDatabase.getInstance().reference


        Log.i("wow","onViewCreated Chat currentUserType: " + (activity as MainActivity).mCurrentUserType)
            chatRef = FirebaseDatabase.getInstance().reference.child("chats/${arguments!!.getString("childUID")}")
        myAdapter = ChatDetailAdapter(this.context!!,chatRef)
        rview.adapter = myAdapter
        rview.itemAnimator?.addDuration = 1000L
        rview.itemAnimator?.removeDuration = 1000L
        rview.itemAnimator?.moveDuration = 1000L
        rview.itemAnimator?.changeDuration = 1000L

        val sendBtn: Button = view.findViewById<Button>(R.id.chat_send_btn)
        sendBtn.setOnClickListener {
            val message = view.findViewById<EditText>(R.id.chat_edittext).text.toString()
            var messageToAdd = Message(FirebaseAuth.getInstance().currentUser!!.uid,message, "")
            Log.i("wow","sendBtn clicked.")
            val messageRef = FirebaseDatabase.getInstance()
                    .getReference("chats/${arguments!!.getString("childUID")}/${UUID.randomUUID()}")
            messageRef.setValue(messageToAdd)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_detail, container, false)
    }
}

class ChatDetailAdapter(context: Context, reference: DatabaseReference): RecyclerView.Adapter<ChatDetailAdapter.MessageViewHolder>() {
    var items: ArrayList<Message> = arrayListOf()
    var mUserUID = FirebaseAuth.getInstance().currentUser!!.uid
    val context = context
    val CHAT_START: Int = 0
    val CHAT_END: Int = 1

    var chatRef = reference
    var childEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
        }


        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            Log.i("wow",p0.value.toString())
            val data = p0.getValue<Message>(Message::class.java)
            items.add(data!!)
            notifyItemInserted(items.indexOf(data!!))
            Log.i("wow","onChildAdded Chat" + data.messageText)
        }

        override fun onChildRemoved(p0: DataSnapshot) {
        }
    }
    init {
        chatRef.addChildEventListener(childEventListener)
    }

    override fun getItemViewType(position: Int): Int {
        if (items.get(position).senderUid.equals(mUserUID))
            return CHAT_START
        else
            return CHAT_END
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view: View
        Log.i("wow","chatdetail viewtype: " + viewType)
        if (viewType == CHAT_START)
             view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_chat_detail_right, parent, false)
        else
            view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_chat_detail_left, parent, false)
        return MessageViewHolder(view)
    }

    fun setAnimation(view: View, position: Int) {
        if (position != items.size-1) {
            var animation: Animation =
                    AnimationUtils.loadAnimation(view.context,
                            android.R.anim.slide_in_left)
            animation.duration = (200) //1000
            view.startAnimation(animation)
            //lastPosition = position
        }
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        try {
            //var imgRes: Int = posterTable.get(items[position].title)!!
            var item = items[position]
            holder.messageText.text = item.messageText
            holder.messageTimestamp.text = item.messageTimeStamp
            // setAnimation(holder.itemView,position)
        }catch(e: IndexOutOfBoundsException){}
    }


    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val moviePoster = view.findViewById<ImageView>(R.id.card_img)
        val messageText = view.findViewById<TextView>(R.id.chat_detail_card_text)
        val messageTimestamp = view.findViewById<TextView>(R.id.chat_detail_card_timestamp)
    }
}
