package net.sergeych.views

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import net.sergeych.raysearch.Params
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

    }
}