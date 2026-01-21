package com.fingerdance

import java.io.Serializable

class Channels(
    var nombre: String,
    var descripcion: String,
    var banner: String,
    var listCanciones: ArrayList<Song> = arrayListOf(),
    var listCancionesKsf: ArrayList<SongKsf> = arrayListOf()
) : Serializable