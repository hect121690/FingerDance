package com.fingerdance

import java.io.Serializable

class Song(var name:String,
           var artist:String,
           var rutaCancion : String,
           var rutaPrevVideo: String,
           var rutaBga : String,
           var displayBpm: String,
           var rutaDisc: String,
           var rutaBanner : String,
           var listLvs: MutableList<Lvs>
) : Serializable
{
}