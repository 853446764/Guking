package com.keyscafe.unlock

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.ArrayList

class UnlockHook : IXposedHookLoadPackage {

    private val TAG = "Guking"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName == "com.keyscafe.unlock") {
            // tell our UI that the module is active
            runCatching {
                XposedHelpers.findAndHookMethod(
                    "com.keyscafe.unlock.MainActivity",
                    lpparam.classLoader,
                    "isActive",
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            param.result = true
                        }
                    }
                )
            }
        }

        if (lpparam.packageName == "com.samsung.android.keyscafe") {
            Log.i(TAG, "injecting into keys cafe...")
            doHook(lpparam.classLoader)
        }
    }

    private fun doHook(classLoader: ClassLoader) {
        runCatching {
            val fragmentCls = XposedHelpers.findClassIfExists(
                "com.samsung.android.keyscafe.latte.edit.ui.EditActivityFragment", 
                classLoader
            ) ?: return

            // find the validation method (boolean -> void)
            val targets = fragmentCls.declaredMethods.filter {
                it.returnType == Void.TYPE && 
                it.parameterTypes.size == 1 && 
                it.parameterTypes[0] == Boolean::class.javaPrimitiveType
            }

            targets.forEach { method ->
                XposedBridge.hookMethod(method, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        bypassMandatoryCheck(param.thisObject)
                    }
                })
            }
        }.onFailure {
            Log.e(TAG, "hook setup failed", it)
        }
    }

    private fun bypassMandatoryCheck(fragment: Any) {
        runCatching {
            // scan fields to find the mandatory keys list and clear it
            for (f in fragment.javaClass.declaredFields) {
                f.isAccessible = true
                val obj = f.get(fragment) ?: continue

                // skip standard android/java classes
                val name = obj.javaClass.name
                if (name.startsWith("java.") || name.startsWith("android.")) continue

                for (innerF in obj.javaClass.declaredFields) {
                    if (innerF.type == ArrayList::class.java) {
                        innerF.isAccessible = true
                        val list = innerF.get(obj) as? ArrayList<*>
                        
                        // make sure it's a list of strings
                        if (list?.firstOrNull() is String) {
                            Log.d(TAG, "cleared ${list.size} mandatory keys")
                            list.clear()
                        }
                    }
                }
            }
        }
    }
}
