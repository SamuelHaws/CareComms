package samuelhaws.carecomms

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.fragment_addchild.*
import kotlinx.android.synthetic.main.fragment_addservice.*
import kotlinx.android.synthetic.main.fragment_signup.*
import org.json.JSONException
import org.json.JSONObject
import samuelhaws.carecomms.R.id.*
import java.io.UnsupportedEncodingException
import java.util.*



class AddChildBaseFragment : Fragment() {
    lateinit var mCreate_button: Button
    lateinit var mImageButton: Button
    var daycareUID = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_addchild, container, false)

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCreate_button = view.findViewById(R.id.button_addchild)
        // button click event handler!
        mCreate_button.setOnClickListener {
            addChild()
        }

        mImageButton = view.findViewById(R.id.selectphoto_button_child)

        mImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    private fun addChild(){
        if (fullname_childadd.text.toString().isEmpty() || nickname_childadd.text.toString().isEmpty() ||
                age_childadd.text.toString().isEmpty() || gender_childadd.text.toString().isEmpty() || healthinfo_childadd.text.toString().isEmpty()){
            Toast.makeText(context, "Please fill out all required fields.",Toast.LENGTH_SHORT).show()
            return
        }

        val newChildUid = UUID.randomUUID().toString()

        val userUid = FirebaseAuth.getInstance().currentUser!!.uid
        var filename: String = ""
        if (selectedPhotoUri != null) {
            filename = UUID.randomUUID().toString()
            val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
            ref.putFile(selectedPhotoUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener {
                            Log.i("wow","Success!")
                            filename = it.toString()
                            val ref = FirebaseDatabase.getInstance().getReference("/users/$userUid/children/$newChildUid")
                            val child = ChildData(newChildUid, age_childadd.text.toString().toInt(), "",
                                    fullname_childadd.text.toString(), gender_childadd.text.toString(), userUid, healthinfo_childadd.text.toString(),
                                    nickname_childadd.text.toString(), filename)
                            //add to mongoDB
                            addChildToMongo(child, userUid)
                            //add to firebase under parent/guardian, key is Uid, value is imageURL (Firebase Storage)

                            ref.setValue(newChildUid)
                        }

                    }
        }
        else {
            val ref = FirebaseDatabase.getInstance().getReference("/users/$userUid/children/$newChildUid")
            val child = ChildData(newChildUid, age_childadd.text.toString().toInt(), "",
                    fullname_childadd.text.toString(), gender_childadd.text.toString(), userUid, healthinfo_childadd.text.toString(),
                    nickname_childadd.text.toString(), filename)
            //add to mongoDB
            addChildToMongo(child, userUid)
            //add to firebase under parent/guardian, key is Uid, value is imageURL (Firebase Storage)

            ref.setValue(newChildUid)
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver,selectedPhotoUri)
            selectphoto_imageview_child.setImageBitmap(bitmap)
            selectphoto_button_child.alpha = 0f
        }
    }

    private fun uploadImageToFirebaseStorage(filename: String) {
        if (selectedPhotoUri == null)
            return
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(selectedPhotoUri!!)
    }

    fun addChildToMongo(childToAdd: ChildData, currentUserUid: String){
        try {
            val requestQueue = Volley.newRequestQueue(this.context)
            val URL = "https://care-comms-android.herokuapp.com/api/child"
            val jsonBody = JSONObject()
            jsonBody.put("child_id", childToAdd.child_id)
            jsonBody.put("age", childToAdd.age)
            jsonBody.put("enrolled_daycare_id", "")
            jsonBody.put("full_name",childToAdd.full_name)
            jsonBody.put("gender",childToAdd.gender)
            jsonBody.put("guardian_id",currentUserUid)
            jsonBody.put("health_info",childToAdd.health_info)
            jsonBody.put("nickname",childToAdd.nickname)
            jsonBody.put("image_url",childToAdd.image_url)
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
