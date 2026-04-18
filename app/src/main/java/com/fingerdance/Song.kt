package com.fingerdance

import com.google.firebase.annotations.concurrent.Background
import java.io.Serializable

class Song(var title:String = "",
           var artist:String = "",
           var displayBpm: String = "",
           var rutaDisc: String = "",
           var rutaTitle : String = "",
           var rutaSong : String = "",
           var rutaPreview: String = "",
           var rutaBGA : String = "",
           var listKsf: ArrayList<Ksf> = arrayListOf(),
           var offset: Long = 0L,
           var channel: String = "",
           var isFavorite: Boolean = false,
           var isSSC: Boolean = false
) : Serializable


data class Ksf(var rutaKsf: String = "",
               var steps: String,
               var level: String = "",
               var rutaBitActive: String = "",
               var stepmaker: String = "",
               var typePlayer: String = "",
               var checkedValues: String = "",
               var typeSteps: String = "",
               var songFile: String = "",
               var chartName: String = "")

