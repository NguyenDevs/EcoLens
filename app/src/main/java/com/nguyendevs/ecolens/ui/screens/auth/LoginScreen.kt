package com.nguyendevs.ecolens.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nguyendevs.ecolens.R
import com.nguyendevs.ecolens.ui.components.EcoLensButton
import com.nguyendevs.ecolens.ui.components.EcoLensOutlinedButton
import com.nguyendevs.ecolens.ui.theme.EcoLensCorners
import com.nguyendevs.ecolens.ui.theme.Primary
import com.nguyendevs.ecolens.ui.theme.TextSecondary

/** Login/Register screen composable. Replaces fragment_login.xml (380 lines) */
@Composable
fun LoginScreen(
        onLoginClick: (email: String, password: String, rememberMe: Boolean) -> Unit,
        onRegisterClick:
                (
                        email: String,
                        password: String,
                        confirmPassword: String,
                        agreeTerms: Boolean) -> Unit,
        onForgotPasswordClick: () -> Unit,
        onGoogleSignInClick: () -> Unit,
        onBiometricClick: () -> Unit,
        showBiometric: Boolean = false,
        emailError: String? = null,
        passwordError: String? = null,
        confirmPasswordError: String? = null,
        modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var agreeTerms by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val isLoginMode = selectedTabIndex == 0

    Column(
            modifier =
                    modifier.fillMaxSize()
                            .background(Color(0xFFF2F4F5))
                            .verticalScroll(scrollState)
                            .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(15.dp))

        // Logo
        Box(modifier = Modifier.size(80.dp).clip(EcoLensCorners.CardLarge)) {
            // Logo image would go here
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Welcome Text
        Text(
                text = stringResource(R.string.welcome_to_ecolens),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
                text = stringResource(R.string.auth_subtitle),
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(280.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Form Card
        Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = EcoLensCorners.CardLarge,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Tab Layout
                TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                    modifier =
                                            Modifier.tabIndicatorOffset(
                                                    tabPositions[selectedTabIndex]
                                            ),
                                    height = 3.dp,
                                    color = Primary
                            )
                        },
                        divider = {}
                ) {
                    Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = {
                                Text(
                                        text = stringResource(R.string.login),
                                        color =
                                                if (selectedTabIndex == 0) Primary
                                                else TextSecondary
                                )
                            }
                    )
                    Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = {
                                Text(
                                        text = stringResource(R.string.register),
                                        color =
                                                if (selectedTabIndex == 1) Primary
                                                else TextSecondary
                                )
                            }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Email Field
                OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email)) },
                        leadingIcon = {
                            Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = TextSecondary
                            )
                        },
                        isError = emailError != null,
                        supportingText = emailError?.let { { Text(it) } },
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                ),
                        keyboardActions =
                                KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                        singleLine = true,
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        focusedLabelColor = Primary,
                                        cursorColor = Primary
                                ),
                        modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Password Field
                OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password)) },
                        leadingIcon = {
                            Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TextSecondary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                        imageVector =
                                                if (passwordVisible) Icons.Default.Visibility
                                                else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                )
                            }
                        },
                        visualTransformation =
                                if (passwordVisible) VisualTransformation.None
                                else PasswordVisualTransformation(),
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(it) } },
                        keyboardOptions =
                                KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction =
                                                if (isLoginMode) ImeAction.Done else ImeAction.Next
                                ),
                        keyboardActions =
                                KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                        onDone = { focusManager.clearFocus() }
                                ),
                        singleLine = true,
                        colors =
                                OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        focusedLabelColor = Primary,
                                        cursorColor = Primary
                                ),
                        modifier = Modifier.fillMaxWidth()
                )

                // Confirm Password (Register only)
                if (!isLoginMode) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(stringResource(R.string.confirm_password)) },
                            leadingIcon = {
                                Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = TextSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                        onClick = {
                                            confirmPasswordVisible = !confirmPasswordVisible
                                        }
                                ) {
                                    Icon(
                                            imageVector =
                                                    if (confirmPasswordVisible)
                                                            Icons.Default.Visibility
                                                    else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                    )
                                }
                            },
                            visualTransformation =
                                    if (confirmPasswordVisible) VisualTransformation.None
                                    else PasswordVisualTransformation(),
                            isError = confirmPasswordError != null,
                            supportingText = confirmPasswordError?.let { { Text(it) } },
                            keyboardOptions =
                                    KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                    ),
                            keyboardActions =
                                    KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            colors =
                                    OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            focusedLabelColor = Primary,
                                            cursorColor = Primary
                                    ),
                            modifier = Modifier.fillMaxWidth()
                    )
                }

                // Remember Me / Forgot Password (Login) or Terms (Register)
                if (isLoginMode) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = { rememberMe = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Primary)
                            )
                            Text(
                                    text = stringResource(R.string.remember_me),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                            )
                        }

                        Text(
                                text = stringResource(R.string.forgot_password),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                modifier =
                                        Modifier.clickable { onForgotPasswordClick() }
                                                .padding(12.dp)
                        )
                    }
                } else {
                    Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                                checked = agreeTerms,
                                onCheckedChange = { agreeTerms = it },
                                colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Text(
                                text = stringResource(R.string.agree_terms),
                                fontSize = 13.sp,
                                color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Button
                EcoLensButton(
                        text =
                                if (isLoginMode) stringResource(R.string.login)
                                else stringResource(R.string.register),
                        onClick = {
                            if (isLoginMode) {
                                onLoginClick(email, password, rememberMe)
                            } else {
                                onRegisterClick(email, password, confirmPassword, agreeTerms)
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Divider with "or"
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
            Text(
                    text = stringResource(R.string.or_continue_with),
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
            )
            Divider(modifier = Modifier.weight(1f), color = Color(0xFFE0E0E0))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Google Sign In
        EcoLensOutlinedButton(
                text = stringResource(R.string.sign_in_with_google),
                onClick = onGoogleSignInClick,
                leadingIcon = {
                    Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
        )

        // Biometric Sign In
        if (showBiometric) {
            Spacer(modifier = Modifier.height(12.dp))

            EcoLensOutlinedButton(
                    text = stringResource(R.string.sign_in_with_biometric),
                    onClick = onBiometricClick,
                    leadingIcon = {
                        Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
