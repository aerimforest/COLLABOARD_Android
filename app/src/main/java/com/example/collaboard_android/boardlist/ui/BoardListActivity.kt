package com.example.collaboard_android.boardlist.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputFilter
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.collaboard_android.board.ui.BoardActivity
import com.example.collaboard_android.boardlist.adapter.BoardListAdapter
import com.example.collaboard_android.boardlist.adapter.BoardListData
import com.example.collaboard_android.databinding.ActivityBoardListBinding
import com.example.collaboard_android.setting.SettingActivity
import com.example.collaboard_android.util.ItemClickListener
import com.example.collaboard_android.util.SharedPreferenceController
import com.google.firebase.database.*
import com.example.collaboard_android.R
import com.example.collaboard_android.inbox.ui.InboxActivity
import com.example.collaboard_android.util.hideKeyboard
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class BoardListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBoardListBinding

    private lateinit var boardListAdapter: BoardListAdapter

    var partCodeList: ArrayList<String> = ArrayList()
    var boardList: MutableList<BoardListData> = mutableListOf()

    private lateinit var currentDate: Calendar

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val databaseReference: DatabaseReference = firebaseDatabase.reference

    private lateinit var USER_NAME: String
    private lateinit var UID: String
    private lateinit var PROFILE_IMG: String

    private var height = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBoardListBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setPrefValue()

        setCurrentDateOnTextView()

        initUserProfile()

        capitalizePartCode()

        goToSettingActivity()

        goToInboxActivity()

        setClickListenerOnAddBtn()

        setKeyListenerOnEditText()
    }

    override fun onResume() {
        super.onResume()

        initValue()

        initRecyclerView()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        height = binding.constraintlayoutBottom.height
    }

    private fun initValue() {
        binding.etParticipationCode.text.clear()
    }

    private fun setPrefValue() {
        SharedPreferenceController.apply {
            USER_NAME = getUserName(this@BoardListActivity).toString()
            UID = getUid(this@BoardListActivity).toString()
            PROFILE_IMG = getProfileImg(this@BoardListActivity).toString()
        }
    }

    private fun initUserProfile() {
        // ????????? ?????? ??????
        val nameString = StringBuilder("Hi, ")
        nameString.append(USER_NAME)
        binding.tvName.text = nameString

        // ????????? ????????? ????????? ??????
        Glide.with(this)
            .load(PROFILE_IMG)
            .into(binding.imgProfile)
    }

    private fun capitalizePartCode() {
        val inputFilter = InputFilter.AllCaps()
        binding.etParticipationCode.filters = arrayOf(inputFilter)
    }

    private fun goToSettingActivity() {
        binding.imgProfile.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            intent.putExtra("height", height)
            startActivity(intent)
            overridePendingTransition(R.anim.horizontal_left_in, R.anim.horizontal_right_out)
        }
    }

    private fun goToInboxActivity() {
        binding.imgbtnNotification.setOnClickListener {
            val intent = Intent(this, InboxActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initRecyclerView() {
        boardListAdapter = BoardListAdapter(this)
        binding.recyclerviewBoardList.adapter = boardListAdapter
        binding.recyclerviewBoardList.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false)

        getPartCodeList()

        initItemClickListener()
    }

    private fun getPartCodeList() {
        partCodeList.clear()
        boardList.clear()
        databaseReference.child("users").child(UID).child("boardlist")
                .addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val map: Map<String, *>? = snapshot.value as Map<String, *>?
                        val keySet: Set<String>? = map?.keys
                        if (keySet != null) {
                            partCodeList.addAll(keySet)
                        }

                        for (i in 0 until partCodeList.size) {
                            if (partCodeList.isNotEmpty()) {
                                databaseReference.child("board").child(partCodeList[i]).child("info")
                                        .addListenerForSingleValueEvent(object: ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val boardName = snapshot.child("boardName").value.toString()
                                                val memberCount = snapshot.child("memberCount").value.toString()
                                                val repoName = snapshot.child("repo").value.toString()
                                                boardList.add(BoardListData(boardName, getMemberCountStr(memberCount), repoName))
                                                boardListAdapter.data = boardList
                                                boardListAdapter.notifyDataSetChanged()
                                            }
                                            override fun onCancelled(error: DatabaseError) {}
                                        })
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
    }

    private fun getMemberCountStr(memberCount: String) : String {
        val memberCountInt = memberCount.toInt()
        return if (memberCountInt <= 1) {
            "$memberCountInt member"
        } else {
            "$memberCountInt members"
        }
    }

    private fun initItemClickListener() {
        boardListAdapter.setItemClickListener(object: ItemClickListener {
            override fun onClickItem(view: View, position: Int) {
                val intent = Intent(this@BoardListActivity, BoardActivity::class.java)
                intent.putExtra("boardName", boardList[position].boardName)
                intent.putExtra("boardCode", partCodeList[position])
                intent.putExtra("repoName", boardList[position].repo)
                intent.putExtra("intentFrom", "BoardListActivity")
                startActivity(intent)
            }
        })
    }

    private fun setCurrentDateOnTextView() {
        currentDate = Calendar.getInstance()
        val month = currentDate.get(Calendar.MONTH) + 1
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        binding.tvDate.text = getFormedDateString(month, day)
    }

    private fun getFormedDateString(month: Int, day: Int) : String {
        val dayString: String = day.toString()
        val monthString: String = when (month) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            else -> "Dec"
        }
        return "$monthString $dayString"
    }

    private fun setClickListenerOnAddBtn() {
        binding.imgbtnAdd.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            startActivity(intent)
        }
    }

    // ???????????? ?????? ??? ???????????? ?????? or ?????? ?????? ?????? ???
    private fun setKeyListenerOnEditText() {
        binding.etParticipationCode.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard(this, binding.etParticipationCode)
                goToBoardActivity()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun goToBoardActivity() {
        var boardName = ""
        var repoName = ""
        val boardCode = binding.etParticipationCode.text.toString().trim().toUpperCase(Locale.ROOT)

        // ??????????????? ???????????? ?????? ??????
        if (boardCode == "") {
            Toast.makeText(this, "Enter the participation code", Toast.LENGTH_SHORT).show()
        }
        else {
            // info?????? ?????? code??? ???????????? ?????? ?????? ????????????
            databaseReference.child("board").child(boardCode).child("info")
                    .addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // ??????????????? ???????????? ????????? DB??? ?????? ??????
                            if (!snapshot.exists()) {
                                Toast.makeText(this@BoardListActivity,
                                        "Invalid participation code", Toast.LENGTH_SHORT).show()
                            }
                            else {
                                boardName = snapshot.child("boardName").value.toString()
                                repoName = snapshot.child("repo").value.toString()

                                // ?????? ???????????? ?????? ?????? ??????
                                databaseReference.child("users").child(UID)
                                        .child("boardlist").child(boardCode).setValue(boardName)

                                // ???????????? ????????? ?????? BoardActivity.kt??? ??????
                                val intent = Intent(this@BoardListActivity, BoardActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                intent.putExtra("boardName", boardName)
                                intent.putExtra("boardCode", boardCode)
                                intent.putExtra("repoName", repoName)
                                intent.putExtra("intentFrom", "ParticipationCode")
                                startActivity(intent)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
        }
    }
}