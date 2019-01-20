package samuelhaws.carecomms

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject
import samuelhaws.carecomms.R.id.*
import samuelhaws.carecomms.R.id.nav_add
import java.io.UnsupportedEncodingException
import java.lang.IndexOutOfBoundsException
import java.util.*
import kotlin.collections.ArrayList

class UserData(val uid: String, val fullname: String, val useremail: String, val usertype: String, val profileImageUrl: String){
    constructor():this("","","","","")
}

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var currentFragmentStr: String
    var mCurrentUserType: String? = null
    lateinit var mNavigationView: NavigationView
    lateinit var mToolbar: Toolbar

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var mDrawerLayout: DrawerLayout
    var mChildren: ArrayList<String> = arrayListOf() //if baseuser, will hold all uids of users children
    var mChildrenData: ArrayList<ChildData> = arrayListOf()
    var mDaycares: ArrayList<Service> = arrayListOf()
    var mView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var initFlag = false
        mToolbar = findViewById(R.id.myToolbar)

        //check users, display appropriate main fragment
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        val currentUserRef = FirebaseDatabase.getInstance().reference.child("users/$currentUserUid")

        mDrawerLayout = findViewById<DrawerLayout>(R.id.main_layout)

        //listener for switching fragments, resets menu items for main activity
        supportFragmentManager.addOnBackStackChangedListener {
            currentFragmentStr = supportFragmentManager.fragments[supportFragmentManager.fragments.count()-1].toString()
            //apply correct menus
            //base user
            if (mCurrentUserType.equals("Base")){
                //basemainfragment
                if (currentFragmentStr.contains("BaseMainFragment")) {
//                    if (!initFlag){ //replace basemainfragment with basemainfragment only first time, so that menu items are correct
//                        supportFragmentManager.beginTransaction().replace(R.id.main_container, BaseMainFragment())
//                                .addToBackStack(null).commitAllowingStateLoss()
//                        Log.i("wow","replaced!")
//                        initFlag = true
//                    }
                    mNavigationView.menu.getItem(0).setVisible(true) //allow Browse Public Daycares
                    //navdrawer clicklistener
                    try {
                        mNavigationView.setNavigationItemSelectedListener { menuItem ->
                            menuItem.isChecked = true
                            // close drawer when item is tapped
                            when (menuItem.itemId) { //nav drawer options
                                nav_add -> {
                                    Log.i("wow","currentusertype: " + mCurrentUserType)
                                    if (mCurrentUserType.equals("Base")) {
                                        mNavigationView.menu.getItem(0).setVisible(false)
                                        supportFragmentManager.beginTransaction()
                                                .replace(R.id.main_container, SearchDaycaresFragment()).addToBackStack(null).commit()
                                    }
                                }
                                nav_signout -> {
                                    signOut()
                                }
                                else -> {
                                }
                            }
                            menuItem.isChecked = false
                            mDrawerLayout.closeDrawers()
                            false
                        }}catch (e: Exception){}
                    //toolbar items
                    mToolbar.menu.getItem(0).setVisible(true)
                    mToolbar.menu.getItem(1).setVisible(false) //disable toolbar search action
                    mToolbar.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.action_addchild -> { //set toolbar add child action
                                if (mCurrentUserType.equals("Base"))
                                    supportFragmentManager.beginTransaction().replace(R.id.main_container, AddChildBaseFragment())
                                            .addToBackStack(null).commitAllowingStateLoss()
                                true
                            }
                            R.id.action_chat -> {
                                mView = supportFragmentManager.fragments[0].view //FIX
                                Log.i("wow","mView: " + mView)
                                val popup = PopupMenu(this!!, mView)
                                for (service: Service in mDaycares){
                                    popup.menu.add(service.daycareName)
                                }
                                popup.setOnMenuItemClickListener {
                                    for (child: ChildData in mChildrenData)
                                        for (service: Service in mDaycares)
                                            if (child.enrolled_daycare_id.equals(service.daycareUID))
                                                openChatDetailFragment(child.child_id)
                                    true
                                }
                                try {
                                    val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                                    fieldMPopup.isAccessible = true
                                    val mPopup = fieldMPopup.get(popup)
                                    mPopup.javaClass
                                            .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                                            .invoke(mPopup, true)
                                }catch(e: Exception) { Log.i("wow",e.toString())
                                }finally {
                                    popup.show()
                                }
                                true
                            }
                            else -> { //no action for search or others in toolbar
                                true
                            }
                        }
                    }
                }
                //searchdaycaresfragment
                else if (currentFragmentStr.contains("SearchDaycaresFragment")) {
                    mNavigationView.menu.getItem(0).setVisible(false) //disable Browse Public Daycares in drawer
                    mToolbar.menu.getItem(0).setVisible(false) //disable toolbar add action
                    mToolbar.menu.getItem(1).setVisible(true) //enable toolbar search action
                    /*mToolbar.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.action_addchild -> { //set toolbar add child action
                                *//*if (mCurrentUserType.equals("Base"))
                                    supportFragmentManager.beginTransaction().replace(R.id.main_container, AddChildBaseFragment())
                                            .addToBackStack(null).commitAllowingStateLoss()*//*
                                true
                            }
                            else -> { //no action for add or others in toolbar

                                true
                            }
                        }
                    }*/
                }
            }
            else if (mCurrentUserType.equals("Provider")){

                mNavigationView.menu.getItem(0).setVisible(false) //hide "Browse All Daycares" in navdrawer
                mToolbar.menu.getItem(0).setVisible(false) //hide add button in toolbar
                mToolbar.menu.getItem(2).setVisible(false) //hide chat button in toolbar
                try {
                    mNavigationView.setNavigationItemSelectedListener { menuItem ->
                        menuItem.isChecked = true
                        // close drawer when item is tapped
                        when (menuItem.itemId) { //nav drawer options
                            nav_signout -> {
                                signOut()
                            }
                            else -> {
                            }
                        }
                        menuItem.isChecked = false
                        mDrawerLayout.closeDrawers()
                        false
                    }}catch (e: Exception){}

            }
            //Provider toolbar menu handling in ProviderMainFragment (for search functionality)
            //if provider, hide "search public daycares" option


        }
        // use valueEventListener to get the value of the user's usertype (base or caregiver) from Firebase and load appropriate fragment
        var valueEventListener = object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }
            override fun onDataChange(p0: DataSnapshot) {
                val data = p0.getValue<UserData>(UserData::class.java)
                mCurrentUserType = data!!.usertype
                Log.i("MainActivity","onDataChange Data: " + data.usertype + mCurrentUserType)
                //

                if (mCurrentUserType.equals("Base") && mToolbar.menu.hasVisibleItems())
                    mToolbar.menu.getItem(1).setVisible(false)
                if (mCurrentUserType.equals("Provider") && mToolbar.menu.hasVisibleItems()) {//if provider, hide "search public daycares" option
                    mNavigationView.menu.getItem(0).setVisible(false)
                    mToolbar.menu.getItem(0).setVisible(false) //hide add button in toolbar
                    mToolbar.menu.getItem(2).setVisible(false) //hide chat button in toolbar
                }
                /*
                try {
                mNavigationView.setNavigationItemSelectedListener { menuItem ->
                    menuItem.isChecked = true
                    // close drawer when item is tapped
                    when (menuItem.itemId) { //nav drawer options
                        nav_add -> {
                            Log.i("wow","currentusertype: " + mCurrentUserType)
                            if (mCurrentUserType.equals("Base")) {
                                mNavigationView.menu.getItem(0).setVisible(false)
                                supportFragmentManager.beginTransaction()
                                        .replace(R.id.main_container, SearchDaycaresFragment()).addToBackStack(null).commit()
                            }
                        }
                        nav_signout -> {
                            signOut()
                        }
                        else -> {
                        }
                    }
                    menuItem.isChecked = false
                    mDrawerLayout.closeDrawers()
                    false
                }}catch (e: Exception){}
                //
                */
                if (mCurrentUserType.equals("Base")) {
                    var hasChildren = false
                    val userUid = FirebaseAuth.getInstance().currentUser!!.uid
                    val childrenRef = FirebaseDatabase.getInstance().reference.child("/users/$userUid/children")
                    Log.i("wow","childrenRef: " + childrenRef)
                    childrenRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(p0: DataSnapshot) {
                            hasChildren = p0.hasChildren()
                            Log.i("wow","hasChildren onDataChange: " + hasChildren)
                            if (hasChildren) { //only display ViewPager Fragment if there is at least one child belonging to base user
                                try {
                                    Log.i("wow","MainActivityonDataChange fragmentinflate, fragment: " + supportFragmentManager.fragments[0].toString())
                                    //supportFragmentManager.beginTransaction().replace(R.id.main_container, BaseMainFragment.newInstance(currentUserUid!!)).commitAllowingStateLoss()

                                } catch (e: IndexOutOfBoundsException){
                                    fetchAllBaseChildIds() // will populate mChildren
                                    //supportFragmentManager.beginTransaction().add(R.id.main_container, BaseMainFragment.newInstance(currentUserUid!!)).commitAllowingStateLoss()
                                }
                            }
                            else {
                                mToolbar.menu?.getItem(0)?.setVisible(false)
                                mToolbar.menu?.getItem(1)?.setVisible(false)
                                mToolbar.menu?.getItem(2)?.setVisible(false)
                                supportFragmentManager.beginTransaction().add(R.id.main_container, AddChildBaseFragment()).commitAllowingStateLoss()
                                fetchAllBaseChildIds()
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }
                    })

                }

                else if (mCurrentUserType.equals("Provider"))
                    supportFragmentManager.beginTransaction().add(R.id.main_container,ProviderMainFragment()).commit()
            }
        }
        currentUserRef.addValueEventListener(valueEventListener)

        //toolbar setup
        setSupportActionBar(findViewById(R.id.myToolbar))
        val appBar = supportActionBar
        appBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp)
        }

        appBar!!.title = "CareComms"

        //Display app icon in toolbar
        appBar.setDisplayShowHomeEnabled(true)
        //appBar.setLogo(R.mipmap.appicon2)
        appBar.setDisplayUseLogoEnabled(true)

        try {
            currentFragmentStr = supportFragmentManager.fragments[supportFragmentManager.fragments.count()-1].toString()
        } catch (e: IndexOutOfBoundsException){
            currentFragmentStr = ""
        }

       /* try { //initial nav drawer setup
            mNavigationView.setNavigationItemSelectedListener { menuItem ->
                menuItem.isChecked = true
                // close drawer when item is tapped
                when (menuItem.itemId) { //nav drawer options
                    nav_add -> {
                        Log.i("wow","currentusertype: " + mCurrentUserType)
                        if (mCurrentUserType.equals("Base")) {
                            mNavigationView.menu.getItem(0).setVisible(false)
                            supportFragmentManager.beginTransaction()
                                    .replace(R.id.main_container, SearchDaycaresFragment()).addToBackStack(null).commit()
                        }
                    }
                    nav_signout -> {
                        signOut()
                    }
                    else -> {
                    }
                }
                menuItem.isChecked = false
                mDrawerLayout.closeDrawers()
                false
            }}catch (e: Exception){}*/

        mToolbar.setOnMenuItemClickListener {//initial toolbar setup
            when(it.itemId){
                R.id.action_addchild -> {
                    if (mCurrentUserType.equals("Base"))
                        supportFragmentManager.beginTransaction().replace(R.id.main_container,AddChildBaseFragment()).addToBackStack(null).commitAllowingStateLoss()
                    true
                }
                R.id.action_chat -> {
                    mView = supportFragmentManager.fragments[0].view
                    val popup = PopupMenu(this!!, mView)
                    for (service: Service in mDaycares){
                        popup.menu.add(service.daycareName)
                    }
                    popup.setOnMenuItemClickListener {
                        for (child: ChildData in mChildrenData)
                            for (service: Service in mDaycares)
                                if (child.enrolled_daycare_id.equals(service.daycareUID))
                                    openChatDetailFragment(child.child_id)
                        true
                    }
                    try {
                        val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
                        fieldMPopup.isAccessible = true
                        val mPopup = fieldMPopup.get(popup)
                        mPopup.javaClass
                                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                                .invoke(mPopup, true)
                    }catch(e: Exception) { Log.i("wow",e.toString())
                    }finally {
                        popup.show()
                    }
                    true
                }

                else -> { true}
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        if (menu?.findItem(R.id.action_addchild) == null)
            inflater.inflate(R.menu.user_base_menu,menu)
        mNavigationView = findViewById(R.id.nav_view)

        return super.onCreateOptionsMenu(menu)
    }

     fun fetchAllBaseChildIds(){
         val currentBaseUserUid = FirebaseAuth.getInstance().currentUser!!.uid
        var childrenRef = FirebaseDatabase.getInstance().reference.child("users/" + currentBaseUserUid + "/children")
        childrenRef.addChildEventListener(object: ChildEventListener {
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {
            }
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                Log.i("BaseMainFragment","onChildAdded: " + p0.key)
                val data = p0.value as String
                mChildren.add(data!!)
                getChildFromMongo(data!!)
                //might need to do a conditional with add or replace as a result
                supportFragmentManager.beginTransaction().replace(R.id.main_container, BaseMainFragment.newInstance(FirebaseAuth.getInstance().uid!!)).commitAllowingStateLoss()
            }
            override fun onChildRemoved(p0: DataSnapshot) {
            }
            override fun onCancelled(p0: DatabaseError) {
            }
        })
    }
    /*//Checks if user has children. Useful for seeing whether to load ViewPager or AddChild Fragment
    private fun hasChildren(): Boolean {
        var hasChildren = false
        val userUid = FirebaseAuth.getInstance().currentUser!!.uid
        val childrenRef = FirebaseDatabase.getInstance().reference.child("/users/$userUid/children")
        Log.i("wow","childrenRef: " + childrenRef)
        childrenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(p0: DataSnapshot) {
                hasChildren = p0.hasChildren()
                Log.i("wow","hasChildren onDataChange: " + hasChildren)
            }

            override fun onCancelled(p0: DatabaseError) {
            }
        })

    }*/

    private fun signOut(){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        //

        val headerView = nav_view.getHeaderView(0)
        val profileName = headerView.findViewById<TextView>(R.id.profName)
        val profileEmail = headerView.findViewById<TextView>(R.id.profEmail)
        val profileImage = headerView.findViewById<ImageView>(R.id.profImg)

        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val profileRef = FirebaseDatabase.getInstance().reference.child("users").child(firebaseUser!!.uid)
        profileRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }
            //for nav drawer
            override fun onDataChange(p0: DataSnapshot) {
                if (p0 != null) {
                    profileEmail.text = p0.child("useremail").value.toString()
                    profileName.text = p0.child("fullname").value.toString()
                    if (!p0.child("profileImageUrl").value.toString().equals("")) //if there is an image to load
                        Picasso.get().load(p0.child("profileImageUrl").value.toString()).into(profileImage)
                }
            }
        })
    }

    //menu expands nav drawer
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                mDrawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun openChatDetailFragment(otherUserUid: String){
        val transaction = supportFragmentManager.beginTransaction()
        val new = ChatDetailFragment.newInstance(otherUserUid)
        transaction.replace(R.id.main_container, new)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    fun updateChildInMongo(child: ChildData, daycareUid: String){
        try {
            val requestQueue = Volley.newRequestQueue(this)
            val URL = "https://care-comms-android.herokuapp.com/api/child/update"
            val jsonBody = JSONObject()
            jsonBody.put("child_id", child.child_id)
            jsonBody.put("age", child.age)
            jsonBody.put("enrolled_daycare_id", daycareUid)
            jsonBody.put("full_name",child.full_name)
            jsonBody.put("gender",child.gender)
            jsonBody.put("guardian_id",child.guardian_id)
            jsonBody.put("health_info",child.health_info)
            jsonBody.put("nickname",child.nickname)
            val mRequestBody = jsonBody.toString()

            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener { response -> Log.i("LOG_VOLLEY", response) }, Response.ErrorListener { error -> Log.e("LOG_VOLLEY", error.toString()) }) {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }
                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray? {
                    try {
                        return mRequestBody?.toByteArray(charset("utf-8"))
                    } catch (uee: UnsupportedEncodingException) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8")
                        return null
                    }
                }
                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    var responseString = ""
                    if (response != null) {
                        responseString = response.statusCode.toString()
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response))
                }
            }

            requestQueue.add(stringRequest)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getChildFromMongo(childId: String) {
        val getURL = "https://care-comms-android.herokuapp.com/api/child/" + childId
        val queue = Volley.newRequestQueue(this)
        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET, getURL, null,
                Response.Listener { response ->
                    val responseChild = response.toString().trimIndent()
                    var kid: ChildData = Gson().fromJson(responseChild, ChildData::class.java)
                    mChildrenData.add(kid)
                    //populate daycare list from firebase
                    FirebaseDatabase.getInstance().reference.child("services").addChildEventListener(object : ChildEventListener {
                        override fun onChildMoved(p0: DataSnapshot, p1: String?) {
                        }

                        override fun onChildChanged(p0: DataSnapshot, p1: String?) {
                        }

                        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                            val data = p0.getValue<Service>(Service::class.java)
                            if (data!!.daycareUID.equals(kid.enrolled_daycare_id))
                                mDaycares.add(data)
                        }

                        override fun onChildRemoved(p0: DataSnapshot) {
                        }

                        override fun onCancelled(p0: DatabaseError) {
                        }

                    })
                    //
                    Log.i("wow", "kid: " + kid)
                },
                Response.ErrorListener { error ->
                })
        queue.add(jsonObjectRequest)
    }


    fun addChildToMongo(){
        try {
            val requestQueue = Volley.newRequestQueue(this)
            val URL = "https://care-comms-android.herokuapp.com/api/child"
            val jsonBody = JSONObject()
            jsonBody.put("child_id", UUID.randomUUID().toString())
            jsonBody.put("age", 5)
            jsonBody.put("enrolled_daycare_id", "123123b")
            jsonBody.put("full_name","Joe Stanley")
            jsonBody.put("gender","male")
            jsonBody.put("guardian_id","123123a")
            jsonBody.put("health_info","diabetes")
            jsonBody.put("nickname","Joeee")
            val mRequestBody = jsonBody.toString()

            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener { response -> Log.i("LOG_VOLLEY", response) }, Response.ErrorListener { error -> Log.e("LOG_VOLLEY", error.toString()) }) {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }
                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray? {
                    try {
                        return mRequestBody?.toByteArray(charset("utf-8"))
                    } catch (uee: UnsupportedEncodingException) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8")
                        return null
                    }
                }
                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    var responseString = ""
                    if (response != null) {
                        responseString = response.statusCode.toString()
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response))
                }
            }

            requestQueue.add(stringRequest)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun updateChildInMongo(){
        try {
            val requestQueue = Volley.newRequestQueue(this)
            val URL = "https://care-comms-android.herokuapp.com/api/child/update"
            val jsonBody = JSONObject()
            jsonBody.put("child_id", "64933179-a698-4b26-94fe-7c2902d4dc53")
            jsonBody.put("age", 5)
            jsonBody.put("enrolled_daycare_id", "123123b")
            jsonBody.put("full_name","Joe Stanley")
            jsonBody.put("gender","male")
            jsonBody.put("guardian_id","123123a")
            jsonBody.put("health_info","Cancer")
            jsonBody.put("nickname","Joeee")
            val mRequestBody = jsonBody.toString()

            val stringRequest = object : StringRequest(Request.Method.POST, URL, Response.Listener { response -> Log.i("LOG_VOLLEY", response) }, Response.ErrorListener { error -> Log.e("LOG_VOLLEY", error.toString()) }) {
                override fun getBodyContentType(): String {
                    return "application/json; charset=utf-8"
                }
                @Throws(AuthFailureError::class)
                override fun getBody(): ByteArray? {
                    try {
                        return mRequestBody?.toByteArray(charset("utf-8"))
                    } catch (uee: UnsupportedEncodingException) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", mRequestBody, "utf-8")
                        return null
                    }
                }
                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    var responseString = ""
                    if (response != null) {
                        responseString = response.statusCode.toString()
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response))
                }
            }

            requestQueue.add(stringRequest)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
}
