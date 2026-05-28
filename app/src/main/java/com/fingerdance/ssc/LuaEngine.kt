import android.util.Log
import com.badlogic.gdx.Gdx
import com.fingerdance.luaFlare
import com.fingerdance.luaJudge
import com.fingerdance.luaNotes
import com.fingerdance.luaRecepts
import com.fingerdance.playerSong
import com.fingerdance.ssc.PlayerSsc
import com.fingerdance.ssc.PlayerSscHD
import com.fingerdance.ssc.PlayerSscHorizontal
import com.fingerdance.ssc.PlayerSscHorizontalHD
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.FileReader

class LuaEngine(
    playerSsc: PlayerSsc? = null,
    playerSscHorizontal: PlayerSscHorizontal? = null,
    playerSscHD: PlayerSscHD? = null,
    playerSscHorizontalHD: PlayerSscHorizontalHD? = null,
    private val widthNotes: Float
) {

    private val player: Any = when {
        playerSsc != null -> playerSsc
        playerSscHorizontal != null -> playerSscHorizontal
        playerSscHD != null -> playerSscHD
        playerSscHorizontalHD != null -> playerSscHorizontalHD
        else -> throw IllegalArgumentException("No player provided")
    }

    private val globals = JsePlatform.standardGlobals()

    init {
        globals.set("DESCRIPTION", LuaValue.valueOf(playerSong.type))
        globals.set("DIFFICULTY", LuaValue.valueOf(playerSong.difficulty))
        globals.set("CREDIT", LuaValue.valueOf(playerSong.stepMaker))
        globals.set("PLAYER", LuaValue.valueOf(playerSong.player))
        globals.set("CHARTNAME", LuaValue.valueOf(playerSong.chartName))
        globals.set("LEVEL", LuaValue.valueOf(playerSong.level))

        registerNotes()
        registerRecepts()
        registerFlare()
        registerJudge()
        registerFlash()
    }

    private fun triggerLuaFlash(duration: Long) {
        when (player) {
            is PlayerSsc -> player.triggerLuaFlash(duration)
            is PlayerSscHorizontal -> player.triggerLuaFlash(duration)
            is PlayerSscHD -> player.triggerLuaFlash(duration)
            is PlayerSscHorizontalHD -> player.triggerLuaFlash(duration)
        }
    }

    // =========================================================
    // NOTES
    // =========================================================

    private fun registerNotes() {
        val table = LuaTable()

        table.set("setX", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                
                luaNotes.screenX = widthNotes * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setY", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                
                luaNotes.screenY = Gdx.graphics.height * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZ", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaNotes.screenZ = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZoom", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaNotes.zoom = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setAlpha", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaNotes.alpha = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setRotate", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaNotes.rotation = arg.tofloat()
                return LuaValue.NIL
            }
        })

        globals.set("Notes", table)
    }

    // =========================================================
    // RECEPTS
    // =========================================================

    private fun registerRecepts() {
        val table = LuaTable()

        table.set("setX", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaRecepts.screenX = widthNotes * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setY", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                
                luaRecepts.screenY = Gdx.graphics.height * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZ", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaRecepts.screenZ = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZoom", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaRecepts.zoom = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setAlpha", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaRecepts.alpha = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setRotate", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaRecepts.rotation = arg.tofloat()
                return LuaValue.NIL
            }
        })

        globals.set("Recepts", table)
    }

    // =========================================================
    // FLARE
    // =========================================================

    private fun registerFlare() {
        val table = LuaTable()

        table.set("setX", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                
                luaFlare.screenX = widthNotes * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setY", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaFlare.screenY = Gdx.graphics.height * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZ", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaFlare.screenZ = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZoom", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaFlare.zoom = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setAlpha", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaFlare.alpha = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setRotate", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaFlare.rotation = arg.tofloat()
                return LuaValue.NIL
            }
        })

        globals.set("Flare", table)
    }

    // =========================================================
    // JUDGE
    // =========================================================

    private fun registerJudge() {
        val table = LuaTable()

        table.set("setX", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.screenX = widthNotes * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setY", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.screenY = Gdx.graphics.height * arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZ", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.screenZ = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setZoom", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.zoom = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setAlpha", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.alpha = arg.tofloat()
                return LuaValue.NIL
            }
        })

        table.set("setRotate", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                luaJudge.rotation = arg.tofloat()
                return LuaValue.NIL
            }
        })

        globals.set("Judge", table)
    }

    // =========================================================
    // FLASH
    // =========================================================

    private fun registerFlash() {
        val table = LuaTable()

        table.set("trigger", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val duration = (arg.tofloat() * 1000f).toLong()
                triggerLuaFlash(duration)
                return LuaValue.NIL
            }
        })

        globals.set("Flash", table)
    }

    // =========================================================
    // EXECUTE
    // =========================================================

    fun executeLua(path: String) {
        try {
            globals.load(FileReader(path), path).call()
        } catch (e: Exception) {
            Log.d("LUA_DEBUG", "Error ejecutando lua: $path\n${e.message}", e)
        }
    }
}