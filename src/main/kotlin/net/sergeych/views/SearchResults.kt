package net.sergeych.views

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sergeych.raysearch.Indexer
import net.sergeych.tools.stringFromHome

@Composable
fun SearchResults(list: List<Indexer.Result>,modifier: Modifier = Modifier) {
    val vscroll = rememberScrollState()

    Box(modifier) {
        Column(
            Modifier.animateContentSize().verticalScroll(vscroll),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in list) {
                Text(r.fd.path.stringFromHome())
            }
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(vscroll)
        )
    }
}