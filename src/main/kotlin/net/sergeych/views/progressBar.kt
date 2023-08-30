package net.sergeych.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sergeych.mp_tools.toDataSize
import net.sergeych.mp_tools.withThousandsSeparator
import net.sergeych.raysearch.FileScanner

@Composable
fun ScanProgressBar() {
    var stats by remember { mutableStateOf(FileScanner.stats.value) }
    var progress by remember { mutableStateOf(0f) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Files: ${stats.processed.files.withThousandsSeparator()} / ${stats.total.files}")
        LinearProgressIndicator(progress, Modifier.weight(1f).align(Alignment.CenterVertically))
        Text("${stats.processed.size.toDataSize()} / ${stats.total.size.toDataSize()}")
    }

    LaunchedEffect(true) {
        FileScanner.stats.collect {
            stats = it
            progress = if(stats.total.size == 0L) 0f else stats.processed.size.toFloat() / stats.total.size
        }
    }

}