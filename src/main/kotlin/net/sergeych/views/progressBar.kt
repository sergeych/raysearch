package net.sergeych.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.sergeych.mp_tools.toDataSize
import net.sergeych.mp_tools.withThousandsSeparator
import net.sergeych.raysearch.Scanner
import net.sergeych.raysearch.SearchFolder

@Composable
fun ScanProgressBar(modifier: Modifier = Modifier.fillMaxWidth()) {
    var stats by remember { mutableStateOf(Scanner.stats.value) }
    var progress by remember { mutableStateOf(0f) }
    var scanning by remember { mutableStateOf(SearchFolder.isScanning.value) }

    Card(
        elevation = 6.dp,
        modifier = modifier,
        backgroundColor = Color(0xFFEEeeEE),
    ) {
        Row(
            Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (scanning || progress < 1f) {
                Text(
                    "Files: ${stats.processed.files.withThousandsSeparator("\u200A")} / ${
                        stats.total.files.withThousandsSeparator("\u200A")
                    }",
                    fontSize = 12.sp
                )
                if (scanning)
                    LinearProgressIndicator(Modifier.weight(1f).align(Alignment.CenterVertically))
                else
                    LinearProgressIndicator(progress, Modifier.weight(1f).align(Alignment.CenterVertically))
                Text(
                    "${stats.processed.size.toDataSize()} / ${stats.total.size.toDataSize()}",
                    fontSize = 12.sp
                )
            } else {
                Text(
                    """
                    Indexed ${stats.total.files.withThousandsSeparator("\u200A")} files, ${stats.total.size.toDataSize()}
                """.trimIndent(),
                    fontSize = 12.sp
                )
            }
        }

        LaunchedEffect(true) {
            launch {
                Scanner.stats.collect {
                    stats = it
                    progress = when {
                        stats.total.size == 0L -> 0f
                        stats.total.files > stats.processed.files ->
                            stats.processed.size.toFloat() / stats.total.size

                        else -> 1f
                    }
                }
            }
            launch {
                SearchFolder.isScanning.collect {
                    scanning = it
                }
            }
        }
    }
}