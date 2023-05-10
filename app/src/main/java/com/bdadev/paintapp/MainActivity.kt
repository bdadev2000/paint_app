package com.bdadev.paintapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bdadev.paintapp.databinding.ActivityMainBinding
import com.skydoves.colorpickerview.ColorPickerDialog
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }

    private lateinit var binding: ActivityMainBinding
    private val openGalleryLauncher : ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == RESULT_OK && result.data!=null){
            binding.imgBackground.setImageURI(result?.data?.data)
        }
    }
    private val requestPermission : ActivityResultLauncher<Array<String>> = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissions ->
        permissions.entries.forEach{
            val permissionName = it.key
            val isGranted = it.value
            if(isGranted){
                Toast.makeText(this@MainActivity,getString(R.string.permission_granted_now_you_can_read_the_storage_files),Toast.LENGTH_SHORT).show()
                val pickerIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickerIntent)
            }else{
                if(permissionName==android.Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this@MainActivity,getString(R.string.oops_you_just_denied_the_permission),Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
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
            val btnBrushSmall: View = brushDialog.findViewById(R.id.btn_brush_small)
            val btnBrushMedium: View = brushDialog.findViewById(R.id.btn_brush_medium)
            val btnBrushBig: View = brushDialog.findViewById(R.id.btn_brush_big)
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
                    ColorEnvelopeListener { envelope, fromUser ->
                        binding.drawCanvas.setBrushColor(
                            envelope.color
                        )
                    })
                .setNegativeButton(
                    getString(android.R.string.cancel)
                ) { dialogInterface, i -> dialogInterface.dismiss() }
                .attachAlphaSlideBar(true) // the default value is true.
                .attachBrightnessSlideBar(true) // the default value is true.
                .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
                .show()
        }

        binding.btnChooseImage.setOnClickListener {
            requestStoragePermission()
        }

        binding.btnUndo.setOnClickListener {
            binding.drawCanvas.onClickUndo()
        }

        binding.btnSave.setOnClickListener {
            if(isReadStorageAllowed()){
                lifecycleScope.launch{
                    saveBitmapFile(getBitmapFromView(binding.container))
                }

            }
        }
    }

    private fun isReadStorageAllowed() : Boolean{
        val result = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showDialog("Drawing App","Need to access your external storage")
        }else{
            requestPermission.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }
    private fun showDialog(title : String, message : String){
        val builder : AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.cancel)){dialog,_->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun getBitmapFromView(view: View) : Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return  returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap : Bitmap?) : String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap!=null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString() + File.separator + "PainApp_" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread{
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully: $result",Toast.LENGTH_SHORT).show()
                        }else{
                            Toast.makeText(this@MainActivity,"Something save file went wrong: $result",Toast.LENGTH_SHORT).show()
                        }
                    }
                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }
}