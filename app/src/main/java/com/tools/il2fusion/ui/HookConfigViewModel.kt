package com.tools.il2fusion.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tools.il2fusion.config.HookConfigRepository
import com.tools.il2fusion.utils.DumpFileParser
import com.tools.il2fusion.utils.RvaUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val MAX_RVA_COUNT = 20

/**
 * Holds UI state for the hook configuration screen and coordinates data operations.
 */
class HookConfigViewModel(
    private val repository: HookConfigRepository = HookConfigRepository(),
    private val dumpFileParser: DumpFileParser = DumpFileParser()
) : ViewModel() {

    private val _state = MutableStateFlow(HookConfigState())
    val state: StateFlow<HookConfigState> = _state.asStateFlow()

    private val _events = Channel<HookConfigEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /**
     * Loads initial dump mode and RVA list; when empty, present a blank row without auto-save.
     */
    fun loadInitial(context: Context) {
        viewModelScope.launch {
            val payload = repository.loadConfig(context)
            if (payload.rvas.isEmpty()) {
                _state.value = HookConfigState(
                    rvaInputs = listOf(""),
                    savedCount = 0,
                    dumpModeEnabled = payload.dumpModeEnabled
                )
            } else {
                _state.value = HookConfigState(
                    rvaInputs = RvaUtils.formatInputs(payload.rvas),
                    savedCount = payload.rvas.size,
                    dumpModeEnabled = payload.dumpModeEnabled
                )
            }
        }
    }

    /**
     * Handles toggling dump mode and pushes the update to storage.
     */
    fun onDumpModeChanged(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.saveDumpMode(context, enabled)
            _state.value = _state.value.copy(dumpModeEnabled = enabled)
            _events.send(HookConfigEvent.ShowMessage(if (enabled) "已切换到 Dump 模式" else "已切换到 文本拦截 模式"))
        }
    }

    /**
     * Updates a single RVA input row in the state.
     */
    fun onRvaChanged(index: Int, value: String) {
        val updated = _state.value.rvaInputs.toMutableList()
        if (index in updated.indices) {
            updated[index] = value
            _state.value = _state.value.copy(rvaInputs = updated)
        }
    }

    /**
     * Adds a new blank RVA row if under max count, otherwise shows a message.
     */
    fun onAddRva() {
        val current = _state.value.rvaInputs
        if (current.size >= MAX_RVA_COUNT) {
            viewModelScope.launch {
                _events.send(HookConfigEvent.ShowMessage("最多支持 $MAX_RVA_COUNT 条 RVA"))
            }
            return
        }
        _state.value = _state.value.copy(rvaInputs = current + "")
    }

    /**
     * Removes the specified RVA row when multiple rows exist.
     */
    fun onRemoveRva(index: Int) {
        val current = _state.value.rvaInputs
        if (current.size <= 1 || index !in current.indices) return
        val updated = current.toMutableList()
        updated.removeAt(index)
        _state.value = _state.value.copy(rvaInputs = updated)
    }

    /**
     * Restores to a blank RVA row without persisting, allowing users to re-enter from scratch.
     */
    fun onRestoreDefault(context: Context) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                rvaInputs = listOf(""),
                savedCount = 0
            )
            _events.send(HookConfigEvent.ShowMessage("已恢复默认为空，请重新填写后保存"))
        }
    }

    /**
     * Saves cleaned RVA entries; rejects empty input with a message.
     */
    fun onSave(context: Context) {
        viewModelScope.launch {
            val cleaned = RvaUtils.normalizeInputs(_state.value.rvaInputs)
            if (cleaned.isEmpty()) {
                _events.send(HookConfigEvent.ShowMessage("至少填入一个有效的 RVA"))
                return@launch
            }
            repository.saveRvas(context, cleaned)
            val formatted = RvaUtils.formatInputs(cleaned)
            _state.value = _state.value.copy(
                rvaInputs = formatted,
                savedCount = cleaned.size
            )
            _events.send(HookConfigEvent.ShowMessage("已保存 ${cleaned.size} 个 RVA"))
        }
    }

    /**
     * Triggers a file parse flow to import RVAs from a dump file.
     */
    fun onFilePicked(context: Context, uri: Uri?) {
        if (uri == null) {
            viewModelScope.launch { _events.send(HookConfigEvent.ShowMessage("未选择文件")) }
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val added = dumpFileParser.extractRvas(context, uri, MAX_RVA_COUNT)
                if (added.isNotEmpty()) {
                    _state.value = _state.value.copy(rvaInputs = added)
                    _events.send(HookConfigEvent.ShowMessage("解析到 ${added.size} 条 RVA"))
                } else {
                    _events.send(HookConfigEvent.ShowMessage("未在文件中找到 set_Text 的 RVA"))
                }
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

/**
 * Immutable UI state container for the hook configuration screen.
 */
data class HookConfigState(
    val rvaInputs: List<String> = emptyList(),
    val dumpModeEnabled: Boolean = false,
    val savedCount: Int = 0,
    val isLoading: Boolean = false
)

/**
 * UI events dispatched to the screen for user feedback.
 */
sealed class HookConfigEvent {
    data class ShowMessage(val text: String) : HookConfigEvent()
}
