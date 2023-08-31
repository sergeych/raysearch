package net.sergeych.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            var busy by remember { mutableStateOf(false) }
            var nothingFound by remember { mutableStateOf(false) }
            val list = remember { mutableStateListOf<Indexer.Result>() }
            val deb = rememberDebouncer(250.milliseconds, 250.milliseconds) {
                if (pattern == "") list.clear()
                else {
                    busy = true
                    list.clear()
                    runCatching {
                        list.addAll(indexer.search(pattern, 50))
                        nothingFound = list.isEmpty()
                    }
                    busy = false
                }
            }

            InputLine {
                pattern = it
                deb.schedule()
            }
            if (busy) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                if (list.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            if (nothingFound)
                                "nothing was found"
                            else
                                "enter text to search above",
                            color = Color.Gray
                        )
                    }

                } else
                    SearchResults(list, Modifier.weight(1f))
            }
            ScanProgressBar()
        }
    }
}