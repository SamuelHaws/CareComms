package samuelhaws.carecomms

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity(), LoginFragment.OnFragmentInteractionListener,
        SignupFragment.OnFragmentInteractionListener, SignupCareProviderFragment.OnFragmentInteractionListener {

    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (FirebaseAuth.getInstance().currentUser != null) { //if already signed in
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        supportFragmentManager.beginTransaction().add(R.id.login_container,LoginFragment()).commit()
    }

    override fun onSignUpRoutine(email: String, passwd: String, accountType: Int) {
        if (accountType == 0) {
            Log.i("Auth","accountType 0")
            supportFragmentManager.beginTransaction().replace(R.id.login_container, SignupFragment.newInstance(email, passwd)).commit()
        }
        else if (accountType == 1) {
            Log.i("Auth", "accountType 1")
            supportFragmentManager.beginTransaction().replace(R.id.login_container, SignupCareProviderFragment.newInstance(email, passwd)).commit()
        }
    }

    override fun onSignInRoutine(){
        supportFragmentManager.beginTransaction().replace(R.id.login_container,LoginFragment()).commit()
    }


}