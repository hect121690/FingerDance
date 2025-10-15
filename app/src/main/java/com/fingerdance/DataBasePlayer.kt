package com.fingerdance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBasePlayer(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game.db"
        private const val DATABASE_VERSION = 3

        const val TABLE_NIVELES = "niveles"
        const val COLUMN_CANAL = "canal"
        const val COLUMN_CANCION = "cancion"
        const val COLUMN_NIVEL = "nivel"
        const val COLUMN_PUNTAJE = "puntaje"
        const val COLUMN_GRADE = "grade"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createNivelesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_NIVELES (
                $COLUMN_CANAL TEXT,
                $COLUMN_CANCION TEXT,
                $COLUMN_NIVEL TEXT,
                $COLUMN_PUNTAJE TEXT,
                $COLUMN_GRADE TEXT
            )
        """
        db.execSQL(createNivelesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NIVELES")
        onCreate(db)
    }


    fun insertNivel(canal: String, cancion: String, nivel: String, puntaje: String, grade: String) {
        val db = this.writableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM $TABLE_NIVELES WHERE $COLUMN_CANAL = ? AND $COLUMN_CANCION = ? AND $COLUMN_NIVEL = ? AND $COLUMN_GRADE = ?",
            arrayOf(canal, cancion, nivel, grade)
        )

        if (!cursor.moveToFirst()) {
            val values = ContentValues().apply {
                put(COLUMN_CANAL, canal)
                put(COLUMN_CANCION, cancion)
                put(COLUMN_NIVEL, nivel)
                put(COLUMN_PUNTAJE, puntaje)
                put(COLUMN_GRADE, grade)
            }
            db.insert(TABLE_NIVELES, null, values)
        }

        cursor.close()
        db.close()
    }

    fun updatePuntaje(canal: String, cancion: String, nivel: String, nuevoPuntaje: String, nuevoGrade: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PUNTAJE, nuevoPuntaje)
            put(COLUMN_GRADE, nuevoGrade)
        }
        db.update(
            TABLE_NIVELES, values,
            "$COLUMN_CANAL = ? AND $COLUMN_CANCION = ? AND $COLUMN_NIVEL = ?",
            arrayOf(canal, cancion, nivel)
        )

        db.close()
    }

    fun getSongScores(db: SQLiteDatabase, canal: String, cancion: String): Array<ObjPuntaje> {
        val puntajes = arrayListOf<ObjPuntaje>()

        val cursor = db.rawQuery(
            "SELECT cancion, puntaje, grade FROM niveles WHERE canal = ? AND cancion = ?",
            arrayOf(canal, cancion)
        )

        // Agregamos cada puntaje a la lista `puntajes`
        if (cursor.moveToFirst()) {
            do {
                val canc = cursor.getString(cursor.getColumnIndexOrThrow("cancion")).toString()
                val punt = cursor.getString(cursor.getColumnIndexOrThrow("puntaje")).toString()
                val grad = cursor.getString(cursor.getColumnIndexOrThrow("grade")).toString()
                val obj = ObjPuntaje(cancion = canc,puntaje = punt, grade = grad)
                puntajes.add(obj)
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
