package com.prowllabs.prowl.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.ui.R

object ProwlBrandIcon {
    /** Matches [prowl_kit.png] (1021×274). */
    const val ASPECT_RATIO = 1021f / 274f

    /** Default compact height for toolbars and in-app branding. */
    val defaultHeight: Dp = 14.dp
}

@Composable
fun ProwlBrandIconView(
    modifier: Modifier = Modifier,
    height: Dp = ProwlBrandIcon.defaultHeight,
    contentDescription: String = "Prowl",
) {
    Image(
        painter = painterResource(R.drawable.prowl_kit),
        contentDescription = contentDescription,
        modifier = modifier
            .height(height)
            .aspectRatio(ProwlBrandIcon.ASPECT_RATIO),
        contentScale = ContentScale.Fit,
    )
}
