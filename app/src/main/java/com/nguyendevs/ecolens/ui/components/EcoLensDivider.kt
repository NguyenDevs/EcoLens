package com.nguyendevs.ecolens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nguyendevs.ecolens.ui.theme.BorderLight
import com.nguyendevs.ecolens.ui.theme.Dimens

/** Horizontal divider matching Widget.App.Divider from styles.xml */
@Composable
fun EcoLensDivider(
        modifier: Modifier = Modifier,
        color: Color = BorderLight,
        thickness: androidx.compose.ui.unit.Dp = Dimens.DividerNormal
) {
    Box(modifier = modifier.fillMaxWidth().height(thickness).background(color))
}

/** Divider with horizontal padding */
@Composable
fun EcoLensDividerPadded(
        modifier: Modifier = Modifier,
        color: Color = BorderLight,
        horizontalPadding: androidx.compose.ui.unit.Dp = Dimens.SpacingXxs
) {
    Box(
            modifier =
                    modifier.fillMaxWidth()
                            .padding(horizontal = horizontalPadding)
                            .height(Dimens.DividerNormal)
                            .background(color)
    )
}
