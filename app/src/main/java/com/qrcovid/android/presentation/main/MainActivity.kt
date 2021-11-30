package com.qrcovid.android.presentation.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.qrcovid.android.R
import com.qrcovid.android.presentation.mask.MaskScanningFragment
import com.qrcovid.android.presentation.qr.QrScanningFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launchWhenResumed {
            viewModel.scanState.collectLatest { scanState ->
                when (scanState) {
                    ScanState.MaskScanning -> {
                        if (supportFragmentManager.fragments.isEmpty()) {
                            openFragment(fragment = MaskScanningFragment.newInstance(), tag = TAG_MASK_FRAGMENT)
                        }
                    }
                    ScanState.QrScanning -> {
                        if (supportFragmentManager.findFragmentByTag(TAG_MASK_FRAGMENT) == null) {
                            replaceFragment(fragment = QrScanningFragment.newInstance(), tag = TAG_QR_FRAGMENT)
                        }
                    }
                    ScanState.FinishScanning -> {
                        supportFragmentManager.popBackStack()
                    }
                }
            }
        }
    }

    private fun openFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragmentContainer, fragment, tag)
            .commit()
    }

    private fun replaceFragment(fragment: Fragment, tag: String?) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG_QR_FRAGMENT = "QR_FRAGMENT"
        private const val TAG_MASK_FRAGMENT = "MASK_FRAGMENT"
    }
}