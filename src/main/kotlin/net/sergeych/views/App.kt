package net.sergeych.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sergeych.raysearch.Indexer
import net.sergeych.raysearch.indexer
import net.sergeych.tools.rememberDebouncer
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
            Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            var pattern by remember { mutableStateOf("") }
            val list = remember { mutableStateListOf<Indexer.Result>() }
            val deb = rememberDebouncer(250.milliseconds, 250.milliseconds) {
                if( pattern == "") list.clear()
                else {
                    val result = indexer.search(pattern)
                    list.clear()
                    list.addAll(result)
                }
            }

            InputLine {
                pattern = it
                deb.schedule()
            }
            ScanProgressBar()
            SearchResults(list,Modifier.fillMaxSize())
        }
    }
}