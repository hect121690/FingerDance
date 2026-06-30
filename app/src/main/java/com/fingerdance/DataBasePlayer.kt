package com.fingerdance

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DataBasePlayer(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game.db"

        // subir versión para recrear la tabla
        private const val DATABASE_VERSION = 14

        const val TABLE_NIVELES = "niveles"

        const val COLUMN_CANAL = "canal"
        const val COLUMN_CANCION = "cancion"
        const val COLUMN_CHECKEDVALUES = "checkedvalues"
        const val COLUMN_PUNTAJE = "puntaje"
        const val COLUMN_GRADE = "grade"
    }

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NIVELES (
                $COLUMN_CANAL TEXT,
                $COLUMN_CANCION TEXT,
                $COLUMN_CHECKEDVALUES TEXT PRIMARY KEY,
                $COLUMN_PUNTAJE TEXT,
                $COLUMN_GRADE TEXT
            )
            """
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {

        if (oldVersion < 14) {

            db.execSQL("DROP TABLE IF EXISTS $TABLE_NIVELES")

            onCreate(db)
        }
    }

    fun insertNivel(
        canal: String,
        cancion: String,
        checkedValues: String
    ) {

        val db = writableDatabase

        val cursor = db.rawQuery(
            """
            SELECT 1
            FROM $TABLE_NIVELES
            WHERE $COLUMN_CHECKEDVALUES = ?
            """,
            arrayOf(checkedValues)
        )

        if (!cursor.moveToFirst()) {

            val values = ContentValues().apply {

                put(COLUMN_CANAL, canal)
                put(COLUMN_CANCION, cancion)
                put(COLUMN_CHECKEDVALUES, checkedValues)

                put(COLUMN_PUNTAJE, "0")
                put(COLUMN_GRADE, "")
            }

            db.insert(TABLE_NIVELES, null, values)
        }

        cursor.close()
        db.close()
    }

    fun updatePuntaje(
        checkedValues: String,
        nuevoPuntaje: String,
        nuevoGrade: String
    ) {

        val db = writableDatabase

        val values = ContentValues().apply {

            put(COLUMN_PUNTAJE, nuevoPuntaje)
            put(COLUMN_GRADE, nuevoGrade)
        }

        db.update(
            TABLE_NIVELES,
            values,
            "$COLUMN_CHECKEDVALUES = ?",
            arrayOf(checkedValues)
        )

        db.close()
    }

    fun getSongScores(
        db: SQLiteDatabase,
        canal: String,
        cancion: String
    ): Array<ObjPuntaje> {

        val puntajes = arrayListOf<ObjPuntaje>()

        val cursor = db.rawQuery(
            """
            SELECT
                $COLUMN_CHECKEDVALUES,
                $COLUMN_PUNTAJE,
                $COLUMN_GRADE
            FROM $TABLE_NIVELES
            WHERE $COLUMN_CANAL = ?
            AND $COLUMN_CANCION = ?
            """,
            arrayOf(canal, cancion)
        )

        if (cursor.moveToFirst()) {

            do {

                puntajes.add(
                    ObjPuntaje(
                        checkedValues = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COLUMN_CHECKEDVALUES
                            )
                        ),
                        puntaje = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COLUMN_PUNTAJE
                            )
                        ),
                        grade = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                COLUMN_GRADE
                            )
                        )
                    )
                )

            } while (cursor.moveToNext())
        }

        cursor.close()

        return puntajes.toTypedArray()
    }

    fun deleteCanal(canal: String) {

        val db = writableDatabase

        db.delete(
            TABLE_NIVELES,
            "$COLUMN_CANAL = ?",
            arrayOf(canal)
        )

        db.close()
    }

    fun deleteCancion(cancion: String) {

        val db = writableDatabase

        db.delete(
            TABLE_NIVELES,
            "$COLUMN_CANCION = ?",
            arrayOf(cancion)
        )

        db.close()
    }
}