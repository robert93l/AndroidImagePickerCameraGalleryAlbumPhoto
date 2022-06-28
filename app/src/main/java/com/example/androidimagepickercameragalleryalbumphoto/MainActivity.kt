package com.example.androidimagepickercameragalleryalbumphoto

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.androidimagepickercameragalleryalbumphoto.utils.EasyPermissionManager
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private val permissionManager = EasyPermissionManager(this)


    private val imageView: ImageView by lazy {
        findViewById(R.id.imageView)
    }

    private val selectePictureLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            //imageView.setImageURI(it)
            uriToFile(this, uri, "select_image_from_gallery")?.let { file ->


                compressImage(file.absolutePath, 0.5)

                textViewSize.text = sizeString

                setImage(imageView, file.absolutePath)
            }
        }

    private val textViewSize: TextView by lazy {
        findViewById(R.id.textViewSize)
    }

    private var tempImageUri: Uri? = null
    private var tempImageFilePath = ""
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->

            if (success) {
                //  imageView.setImageURI(tempImageUri)
                compressImage(tempImageFilePath, 0.5)
                textViewSize.text = sizeString

                setImage(imageView, tempImageFilePath)
            }
        }

    private var sizeString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        findViewById<Button>(R.id.buttonCamera).setOnClickListener {

            permissionManager.requestPermission(
                "permission",
                "permission necesary",
                "setting",
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                //AFTER PERMISSION GRANTED
                tempImageUri = FileProvider.getUriForFile(
                    this,
                    "com.example.androidimagepickercameragalleryalbumphoto.provider",
                    createImageFile().also {

                        tempImageFilePath = it.absolutePath

                    })

                cameraLauncher.launch(tempImageUri)
            }

        }

        findViewById<Button>(R.id.buttonAlbum).setOnClickListener {
            permissionManager.requestPermission(
                "permission",
                "permission necesary",
                "setting",
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )

            ) {
                //after permission granted
                selectePictureLauncher.launch("image/*")
            }
        }
    }

    private fun createImageFile(fileName: String = "temp_image"): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File.createTempFile(fileName, ".jpg", storageDir)
    }

    private fun uriToFile(context: Context, uri: Uri, fileName: String): File? {

        context.contentResolver.openInputStream(uri)?.let { inputStream ->

            val tempFile = createImageFile(fileName)
            val fileOutputStream = FileOutputStream(tempFile)

            inputStream.copyTo(fileOutputStream)
            inputStream.close()
            fileOutputStream.close()

            return tempFile
        }
        return null
    }

    private fun compressImage(filePath: String, targetMB: Double = 1.0) {

        sizeString = ""

        var image: Bitmap = BitmapFactory.decodeFile(filePath)

        val exif = ExifInterface(filePath)
        val exifOrientation: Int = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
        )

        val exifDegree: Int = exifOrientationToDegrees(exifOrientation)

        image = rotateImage(image, exifDegree.toFloat())

        try {

            val fileSizeInMB = getFileSizeInMB(filePath)

            sizeString += "size before: ${String.format("%.2f", fileSizeInMB)}"

            var quality = 100
            if (fileSizeInMB > targetMB) {
                quality = ((targetMB / fileSizeInMB) * 100).toInt()
            }

            val fileOutputStream = FileOutputStream(filePath)
            image.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream)
            fileOutputStream.close()

            sizeString += "\nsize after: ${String.format("%.2f", getFileSizeInMB(filePath))}"

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getFileSizeInMB(filePath: String): Double {

        val file = File(filePath)
        val length = file.length()

        val fileSizeInKB = (length / 1024).toString().toDouble()
        return (fileSizeInKB / 1024).toString().toDouble()
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)

        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun setImage(imageView: ImageView, filePath: String) {
        Glide.with(imageView.context).asBitmap().load(filePath).skipMemoryCache(true)
            .diskCacheStrategy(
                DiskCacheStrategy.NONE
            ).into(imageView)
    }

    private fun exifOrientationToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                90
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                180
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                270
            }

            else -> 0
        }
    }


}