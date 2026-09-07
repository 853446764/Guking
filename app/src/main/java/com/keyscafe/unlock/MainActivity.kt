package com.keyscafe.unlock

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        if (isActive()) {
            statusText.text = "Guking 已激活，快去调教 Keys Cafe 吧"
            statusText.setTextColor(Color.parseColor("#4CAF50")) // 修复了一个轻微的旧版本 API 调用警告
        }
    }

    // xposed hook 会拦截这个方法返回 true
    private fun isActive(): Boolean = false
}
