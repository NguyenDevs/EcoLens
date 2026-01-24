package com.nguyendevs.ecolens.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.TextSecondary

/** Expandable search bar composable. Matches the search bar behavior in activity_main.xml */
@Composable
fun SearchBar(
        isExpanded: Boolean,
        onExpandClick: () -> Unit,
        onSearchSubmit: (String) -> Unit,
        onCollapseClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val cardWidth by
            animateDpAsState(
                    targetValue = if (isExpanded) 320.dp else 56.dp,
                    label = "search_width"
            )

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            focusRequester.requestFocus()
        } else {
            searchText = ""
        }
    }

    EcoLensCard(
            modifier =
                    modifier.animateContentSize()
                            .height(56.dp)
                            .then(
                                    if (!isExpanded) {
                                        Modifier.clickable(
                                                interactionSource =
                                                        remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = onExpandClick
                                        )
                                    } else Modifier
                            ),
            borderColor = Color.Transparent,
            elevation = Dimens.ElevationSm
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.SpacingMd),
                verticalAlignment = Alignment.CenterVertically
        ) {
            // Search icon
            Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(Dimens.IconMd)
            )

            // Expanded content
            AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Text input
                    BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier =
                                    Modifier.weight(1f)
                                            .padding(horizontal = Dimens.SpacingMd)
                                            .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle =
                                    EcoLensTextStyles.Body1.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                    ),
                            cursorBrush = SolidColor(Primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions =
                                    KeyboardActions(
                                            onSearch = {
                                                onSearchSubmit(searchText)
                                                focusManager.clearFocus()
                                            }
                                    ),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (searchText.isEmpty()) {
                                        Text(
                                                text = stringResource(R.string.search_hint),
                                                style = EcoLensTextStyles.Body1,
                                                color = TextSecondary
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                    )

                    // Close button
                    IconButton(
                            onClick = {
                                searchText = ""
                                focusManager.clearFocus()
                                onCollapseClick()
                            },
                            modifier = Modifier.size(Dimens.TouchTargetMin)
                    ) {
                        Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
