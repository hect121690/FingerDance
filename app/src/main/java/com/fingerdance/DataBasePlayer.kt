package com.fingerdance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBasePlayer(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game.db"
        private const val DATABASE_VERSION = 8

        const val TABLE_NIVELES = "niveles"
        const val COLUMN_CANAL = "canal"
        const val COLUMN_CANCION = "cancion"
        const val COLUMN_NIVEL = "nivel"
        const val COLUMN_PUNTAJE = "puntaje"
        const val COLUMN_GRADE = "grade"
        const val COLUMN_TYPE = "type"
        const val COLUMN_PLAYER = "player"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createNivelesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_NIVELES (
                $COLUMN_CANAL TEXT,
                $COLUMN_CANCION TEXT,
                $COLUMN_NIVEL TEXT,
                $COLUMN_PUNTAJE TEXT,
                $COLUMN_GRADE TEXT,
                $COLUMN_TYPE TEXT,
                $COLUMN_PLAYER TEXT
            )
        """
        db.execSQL(createNivelesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NIVELES")
        onCreate(db)
    }

    fun insertNivel(canal: String, cancion: String, nivel: String, puntaje: String, grade: String, type: String, player: String) {
        val db = this.writableDatabase
        val cursor = db.rawQuery(
            """
                SELECT 1 FROM $TABLE_NIVELES 
                WHERE $COLUMN_CANAL = ? 
                AND $COLUMN_CANCION = ? 
                AND $COLUMN_NIVEL = ? 
                AND $COLUMN_GRADE = ?
                AND $COLUMN_TYPE = ?
                AND $COLUMN_PLAYER = ?
            """,
            arrayOf(canal, cancion, nivel, grade, type, player)
        )

        if (!cursor.moveToFirst()) {
            val values = ContentValues().apply {
                put(COLUMN_CANAL, canal)
                put(COLUMN_CANCION, cancion)
                put(COLUMN_NIVEL, nivel)
                put(COLUMN_PUNTAJE, puntaje)
                put(COLUMN_GRADE, grade)
                put(COLUMN_TYPE, type)
                put(COLUMN_PLAYER, player)
            }
            db.insert(TABLE_NIVELES, null, values)
        }

        cursor.close()
        db.close()
    }

    fun updatePuntaje( canal: String, cancion: String, nivel: String, type: String,player: String, nuevoPuntaje: String, nuevoGrade: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PUNTAJE, nuevoPuntaje)
            put(COLUMN_GRADE, nuevoGrade)
        }
        if(type == ""){
            type == "NORMAL"
        }
        db.update(
            TABLE_NIVELES,
            values,
            "$COLUMN_CANAL = ? AND $COLUMN_CANCION = ? AND $COLUMN_NIVEL = ? AND $COLUMN_TYPE = ? AND $COLUMN_PLAYER = ?",
            arrayOf(canal, cancion, nivel, type, player)
        )

        db.close()
    }


    fun getSongScores(db: SQLiteDatabase, canal: String, cancion: String): Array<ObjPuntaje> {
        val puntajes = arrayListOf<ObjPuntaje>()

        val cursor = db.rawQuery(
            """
                SELECT cancion, puntaje, grade, type, player
                FROM niveles 
                WHERE canal = ? AND cancion = ?
            """,
            arrayOf(canal, cancion)
        )

        if (cursor.moveToFirst()) {
            do {
                val canc = cursor.getString(cursor.getColumnIndexOrThrow("cancion"))
                val punt = cursor.getString(cursor.getColumnIndexOrThrow("puntaje"))
                val grad = cursor.getString(cursor.getColumnIndexOrThrow("grade"))
                val type = cursor.getString(cursor.getColumnIndexOrThrow("type"))
                val player = cursor.getString(cursor.getColumnIndexOrThrow("player"))

                puntajes.add(
                    ObjPuntaje(
                        cancion = canc,
                        puntaje = punt,
                        grade = grad,
                        type = type,
                        player = player
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        return puntajes.toTypedArray()
    }


    fun deleteCanal(canal: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NIVELES, "$COLUMN_CANAL = ?", arrayOf(canal))
        db.close()
    }

    fun deleteCancion(cancion: String) {
        val db = this.writableDatabase
        db.delete(TABLE_NIVELES, "$COLUMN_CANCION = ?", arrayOf(cancion))
        db.close()
    }

}
