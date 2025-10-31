package com.ubergeek42.WeechatAndroid.views

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import com.ubergeek42.WeechatAndroid.databinding.EditTextActivityBinding


// While since API 21 it is possible to easily disallow showing the software keyboard
// by using `TextView.showSoftInputOnFocus`,
// to better mimic dialogs and to use the code that worked well before, here we:
//   * Set a window flag that effectively positions the window on top of the input method.
//     This makes the keyboard, if open, stay open in the previous window.
//   * Ignore IME insets for the purpose of the UI.
//     As the keyboard is open, even if is not shown in this window, we get its insets here.
abstract class EditTextActivity(val allowSoftKeyboard: Boolean) : AppCompatActivity()  {
    lateinit var ui: EditTextActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (!allowSoftKeyboard) {
            window.setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }

        window.decorView.apply {
            val onInsetsChanged = if (allowSoftKeyboard)
                ::onSystemBarsAndImeInsetsChanged else ::onSystemBarsInsetsChanged
            onInsetsChanged { insets ->
                updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = insets.bottom)
            }
        }

        ui = EditTextActivityBinding.inflate(layoutInflater)
        setContentView(ui.root)

        ui.toolbar.setNavigationOnClickListener { finish() }
    }
}