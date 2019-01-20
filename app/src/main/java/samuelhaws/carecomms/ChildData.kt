package samuelhaws.carecomms

import java.io.Serializable

data class ChildData (
        val child_id : String ,
        val age : Int ,
        val enrolled_daycare_id : String,
        val full_name : String  ,
        val gender : String ,
        val guardian_id : String,
        val health_info : String,
        var nickname : String,
        val image_url : String
):Serializable{
    constructor() : this("",-1,"","","", "","","","")
}