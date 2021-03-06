package com.example.collaboard_android.calendar.ui

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.collaboard_android.R
import com.example.collaboard_android.calendar.adapter.DeadlineAdapter
import com.example.collaboard_android.calendar.adapter.DeadlineDTO
import com.example.collaboard_android.databinding.ActivityCalendarBinding
import com.example.collaboard_android.util.SharedPreferenceController
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.format.MonthArrayTitleFormatter
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import java.util.*
import kotlin.collections.ArrayList

class CalendarActivity : AppCompatActivity() {

    private var deadlineList = arrayListOf<DeadlineDTO>()

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var deadline: DatabaseReference

    private var day: Int = Calendar.getInstance().get(Calendar.DATE)
    private var month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var year: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var lastDay: Int = 0

    private var currentBoard = ""

    val calList = ArrayList<CalendarDay>()
    var state: Boolean = true

    val database: FirebaseDatabase = FirebaseDatabase.getInstance() // 파이어베이스 데이터베이스 연동

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            ActivityCalendarBinding.inflate(layoutInflater) // inflate 메소드를 호출하여 Binding Class 변수 초기화
        val view = binding.root
        setContentView(view) // binding 변수의 root 뷰를 가져와서 setContentView 메소드의 인자로 전달

        hideTodoList()

        // 백버튼 클릭
        binding.imgbtnBackCalendar.setOnClickListener {
            finish()
        }

        // 현재 달의 말일 계산
        lastDay = getLastDay(year, month, day)

        currentBoard = intent.getStringExtra("boardCode").toString()

        getAllDeadline() // firebase에서 선택한 달의 모든 deadline 불러옴

        deadline =
            database.getReference("users/${SharedPreferenceController.getUid(this)}/deadline/$currentBoard") // DB 테이블 연결

        binding.rvCalendarDeadline.adapter = DeadlineAdapter(this, deadlineList)
        binding.rvCalendarDeadline.layoutManager = LinearLayoutManager(this)
        binding.rvCalendarDeadline.setHasFixedSize(true) // recyclerview 크기 고정

        // slindingPanel 바깥쪽을 클릭한 경우 panel 내려감
        binding.splDeadline.setFadeOnClickListener {
            binding.splDeadline.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        }

        binding.splDeadline.addPanelSlideListener(object :
            SlidingUpPanelLayout.PanelSlideListener {

            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                binding.rvCalendarDeadline.scheduleLayoutAnimation() // item 애니메이션
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
            }
        })

        // today 날짜 색상 적용
        binding.mcvCalendar.addDecorator(TodayDeco())

        // month 영어로 설정
        binding.mcvCalendar.setTitleFormatter(MonthArrayTitleFormatter(resources.getTextArray(R.array.months_array)))

        // weekdays 영어로 설정
        val locale = Locale("en_US")
        Locale.setDefault(locale)
        val config = Configuration()
        config.locale = locale
        baseContext.applicationContext.resources.updateConfiguration(config, null)

        // 날짜 클릭
        binding.mcvCalendar.setOnDateChangedListener { widget, date, selected ->

            year = date.year
            month = date.month
            day = date.day
            val selectedDate = "$year-$month-$day"

            deadline.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    if (date == CalendarDay.today()) {
                        binding.mcvCalendar.addDecorator(TodayWhiteDeco())
                    } else {
                        binding.mcvCalendar.addDecorator(TodayDeco())
                    }

                    // ArrayList 비워줌
                    deadlineList.clear()

                    // 선택한 날짜와 동일한 date 값을 가진 데이터 불러옴
                    for (postSnapshot in dataSnapshot.children) {
                        if (postSnapshot.child("date").value.toString() == selectedDate) {
                            val item = postSnapshot.getValue(DeadlineDTO::class.java)
                            if (item != null) {
                                deadlineList.add(item)
                            }
                        }
                    }
                    binding.rvCalendarDeadline.adapter?.notifyDataSetChanged()

                    if (deadlineList.isNotEmpty()) {
                        showTodoList()

                    } else {
                        hideTodoList()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
        }

        // month change
        binding.mcvCalendar.setOnMonthChangedListener { widget, date ->
            // 현재 달의 말일 계산
            year = date.year
            month = date.month
            day = date.day
            lastDay = getLastDay(year, month, day)

            getAllDeadline()
        }
    }

    private fun showTodoList() {
        if (!state) {
            val animation: Animation =
                AnimationUtils.loadAnimation(applicationContext, R.anim.item_animation_up)
            binding.linearlayoutDeadline.startAnimation(animation)
        }
        state = true
    }

    private fun hideTodoList() {
        if (state) {
            val animation: Animation =
                AnimationUtils.loadAnimation(applicationContext, R.anim.item_animation_down)
            binding.linearlayoutDeadline.startAnimation(animation)
        }
        state = false
    }

    // 선택한 달의 말일 계산
    private fun getLastDay(year: Int, month: Int, day: Int): Int {
        val lastDay: Calendar = Calendar.getInstance()
        lastDay.set(year, (month - 1), day)

        return lastDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // firebase에서 선택한 달의 모든 deadline 호출
    private fun getAllDeadline() {

        deadline =
            database.getReference("users/${SharedPreferenceController.getUid(this)}/deadline/$currentBoard")

        deadline.orderByChild("date").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                // calList 비워줌
                calList.clear()

                for (postSnapshot in dataSnapshot.children) {
                    for (i in 1..lastDay) {
                        val strDate = "$year-$month-$i"
                        if (postSnapshot.child("date").value.toString() == strDate) {
                            calList.add(CalendarDay.from(year, month, i))
                            break
                        }
                    }
                }
                dotDecoration()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    // 숫자 아래 점
    private fun dotDecoration() {
        for (calDay in calList) {
            binding.mcvCalendar.addDecorator(
                DotDecorator(
                    calDay
                )
            )
        }
    }
}
