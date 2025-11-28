package com.tools.textextracttool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tools.textextracttool.config.HookConfigStore
import com.tools.textextracttool.ui.theme.TextExtractToolTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TextExtractToolTheme {
                HookConfigScreen()
            }
        }
    }
}

@Composable
fun HookConfigScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rvaList = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val saved = HookConfigStore.loadRvas(context).map { it.toString() }
        if (saved.isEmpty()) {
            rvaList.add("0x1d236e8")
        } else {
            rvaList.addAll(saved)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("目标 RVA 列表（十六进制或十进制）")
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rvaList.forEachIndexed { index, value ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedTextField(
                                value = value,
                                onValueChange = { new -> rvaList[index] = new },
                                label = { Text("RVA #${index + 1}") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                            )
                            OutlinedButton(
                                onClick = { if (rvaList.size > 1) rvaList.removeAt(index) },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("删除")
                            }
                        }
                    }
                    OutlinedButton(onClick = { rvaList.add("") }) {
                        Text("添加一行")
                    }
                }
            }
            Divider()
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val parsed = rvaList.mapNotNull { parseRva(it) }
                            HookConfigStore.saveRvas(context, parsed)
                        }
                    }
                ) {
                    Text("保存到本地")
                }
            }
            Text(
                "提示：LSPosed 同时勾选多个 App 会导致 hook 仅生效于当前进程，请一次只勾选一个目标。",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HookConfigScreenPreview() {
    TextExtractToolTheme {
        HookConfigScreen()
    }
}

private fun parseRva(input: String): Long? {
    val value = input.trim()
    if (value.isEmpty()) return null
    return if (value.startsWith("0x", ignoreCase = true)) {
        value.removePrefix("0x").removePrefix("0X").toLongOrNull(16)
    } else {
        value.toLongOrNull(10)
    }
}
