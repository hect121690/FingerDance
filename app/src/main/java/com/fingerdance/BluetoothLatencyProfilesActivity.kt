package com.fingerdance

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class BluetoothLatencyProfilesActivity :
    AppCompatActivity() {

    private lateinit var themes:
            SharedPreferences

    private lateinit var profilesContainer:
            LinearLayout

    private lateinit var txtEmpty:
            TextView

    private lateinit var txtDescription:
            TextView


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_bluetooth_latency_profiles
        )

        /*
         * IMPORTANTE:
         *
         * Si en tu proyecto "themes" se inicializa de otra
         * forma, usa AQUÍ exactamente la inicialización que
         * ya tienes.
         *
         * Lo importante es que todas las operaciones posteriores
         * usan este mismo SharedPreferences.
         */
        themes =
            getSharedPreferences(
                "themes",
                MODE_PRIVATE
            )

        profilesContainer =
            findViewById(
                R.id.profilesContainer
            )

        txtEmpty =
            findViewById(
                R.id.txtEmpty
            )

        txtDescription =
            findViewById(
                R.id.txtDescription
            )
    }


    override fun onResume() {

        super.onResume()

        /*
         * Cada vez que regresamos comprobamos nuevamente.
         *
         * Si mientras estaba abierta esta Activity el usuario
         * conectó audífonos:
         *
         * abrimos calibración automáticamente.
         */
        val bluetoothDevice =
            BluetoothLatencyProfileManager
                .getConnectedBluetoothDevice(
                    this
                )

        if (bluetoothDevice != null) {

            openCalibration()

            return
        }

        loadProfiles()
    }


    private fun openCalibration() {

        val intent =
            Intent(
                this,
                BluetoothLatencyActivity::class.java
            )

        startActivity(intent)

        /*
         * La cerramos porque esta pantalla solo debe
         * existir cuando NO tenemos Bluetooth.
         */
        finish()
    }


    // ========================================================================
    // PERFILES
    // ========================================================================

    private fun loadProfiles() {

        profilesContainer.removeAllViews()

        val profiles =
            BluetoothLatencyProfileManager.getAllProfiles()


        txtEmpty.visibility =
            if (profiles.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        profiles.forEach { profile ->

            addProfileView(
                profile
            )
        }
    }


    private fun addProfileView(
        profile:
        BluetoothLatencyProfileManager
        .BluetoothLatencyProfile
    ) {

        val view =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_bluetooth_latency_profile,
                    profilesContainer,
                    false
                )

        val txtModel =
            view.findViewById<TextView>(
                R.id.txtModel
            )

        val txtLatency =
            view.findViewById<TextView>(
                R.id.txtLatency
            )

        val btnEdit =
            view.findViewById<MaterialButton>(
                R.id.btnEdit
            )

        val btnDelete =
            view.findViewById<MaterialButton>(
                R.id.btnDelete
            )

        txtModel.text =
            profile.model

        txtLatency.text =
            "${profile.latencyMs} ms"

        btnEdit.setOnClickListener {

            showEditDialog(
                profile
            )
        }

        btnDelete.setOnClickListener {

            showDeleteDialog(
                profile
            )
        }

        profilesContainer.addView(
            view
        )
    }


    // ========================================================================
    // EDITAR
    // ========================================================================

    private fun showEditDialog(
        profile:
        BluetoothLatencyProfileManager
        .BluetoothLatencyProfile
    ) {

        val input =
            EditText(this).apply {

                inputType =
                    InputType.TYPE_CLASS_NUMBER

                setText(
                    profile.latencyMs.toString()
                )

                selectAll()
            }

        val container =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                val padding =
                    (24 *
                            resources
                                .displayMetrics
                                .density)
                        .toInt()

                setPadding(
                    padding,
                    padding / 2,
                    padding,
                    0
                )

                addView(input)
            }

        AlertDialog.Builder(this)
            .setTitle(
                profile.model
            )
            .setMessage(
                "Latencia del perfil en milisegundos"
            )
            .setView(
                container
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Guardar"
            ) { _, _ ->

                val latency =
                    input.text
                        .toString()
                        .toIntOrNull()
                        ?.coerceIn(
                            0,
                            1000
                        )
                        ?: return@setPositiveButton

                BluetoothLatencyProfileManager
                    .updateLatency(
                        profileId = profile.id,
                        latencyMs = latency
                    )

                loadProfiles()
            }
            .show()
    }


    // ========================================================================
    // BORRAR
    // ========================================================================

    private fun showDeleteDialog(
        profile:
        BluetoothLatencyProfileManager
        .BluetoothLatencyProfile
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Borrar perfil"
            )
            .setMessage(
                "¿Quieres borrar la calibración de ${profile.model}?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Borrar"
            ) { _, _ ->

                BluetoothLatencyProfileManager
                    .deleteProfile(
                        themes = themes,
                        profileId = profile.id
                    )

                loadProfiles()
            }
            .show()
    }
}