package net.sergeych.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.sergeych.raysearch.Indexer
import net.sergeych.tools.stringFromHome

@Composable
fun SearchResults(list: List<Indexer.Result>,modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for( r in list) {
            Text(r.fd.path.stringFromHome())
        }
    }

}