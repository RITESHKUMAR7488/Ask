package com.example.ask.addModule.uis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.example.ask.MainActivity
import com.example.ask.R
import com.example.ask.addModule.models.QueryModel
import com.example.ask.addModule.viewModels.AddViewModel
import com.example.ask.databinding.ActivityAddQueryBinding
import com.example.ask.utilities.BaseActivity
import com.example.ask.utilities.UiState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AddQueryActivity : BaseActivity() {

    private lateinit var binding: ActivityAddQueryBinding
    private val addViewModel: AddViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_query)

        initPermissionLauncher()
        initImagePickerLauncher()

        setupUI()
        setupObservers()
    }

    // Initialize image picker launcher after binding is set
    private fun initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let {
                    binding.ivSelectedImage.visibility = View.VISIBLE
                    binding.tvImageHint.visibility = View.GONE
                    Glide.with(this).load(it).into(binding.ivSelectedImage)
                }
            }
        }
    }

    // Initialize permission request launcher
    private fun initPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openImagePicker()
            } else {
                motionToastUtil.showWarningToast(this, "Permission required to select image")
            }
        }
    }

    private fun setupUI() {
        // Category dropdown
        val categories = arrayOf("Lost & Found", "Academic Help", "Technical Support", "General Question", "Emergency", "Event", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(categoryAdapter)
        binding.spinnerCategory.setText(categories[0], false)

        // Priority dropdown
        val priorities = arrayOf("NORMAL", "HIGH", "LOW")
        val priorityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorities)
        binding.spinnerPriority.setAdapter(priorityAdapter)
        binding.spinnerPriority.setText(priorities[0], false)

        binding.btnSelectImage.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupObservers() {
        addViewModel.imageUpload.observe(this) { response ->
            uploadedImageUrl = response.image?.displayUrl
            if (uploadedImageUrl != null) {
                submitQuery()
            } else {
                binding.progressBar.visibility = View.GONE
                binding.btnSubmit.isEnabled = true
                motionToastUtil.showFailureToast(this, "Failed to upload image")
            }
        }

        addViewModel.imageUploadError.observe(this) { error ->
            binding.progressBar.visibility = View.GONE
            binding.btnSubmit.isEnabled = true
            motionToastUtil.showFailureToast(this, "Image upload failed: ${error.localizedMessage}")
        }

        addViewModel.addQuery.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnSubmit.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    motionToastUtil.showSuccessToast(this, "Query submitted successfully!")
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                    finish()
                }
                is UiState.Failure -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmit.isEnabled = true
                    motionToastUtil.showFailureToast(this, state.error)
                }
            }
        }
    }

    private fun checkPermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun validateAndSubmit() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()

        when {
            title.isBlank() -> {
                binding.etTitle.error = "Please enter a title"
                binding.etTitle.requestFocus()
            }
            description.isBlank() -> {
                binding.etDescription.error = "Please enter a description"
                binding.etDescription.requestFocus()
            }
            location.isBlank() -> {
                binding.etLocation.error = "Please enter a location"
                binding.etLocation.requestFocus()
            }
            else -> {
                if (selectedImageUri != null) {
                    uploadImage()
                } else {
                    submitQuery()
                }
            }
        }
    }

    private fun uploadImage() {
        selectedImageUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }

                val apiKey = "6d207e02198a847aa98d0a2a901485a5" // Use actual API key
                addViewModel.uploadImage(file, apiKey)

            } catch (e: Exception) {
                motionToastUtil.showFailureToast(this, "Failed to process image: ${e.localizedMessage}")
            }
        }
    }

    private fun submitQuery() {
        val userModel = preferenceManager.userModel
        val queryModel = QueryModel(
            userId = userModel?.uid,
            userName = userModel?.fullName,
            userProfileImage = userModel?.imageUrl,
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            category = binding.spinnerCategory.text.toString(),
            location = binding.etLocation.text.toString().trim(),
            imageUrl = uploadedImageUrl,
            priority = binding.spinnerPriority.text.toString(),
            tags = binding.etTags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        )

        addViewModel.addQuery(queryModel)
    }
}
