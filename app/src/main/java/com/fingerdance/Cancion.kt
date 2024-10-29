package com.fingerdance

import java.io.Serializable

class Cancion(var name:String,
              var artist:String,
              var bpm: String,
              var tickCount: String,
              var prevVideo: String,
              var rutaPrevVideo: String,
              var video: String,
              var song: String,
              var rutaBanner : String,
              var rutaCancion : String,
              var rutaSteps : String,
              var rutaVideo : String,
              var listLvs: MutableList<Lvs>
) : Serializable
{
}