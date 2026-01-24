package com.nguyendevs.ecolens.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.components.EcoLensCard
import com.nguyendevs.ecolens.ui.components.EcoLensDivider
import com.nguyendevs.ecolens.ui.theme.Dimens
import com.nguyendevs.ecolens.ui.theme.EcoLensTextStyles
import com.nguyendevs.ecolens.ui.theme.EcoLensTheme
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.PrimaryDark

/** Settings screen composable. Replaces screen_settings.xml (666 lines) */
@Composable
fun SettingsScreen(
        currentLanguage: String,
        isDarkMode: Boolean,
        isLoggedIn: Boolean,
        onLanguageClick: () -> Unit,
        onDarkModeToggle: (Boolean) -> Unit,
        onFeedbackClick: () -> Unit,
        onFacebookClick: () -> Unit,
        onInstagramClick: () -> Unit,
        onTiktokClick: () -> Unit,
        onAboutClick: () -> Unit,
        onChangeUsernameClick: () -> Unit,
        onChangePasswordClick: () -> Unit,
        onLinkGoogleClick: () -> Unit,
        onDeleteAccountClick: () -> Unit,
        onLogoutClick: () -> Unit,
        modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var isAccountExpanded by remember { mutableStateOf(false) }

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(top = 10.dp, bottom = 10.dp)
    ) {
        // Header
        Text(
                text = stringResource(R.string.settings_title),
                style = EcoLensTextStyles.Display,
                color = Primary,
                modifier =
                        Modifier.padding(
                                start = Dimens.PaddingScreenHorizontal,
                                end = Dimens.PaddingScreenHorizontal,
                                bottom = 15.dp
                        )
        )

        // Divider
        EcoLensDivider(color = EcoLensTheme.extendedColors.borderNormal)

        // Scrollable content
        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(
                                        start = Dimens.PaddingScreenHorizontal,
                                        end = Dimens.PaddingScreenHorizontal,
                                        top = 10.dp,
                                        bottom = 14.dp
                                )
        ) {
            // Preferences Section
            SectionHeader(text = stringResource(R.string.preferences))

            EcoLensCard {
                Column {
                    // Language Option
                    SettingsItem(
                            icon = Icons.Default.Language,
                            title = stringResource(R.string.language_label),
                            onClick = onLanguageClick,
                            trailing = {
                                Text(
                                        text = currentLanguage,
                                        style = EcoLensTextStyles.Body1,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                )
                            }
                    )

                    SettingsDivider()

                    // Dark Mode Option
                    SettingsItem(
                            icon =
                                    if (isDarkMode) Icons.Outlined.DarkMode
                                    else Icons.Outlined.LightMode,
                            title = stringResource(R.string.dark_mode),
                            onClick = { onDarkModeToggle(!isDarkMode) },
                            trailing = {
                                Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = null,
                                        colors =
                                                SwitchDefaults.colors(
                                                        checkedThumbColor = Primary,
                                                        checkedTrackColor =
                                                                Primary.copy(alpha = 0.5f)
                                                )
                                )
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Support Section
            SectionHeader(text = stringResource(R.string.support))

            EcoLensCard {
                SettingsItem(
                        icon = Icons.Default.Email,
                        title = stringResource(R.string.email_support),
                        onClick = onFeedbackClick
                )
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // About Section
            SectionHeader(text = stringResource(R.string.about_label))

            EcoLensCard {
                Column {
                    // Facebook
                    SettingsItemWithDrawable(
                            iconResId = R.drawable.ic_facebook,
                            title = stringResource(R.string.follow_on_facebook),
                            onClick = onFacebookClick
                    )

                    SettingsDivider()

                    // Instagram
                    SettingsItemWithDrawable(
                            iconResId = R.drawable.ic_instagram,
                            title = stringResource(R.string.follow_on_instagram),
                            onClick = onInstagramClick
                    )

                    SettingsDivider()

                    // TikTok
                    SettingsItemWithDrawable(
                            iconResId = R.drawable.ic_tiktok,
                            title = stringResource(R.string.follow_on_tiktok),
                            onClick = onTiktokClick
                    )

                    SettingsDivider()

                    // About
                    SettingsItem(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.about_app_title),
                            onClick = onAboutClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.SpacingLg))

            // Account Section
            SectionHeader(text = stringResource(R.string.account))

            EcoLensCard {
                Column {
                    // Account Details (Expandable)
                    ExpandableSettingsItem(
                            icon = Icons.Default.Person,
                            title = stringResource(R.string.account_details),
                            isExpanded = isAccountExpanded,
                            onClick = { isAccountExpanded = !isAccountExpanded }
                    )

                    // Expandable Content
                    AnimatedVisibility(
                            visible = isAccountExpanded && isLoggedIn,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(start = 48.dp, end = Dimens.SpacingMd)) {
                            SettingsDivider()

                            // Change Username
                            SubSettingsItem(
                                    text = stringResource(R.string.change_username),
                                    onClick = onChangeUsernameClick
                            )

                            SettingsDivider()

                            // Change Password
                            SubSettingsItem(
                                    text = stringResource(R.string.change_password),
                                    onClick = onChangePasswordClick
                            )

                            SettingsDivider()

                            // Link Google
                            SubSettingsItem(
                                    text = stringResource(R.string.link_google_account),
                                    onClick = onLinkGoogleClick
                            )

                            SettingsDivider()

                            // Delete Account
                            SubSettingsItem(
                                    text = stringResource(R.string.delete_account),
                                    textColor = MaterialTheme.colorScheme.error,
                                    onClick = onDeleteAccountClick
                            )
                        }
                    }

                    SettingsDivider()

                    // Logout
                    SettingsItem(
                            icon = Icons.Default.Logout,
                            title = stringResource(R.string.logout),
                            onClick = onLogoutClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
            text = text,
            style = EcoLensTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Dimens.SpacingXxs, bottom = Dimens.SpacingSm)
    )
}

@Composable
private fun SettingsItem(
        icon: ImageVector,
        title: String,
        onClick: () -> Unit,
        trailing: @Composable (() -> Unit)? = null
) {
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier = Modifier.size(Dimens.IconLg).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.SpacingMd))

        Text(
                text = title,
                style = EcoLensTextStyles.Body1,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark,
                modifier = Modifier.weight(1f)
        )

        trailing?.invoke()
    }
}

@Composable
private fun SettingsItemWithDrawable(iconResId: Int, title: String, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier = Modifier.size(Dimens.IconLg).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.SpacingMd))

        Text(
                text = title,
                style = EcoLensTextStyles.Body1,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark,
                modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExpandableSettingsItem(
        icon: ImageVector,
        title: String,
        isExpanded: Boolean,
        onClick: () -> Unit
) {
    val rotationAngle by
            animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "arrow_rotation"
            )

    Row(
            modifier =
                    Modifier.fillMaxWidth().clickable(onClick = onClick).padding(Dimens.SpacingMd),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
                modifier = Modifier.size(Dimens.IconLg).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryDark,
                    modifier = Modifier.size(31.dp)
            )
        }

        Spacer(modifier = Modifier.width(Dimens.SpacingMd))

        Text(
                text = title,
                style = EcoLensTextStyles.Body1,
                fontWeight = FontWeight.Bold,
                color = PrimaryDark,
                modifier = Modifier.weight(1f)
        )

        Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = PrimaryDark,
                modifier = Modifier.size(24.dp).rotate(rotationAngle)
        )
    }
}

@Composable
private fun SubSettingsItem(text: String, textColor: Color = PrimaryDark, onClick: () -> Unit) {
    Row(
            modifier =
                    Modifier.fillMaxWidth()
                            .clickable(onClick = onClick)
                            .padding(vertical = 10.dp, horizontal = Dimens.SpacingSm),
            verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
                text = text,
                style = EcoLensTextStyles.Body2,
                color = textColor,
                modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsDivider() {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .padding(horizontal = Dimens.SpacingXxs)
                            .height(1.dp)
                            .background(EcoLensTheme.extendedColors.borderNormal)
    )
}
