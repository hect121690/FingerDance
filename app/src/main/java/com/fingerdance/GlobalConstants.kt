package com.fingerdance

import com.google.firebase.database.FirebaseDatabase

// ========== CONSTANTES DE API ==========
const val API_KEY = "AIzaSyCL1ukVSzaKtIZZo3PFqfHXdlWIAxD1hGM"
const val FOLDER_ID = "19cM-WcAJyzo7w-7sbrPUzufMu_-gi9bS"
const val FOLDER_ID_THEMES = "1mqNKLVyhcQ8I7rXOD4z9Sg1Glrv8YVDX"
const val FOLDER_ID_CHANNELS_BGA = "1YAAWzSJw6o2h8G4cj80fle6G45wf937p"
const val KEY_CHANNELS_CACHE = "channels_cache"

// ========== VARIABLES GLOBALES - DRIVE ==========
var listFilesDrive = arrayListOf<Pair<String, String>>()
var listThemesDrive = arrayListOf<Pair<String, String>>()
var listChannelsDrive = mutableListOf<MainActivity.ChannelsDrive>()

// ========== VARIABLES GLOBALES - USUARIO ==========
var userName = ""
var firebaseDatabase : FirebaseDatabase? = null
var listGlobalRanking = arrayListOf<Cancion>()
var listAllowDevices = arrayListOf<String>()
var deviceIdFind = ""
var idSala = ""

// ========== VARIABLES GLOBALES - UI ==========
var medidaFlechas = 0f
var heightLayoutBtns = 0f
var heightBtns  = 0f
var widthBtns = 0f
var padPositions = listOf<Array<Float>>()
var padPositionsHD = listOf<Array<Float>>()
var touchAreas = listOf<Array<Float>>()
var colWidth = 0f

// ========== VARIABLES GLOBALES - VERSION ==========
var numberUpdate = ""
var versionUpdate = ""
var flagActiveAllows = false

// ========== VARIABLES GLOBALES - SETTINGS ==========
var showPadB : Int = 0
var hideImagesPadA : Boolean = false
var skinPad = ""
var alphaPadB = 1f
