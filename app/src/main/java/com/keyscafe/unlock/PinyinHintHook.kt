package com.keyscafe.unlock

import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge

object PinyinHintHook {
    private const val TAG = "Guking-Pinyin"

    // 小鹤双拼键位字典
    private val pinyinMap = mapOf(
        'Q' to "iu", 'W' to "ei", 'E' to "e", 'R' to "uan", 'T' to "ue", 'Y' to "un", 'U' to "u", 'I' to "i", 'O' to "o", 'P' to "ie",
        'A' to "a", 'S' to "ong", 'D' to "iang", 'F' to "en", 'G' to "eng", 'H' to "ang", 'J' to "an", 'K' to "ao", 'L' to "ai",
        'Z' to "ou", 'X' to "ia", 'C' to "iao", 'V' to "ui", 'B' to "in", 'N' to "iao", 'M' to "ian"
    )

    // 防止Hook画字时把自己给拦截了，陷入死循环
    private val isDrawing = ThreadLocal.withInitial { false }
    
    // 全局复用一支画笔，避免打字太快触发 GC 导致卡顿
    private val hintPaint = Paint()

    fun initHook() {
        try {
            Canvas::class.java.declaredMethods
                .filter { it.name == "drawText" }
                .forEach { method ->
                    val params = method.parameterTypes
                    
                    // 拦截常规的 drawText(String, ...)
                    if (params.size == 4 && params[0] == String::class.java) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                if (isDrawing.get()) return
                                val text = param.args[0] as? String ?: return
                                if (text.length == 1) {
                                    injectHint(param, text[0], param.args[1] as Float, param.args[2] as Float, param.args[3] as Paint)
                                }
                            }
                        })
                    }
                    
                    // 拦截带范围的 drawText(CharSequence, start, end, ...)
                    if (params.size == 6 && params[0] == CharSequence::class.java) {
                        XposedBridge.hookMethod(method, object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                if (isDrawing.get()) return
                                val seq = param.args[0] as? CharSequence ?: return
                                val start = param.args[1] as Int
                                val end = param.args[2] as Int
                                if (end - start == 1) {
                                    injectHint(param, seq[start], param.args[3] as Float, param.args[4] as Float, param.args[5] as Paint)
                                }
                            }
                        })
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "hook canvas 失败", e)
        }
    }

    private fun injectHint(param: XC_MethodHook.MethodHookParam, char: Char, x: Float, y: Float, paint: Paint) {
        // 忽略太小的字（比如候选词、状态栏或者小符号）
        if (paint.textSize < 30f) return 

        val hint = pinyinMap[char.uppercaseChar()] ?: return
        val canvas = param.thisObject as Canvas
        
        // "偷"走原画笔，完美白嫖当前的主题色和字体风格
        hintPaint.set(paint)
        hintPaint.textSize = paint.textSize * 0.35f
        hintPaint.alpha = 100 // 半透明，若隐若现
        
        // 优雅地挪到主字母的右上角
        val offsetX = paint.textSize * 0.3f
        val offsetY = -paint.textSize * 0.4f

        isDrawing.set(true)
        try {
            canvas.drawText(hint, x + offsetX, y + offsetY, hintPaint)
        } finally {
            isDrawing.set(false)
        }
    }
}
