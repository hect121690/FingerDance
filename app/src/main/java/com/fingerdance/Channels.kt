package com.fingerdance

import java.io.Serializable

class Channels(
    var nombre: String,
    var descripcion: String,
    var banner: String,
    var ruta: String,
    var listCanciones: ArrayList<Cancion> = arrayListOf(),
    var listCancionesKsf: ArrayList<SongKsf> = arrayListOf()
) : Serializable {



}