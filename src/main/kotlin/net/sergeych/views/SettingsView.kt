package net.sergeych.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Trash2
import net.sergeych.raysearch.Params
import net.sergeych.raysearch.SearchFolder
import net.sergeych.raysearch.dbs
import net.sergeych.raysearch.save


@Preview
@Composable
fun SettingsPreview() {
    Column {
        SettingsView() {}
    }
}

@Preview
@Composable
fun SettingsView(
    modifier: Modifier = Modifier.padding(horizontal = 8.dp),
    onDone: () -> Unit
) {
    var runOnBackground by remember { mutableStateOf(Params.runInBackground) }
    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton({ onDone() }) {
                Icon(
                    FeatherIcons.ArrowLeft, "back",
                )
            }
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }
        LabelledCheckBox(runOnBackground, "Run in background", Modifier.padding(vertical = 4.dp)) {
            println("resetting 1 $runOnBackground")
            runOnBackground = !runOnBackground
            println("resetting 2 $runOnBackground")
            Params.copy(runInBackground = runOnBackground).save()
        }
        Text(
            "When application is in background, it monitors file changes and keep the index up to date even when you close the window. You can activate it back with the tray icon. Otherwise the application closes with its window.",
            fontSize = 12.sp
        )
        Heading("Indexed root paths:")
        val roots = dbs { SearchFolder.roots(it) }
        val userHome = System.getProperty("user.home")
        for (r in roots) {
            Card(Modifier.padding(4.dp).fillMaxWidth()) {
                Row {
                    Text(r.pathString, Modifier.padding(6.dp).weight(1f))
                    val enabled = r.pathString != userHome
                    IconButton({}, enabled = enabled) {
                        Image(
                            FeatherIcons.Trash2, "delete",
                            alpha = if (enabled) DefaultAlpha else 0.1f
                        )
                    }
                }
            }
        }
    }
}