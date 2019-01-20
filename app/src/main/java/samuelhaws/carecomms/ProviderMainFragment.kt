package samuelhaws.carecomms

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import samuelhaws.carecomms.ChildData
import samuelhaws.carecomms.MainActivity
import java.util.*
import kotlin.collections.ArrayList

class ProviderMainFragment: Fragment(), DaycareAdapter.MyItemClickListener {
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var myAdapter: DaycareAdapter
    var mCurrentUserDaycareUid: String? = null
    val children: ArrayList<ChildData> = arrayListOf()

    override fun onContactBtnClickedFromAdapter(view: View, position: Int) {
        (activity as MainActivity).openChatDetailFragment(myAdapter.items[position].child_id)
    }

    override fun onItemLongClickedFromAdapter(position: Int) {
        //activity!!.startActionMode(ActionBarCallback(position))
    }

    override fun onItemClickedFromAdapter(position: Int) {
        //var itemPosition : Int = myAdapter.
       /* if (mCallingActivity.contains("Task2Activity"))
            (activity as Task2Activity).helper(position,myAdapter.items)
        else if (mCallingActivity.contains("Task3Activity"))
            (activity as Task3Activity).movieDetailHelper(position,myAdapter.items)*/

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("wow","ProvFragment onCreate")
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_provider_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val mView = view
        val rview = view.findViewById<RecyclerView>(R.id.daycare_recyclerview)
        val layoutManager = LinearLayoutManager(view.context)
        //rview.hasFixedSize()
        rview.layoutManager = layoutManager
        mLayoutManager = layoutManager

        FirebaseDatabase.getInstance().reference
                .child("users/${FirebaseAuth.getInstance().currentUser!!.uid}/daycareID")
                .addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(p0: DataSnapshot) {
                        mCurrentUserDaycareUid = p0.value as String
                        setAdapter(rview,mCurrentUserDaycareUid)
                    }
                    override fun onCancelled(p0: DatabaseError) {
                    }
                })

    }

    fun search(query: String) {
        var pos: Int = 0
        var i: Int = 0
        var found: Boolean = false
        for (item in myAdapter.items) {
            if (item.full_name.toUpperCase().contains(query.toUpperCase())){
                pos = i
                found = true
                break
            }
            i++
        }
        if (found)
            mLayoutManager.scrollToPosition(pos)
        else
            Toast.makeText(this.context,"Search returned no results.",Toast.LENGTH_SHORT).show()
    }

   /* override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        if (activity.toString().contains("MainActivity")){
            if (menu?.findItem(R.id.action_searchdaycares) == null)
                inflater?.inflate(R.menu.user_base_menu,menu)

            val search = menu?.findItem(R.id.action_searchdaycares)!!.actionView as SearchView

            if (search != null) {
                search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        search(query!!)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        return true
                    }
                })
            }
        }

    }*/


    fun setAdapter(rview: RecyclerView, daycareUID: String?){
        myAdapter = DaycareAdapter(this.context!!, daycareUID)
        myAdapter.setMyItemClickListener(this)
        rview.adapter = myAdapter

        Log.i("wow","setAdapterProviderMainFragment, rviewadd: " + rview.adapter)

        rview.itemAnimator?.addDuration = 1000L
        rview.itemAnimator?.removeDuration = 1000L
        rview.itemAnimator?.moveDuration = 1000L
        rview.itemAnimator?.changeDuration = 1000L

        (activity as MainActivity).mToolbar.setOnMenuItemClickListener() {
            when (it.itemId){
                R.id.action_addchild -> {
                    //
                    true
                }
                R.id.action_searchdaycares -> {
                    val search = (activity as MainActivity).mToolbar?.menu.findItem(R.id.action_searchdaycares)!!.actionView as SearchView

                    if (search != null) {
                        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                            override fun onQueryTextSubmit(query: String?): Boolean {
                                search(query!!)
                                return true
                            }

                            override fun onQueryTextChange(newText: String?): Boolean {
                                return true
                            }
                        })
                    }

                    true
                }
                else -> true
            }
        }
    }

}

class DaycareAdapter(context: Context, daycareUID: String?): RecyclerView.Adapter<DaycareAdapter.ChildViewHolder>() {

