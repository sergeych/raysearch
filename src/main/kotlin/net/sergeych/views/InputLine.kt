package net.sergeych.views

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import kotlinx.coroutines.delay

@Composable
fun InputLine() {
    var text by remember { mutableStateOf("") }
    val fr = FocusRequester()

    TextField(
        text, { text = it }, Modifier.fillMaxWidth().focusRequester(fr),
        label = {
            Text("enter search string here")
        },
        leadingIcon = {
            Icon(imageVector = FeatherIcons.Search, "search")
        },
        maxLines = 1
    )

    LaunchedEffect(true) {
        delay(300)
        fr.requestFocus()
    }

}