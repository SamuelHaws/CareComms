import android.app.Activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_signup.*
import android.support.v4.app.Fragment
import android.widget.*
import kotlinx.android.synthetic.main.fragment_signup_careprovider.*
import samuelhaws.carecomms.AddServiceFragment
import samuelhaws.carecomms.MainActivity
import samuelhaws.carecomms.R
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class SignupCareProviderFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null
    private var spinner: Spinner? = null
    var toAdd: Boolean = false
    var options = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Auth","CareProviderSignup")
        arguments?.let{
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        //set up spinner
        options = arrayListOf("I will be added to an existing daycare.","I manage a daycare and would like to add it.")


    }

    //store value of spinner option, will determine if we create new daycare service upon successful provider registration
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        toAdd = !options[position].contains("existing")
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup_careprovider,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinner = this.spinner_register
        //spinner!!.setOnItemSelectedListener....
        val aa = ArrayAdapter(this.context,android.R.layout.simple_spinner_item,options)
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner!!.adapter = aa

        if(!param1!!.isEmpty())
            email_register_provider.text = Editable.Factory.getInstance().newEditable(param1)
        if(!param2!!.isEmpty())
            password_register_provider.text = Editable.Factory.getInstance().newEditable(param2)

        register_button_provider.setOnClickListener {
            performRegister()
        }

        already_have_account_provider.setOnClickListener{
            listener!!.onSignInRoutine()
        }

        selectphoto_button_provider.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver,selectedPhotoUri)
            selectphoto_imageview_provider.setImageBitmap(bitmap)
            selectphoto_button_provider.alpha = 0f
        }
    }

    private fun performRegister() {
        val email = email_register_provider.text.toString()
        val password = password_register_provider.text.toString()
        val name = username_register_provider.text.toString()

        if (!spinner!!.selectedItem.toString().contains("existing"))
            toAdd = true

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()){
            Toast.makeText(context, "Please fill in required fields.", Toast.LENGTH_SHORT).show()
            return
        }

       /* if (selectedPhotoUri == null){
            Toast.makeText(context,"Please select your profile image",Toast.LENGTH_SHORT).show()
            return
        }*/


        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener {
                    if (!it.isSuccessful)
                        return@addOnCompleteListener
                    //else if succesfsful
                    uploadImageToFirebaseStorage()
                }
                .addOnFailureListener {
                    Toast.makeText(context,"Failed to create user ${it.message}",Toast.LENGTH_SHORT).show()
                }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) {
            saveProviderToFirebaseDatabase("")
            return
        }

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        saveProviderToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                }
    }

    private fun saveProviderToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = UserData(uid, username_register_provider.text.toString(),email_register_provider.text.toString(),"Provider",profileImageUrl,"")
        ref.setValue(user)
                .addOnSuccessListener {
                    if(toAdd)
                        fragmentManager!!.beginTransaction().replace(R.id.login_container, AddServiceFragment.newInstance()).commit()
                    else {
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK.or(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        startActivity(intent)
                    }

                }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        }
        else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onSignInRoutine()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                SignupCareProviderFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}