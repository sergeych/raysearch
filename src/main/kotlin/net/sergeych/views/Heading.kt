package net.sergeych.views

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier.padding(top = 10.dp, bottom = 2.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
}