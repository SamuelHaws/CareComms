package samuelhaws.carecomms


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.fragment_login.*
import samuelhaws.carecomms.MainActivity

class LoginFragment: Fragment() {
    private var listener: OnFragmentInteractionListener? = null
    lateinit var mLoginBtn: Button
    lateinit private var mGoogleSignInClient: GoogleSignInClient
    lateinit private var mGoogleApiClient: GoogleApiClient
    lateinit private var mResult: GoogleSignInResult

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {  }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLoginBtn = view.findViewById(R.id.login_button)
        login_button.setOnClickListener {
            performLogin()
        }
        // switch to sign up!
        back_to_register.setOnClickListener {
            view.hideKeyboard() //hide keyboard so popup menu is in correct location
            showPopup(view) //display popup menu
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken,0)
    }
    //taken from Coding in Flow Youtube tutorial
    private fun showPopup(v: View) {
        val popup = PopupMenu(context!!, v)
        val menuInflater = popup.menuInflater
        menuInflater.inflate(R.menu.popup_menu_register, popup.menu)
        popup.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.register_parent -> {
                    val email = email_login.text.toString()
                    val password = password_login.text.toString()
                    Log.i("Auth","reg_par click")
                    listener!!.onSignUpRoutine(email,password,0)
                    return@setOnMenuItemClickListener true
                }
                R.id.register_careprovider -> {
                    val email = email_login.text.toString()
                    val password = password_login.text.toString()
                    Log.i("Auth","reg_care click")
                    listener!!.onSignUpRoutine(email,password,1)
                    return@setOnMenuItemClickListener true
                }
                else -> {
                    return@setOnMenuItemClickListener false
                }
            }
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

    private fun performLogin() {
        val email = email_login.text.toString()
        val password = password_login.text.toString()

        if (email.isEmpty() || password.isEmpty()){
            Toast.makeText(context, "Please fill out email/pw",Toast.LENGTH_SHORT).show()
            return
        }

        //firebase auth with username/password
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Log.i("wow", "Login success: as ${it.result!!.user.uid}")
                        val intent = Intent(context, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
                }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        fun onSignUpRoutine(email: String, passwd: String, accountType: Int)
        //fun onSignInRoutine()
    }
}