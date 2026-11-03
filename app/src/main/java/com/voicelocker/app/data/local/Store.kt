package com.voicelocker.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.voicelocker.app.data.model.Note
import com.voicelocker.app.util.hashSecret
import com.voicelocker.app.util.normalize
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent, Compose-observable store. Secrets are kept only as salted SHA-256 hashes.
 */
object Store {
    private lateinit var p: SharedPreferences
    private var rev by mutableIntStateOf(0)

    fun init(c: Context) {
        if (!::p.isInitialized) p = c.applicationContext.getSharedPreferences("vsl", Context.MODE_PRIVATE)
    }

    private fun <T> r(f: () -> T): T {
        @Suppress("UNUSED_VARIABLE")
        val x = rev
        return f()
    }

    private fun put(k: String, v: Any?) {
        p.edit().apply {
            when (v) {
                is Boolean -> putBoolean(k, v)
                is Int -> putInt(k, v)
                is String -> putString(k, v)
                null -> remove(k)
            }
        }.apply()
        rev++
    }

    var onboarded: Boolean
        get() = r { p.getBoolean("ob", false) }
        set(v) = put("ob", v)

    var voiceEnabled: Boolean
        get() = r { p.getBoolean("ve", false) }
        set(v) = put("ve", v)

    var fingerprintEnabled: Boolean
        get() = r { p.getBoolean("fp", false) }
        set(v) = put("fp", v)

    var systemLockOff: Boolean
        get() = r { p.getBoolean("sys", false) }
        set(v) = put("sys", v)

    var language: String
        get() = r { p.getString("lang", "English")!! }
        set(v) = put("lang", v)

    var themeIndex: Int
        get() = r { p.getInt("theme", 0) }
        set(v) = put("theme", v)

    /** 0 = follow system, 1 = light, 2 = dark */
    var themeMode: Int
        get() = r { p.getInt("mode", 0) }
        set(v) = put("mode", v)

    val hasVoice get() = r { p.contains("vh") }
    fun setVoicePhrase(t: String) = put("vh", hashSecret(normalize(t)))
    fun checkVoice(t: String) = normalize(t).isNotEmpty() && p.getString("vh", null) == hashSecret(normalize(t))

    val hasPin get() = r { p.contains("ph") }
    fun setPin(s: String) = put("ph", hashSecret("pin:$s"))
    fun checkPin(s: String) = p.getString("ph", null) == hashSecret("pin:$s")

    val hasPattern get() = r { p.contains("pt") }
    fun setPattern(l: List<Int>) = put("pt", hashSecret("pat:${l.joinToString("-")}"))
    fun checkPattern(l: List<Int>) = p.getString("pt", null) == hashSecret("pat:${l.joinToString("-")}")

    val hasSecurity get() = r { p.contains("sq") }
    val securityQuestion get() = r { p.getString("sq", null) }
    fun setSecurity(q: String, a: String) {
        put("sq", q)
        put("sa", hashSecret(normalize(a)))
    }
    fun checkAnswer(a: String) = p.getString("sa", null) == hashSecret(normalize(a))

    /** The lock screen is shown when at least one lock method and the mandatory security question are configured. */
    val lockEnabled get() = r { hasSecurity && ((voiceEnabled && hasVoice) || hasPin || hasPattern || fingerprintEnabled) }

    val notes: List<Note>
        get() = r {
            val raw = p.getString("notes", null) ?: return@r emptyList()
            val a = JSONArray(raw)
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Note(
                    o.getString("id"),
                    o.getString("title"),
                    o.getString("text"),
                    o.optString("summary").ifEmpty { null },
                    o.getLong("created")
                )
            }
        }

    fun saveNotes(l: List<Note>) = put(
        "notes",
        JSONArray(l.map {
            JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("text", it.text)
                .put("summary", it.summary ?: "")
                .put("created", it.created)
        }).toString()
    )
}

