package com.example.collaboard_android.board.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.collaboard_android.R
import com.example.collaboard_android.databinding.DialogAddTaskBinding
import com.example.collaboard_android.util.hideKeyboard
import com.example.collaboard_android.util.showKeyboard
import java.util.*

class AddTaskDialogFragment(val itemClick: (String, Int, IntArray) -> Unit) : DialogFragment() {

    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private var selectLabel = -1

    private lateinit var currentDate: Calendar

    private lateinit var year: NumberPicker
    private lateinit var month: NumberPicker
    private lateinit var date: NumberPicker

    private var selectYear = 0
    private var selectMonth = 0
    private var selectDate = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = DialogAddTaskBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initCloseButton()

        initAddButton()

        setKeyListenerOnEditText()

        initEditImageButton()

        initLabelSpinner()

        initDatePicker()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initEditImageButton() {
        binding.imgbtnEdit.setOnClickListener {
            setEditTextEnabled()
            showKeyboard(context!!)
        }
        binding.etDescription.setOnTouchListener { _, _ ->
            setEditTextEnabled()
            showKeyboard(context!!)
            true
        }
    }

    private fun setEditTextEnabled() {
        binding.etDescription.apply {
            requestFocus()
            isFocusable = true
            isCursorVisible = true
            isCursorVisible = true
            setSelection(this.length())
        }
        binding.viewUnderline.setBackgroundColor(ContextCompat.getColor(context!!, R.color.blue_main))
    }

    private fun setKeyListenerOnEditText() {
        binding.etDescription.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KEYCODE_ENTER || keyCode == EditorInfo.IME_ACTION_DONE) {
                setEditTextDisabled()
                hideKeyboard(context!!, binding.etDescription)
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun setEditTextDisabled() {
        binding.etDescription.apply {
            setTextColor(ContextCompat.getColor(context!!, R.color.black_description))
            clearFocus()
            isCursorVisible = false
        }
        binding.viewUnderline.setBackgroundColor(ContextCompat.getColor(context!!, R.color.gray_divider))
    }

    private fun initLabelSpinner() {
        val item = resources.getStringArray(R.array.label_array)

        val labelAdapter = ArrayAdapter(context!!, R.layout.item_label_spinner, item)
        binding.spinnerLabel.adapter = labelAdapter

        binding.spinnerLabel.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectLabel = when (position) {
                    0 -> 0 // feature
                    1 -> 1 // fix
                    2 -> 2 // network
                    3 -> 3 // refactor
                    4 -> 4 // chore
                    5 -> 5 // style
                    else -> -1
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

        }
    }

    private fun initDatePicker() {
        year = binding.datePicker.year
        month = binding.datePicker.month
        date = binding.datePicker.date

        setDateRange()

        initDateValue()

        setDatePickerMaxValue()

        setDateValue()

        setPickerLimit()

        setListenerOnDatePicker()
    }

    private fun setDateRange() {
        // ?????? ?????? ????????????
        currentDate = Calendar.getInstance()

        // minValue = ?????? ?????? ??????
        year.minValue = currentDate.get(Calendar.YEAR)
        month.minValue = currentDate.get(Calendar.MONTH) + 1
        date.minValue = currentDate.get(Calendar.DAY_OF_MONTH)

        // maxValue = ?????? ?????? ??????
        year.maxValue = currentDate.get(Calendar.YEAR) + 1
        month.maxValue = 12
        date.maxValue = 31
    }

    private fun setDatePickerMaxValue() {
        // year??? ?????? month maxValue ??????
        if (year.value == currentDate.get(Calendar.YEAR)) {
            month.maxValue = 12
        } else {
            month.maxValue = 12
        }

        // month??? ?????? month, date maxValue ??????
        if (month.value == currentDate.get(Calendar.MONTH) + 1) {
            setMonthMax()
        } else {
            setMonthMax()
        }
    }

    private fun initDateValue() {
        year.value = currentDate.get(Calendar.YEAR)
        month.value = currentDate.get(Calendar.MONTH) + 1
        date.value = currentDate.get(Calendar.DAY_OF_MONTH)
    }

    private fun setDateValue() {
        selectYear = year.value
        selectMonth = month.value
        selectDate = date.value
    }

    private fun setPickerLimit() {
        // ?????? ????????? ??????
        year.wrapSelectorWheel = false
        month.wrapSelectorWheel = false
        date.wrapSelectorWheel = false

        // edittext ?????? ??????
        year.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        month.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        date.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
    }

    private fun setListenerOnDatePicker() {
        // year picker change listener
        year.setOnValueChangedListener { _, _, _ ->
            if(year.value == currentDate.get(Calendar.YEAR)) {
                setPickerMinMaxValue(true)
            } else {
                setPickerMinMaxValue(false)
            }
            setDateValue()
        }

        // month picker change listener
        month.setOnValueChangedListener { _, _, _ ->
            if(year.value == currentDate.get(Calendar.YEAR) && month.value == currentDate.get(
                            Calendar.MONTH) + 1) {
                // ?????? ????????? ?????? ????????? ???
                setPickerMinMaxValue(true)
            } else {
                setPickerMinMaxValue(false)
            }
            setDateValue()
        }

        // date picker change listener
        date.setOnValueChangedListener { _, _, _ ->
            if(year.value == currentDate.get(Calendar.YEAR)
                    && month.value == currentDate.get(Calendar.MONTH) + 1) {
                // ?????? ????????? ?????? ????????? ???
                setPickerMinMaxValue(true)
            } else {
                setPickerMinMaxValue(false)
            }
            setDateValue()
        }
    }

    private fun setPickerMinMaxValue(isCurrent: Boolean) {
        if (isCurrent) {
            month.minValue = currentDate.get(Calendar.MONTH) + 1
            date.minValue = currentDate.get(Calendar.DAY_OF_MONTH)
        } else {
            month.minValue = 1
            date.minValue = 1
        }
        month.maxValue = 12
        setMonthMax()
    }

    // ??? ?????? ?????? ????????? ?????? ???????????? ??????
    private fun setMonthMax() {
        val month = binding.datePicker.month
        val date = binding.datePicker.date

        when (month.value) {
            2 -> {
                date.maxValue = 29
            }
            4, 6, 9, 11 -> {
                date.maxValue = 30
            }
            1, 3, 5, 7, 8, 10, 12 -> {
                date.maxValue = 31
            }
        }
    }

    private fun initCloseButton() {
        binding.buttonCancel.setOnClickListener {
            this.dismiss()
        }
    }

    private fun initAddButton() {
        binding.buttonAdd.setOnClickListener {
            if (binding.etDescription.text.toString() == "") {
                Toast.makeText(context!!, "Enter a description", Toast.LENGTH_SHORT).show()
                hideKeyboard(context!!, binding.etDescription)
            }
            else {
                val pickData = intArrayOf(selectYear, selectMonth, selectDate)
                itemClick(binding.etDescription.text.toString(), selectLabel, pickData)
                hideKeyboard(context!!, binding.etDescription)
                this.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}