package com.tools.il2fusion.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tools.il2fusion.ui.theme.Il2FusionTheme
import kotlinx.coroutines.flow.collect

/**
 * Hosts the root Compose tree with a clean single-page layout.
 */
@Composable
fun HookConfigApp(viewModel: HookConfigViewModel = viewModel()) {
    Il2FusionTheme {
        val context = LocalContext.current
        val focusManager = LocalFocusManager.current
        val state by viewModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val filePickerLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            viewModel.onFilePicked(context, uri)
        }
        val tabs = remember { SideTab.entries.toList() }
        var selectedTab by rememberSaveable { mutableStateOf(SideTab.HookConfig) }

        LaunchedEffect(Unit) {
            viewModel.loadInitial(context)
        }

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is HookConfigEvent.ShowMessage -> snackbarHostState.showSnackbar(event.text)
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { inner ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                SideNav(
                    tabs = tabs,
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(min = 64.dp, max = 80.dp)
                )

                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    when (selectedTab) {
                        SideTab.HookConfig -> HookConfigScreen(
                            state = state,
                            onDumpModeChanged = { viewModel.onDumpModeChanged(context, it) },
                            onRvaChanged = viewModel::onRvaChanged,
                            onAddRva = viewModel::onAddRva,
                            onRemoveRva = viewModel::onRemoveRva,
                            onSave = { viewModel.onSave(context) },
                            onRestoreDefault = { viewModel.onRestoreDefault(context) },
                            onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

enum class SideTab(val title: String) {
    HookConfig("Hook")
}

/**
 * Simple left rail for feature tabs, ready to expand with more pages.
 */
@Composable
fun SideNav(
    tabs: List<SideTab>,
    selected: SideTab,
    onSelect: (SideTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tabs.forEach { tab ->
            SideNavItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelect(tab) }
            )
        }
    }
}

@Composable
private fun SideNavItem(
    tab: SideTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier
            .size(52.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = contentColor
            )
        }
    }
}

/**
 * Lays out the full hook configuration experience with stylized sections.
 */
@Composable
fun HookConfigScreen(
    state: HookConfigState,
    onDumpModeChanged: (Boolean) -> Unit,
    onRvaChanged: (Int, String) -> Unit,
    onAddRva: () -> Unit,
    onRemoveRva: (Int) -> Unit,
    onSave: () -> Unit,
    onRestoreDefault: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(state.dumpModeEnabled, state.savedCount)

            ModeCard(
                dumpModeEnabled = state.dumpModeEnabled,
                onDumpModeChanged = onDumpModeChanged
            )

            FileCard(
                isLoading = state.isLoading,
                onPickFile = onPickFile
            )

            RvaEditorCard(
                rvaInputs = state.rvaInputs,
                onRvaChanged = onRvaChanged,
                onAddRva = onAddRva,
                onRemoveRva = onRemoveRva,
                savedCount = state.savedCount
            )

            ActionRow(
                onSave = onSave,
                onRestoreDefault = onRestoreDefault
            )

            FooterNote()
        }
    }
}

/**
 * Shows the hero header with quick stats and context.
 */
@Composable
fun HeaderCard(dumpModeEnabled: Boolean, savedCount: Int) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "il2Fusion",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = if (dumpModeEnabled) "Dump 模式已开启" else "文本拦截模式",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(text = "已保存 $savedCount 条 RVA")
                Badge(text = if (dumpModeEnabled) "仅 Dump" else "拦截 & 记录")
            }
        }
    }
}

/**
 * Visual tag used to surface small counters or statuses.
 */
@Composable
fun Badge(text: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Renders dump mode toggle with explanatory copy.
 */
@Composable
fun ModeCard(
    dumpModeEnabled: Boolean,
    onDumpModeChanged: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Il2CppDumper 模式",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = if (dumpModeEnabled) "仅执行 dump，不拦截文本" else "关闭后仅拦截文本并记录数据库",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = dumpModeEnabled,
                onCheckedChange = onDumpModeChanged
            )
        }
    }
}

/**
 * Provides file selection and shows loading state when parsing.
 */
@Composable
fun FileCard(
    isLoading: Boolean,
    onPickFile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "从 dump 文件解析 RVA",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "打开 Download 目录选择 .cs dump 文件，自动抓取 set_Text 上方的 RVA。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("选择文件并解析")
            }
            AnimatedVisibility(visible = isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Displays the editable RVA list with add/remove controls.
 */
@Composable
fun RvaEditorCard(
    rvaInputs: List<String>,
    onRvaChanged: (Int, String) -> Unit,
    onAddRva: () -> Unit,
    onRemoveRva: (Int) -> Unit,
    savedCount: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "目标 RVA 列表",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "当前已保存：$savedCount 个",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            rvaInputs.forEachIndexed { index, value ->
                RvaField(
                    index = index,
                    value = value,
                    onValueChange = { onRvaChanged(index, it) },
                    onRemove = { onRemoveRva(index) },
                    isRemovable = rvaInputs.size > 1
                )
            }
            OutlinedButton(
                onClick = onAddRva,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("添加一行")
            }
        }
    }
}

/**
 * Single RVA row with text input and optional remove button.
 */
@Composable
fun RvaField(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
    isRemovable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("RVA #${index + 1}") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
        )
        OutlinedButton(
            onClick = onRemove,
            enabled = isRemovable
        ) {
            Text("删除")
        }
    }
}

/**
 * Action row for saving and restoring defaults.
 */
@Composable
fun ActionRow(
    onSave: () -> Unit,
    onRestoreDefault: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f)
        ) {
            Text("保存到本地")
        }
        OutlinedButton(
            onClick = onRestoreDefault,
            modifier = Modifier.weight(1f)
        ) {
            Text("恢复默认")
        }
    }
}

/**
 * Footer hint reminding users to limit LSPosed selection.
 */
@Composable
fun FooterNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "提示：LSPosed 同时勾选多个 App 会导致 hook 仅生效于当前进程，请一次只勾选一个目标。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
