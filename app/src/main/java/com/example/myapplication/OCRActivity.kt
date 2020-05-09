package com.example.myapplication

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import kotlinx.android.synthetic.main.activity_ocr.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest


class OCRActivity : AppCompatActivity() {

    lateinit var ocrImage: ImageView
    lateinit var resultEditText: EditText
    lateinit var translateButton: Button
    lateinit var image_uri: Uri
    private val IMAGE_PICK_GALLERY_CODE = 1000
    private val IMAGE_PICK_CAMERA_CODE = 1001
    private val CAMERA_REQUEST_CODE = 200
    private val STORAGE_REQUEST_CODE = 400
    var PERMISSION_REQ_CODE: Int = 100
    var isPermissionsGranted: Boolean = false
    var permissions: Array<String> = arrayOf(WRITE_EXTERNAL_STORAGE, CAMERA)
    lateinit var currentPhotoPath: String

    lateinit var uriFinale: Uri

    var cameraPermission: Array<String> = arrayOf(WRITE_EXTERNAL_STORAGE, CAMERA)
    var storagePermission: Array<String> = arrayOf(WRITE_EXTERNAL_STORAGE)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ocr)

        resultEditText = findViewById(R.id.ocrResultEt)
        ocrImage = findViewById(R.id.ocrImageView)
        translateButton = findViewById(R.id.goToTranslate)




        //set an onclick listener on the button to trigger the @pickImage() method
        selectImageBtn.setOnClickListener {
            showImageImportDialog()
        }

        //set an onclick listener on the button to trigger the @processImage method
        processImageBtn.setOnClickListener {
            processImage(processImageBtn)
        }

        translateButton.setOnClickListener {

            val translate = resultEditText.text.toString().trim()
            val intent = Intent(this, TranslateActivity::class.java)
            intent.putExtra("translate", translate)
            startActivity(intent)
        }

    }


    private fun showImageImportDialog() {
        val items: Array<String> = arrayOf("Gallery", " Camera")
        val dialog =
            AlertDialog.Builder(this)
        //set title
        dialog.setTitle("Select Image")
        dialog.setItems(items) { dialog, which ->
            System.out.println(which == 0)
            System.out.println(which == 1)
            if (which == 0) {
                if (!checkStoragePermission()) {
                    requestStoragePermission()
                } else {
                    pickImage()
                }
            }
            else if (which == 1) {
                if (!checkCameraPermission())
                    requestCameraPermission()
                } else {
                    pickCamera()
            }
        }
        dialog.create().show()
    }


    private fun pickCamera() {
        System.out.println("Camera")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, IMAGE_PICK_CAMERA_CODE)
                System.out.println("pickCamera")
            }
        }
    }


    fun pickImage() {
        System.out.println("Image")
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            IMAGE_PICK_GALLERY_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                ocrImage.setImageURI(data!!.data)

            }

            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                ocrImage.setImageURI(data!!.data)
                val imageBitmap = data.extras!!.get("data") as Bitmap
                ocrImage.setImageBitmap(imageBitmap)

            }


        }
    }

    fun processImage(v: View) {
        if (ocrImage.drawable != null) {
            resultEditText.setText("")
            v.isEnabled = false
            val bitmap = (ocrImage.drawable as BitmapDrawable).bitmap
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    v.isEnabled = true
                    processResultText(firebaseVisionText)
                }
                .addOnFailureListener {
                    v.isEnabled = true
                    resultEditText.setText("Failed")
                }
        } else {
            Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
        }

    }


    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            resultEditText.setText("No Text Found")
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            resultEditText.append(blockText + "\n")
        }
    }


    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE
        )
    }

    /**
     * check if permission to access storage has been granted
     * @return
     */
    private fun checkStoragePermission(): Boolean {
        var result1: Boolean = ContextCompat.checkSelfPermission(
            this,
            storagePermission[0]) == PackageManager.PERMISSION_GRANTED
        return result1

    }

    /**
     * request permission to access phones camera
     */
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            cameraPermission, CAMERA_REQUEST_CODE
        )
    }

    /**
     * check permission to use camera has been granted
     * @return
     */
    private fun checkCameraPermission(): Boolean {
        /* check camera permission and return result
        in order to get higg quality image have to save
        image to external storage first
        before inserting to image view
         */
        val result: Boolean = ContextCompat.checkSelfPermission(this,
            cameraPermission[0]) == PackageManager.PERMISSION_GRANTED
        val result1: Boolean = ContextCompat.checkSelfPermission(this,
            cameraPermission[1]) == PackageManager.PERMISSION_GRANTED
        return result && result1
    }


    /**
     * handling the permissions
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_REQUEST_CODE -> if (grantResults.size > 0) {
                val cameraAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
                val writeStorageAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
                if (cameraAccepted && writeStorageAccepted) {
                    System.out.println("Request Camera")
                    pickCamera()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_REQUEST_CODE -> if (grantResults.size > 0) {
                val writeStorageAccepted = grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED
                if (writeStorageAccepted) {
                    System.out.println("Request Image")
                    pickImage()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}


