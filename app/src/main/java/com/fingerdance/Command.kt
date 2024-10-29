package com.fingerdance

import android.graphics.Bitmap
import java.io.Serializable

class Command(var value:String,
              var descripcion:String,
              var rutaCommandImg : String,
              var listCommandValues : ArrayList<CommandValues>) : Serializable
{
}