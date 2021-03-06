package com.example.collaboard_android.board.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.example.collaboard_android.board.adapter.TaskData
import com.example.collaboard_android.board.adapter.UserInfo
import com.example.collaboard_android.board.adapter.ViewPagerAdapter
import com.example.collaboard_android.calendar.ui.CalendarActivity
import com.example.collaboard_android.databinding.ActivityBoardBinding
import com.example.collaboard_android.issue.ui.IssueActivity
import com.example.collaboard_android.model.NotificationModel
import com.example.collaboard_android.util.SharedPreferenceController
import com.google.firebase.database.*
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

class BoardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardBinding

    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private lateinit var BOARD_NAME: String
    private lateinit var BOARD_CODE: String
    private lateinit var REPO_NAME: String

    private lateinit var TOKEN: String
    private lateinit var UID: String
    private lateinit var USER_NAME: String
    private lateinit var PROFILE_IMG: String
    private lateinit var PUSH_TOKEN: String

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val databaseReference: DatabaseReference = firebaseDatabase.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mContext = this

        initValue()

        setPrefValue()

        getIntentValue()

        clickRepoTitle()

        initIssueButton()

        initCalendarButton()

        initBackButton()

        initViewPager()

        setViewPagerPaging()

        passUserInfoToServer()
    }

    private fun initValue() {
        frag_board_name = ""
        frag_board_code = ""
    }

    private fun setPrefValue() {
        SharedPreferenceController.apply {
            TOKEN = getAccessToken(this@BoardActivity).toString()
            UID = getUid(this@BoardActivity).toString()
            USER_NAME = getUserName(this@BoardActivity).toString()
            PROFILE_IMG = getProfileImg(this@BoardActivity).toString()
            PUSH_TOKEN = getPushToken(this@BoardActivity).toString()
        }
    }

    private fun getIntentValue() {
        val intentFrom = intent.getStringExtra("intentFrom").toString()
        Log.d("getIntentValue", intentFrom)
        when (intentFrom) {
            // ?????? ???????????? > ????????? ?????? ???????????? ??????
            "BoardListActivity" -> {
                setBoardInfo()
            }
            // ??? ????????? ???????????? ???????????? ??????
            "ShowPartCodeDialogFragment" -> {
                setBoardInfo()
            }
            // ??????????????? ???????????? ???????????? ??????
            "ParticipationCode" -> {
                setBoardInfo()
            }
            else -> {
                BOARD_NAME = "error"
                BOARD_CODE = "error"
                REPO_NAME = "error"
            }
        }
        initBoardName()
    }

    private fun setBoardInfo() {
        BOARD_NAME = intent.getStringExtra("boardName").toString()
        BOARD_CODE = intent.getStringExtra("boardCode").toString()
        REPO_NAME = intent.getStringExtra("repoName").toString()
        frag_board_name = BOARD_NAME
        frag_board_code = BOARD_CODE
    }

    private fun initBoardName() {
        binding.tvRepoName.text = BOARD_NAME
    }

    private fun initViewPager() {
        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.fragments = listOf(
            TodoFragment(),
            InProgressFragment(),
            DoneFragment()
        )
        binding.viewpagerBoard.adapter = viewPagerAdapter
        binding.viewpagerBoard.clipToPadding = false
    }

    private fun setViewPagerPaging() {
        val dpValue = 14
        val d: Float = resources.displayMetrics.density
        val margin = (dpValue * d).toInt()
        
        binding.viewpagerBoard.setPadding(margin, 0, margin, 0)
        binding.viewpagerBoard.pageMargin = (margin / 1.7).toInt()
    }

    // board - users??? ?????? user uid ??????
    private fun passUserInfoToServer() {
        val userPath = databaseReference.child("board").child(BOARD_CODE).child("users")
        val userModel = UserInfo(UID, TOKEN, USER_NAME, PROFILE_IMG, PUSH_TOKEN)

        userPath.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map: Map<String, *>? = snapshot.value as Map<String, *>?
                val keySet: Set<String>? = map?.keys
                val list: ArrayList<String> = ArrayList()
                if (keySet != null) {
                    list.addAll(keySet)
                } else {
                    userPath.child(UID).setValue(userModel)
                    increaseMemberCount()
                    return
                }

                var increaseFlag = false
                for (i in 0 until list.size) {
                    // board - users??? ?????? user??? uid??? ?????? ??????
                    if (list[i] == UID) {
                        increaseFlag = false
                        break
                    }
                    // ?????? user??? uid??? ????????? users??? ?????? ????????? ?????? ??????
                    else if (list[i] != UID || map.isNullOrEmpty()) {
                        increaseFlag = true
                    }
                }
                if (increaseFlag) {
                    // board - users??? ?????? user??? uid column ??????
                    userPath.child(UID).setValue(userModel)
                    increaseMemberCount()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun increaseMemberCount() {
        val boardInfoPath = databaseReference.child("board").child(BOARD_CODE).child("info")
        // board - user??? ?????? user??? uid??? ????????? ?????? ??? +1 ??????
       boardInfoPath.addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentCount: Int = (snapshot.child("memberCount").value as Long).toInt()
                        boardInfoPath.child("memberCount").setValue(currentCount + 1)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
    }

    private fun clickRepoTitle() {
        binding.tvRepoName.setOnClickListener {
            val clipboardManager = this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("BOARD_CODE", BOARD_CODE)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(this, "Participation code copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initIssueButton() {
        binding.imgbtnIssue.setOnClickListener {
            val splitString = REPO_NAME.split("/")
            val intent = Intent(this, IssueActivity::class.java)
            intent.putExtra("owner", splitString[0])
            intent.putExtra("repo", splitString[1])
            startActivity(intent)
        }
    }

    private fun initCalendarButton() {
        binding.imgbtnCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            intent.putExtra("boardCode", BOARD_CODE)
            startActivity(intent)
        }
    }

    private fun initBackButton() {
        binding.imgbtnBackBoard.setOnClickListener {
            finish()
        }
    }

    fun getCurrentFrag() : Int {
        return when (binding.viewpagerBoard.currentItem) {
            0 -> 0
            1 -> 1
            else -> 2
        }
    }

    fun movePage(index: Int) {
        val handler = Handler()
        handler.postDelayed({
            binding.viewpagerBoard.post {
                binding.viewpagerBoard.setCurrentItem(index, true)
            }
        }, 500)
    }

    fun putTodoTaskInDatabase(list: MutableList<TaskData>) {
        val recyclerMap = HashMap<String, MutableList<TaskData>>()
        recyclerMap["recyclerArranging"] = list
        databaseReference.child("board").child(BOARD_CODE).child("todo")
                .updateChildren(recyclerMap as Map<String, Any>)
    }

    fun putInProgressTaskInDatabase(list: MutableList<TaskData>) {
        val recyclerMap = HashMap<String, MutableList<TaskData>>()
        recyclerMap["recyclerArranging"] = list
        databaseReference.child("board").child(BOARD_CODE).child("inProgress")
                .updateChildren(recyclerMap as Map<String, Any>)
    }

    fun putDoneTaskInDatabase(list: MutableList<TaskData>) {
        val recyclerMap = HashMap<String, MutableList<TaskData>>()
        recyclerMap["recyclerArranging"] = list
        databaseReference.child("board").child(BOARD_CODE).child("done")
                .updateChildren(recyclerMap as Map<String, Any>)
    }

    private fun getPushMsgContent(startFrag: Int, finFrag: Int, addPosition: Int, addFlag: Boolean) : String {
        var startFragStr = ""
        var finFragStr = ""
        var addPositionStr = ""

        startFragStr = getPositionStr(startFrag)
        finFragStr = getPositionStr(finFrag)
        addPositionStr = getPositionStr(addPosition)

        return if (addFlag)
        // ????????? ?????? ???????????? ??????
            "$USER_NAME added task to $addPositionStr"
        else
        // ????????? ????????? ??????
            "$USER_NAME moved task from $startFragStr to $finFragStr"
    }

    private fun getPositionStr(position: Int) : String {
        return when (position) {
            0 -> "To do"
            1 -> "In progress"
            else -> "Done"
        }
    }

    fun sendPushNotification(startFrag: Int, finFrag: Int, addPosition: Int, addFlag: Boolean) {
        if (startFrag != finFrag) {
            val pushMsg = getPushMsgContent(startFrag, finFrag, addPosition, addFlag)
            Toast.makeText(this, pushMsg, Toast.LENGTH_SHORT).show()

            val userPath = databaseReference.child("board").child(BOARD_CODE).child("users")
            // push ?????? ?????????
            userPath.addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val map: Map<String, *> = snapshot.value as Map<String, *>
                            val keySet: Set<String> = map.keys
                            val list: ArrayList<String> = ArrayList()
                            list.addAll(keySet)

                            for (i in 0 until list.size) {
                                if (list[i] == UID)
                                    continue
                                else if (list[i].isNotEmpty()) {
                                    userPath.child(list[i]).addListenerForSingleValueEvent(object: ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            sendFcm(snapshot.child("pushToken").value.toString(), pushMsg)
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                                }
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
        }
    }

    private fun sendFcm(pushToken: String, pushMsg: String) {
        val gson = Gson()
        val notificationModel = NotificationModel()

        // background push
        notificationModel.apply {
            to = pushToken
            notification.title = "COLLABOARD"
            notification.text = pushMsg
        }

        // foreground push
        notificationModel.data.apply {
            title = BOARD_NAME
            text = pushMsg
        }

        val requestBody = RequestBody.create("application/json; charset=utf8".toMediaTypeOrNull(),
                gson.toJson(notificationModel))

        val request = Request.Builder()
                .header("Content-Type", "application/json")
                .addHeader("Authorization", "key=AAAAwuxUFck:APA91bHuao1MBOGFCeSAhTC2ovYXzyu7JjT_8QevbF1lLB_WRv-e1-iWFqQvRqhgwGW9ewOLQibnvBn5bSyvZipOlic4wvLiNH1mBZeHzcN6lyN95xUQBXyz_UtpUprW7OjSO--6X2xB")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .build()

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("okhttptest", e.message.toString())
            }
            override fun onResponse(call: Call, response: Response) {
                Log.d("okhttptest", response.message)
            }
        })
    }

    companion object {
        lateinit var mContext: BoardActivity
            private set

        var frag_board_name = ""
        var frag_board_code = ""
    }
}