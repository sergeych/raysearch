package net.sergeych.views

import androidx.compose.animation.animateContentSize
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sergeych.Config
import net.sergeych.raysearch.Indexer
import net.sergeych.sprintf.sprintf
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.getLastModifiedTime

@Preview
@Composable
fun TestCard() {
    FileCard(Paths.get("/to/loo/nhpath/exists/q/some_file.java"))
}
@Composable fun FileCard(file: Path) {
    Card(Modifier.fillMaxWidth().padding(4.dp)) {
        Row(Modifier.padding(2.dp).padding(end=5.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(file.fileName.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.clickable {
                        Config.openFile(file)
                    })
                Text(file.parent.toString(), fontSize = 12.sp,
                    modifier = Modifier.clickable {
                        Config.openFolder(file)
                    })
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val d = file.getLastModifiedTime().toInstant()
                Text("%tT".sprintf(d), fontSize = 14.sp)
                Text("%1!tF".sprintf(d), fontSize = 14.sp)
            }
        }
    }

}

@Composable
fun SearchResults(list: List<Indexer.Result>,modifier: Modifier = Modifier) {
    val vscroll = rememberScrollState()

    Box(modifier) {
        Column(
            Modifier.animateContentSize().verticalScroll(vscroll),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (r in list) FileCard(r.fd.path)
        }
        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(vscroll)
        )
    }
}