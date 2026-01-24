package com.nguyendevs.ecolens.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.Primary

/** EcoLens primary button. Matches Widget.App.Button from styles.xml */
@Composable
fun EcoLensButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        containerColor: Color = Primary,
        contentColor: Color = Color.White
) {
    Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp),
            enabled = enabled,
            shape = EcoLensCorners.Button,
            colors =
                    ButtonDefaults.buttonColors(
                            containerColor = containerColor,
                            contentColor = contentColor,
                            disabledContainerColor = containerColor.copy(alpha = 0.5f),
                            disabledContentColor = contentColor.copy(alpha = 0.5f)
                    ),
            contentPadding =
                    PaddingValues(
                            horizontal = Dimens.PaddingButtonHorizontal,
                            vertical = Dimens.PaddingButtonVertical
                    ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = Dimens.ElevationSm)
    ) {
        Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
        )
    }
}

/** EcoLens outlined button. Matches Widget.App.Button.Outlined from styles.xml */
@Composable
fun EcoLensOutlinedButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        contentColor: Color = Primary,
        leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().height(56.dp),
            enabled = enabled,
            shape = EcoLensCorners.Button,
            colors =
                    ButtonDefaults.outlinedButtonColors(
                            contentColor = contentColor,
                            disabledContentColor = contentColor.copy(alpha = 0.5f)
                    ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
            contentPadding =
                    PaddingValues(
                            horizontal = Dimens.PaddingButtonHorizontal,
                            vertical = Dimens.PaddingButtonVertical
                    )
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
        )
    }
}

/** EcoLens text button. Matches Widget.App.Button.Text from styles.xml */
@Composable
fun EcoLensTextButton(
        text: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        contentColor: Color = Primary
) {
    TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = EcoLensCorners.Button,
            colors =
                    ButtonDefaults.textButtonColors(
                            contentColor = contentColor,
                            disabledContentColor = contentColor.copy(alpha = 0.5f)
                    )
    ) {
        Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
        )
    }
}
