package net.sergeych.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import net.sergeych.raysearch.Indexer
import net.sergeych.raysearch.indexer
import net.sergeych.tools.rememberDebouncer
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun App() {
    MaterialTheme {
        Column(
//            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            var pattern by remember { mutableStateOf("") }
            val list = remember { mutableStateListOf<Indexer.Result>() }
            val deb = rememberDebouncer(250.milliseconds, 250.milliseconds) {
                if (pattern == "") list.clear()
                else {
                    list.clear()
                    list.addAll(indexer.search(pattern,50))
                }
            }

            InputLine {
                pattern = it
                deb.schedule()
            }
            SearchResults(list, Modifier.weight(1f))
            ScanProgressBar()
        }
    }
}