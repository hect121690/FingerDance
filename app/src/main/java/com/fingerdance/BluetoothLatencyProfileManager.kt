package com.fingerdance

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import java.security.MessageDigest

object BluetoothLatencyProfileManager {

    /*
     * Índice con todos los perfiles que tenemos guardados.
     */
    private const val KEY_PROFILE_IDS =
        "bluetooth_latency_profile_ids"

    /*
     * Prefijos.
     */
    private const val PREFIX_MODEL =
        "bluetooth_latency_model_"

    private const val PREFIX_LATENCY =
        "bluetooth_latency_value_"


    // ========================================================================
    // DATA CLASSES
    // ========================================================================

    data class BluetoothAudioDevice(
        val id: String,
        val model: String,
        val address: String?,
        val audioDeviceInfo: AudioDeviceInfo
    )

    data class BluetoothLatencyProfile(
        val id: String,
        val model: String,
        val latencyMs: Int
    )


    // ========================================================================
    // DETECTAR BLUETOOTH
    // ========================================================================

    fun getConnectedBluetoothDevice(
        context: Context
    ): BluetoothAudioDevice? {

        val audioManager =
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as AudioManager

        val outputs =
            audioManager.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )

        val bluetoothDevice =
            outputs.firstOrNull {
                isBluetoothAudioDevice(it)
            }
                ?: return null

        val model =
            bluetoothDevice.productName
                ?.toString()
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "Audífonos Bluetooth"

        /*
         * getAddress() existe desde API 28.
         *
         * En algunos fabricantes puede venir vacío,
         * por eso tenemos fallback.
         */
        val address =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                bluetoothDevice.address
                    .takeIf {
                        it.isNotBlank()
                    }

            } else {

                null
            }

        /*
         * IMPORTANTE:
         *
         * No guardamos directamente la dirección dentro de
         * las llaves de SharedPreferences.
         *
         * Creamos un ID hash.
         */
        val sourceId =
            address ?: "MODEL:$model"

        val id =
            createStableId(sourceId)

        return BluetoothAudioDevice(
            id = id,
            model = model,
            address = address,
            audioDeviceInfo = bluetoothDevice
        )
    }


    private fun isBluetoothAudioDevice(
        device: AudioDeviceInfo
    ): Boolean {

        return when (device.type) {

            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> true

            /*
             * Hearing Aid fue agregado en API 28.
             */
            AudioDeviceInfo.TYPE_HEARING_AID -> true

            else -> {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.S
                ) {

                    device.type ==
                            AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            device.type ==
                            AudioDeviceInfo.TYPE_BLE_SPEAKER

                } else {

                    false
                }
            }
        }
    }


    // ========================================================================
    // OBTENER PERFIL
    // ========================================================================

    fun getProfile(
        themes: SharedPreferences,
        deviceId: String
    ): BluetoothLatencyProfile? {

        val latencyKey =
            PREFIX_LATENCY + deviceId

        if (!themes.contains(latencyKey))
            return null

        val model =
            themes.getString(
                PREFIX_MODEL + deviceId,
                "Audífonos Bluetooth"
            ) ?: "Audífonos Bluetooth"

        val latency =
            themes.getInt(
                latencyKey,
                0
            )

        return BluetoothLatencyProfile(
            id = deviceId,
            model = model,
            latencyMs = latency
        )
    }


    fun getProfileForConnectedDevice(
        context: Context,
        themes: SharedPreferences
    ): BluetoothLatencyProfile? {

        val device =
            getConnectedBluetoothDevice(context)
                ?: return null

        return getProfile(
            themes,
            device.id
        )
    }


    // ========================================================================
    // GUARDAR
    // ========================================================================

    fun saveProfile(
        themes: SharedPreferences,
        device: BluetoothAudioDevice,
        latencyMs: Int
    ) {

        val currentIds =
            themes.getStringSet(
                KEY_PROFILE_IDS,
                emptySet()
            )
                ?.toMutableSet()
                ?: mutableSetOf()

        currentIds.add(
            device.id
        )

        themes.edit()
            .putStringSet(
                KEY_PROFILE_IDS,
                currentIds
            )
            .putString(
                PREFIX_MODEL + device.id,
                device.model
            )
            .putInt(
                PREFIX_LATENCY + device.id,
                latencyMs
            )
            .apply()
    }


    fun updateLatency(
        themes: SharedPreferences,
        profileId: String,
        latencyMs: Int
    ) {

        themes.edit()
            .putInt(
                PREFIX_LATENCY + profileId,
                latencyMs
            )
            .apply()
    }


    // ========================================================================
    // BORRAR
    // ========================================================================

    fun deleteProfile(
        themes: SharedPreferences,
        profileId: String
    ) {

        val currentIds =
            themes.getStringSet(
                KEY_PROFILE_IDS,
                emptySet()
            )
                ?.toMutableSet()
                ?: mutableSetOf()

        currentIds.remove(
            profileId
        )

        themes.edit()
            .putStringSet(
                KEY_PROFILE_IDS,
                currentIds
            )
            .remove(
                PREFIX_MODEL + profileId
            )
            .remove(
                PREFIX_LATENCY + profileId
            )
            .apply()
    }


    // ========================================================================
    // LISTAR
    // ========================================================================

    fun getAllProfiles(
        themes: SharedPreferences
    ): List<BluetoothLatencyProfile> {

        val ids =
            themes.getStringSet(
                KEY_PROFILE_IDS,
                emptySet()
            )
                ?: emptySet()

        return ids.mapNotNull { id ->

            getProfile(
                themes,
                id
            )

        }
            .sortedBy {
                it.model.lowercase()
            }
    }


    // ========================================================================
    // LATENCIA PARA EL JUEGO
    // ========================================================================

    fun getCurrentLatency(
        context: Context,
        themes: SharedPreferences
    ): Int {

        /*
         * NO hay Bluetooth:
         *
         * speaker
         * cable
         * USB
         * etc.
         *
         * = 0 ms
         */
        val device =
            getConnectedBluetoothDevice(context)
                ?: return 0

        /*
         * Hay Bluetooth pero nunca fue calibrado:
         *
         * = 0 ms
         */
        val profile =
            getProfile(
                themes,
                device.id
            )
                ?: return 0

        return profile.latencyMs
    }


    // ========================================================================
    // ID
    // ========================================================================

    private fun createStableId(
        value: String
    ): String {

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray()
                )

        /*
         * 16 bytes son suficientes para nuestras llaves.
         */
        return digest
            .take(16)
            .joinToString("") {
                "%02x".format(it)
            }
    }
}