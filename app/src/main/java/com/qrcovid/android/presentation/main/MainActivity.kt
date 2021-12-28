package com.qrcovid.android.presentation.main

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.*
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.lifecycleScope
import com.qrcovid.android.R
import com.qrcovid.android.databinding.ActivityMainBinding
import com.qrcovid.android.presentation.mask.CameraFragment
import com.qrcovid.android.presentation.qr.QrScanningFragment
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity(), IQrScanning, IMaskScanning {

    private val viewModel by viewModels<MainViewModel> {
        SavedStateViewModelFactory(application, this)
    }
    private lateinit var binding: ActivityMainBinding

    private val permissionsCallback by lazy {
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            viewModel::grantPermissions
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeState()
        permissionsCallback.launch(arrayOf(CAMERA_PERMISSION))

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        todoDelete()
    }

    override fun onBackPressed() {}

    // TODO: delete
    private fun todoDelete() {
        binding.pop.setOnClickListener {
            viewModel.back()
        }

        binding.finish.setOnClickListener {
            viewModel.finish()
        }

        binding.mask.setOnClickListener {
            viewModel.openMask()
        }

        binding.qr.setOnClickListener {
            viewModel.openQr()
        }
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.scanState.collectLatest { scanState ->
                when (scanState) {
                    ScanState.Default -> {
                        removeAllFragments()
                    }
                    ScanState.MaskScanning -> {
                        openMaskFragment()
                    }
                    ScanState.QrScanning -> {
                        openQrFragment()
                    }
                    ScanState.FinishScanning -> {
                        supportFragmentManager.popBackStack(BACK_STACK_QR_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    }
                }
            }

            viewModel.errorState.collectLatest { errorState ->
                when (errorState) {
                    ErrorState.Default -> {}
                    ErrorState.PermissionError -> {
                        Toast.makeText(this@MainActivity, R.string.error_no_permissions, Toast.LENGTH_LONG)
                            .show()
                    }
                }
                if (errorState !is ErrorState.Default) {
                    viewModel.setDefaultErrorState()
                }
            }
        }
    }

    private fun removeAllFragments() {
        supportFragmentManager.findFragmentByTag(TAG_MASK_FRAGMENT)?.let { fragment ->
            supportFragmentManager.commit {
                remove(fragment)
            }
        }
        supportFragmentManager.findFragmentByTag(TAG_QR_FRAGMENT)?.let { fragment ->
            supportFragmentManager.commit {
                remove(fragment)
            }
        }
    }

    // Could be reached on start or after qr session end
    private fun openMaskFragment() {
        if (supportFragmentManager.findFragmentByTag(TAG_MASK_FRAGMENT) == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                add<CameraFragment>(R.id.fragmentContainer, TAG_MASK_FRAGMENT)
            }
        } else if (supportFragmentManager.findFragmentByTag(TAG_QR_FRAGMENT) != null) {
            supportFragmentManager.popBackStack(BACK_STACK_QR_FRAGMENT, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
    }

    // Could be reached after successful mask verification
    private fun openQrFragment() {
        if (supportFragmentManager.findFragmentByTag(TAG_QR_FRAGMENT) == null) {
            supportFragmentManager.commit {
                setReorderingAllowed(true)
                replace<QrScanningFragment>(R.id.fragmentContainer, TAG_QR_FRAGMENT)
                addToBackStack(BACK_STACK_QR_FRAGMENT)
            }
        }
    }


    override fun onMaskSuccess() {

    }

    override fun onMaskFailure() {

    }

    override fun onQrSuccess() {

    }

    override fun onQrFailure() {

    }

    companion object {
        private const val TAG_QR_FRAGMENT = "QR_FRAGMENT"
        private const val TAG_MASK_FRAGMENT = "MASK_FRAGMENT"
        private const val TAG_ERROR_FRAGMENT = "ERROR_FRAGMENT"
        private const val BACK_STACK_QR_FRAGMENT = "BACK_STACK_QR_FRAGMENT"
        private const val CAMERA_PERMISSION = "android.permission.CAMERA"
    }
}