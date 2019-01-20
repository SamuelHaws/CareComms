package samuelhaws.carecomms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_addservice.*
import kotlinx.android.synthetic.main.fragment_signup.*
import samuelhaws.carecomms.MainActivity
import java.util.*

class Service(val daycareUID: String, val daycareOwner: String, val daycareName: String, val daycareDesc: String, val daycareAddr: String, val public: Boolean){
    constructor():this("","","","", "",false)
}

class AddServiceFragment : Fragment() {
    lateinit var create_button: Button
    var daycareUID = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_addservice, container, false)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        create_button = view.findViewById(R.id.button_add)
        // button click event handler!
        create_button.setOnClickListener {
            addService()
        }
    }

    private fun addService(){
        if (daycare_name_add.text.toString().isEmpty() || street_addr_add.text.toString().isEmpty() ||
                city_add.text.toString().isEmpty() || state_add.text.toString().isEmpty() || country_add.text.toString().isEmpty()){
            Toast.makeText(context, "Please fill out all required fields.",Toast.LENGTH_SHORT).show()
            return
        }

        val checked: Boolean = view!!.findViewById<CheckBox>(R.id.addservice_publiccheck).isChecked
        Log.i("wow",checked.toString())
        //add daycareID to user
        val userDaycareRef = FirebaseDatabase.getInstance()
                .getReference("/users/${FirebaseAuth.getInstance().currentUser!!.uid}/daycareID")
        userDaycareRef.setValue(daycareUID)

        //add daycare to services
        val servicesDaycareRef = FirebaseDatabase.getInstance().getReference("/services/$daycareUID")
        val service = Service(daycareUID, FirebaseAuth.getInstance().currentUser!!.uid, daycare_name_add.text.toString(),
                description_add.text.toString(), street_addr_add.text.toString() + " " + city_add.text.toString() + " "
                + state_add.text.toString() + " " + country_add.text.toString(),checked )
        servicesDaycareRef.setValue(service)
                .addOnSuccessListener {
                    Log.i("Auth","ref.setValue succeeded.")
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Log.i("Auth","ref.setValue failed.")
                }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
                AddServiceFragment().apply {
                    arguments = Bundle().apply {

                    }
                }
    }
}
