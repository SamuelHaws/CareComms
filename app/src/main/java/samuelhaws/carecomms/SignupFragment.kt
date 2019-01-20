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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_signup.*
import android.support.v4.app.Fragment
import android.widget.EditText
import com.google.firebase.auth.UserInfo
import samuelhaws.carecomms.MainActivity
import samuelhaws.carecomms.R
import java.util.*

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UserData(val uid: String, val fullname: String, val useremail: String, val usertype: String, val profileImageUrl: String, var daycareID: String){
    constructor():this("","","","","", "")
}

class SignupFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Auth","RegSignup")
        arguments?.let{
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signup,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if(!param1!!.isEmpty())
            email_register.text = Editable.Factory.getInstance().newEditable(param1)
        if(!param2!!.isEmpty())
            password_register.text = Editable.Factory.getInstance().newEditable(param2)

        register_button.setOnClickListener {
            performRegister()
        }

        already_have_account.setOnClickListener{
            listener!!.onSignInRoutine()
        }

        selectphoto_button.setOnClickListener {
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
            selectphoto_imageview.setImageBitmap(bitmap)
            selectphoto_button.alpha = 0f
        }
    }

    private fun performRegister() {
        val email = email_register.text.toString()
        val password = password_register.text.toString()
        val name = username_register.text.toString()

        if (email.isEmpty() || password.isEmpty() || name.isEmpty()){
            Toast.makeText(context, "Please fill in required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener {
                    Log.i("wow","firebaseauth registr oncomplte")
                    uploadImageToFirebaseStorage()
                }
                .addOnFailureListener {
                    Log.i("wow","firebaseauth registr falure")
                    Toast.makeText(context,"Failed to create user ${it.message}",Toast.LENGTH_SHORT).show()
                }
    }

    private fun uploadImageToFirebaseStorage() {
        if (selectedPhotoUri == null) {
            saveUserToFirebaseDatabase("")
            return
        }

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener {
                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener {
                }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val ref = FirebaseDatabase.getInstance().getReference("users/$uid")
        val user = UserData(uid, username_register.text.toString(),email_register.text.toString(),"Base",profileImageUrl,"")
        ref.setValue(user)
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
                SignupFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}