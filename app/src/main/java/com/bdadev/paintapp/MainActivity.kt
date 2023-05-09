package com.bdadev.paintapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.bdadev.paintapp.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.drawCanvas.setBrushSize(20.toFloat())
        onActionClick()
    }

    private fun onActionClick() {
        binding.btnBrushSize.setOnClickListener {
            var brushDialog = Dialog(this)
            brushDialog.setContentView(R.layout.dialog_choose_brush_size)
            val btnBrushSmall : View = brushDialog.findViewById(R.id.btn_brush_small)
            val btnBrushMedium : View = brushDialog.findViewById(R.id.btn_brush_medium)
            val btnBrushBig : View = brushDialog.findViewById(R.id.btn_brush_big)
            btnBrushSmall.setOnClickListener {
                binding.drawCanvas.setBrushSize(10.toFloat())
                brushDialog.dismiss()
            }
            btnBrushMedium.setOnClickListener {
                binding.drawCanvas.setBrushSize(15.toFloat())
                brushDialog.dismiss()
            }
            btnBrushBig.setOnClickListener {
                binding.drawCanvas.setBrushSize(20.toFloat())
                brushDialog.dismiss()
            }
            brushDialog.show()
        }
        binding.btnColorPicker.setOnClickListener {
            ColorPickerDialog.Builder(this)
                .setTitle("ColorPicker Dialog")
                .setPreferenceName("MyColorPickerDialog")
                .setPositiveButton(getString(R.string.confirm),
                    ColorEnvelopeListener { envelope, fromUser -> binding.drawCanvas.setBrushColor(envelope.color) })
                .setNegativeButton(
                    getString(android.R.string.cancel)
                ) { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(true) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .show()
        }
    }
}