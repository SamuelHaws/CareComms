package samuelhaws.carecomms

import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.w3c.dom.Text
import samuelhaws.carecomms.BaseMainFragment
import samuelhaws.carecomms.ChildData

class ChildDetailFragment: Fragment() {
    var mChild: ChildData? = null

    companion object {
        fun newInstance(child_id: String, pagerPosition: Int?): ChildDetailFragment {
            val args: Bundle = Bundle()
            args.putString("id",child_id)
            if (pagerPosition != null)
                args.putInt("pagerPosition",pagerPosition!!)

            val fragment = ChildDetailFragment()
            fragment.setArguments(args)
            return fragment
        }
    }

    fun getChildFromMongo(childId: String) {
        val getURL = "https://care-comms-android.herokuapp.com/api/child/" + childId
        val queue = Volley.newRequestQueue(this.context)
        var jsonObjectRequest = JsonObjectRequest(Request.Method.GET,getURL,null,
                Response.Listener { response ->
                    val responseChild = response.toString().trimIndent()
                    var child:  ChildData = Gson().fromJson(responseChild,ChildData::class.java)
                    mChild = child
                    val imgURL = child.image_url
                    //(this.parentFragment as BaseMainFragment).mViewPager.adapter!!.notifyDataSetChanged()
                    val tabTitle = (mChild as ChildData).nickname
                    Log.i("wow","detailgetChild tabTitle" + tabTitle)
                    //populate fragment info
                    if (view?.findViewById<TextView>(R.id.child_nickname) != null){
                        var imageView: ImageView = view!!.findViewById(R.id.child_imgView)

                        if (!imgURL.equals("")) { //if there is an image to load
                            Log.i("wow", "imgURL: " + imgURL)
                            Picasso.get().load(child.image_url).into(imageView)
                        }
                        var textview_nickname: TextView = view!!.findViewById(R.id.child_nickname)
                        textview_nickname.text = mChild?.nickname
                        var textview_full_name: TextView = view!!.findViewById(R.id.child_full_name)
                        textview_full_name.text = mChild?.full_name
                        var textview_health_info: TextView = view!!.findViewById(R.id.child_health_info)
                        textview_health_info.text = mChild?.health_info

                        Log.i("wow","detailoNViewCreated full_name: " + textview_full_name.text)
                        Log.i("wow", "childdetail view: " + this.parentFragment)
                        Log.i("wow","bool view!!.findViewById<TabLayout>(R.id.viewpager_tablayout_base): " + (view!!.findViewById<TabLayout>(R.id.viewpager_tablayout_base) == null))
                        (this.parentFragment as BaseMainFragment).mTabLayout.getTabAt(arguments!!.getInt("pagerPosition"))?.text = tabTitle
                        //view!!.findViewById<TabLayout>(R.id.viewpager_tablayout_base).getTabAt(arguments!!.getInt("pagerPosition"))?.text = tabTitle
                    }
                },
                Response.ErrorListener { error ->
                })
        queue.add(jsonObjectRequest)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_childdetail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view!!, savedInstanceState)
        Log.i("wow","childdetail onViewCreated")
        getChildFromMongo(arguments!!.getString("id"))


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("wow","childdetail onCreate")
        super.onCreate(savedInstanceState)
    }
}