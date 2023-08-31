package net.sergeych.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import net.sergeych.tools.rememberDebouncer
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun InputLine(onChanged: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val fr = FocusRequester()
    val changer = rememberDebouncer(300.milliseconds) {
        onChanged(text)
    }
    TextField(
        text, {
            text = it
            changer.schedule()
        }, Modifier.fillMaxWidth().focusRequester(fr),
        label = {
            Text("enter search string here")
        },
        leadingIcon = {
//            Icon(imageVector = FeatherIcons.Search, "search")
            Image(
                painterResource("raysearch.svg"),
                "8-rays.dev search",
                modifier = Modifier.height(48.dp),
                contentScale = ContentScale.Fit
            )
        },
        maxLines = 1
    )

    LaunchedEffect(true) {
        delay(300)
        fr.requestFocus()
    }

}