    lateinit var posterTable : MutableMap < String , Int >
    var items: ArrayList<ChildData> = arrayListOf()
    val mRef = FirebaseDatabase.getInstance().reference.child("services/${daycareUID}/children")
    val context = context

    fun getChild(childId: String) {
        val getURL = "https://care-comms-android.herokuapp.com/api/child/" + childId
        val queue = Volley.newRequestQueue(context)
        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET,getURL,null,
                Response.Listener { response ->
                    val responseChild = response.toString().trimIndent()
                    var child:  ChildData = Gson().fromJson(responseChild,ChildData::class.java)
                    items.add(child)
                    notifyItemInserted(items.indexOf(child))
                },
                Response.ErrorListener { error ->
                })
        queue.add(jsonObjectRequest)
    }

    var childEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<ChildData>(ChildData::class.java)
            val key = p0.key
            notifyDataSetChanged()
        }


        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<ChildData>(ChildData::class.java)
            val key = p0.key
            var itemChanged: ChildData? = null
            for (item: ChildData in items) {
                if (item.child_id == data!!.child_id)
                    itemChanged = item
            }
            if (itemChanged != null){
                items[(items.indexOf(itemChanged))] = data!!
            }
            notifyDataSetChanged()
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<String>(String::class.java)
            val key = p0.key
            getChild(data!!)
            Log.i("wow","onChildAddedPROVRAG")
        }

        override fun onChildRemoved(p0: DataSnapshot) {
            val data = p0.getValue<ChildData>(ChildData::class.java)
            val key = p0.key
            var itemToRemove: ChildData? = null
            for (item: ChildData in items) {
                //if (item.vote_count == data!!.vote_count && !isOriginalMovie(key!!))
                 //   itemToRemove = item
            }
            if (itemToRemove != null){
                items.removeAt(items.indexOf(itemToRemove))
                notifyItemRemoved(items.indexOf(itemToRemove)+1)
            }
        }

        fun isOriginalMovie(key: String): Boolean{
            return (key == "0" || key == "1" || key == "2" || key == "3" || key == "4" || key == "5" ||
                    key == "6" || key == "7" || key == "8" || key == "9")
        }
    }

    init {
        mRef.addChildEventListener(childEventListener)
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_daycare_children, parent, false)
        return ChildViewHolder(view)
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

    override fun onBindViewHolder(holder: ChildViewHolder, position: Int) {
        try {
            Log.i("wow","onBindViewHolderPROVFRAG")
            var item = items[position]
            Picasso.get().load(item.image_url).into(holder.childImgView)
            holder.childNickname.text = item.nickname
            holder.childFullName.text = item.full_name
            holder.childAge.text = item.age.toString()
            holder.childHealthInfo.text = item.health_info
            // setAnimation(holder.itemView,position)
        }catch(e: IndexOutOfBoundsException){}
    }

    inner class ChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val childImgView = view.findViewById<ImageView>(R.id.child_card_img)
        val childNickname = view.findViewById<TextView>(R.id.child_card_nickname)
        val childFullName = view.findViewById<TextView>(R.id.child_card_fullname)
        val childAge = view.findViewById<TextView>(R.id.child_card_age)
        val childHealthInfo = view.findViewById<TextView>(R.id.child_card_healthinfo)
        var contactBtn = view.findViewById<ImageButton>(R.id.child_card_contact_btn)

        init {
            view.setOnClickListener {
                //through tapping
                if (myListener != null)
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        myListener!!.onItemClickedFromAdapter(adapterPosition)
                    }
            }
            view.setOnLongClickListener {
                if (myListener != null)
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        myListener!!.onItemLongClickedFromAdapter(adapterPosition)
                    }
                true
            }
            contactBtn.setOnClickListener{
                if (myListener != null)
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        myListener!!.onContactBtnClickedFromAdapter(it,adapterPosition)
                    }
            }
        }
    }

    interface MyItemClickListener {
        fun onItemClickedFromAdapter(position: Int)
        fun onItemLongClickedFromAdapter(position: Int)
        fun onContactBtnClickedFromAdapter(view: View, position: Int)
    }

    var myListener: MyItemClickListener? = null
    //...

    fun setMyItemClickListener( listener: MyItemClickListener ) {
        this.myListener = listener
    }
}

