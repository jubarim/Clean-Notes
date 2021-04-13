package com.codingwithmitch.cleannotes.framework.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.codingwithmitch.cleannotes.R

class MainActivity : AppCompatActivity() {

    private val TAG: String = "AppDebug"

    override fun onCreate(savedInstanceState: Bundle?) {
        (application as BaseApplication).appComponent
            .inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

}
