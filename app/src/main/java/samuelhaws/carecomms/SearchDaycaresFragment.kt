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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.util.ArrayList

class SearchDaycaresFragment: Fragment(), DaycareSearchAdapter.MyItemClickListener {
    lateinit var mLayoutManager: LinearLayoutManager
    lateinit var myAdapter: DaycareSearchAdapter
    val children: ArrayList<ChildData> = arrayListOf()
    lateinit var childToChat: String //to hold UID of child for chatting about

    fun getChildName(childId: String) {
        val getURL = "https://care-comms-android.herokuapp.com/api/child/" + childId
        val queue = Volley.newRequestQueue(this.context)
        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET,getURL,null,
                Response.Listener { response ->
                    val responseChild = response.toString().trimIndent()
                    var child:  ChildData = Gson().fromJson(responseChild,ChildData::class.java)
                        children.add(child)
                },
                Response.ErrorListener { error ->
                })
        queue.add(jsonObjectRequest)
    }

    @SuppressLint("RestrictedApi")
    override fun onOverflowMenuClickedFromAdapter(view: View, position: Int) {
        val popup = PopupMenu(context!!, view)
        for (child: ChildData in children){
            popup.menu.add(child.nickname)
        }
        popup.setOnMenuItemClickListener {
            for (child: ChildData in children)
                if (it.title == child.nickname) { //update firebase , then update mongo
                    myAdapter.mRef.child("${myAdapter.items[position].daycareUID}/children/${children[children.indexOf(child)].child_id}")
                            .setValue(children[children.indexOf(child)].child_id)
                    (activity as MainActivity).updateChildInMongo(child,myAdapter.items[position].daycareUID)
                }
true
        }
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popup)
            mPopup.javaClass
                    .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                    .invoke(mPopup, true)
        }catch(e: Exception) {
        }finally {
            popup.show()
        }

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
        for (uid: String in (activity as MainActivity).mChildren)
            getChildName(uid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_search_daycares, container, false)
    }

    fun search(query: String) {
        var pos: Int = 0
        var i: Int = 0
        var found: Boolean = false
        for (item in myAdapter.items) {
            if (item.daycareName.toUpperCase().contains(query.toUpperCase())){
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val mView = view
        val rview = view.findViewById<RecyclerView>(R.id.search_daycares_recyclerview)
        val layoutManager = LinearLayoutManager(view.context)
        //rview.hasFixedSize()
        rview.layoutManager = layoutManager
        mLayoutManager = layoutManager

        myAdapter = DaycareSearchAdapter(this.context!!)
        myAdapter.setMyItemClickListener(this)
        rview.adapter = myAdapter

        rview.itemAnimator?.addDuration = 1000L
        rview.itemAnimator?.removeDuration = 1000L
        rview.itemAnimator?.moveDuration = 1000L
        rview.itemAnimator?.changeDuration = 1000L

        (activity as MainActivity).mToolbar.setOnMenuItemClickListener {
            when (it.itemId){
                R.id.action_addchild -> {
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
                R.id.action_chat -> {
                    val popup = PopupMenu(this.context!!, view)
                    for (service: Service in (activity as MainActivity).mDaycares){
                        popup.menu.add(service.daycareName)
                    }
                    popup.setOnMenuItemClickListener {
                        for (child: ChildData in (activity as MainActivity).mChildrenData)
                            for (service: Service in (activity as MainActivity).mDaycares)
                                if (child.enrolled_daycare_id.equals(service.daycareUID))
                                    (activity as MainActivity).openChatDetailFragment(child.child_id)
                        true
                    }
                    try {
                        val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                        fieldMPopup.isAccessible = true
                        val mPopup = fieldMPopup.get(popup)
                        mPopup.javaClass
                                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                                .invoke(mPopup, true)
                    }catch(e: Exception) {
                    }finally {
                        popup.show()
                    }
                    true
                }
                else -> true
            }
        }
    }


}

class DaycareSearchAdapter(context: Context): RecyclerView.Adapter<DaycareSearchAdapter.ServiceViewHolder>() {
    var items: ArrayList<Service> = arrayListOf()
    val mRef = FirebaseDatabase.getInstance().reference.child("services")

    var childEventListener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError) {
            Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
        }

        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<Service>(Service::class.java)
            val key = p0.key
            notifyDataSetChanged()
        }


        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<Service>(Service::class.java)
            val key = p0.key
            var itemChanged: Service? = null
            for (item: Service in items) {
                if (item.daycareUID == data!!.daycareUID)
                    itemChanged = item
            }
            if (itemChanged != null){
                items[(items.indexOf(itemChanged))] = data!!
            }
            /*for (ds: DataSnapshot in p0.children) {
                items[p0.children.indexOf(ds)] = data!!
            }*/
            notifyDataSetChanged()
        }

        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            val data = p0.getValue<Service>(Service::class.java)
            Log.i("wow","searchday onchildadded" + data!!.public)
            val key = p0.key
            if (data!!.public)
                items.add(data!!)

            notifyItemInserted(items.indexOf(data!!))
        }

        override fun onChildRemoved(p0: DataSnapshot) {
            val data = p0.getValue<Service>(Service::class.java)
            val key = p0.key
            var itemToRemove: Service? = null
            for (item: Service in items) {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_search_daycares, parent, false)
        return ServiceViewHolder(view)
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

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {

        try {
            //var imgRes: Int = posterTable.get(items[position].title)!!
            var item = items[position]
            var name: String = item.daycareName
            var description: String = item.daycareDesc
            var address: String = item.daycareAddr
            //holder.moviePoster.setImageResource(imgRes)
            holder.daycareName.text = name
            holder.daycareDesc.text = description
            holder.daycareAddress.text = address
            // setAnimation(holder.itemView,position)
        }catch(e: IndexOutOfBoundsException){}
/*

        try {
            var imgRes: Int = posterTable.get(items[position].title)!!
            var title: String = items[position].title
            var overview: String = items[position].overview
            holder.moviePoster.setImageResource(imgRes)
            holder.movieTitle.text = title
            holder.movieOverview.text = overview
            holder.movieSelect.isChecked = items[position].checked
            holder.movieRelDate = items[position].release_date
            // setAnimation(holder.itemView,position)
        }catch(e: IndexOutOfBoundsException){}
*/


    }


    inner class ServiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        //val moviePoster = view.findViewById<ImageView>(R.id.card_img)
        val daycareName = view.findViewById<TextView>(R.id.search_card_name)
        val daycareDesc = view.findViewById<TextView>(R.id.search_card_desc)
        val daycareAddress = view.findViewById<TextView>(R.id.search_card_address)
        var addBtn = view.findViewById<ImageButton>(R.id.cardview_search_add_btn)
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
            addBtn.setOnClickListener{
                if (myListener != null)
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        myListener!!.onOverflowMenuClickedFromAdapter(it,adapterPosition)
                    }
            }
        }
    }


    interface MyItemClickListener {
        fun onItemClickedFromAdapter(position: Int)
        fun onItemLongClickedFromAdapter(position: Int)
        fun onOverflowMenuClickedFromAdapter(view: View, position: Int)
    }

    var myListener: MyItemClickListener? = null
    //...

    fun setMyItemClickListener( listener: MyItemClickListener ) {
        this.myListener = listener
    }
}
