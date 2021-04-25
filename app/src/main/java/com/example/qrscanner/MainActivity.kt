package com.example.qrscanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.result.ParsedResult
import com.google.zxing.client.result.ResultParser
import com.google.zxing.common.HybridBinarizer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.InputStream
import java.util.concurrent.ExecutionException


private const val PERMISSION_REQUEST_CAMERA = 0
private const val TYPE_KEY = "type_key"
private const val TEXT_KEY = "text_key"
open class MainActivity : AppCompatActivity() {
    lateinit var previewView: PreviewView
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var qrCode: String? = null
    private var type: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.activity_main_previewView);

        activity_main_qrCodeFoundButton.visibility = View.INVISIBLE
        activity_main_qrCodeFoundButton.setOnClickListener {
            if (type == null) {
                Toast.makeText(applicationContext, "Ooops, invalid qr", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(applicationContext, QRResultActivity::class.java)
            intent.putExtra(TYPE_KEY, type!!)
            intent.putExtra(TEXT_KEY, qrCode!!)
            startActivity(intent)
        }

        openAlbum.setOnClickListener { openSystemAlbum() }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        requestCamera()
    }

    private fun requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )) {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CAMERA
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CAMERA
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                bindCameraPreview(cameraProvider)
            } catch (e: ExecutionException) {
                Toast.makeText(this, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            } catch (e: InterruptedException) {
                Toast.makeText(this, "Error starting camera " + e.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraPreview(cameraProvider: ProcessCameraProvider) {
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        val preview = Preview.Builder()
                .build()
        val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), QRCodeImageAnalyzer(object :
            QRCodeFoundListener {
            override fun getParsedResult(result: ParsedResult?) {
                this@MainActivity.qrCode = result.toString()
                this@MainActivity.type = result?.type?.name
                QRApp.instance.lastParsedResult = result
                activity_main_qrCodeFoundButton.visibility = View.VISIBLE
            }

            override fun qrCodeNotFound() {
                activity_main_qrCodeFoundButton.visibility = View.INVISIBLE
            }
        }))
        val camera: Camera = cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    private fun openSystemAlbum() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PERMISSION_REQUEST_CAMERA)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }

        when (requestCode) {
            1 -> finish()
            PERMISSION_REQUEST_CAMERA -> {
                val uri: Uri? = data?.data
                val inputStream: InputStream? = contentResolver.openInputStream(uri!!)
                var bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap == null) {
                    Log.e("TAG", "uri is not a bitmap,$uri")
                    return
                }
                val width = bitmap.width
                val height = bitmap.height
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                bitmap.recycle()
                bitmap = null
                val source = RGBLuminanceSource(width, height, pixels)
                val bBitmap = BinaryBitmap(HybridBinarizer(source))
                val reader = MultiFormatReader()
                try {
                    val result = ResultParser.parseResult(reader.decode(bBitmap))
                    this@MainActivity.qrCode = result.toString()
                    this@MainActivity.type = result?.type?.name
                    QRApp.instance.lastParsedResult = result
                    val intent = Intent(applicationContext, QRResultActivity::class.java)
                    intent.putExtra(TYPE_KEY, type).putExtra(TEXT_KEY, qrCode)
                    startActivity(intent)
                } catch (e: NotFoundException) {
                    Toast.makeText(applicationContext, "No qr on photos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
