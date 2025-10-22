package com.example.ask.profileModule.uis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController // <-- ADD THIS LINE
import com.bumptech.glide.Glide
import com.example.ask.R
import com.example.ask.databinding.FragmentProfileBinding
import com.example.ask.onBoardingModule.models.UserModel
import com.example.ask.profileModule.viewModels.ProfileViewModel
import com.example.ask.utilities.BaseFragment
import com.example.ask.utilities.UiState
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.sanju.motiontoast.MotionToast

@AndroidEntryPoint
class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        binding.viewModel = profileViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        setupToolbar()
        initPermissionLauncher()
        initImagePickerLauncher()
        setupClickListeners()
        observeViewModel()
    }

//    private fun setupToolbar() {
//        binding.toolbar.setNavigationOnClickListener {
//            findNavController().navigateUp()
//        }
//    }

    // --- Coroutine Usage ---
    // 'viewLifecycleOwner.lifecycleScope.launch' starts a coroutine tied to the Fragment's view lifecycle.
    // 'repeatOnLifecycle(Lifecycle.State.STARTED)' ensures the collection starts when the view
    // is started and stops when it's stopped, automatically handling cancellations.
    // This is the recommended way to collect Flows in the UI layer.

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Profile State
                launch {
                    profileViewModel.userProfileState.collect { state ->
                        handleUserProfileState(state)
                    }
                }
                // Observe Update State
                launch {
                    profileViewModel.updateProfileState.collect { state ->
                        state?.let { // Process only non-null states
                            handleUpdateProfileState(it)
                            // Reset state in ViewModel after handling
                            profileViewModel.resetUpdateState()
                        }
                    }
                }
                // Observe Image Upload State
                launch {
                    profileViewModel.uploadImageState.collect { state ->
                        state?.let { // Process only non-null states
                            handleUploadImageState(it)
                            // Reset state in ViewModel after handling
                            profileViewModel.resetUploadState()
                        }
                    }
                }
            }
        }
    }

    private fun handleUserProfileState(state: UiState<UserModel>) {
        when (state) {
            is UiState.Loading -> {
                showLoading(true)
                binding.btnSaveProfile.isEnabled = false
            }
            is UiState.Success -> {
                showLoading(false)
                binding.btnSaveProfile.isEnabled = true
                populateProfileData(state.data)
            }
            is UiState.Failure -> {
                showLoading(false)
                binding.btnSaveProfile.isEnabled = false
                showErrorSnackbar("Error loading profile: ${state.error}")
            }
        }
    }

    private fun handleUpdateProfileState(state: UiState<Unit>) {
        when (state) {
            is UiState.Loading -> {
                showLoading(true, "Saving...") // Show overlay with text
            }
            is UiState.Success -> {
                showLoading(false)
                motionToastUtil.showSuccessToast(requireActivity(), "Profile updated successfully!", MotionToast.SHORT_DURATION)
                // Optionally navigate back or refresh
                // findNavController().navigateUp()
            }
            is UiState.Failure -> {
                showLoading(false)
                showErrorSnackbar("Update failed: ${state.error}")
            }
        }
    }

    private fun handleUploadImageState(state: UiState<String>) {
        when (state) {
            is UiState.Loading -> {
                binding.imageUploadProgressBar.visibility = View.VISIBLE
                binding.fabEditImage.isEnabled = false
            }
            is UiState.Success -> {
                binding.imageUploadProgressBar.visibility = View.GONE
                binding.fabEditImage.isEnabled = true
                motionToastUtil.showSuccessToast(requireActivity(), "Profile picture updated!", MotionToast.SHORT_DURATION)
                // Glide will be updated automatically when userProfileState is refreshed
            }
            is UiState.Failure -> {
                binding.imageUploadProgressBar.visibility = View.GONE
                binding.fabEditImage.isEnabled = true
                showErrorSnackbar("Image upload failed: ${state.error}")
            }
        }
    }

    private fun populateProfileData(user: UserModel) {
        binding.etFullName.setText(user.fullName ?: "")
        binding.etEmail.setText(user.email ?: "")
        binding.etMobileNumber.setText(user.mobileNumber ?: "")
        binding.etAddress.setText(user.address ?: "")

        Glide.with(this)
            .load(user.imageUrl)
            .placeholder(R.drawable.ic_person) // Placeholder
            .error(R.drawable.ic_person)       // Error image
            .circleCrop()
            .into(binding.ivProfileImage)
    }

    private fun setupClickListeners() {
        binding.fabEditImage.setOnClickListener {
            checkPermissionAndOpenPicker()
        }

        binding.btnSaveProfile.setOnClickListener {
            hideKeyboard() // Hide keyboard before processing
            val fullName = binding.etFullName.text.toString().trim()
            val mobile = binding.etMobileNumber.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            profileViewModel.updateUserProfile(fullName, mobile, address)
        }
    }

    // --- Image Picker and Permissions ---

    private fun initPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openImagePicker()
            } else {
                motionToastUtil.showWarningToast(requireActivity(), "Storage permission is required to select an image.", MotionToast.SHORT_DURATION)
            }
        }
    }

    private fun initImagePickerLauncher() {
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    // Upload the selected image
                    profileViewModel.uploadAndProfileImage(uri)
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
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Show rationale if needed (e.g., in a dialog)
                showPermissionRationaleDialog(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showPermissionRationaleDialog(permission: String){
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Permission Needed")
            .setMessage("This app needs permission to access your gallery to select a profile picture.")
            .setPositiveButton("Grant") { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        // For modern Android versions, prefer ActivityResultContracts.PickVisualMedia
        // val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
        imagePickerLauncher.launch(intent)
    }

    // --- UI Helper Functions ---

    private fun showLoading(isLoading: Boolean, message: String = "") {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Optionally update a text view within the overlay with the message
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.error_color))
            .setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            .show()
    }

    private fun hideKeyboard() {
        val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding to prevent memory leaks
    }
}