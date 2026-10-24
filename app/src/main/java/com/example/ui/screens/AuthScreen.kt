package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.LeafRepository
import com.example.ui.components.ServerUrlDialog
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LeafGreen
import com.example.ui.theme.LeafGreenGlow
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: LeafRepository,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register
    var isVerificationStep by remember { mutableStateOf(false) }

    // Form fields
    var loginInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var gmailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordConfirmInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Verification step
    var verificationCode by remember { mutableStateOf("") }
    var cooldownSeconds by remember { mutableIntStateOf(0) }

    // Status
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }

    // Cooldown countdown timer
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds -= 1
        }
    }

    if (showServerDialog) {
        ServerUrlDialog(
            currentUrl = repository.getBaseUrl(),
            onDismiss = { showServerDialog = false },
            onSave = { newUrl ->
                repository.setBaseUrl(newUrl)
                showServerDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Server Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = { showServerDialog = true },
                    modifier = Modifier.testTag("auth_server_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки сервера",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PC Web Player Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_leaf),
                        contentDescription = "Leaf",
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Leaf",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = com.example.ui.theme.SpaceGroteskFontFamily,
                            fontSize = 32.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Музыка для души",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error / Success Banner
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                successMessage?.let { msg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = LeafGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = msg,
                            color = LeafGreen,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Verification Code Flow OR Main Form
            AnimatedContent(
                targetState = isVerificationStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "authStepTransition"
            ) { inVerification ->
                if (inVerification) {
                    // STEP 2: Code Verification
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Код подтверждения",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Мы отправили 6-значный код на почту:\n${gmailInput.trim()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { if (it.length <= 10) verificationCode = it },
                            label = { Text("Код из письма") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {}),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LeafGreen,
                                focusedLabelColor = LeafGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("verification_code_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (verificationCode.isBlank()) {
                                    errorMessage = "Введите код из письма"
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val result = if (selectedTab == 0) {
                                        repository.verifyLogin(gmailInput.trim(), verificationCode.trim())
                                    } else {
                                        repository.verifyRegister(gmailInput.trim(), verificationCode.trim())
                                    }
                                    isLoading = false
                                    if (result.isSuccess) {
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Неверный код"
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("verify_code_submit")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Text("Подтвердить и войти", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Resend Button
                        TextButton(
                            onClick = {
                                if (cooldownSeconds > 0) return@TextButton
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    val res = if (selectedTab == 0) {
                                        repository.resendLogin(gmailInput.trim())
                                    } else {
                                        repository.resendRegister(gmailInput.trim())
                                    }
                                    isLoading = false
                                    if (res.isSuccess) {
                                        cooldownSeconds = 60
                                        successMessage = "Код отправлен повторно"
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "Не удалось отправить код"
                                    }
                                }
                            },
                            enabled = cooldownSeconds == 0 && !isLoading
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = LeafGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (cooldownSeconds > 0) "Отправить снова ($cooldownSeconds с)" else "Отправить код повторно",
                                color = if (cooldownSeconds > 0) TextTertiary else LeafGreen
                            )
                        }

                        TextButton(
                            onClick = {
                                isVerificationStep = false
                                errorMessage = null
                                successMessage = null
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Вернуться назад", color = TextSecondary)
                        }
                    }
                } else {
                    // STEP 1: Main Credentials Form
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Tabs: Login / Register
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = ObsidianCard,
                            contentColor = LeafGreen,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = LeafGreen
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCard)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    errorMessage = null
                                    successMessage = null
                                },
                                text = { Text("Вход", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_login")
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    errorMessage = null
                                    successMessage = null
                                },
                                text = { Text("Регистрация", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_register")
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Login field
                        OutlinedTextField(
                            value = loginInput,
                            onValueChange = { loginInput = it },
                            label = { Text("Логин") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_login")
                        )

                        // Username field (only for registration)
                        if (selectedTab == 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = usernameInput,
                                onValueChange = { usernameInput = it },
                                label = { Text("Отображаемое имя (Ник)") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_username")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Gmail field
                        OutlinedTextField(
                            value = gmailInput,
                            onValueChange = { gmailInput = it },
                            label = { Text("Почта (Gmail)") },
                            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null, tint = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_gmail")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Password field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Пароль") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password")
                        )

                        // Password confirm field (only for registration)
                        if (selectedTab == 1) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = passwordConfirmInput,
                                onValueChange = { passwordConfirmInput = it },
                                label = { Text("Подтверждение пароля") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_password_confirm")
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                val login = loginInput.trim()
                                val gmail = gmailInput.trim()
                                val pass = passwordInput.trim()

                                if (login.isBlank() || gmail.isBlank() || pass.isBlank()) {
                                    errorMessage = "Заполните все обязательные поля"
                                    return@Button
                                }

                                if (selectedTab == 1) {
                                    val user = usernameInput.trim()
                                    val passConf = passwordConfirmInput.trim()
                                    if (user.isBlank()) {
                                        errorMessage = "Укажите отображаемое имя"
                                        return@Button
                                    }
                                    if (pass != passConf) {
                                        errorMessage = "Пароли не совпадают"
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val res = repository.register(login, user, gmail, pass, passConf)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            isVerificationStep = true
                                            cooldownSeconds = 60
                                            successMessage = "Код подтверждения отправлен на $gmail"
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.message ?: "Ошибка регистрации"
                                        }
                                    }
                                } else {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        val res = repository.login(login, gmail, pass)
                                        isLoading = false
                                        if (res.isSuccess) {
                                            isVerificationStep = true
                                            cooldownSeconds = 60
                                            successMessage = "Код подтверждения отправлен на $gmail"
                                        } else {
                                            errorMessage = res.exceptionOrNull()?.message ?: "Ошибка входа"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = LeafGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("auth_submit_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                            } else {
                                Text(
                                    text = if (selectedTab == 0) "Войти" else "Зарегистрироваться",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
