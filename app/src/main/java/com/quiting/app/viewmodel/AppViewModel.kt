package com.quiting.app.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quiting.app.data.LinkParser
import com.quiting.app.data.ProfileStore
import com.quiting.app.data.VpnProfile
import com.quiting.app.ui.AppStyle
import com.quiting.app.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ProfileStore(app)

    val profiles = mutableStateListOf<VpnProfile>().apply { addAll(store.loadProfiles()) }

    var selectedId by mutableStateOf(store.getString("selected", ""))
        private set

    var style by mutableStateOf(
        runCatching { AppStyle.valueOf(store.getString("style", AppStyle.MATERIAL_YOU.name)) }
            .getOrDefault(AppStyle.MATERIAL_YOU)
    )
        private set

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(store.getString("mode", ThemeMode.SYSTEM.name)) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
        private set

    var busy by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    val selected: VpnProfile?
        get() = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()

    private fun persist() = store.saveProfiles(profiles.toList())

    fun chooseStyle(s: AppStyle) {
        style = s
        store.putString("style", s.name)
    }

    fun chooseMode(m: ThemeMode) {
        themeMode = m
        store.putString("mode", m.name)
    }

    fun select(id: String) {
        selectedId = id
        store.putString("selected", id)
    }

    fun toggleFavorite(id: String) {
        val i = profiles.indexOfFirst { it.id == id }
        if (i >= 0) {
            profiles[i] = profiles[i].copy(favorite = !profiles[i].favorite)
            persist()
        }
    }

    fun remove(id: String) {
        profiles.removeAll { it.id == id }
        persist()
        if (selectedId == id) select(profiles.firstOrNull()?.id ?: "")
    }

    fun showMessage(text: String) {
        message = text
    }

    fun dismissMessage() {
        message = null
    }

    fun importText(text: String) {
        val parsed = LinkParser.parseMany(text)
        if (parsed.isEmpty()) {
            message = "No vless, vmess, trojan or ss links found"
            return
        }
        profiles.addAll(parsed)
        persist()
        if (selectedId.isBlank()) select(parsed.first().id)
        message = "Imported ${parsed.size}"
    }

    fun importSubscription(url: String) {
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { runCatching { LinkParser.fetchSubscription(url) } }
            busy = false
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    message = "Subscription has no supported links"
                } else {
                    profiles.addAll(list)
                    persist()
                    if (selectedId.isBlank()) select(list.first().id)
                    message = "Imported ${list.size} from subscription"
                }
            }.onFailure {
                message = "Subscription failed: ${it.message ?: "unknown error"}"
            }
        }
    }
}
