package com.fingerdance

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton

object BluetoothLatencyDialogManager {

    private var dialogShowing = false
    private var lastNotifiedDeviceId: String? = null
    fun checkAndShow(
        activity: Activity,
        forceShow: Boolean = false
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (dialogShowing && !forceShow) return

        val device = BluetoothLatencyProfileManager
            .getConnectedBluetoothDevice(activity)
            ?: run {
                lastNotifiedDeviceId = null
                return
            }

        if (!forceShow && lastNotifiedDeviceId == device.id) {
            return
        }

        lastNotifiedDeviceId = device.id

        val profile = BluetoothLatencyProfileManager.getProfile(
            device.id
        )

        if (profile != null) {
            showExistingProfileDialog(
                activity = activity,
                device = device,
                profile = profile
            )
        } else {
            showNoProfileDialog(
                activity = activity,
                device = device
            )
        }
    }

    private fun showExistingProfileDialog(
        activity: Activity,
        device: BluetoothLatencyProfileManager.BluetoothAudioDevice,
        profile: BluetoothLatencyProfileManager.BluetoothLatencyProfile
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        dialogShowing = true

        val dialog = Dialog(activity)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                activity.dpToPx(24),
                activity.dpToPx(24),
                activity.dpToPx(24),
                activity.dpToPx(20)
            )

            background = GradientDrawable().apply {
                setColor("#101425".toColorInt())
                cornerRadius = activity.dpToPx(12).toFloat()
                setStroke(
                    activity.dpToPx(2),
                    "#20DFFF".toColorInt()
                )
            }
        }

        val title = TextView(activity).apply {
            text = "AUDIO BLUETOOTH DETECTADO"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val deviceText = TextView(activity).apply {
            text = device.model
            setTextColor("#20DFFF".toColorInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(
                0,
                activity.dpToPx(12),
                0,
                0
            )
        }

        val message = TextView(activity).apply {
            text = "Se encontró un perfil de latencia guardado.\n\nSe utilizará una latencia de ${profile.latencyMs} ms."
            setTextColor("#D0D6E6".toColorInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(
                0,
                activity.dpToPx(14),
                0,
                activity.dpToPx(20)
            )
        }

        val btnAccept = createDialogButton(
            activity = activity,
            text = "ACEPTAR"
        )

        val btnEdit = createDialogButton(
            activity = activity,
            text = "VOLVER A EDITAR"
        )

        btnAccept.setOnClickListener {
            dialog.dismiss()
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(
                activity,
                BluetoothLatencyActivity::class.java
            )

            activity.startActivity(intent)
        }

        container.addView(title)
        container.addView(deviceText)
        container.addView(message)

        container.addView(
            btnAccept,
            createButtonParams(activity)
        )

        container.addView(
            btnEdit,
            createButtonParams(activity)
        )

        dialog.setContentView(container)

        dialog.setOnDismissListener {
            dialogShowing = false
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            setLayout(
                (activity.resources.displayMetrics.widthPixels * 0.88f).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }

        dialog.show()

        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showNoProfileDialog(
        activity: Activity,
        device: BluetoothLatencyProfileManager.BluetoothAudioDevice
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        dialogShowing = true

        val dialog = Dialog(activity)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                activity.dpToPx(24),
                activity.dpToPx(24),
                activity.dpToPx(24),
                activity.dpToPx(20)
            )

            background = GradientDrawable().apply {
                setColor("#101425".toColorInt())
                cornerRadius = activity.dpToPx(12).toFloat()
                setStroke(
                    activity.dpToPx(2),
                    "#20DFFF".toColorInt()
                )
            }
        }

        val title = TextView(activity).apply {
            text = "AUDIO BLUETOOTH DETECTADO"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val deviceText = TextView(activity).apply {
            text = device.model
            setTextColor("#20DFFF".toColorInt())
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(
                0,
                activity.dpToPx(12),
                0,
                0
            )
        }

        val message = TextView(activity).apply {
            text = "No existe un perfil de latencia para este dispositivo.\n\n¿Quieres configurar la latencia ahora?"
            setTextColor("#D0D6E6".toColorInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(
                0,
                activity.dpToPx(14),
                0,
                activity.dpToPx(20)
            )
        }

        val btnAccept = createDialogButton(
            activity = activity,
            text = "CONFIGURAR"
        )

        val btnCancel = createDialogButton(
            activity = activity,
            text = "CANCELAR"
        )

        btnAccept.setOnClickListener {
            dialog.dismiss()

            val intent = Intent(
                activity,
                BluetoothLatencyActivity::class.java
            )

            activity.startActivity(intent)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        container.addView(title)
        container.addView(deviceText)
        container.addView(message)

        container.addView(
            btnAccept,
            createButtonParams(activity)
        )

        container.addView(
            btnCancel,
            createButtonParams(activity)
        )

        dialog.setContentView(container)

        dialog.setOnDismissListener {
            dialogShowing = false
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialog.show()

        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun createDialogButton(
        activity: Activity,
        text: String
    ): MaterialButton {
        return MaterialButton(activity).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 13f
            isAllCaps = true

            insetTop = 0
            insetBottom = 0

            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    "#101425".toColorInt()
                )

            strokeColor =
                android.content.res.ColorStateList.valueOf(
                    "#20DFFF".toColorInt()
                )

            strokeWidth = activity.dpToPx(2)
            cornerRadius = activity.dpToPx(8)

            minimumHeight = activity.dpToPx(46)
        }
    }

    private fun createButtonParams(
        activity: Activity
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            activity.dpToPx(46)
        ).apply {
            topMargin = activity.dpToPx(10)
        }
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}