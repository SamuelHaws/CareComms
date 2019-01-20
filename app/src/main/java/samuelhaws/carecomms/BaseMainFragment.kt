package samuelhaws.carecomms

import android.app.Activity
import android.content.Context
import android.support.v4.app.FragmentManager
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_base_main.*
import kotlinx.android.synthetic.main.fragment_login.*
import samuelhaws.carecomms.ChildDetailFragment
import samuelhaws.carecomms.MainActivity
import samuelhaws.carecomms.R.id.viewpager_tablayout_base

class BaseMainFragment: Fragment() {
    lateinit var mGuardianUid: String //holds current user's Uid, passed by MainActivity
    lateinit var mTabLayout: TabLayout
    var fragmChildren: ArrayList<String> = arrayListOf() //holds all childdata for current user
    lateinit var mViewPager: ViewPager

    companion object {
        fun newInstance(uid: String): BaseMainFragment {
            val args: Bundle = Bundle()
            args.putString("UID",uid)
            val fragment = BaseMainFragment()
            fragment.setArguments(args)
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("wow","BaseMainFragment onCreate")
        mGuardianUid = arguments!!.getString("UID")
        fragmChildren = (activity as MainActivity).mChildren
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_base_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewPager = view.findViewById(R.id.viewpager_main_base)
        mTabLayout = view.findViewById(R.id.viewpager_tablayout_base)

        val fragmentAdapter = ChildPagerAdapter(childFragmentManager!!,fragmChildren)
        fragmentAdapter.notifyDataSetChanged()
        Log.i("wow","onViewCreated mChildren: " + fragmChildren)
        mViewPager.adapter = fragmentAdapter
        mTabLayout.setupWithViewPager(mViewPager)
    }

    override fun onStart() {
        super.onStart()
        mViewPager.adapter!!.notifyDataSetChanged()
    }
}


class ChildPagerAdapter(fm: FragmentManager, childUids: ArrayList<String>) : FragmentPagerAdapter(fm) {
    val childUids = childUids


    override fun getItem(position: Int): Fragment? {
        Log.i("wow","adapter varchildren: " + childUids)
        return ChildDetailFragment.newInstance(childUids[position],position)
    }

    override fun getCount(): Int {
        return childUids.size
    }

    override fun getPageTitle(position: Int): CharSequence? {

        return ""//childUids[position]
    }


}