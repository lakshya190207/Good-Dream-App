package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.cushionPressEffect
import com.example.ui.theme.*
import com.example.ui.viewmodel.OtpRequestResult
import com.example.ui.viewmodel.OtpVerifyResult
import com.example.ui.viewmodel.PasscodeAuthResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AuthMode {
    LOG_IN,
    SIGN_IN_REGISTER
}

private enum class LoginMethod {
    OTP,
    PASSCODE
}

/**
 * Premium, clutter-free authentication portal for Good Dream Sanctuary.
 * Separates Returning Member Log-In from New Member Registration (Sign-In).
 * Features:
 * - Login with Email OTP or personal Passcode
 * - New user registration with name and secure Passcode setup
 * - Cryptographic rate limiting and brute-force lockouts
 * - Zero on-device credential leaks or plain-text backdoors
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLoginScreen(
    pendingOtp: String? = null,
    otpExpiresEpochMs: Long = 0L,
    customerOtpLockoutUntilEpochMs: Long = 0L,
    onRequestOtp: suspend (String) -> OtpRequestResult,
    onVerifyOtpDetailed: (String, String, String?, String?) -> OtpVerifyResult,
    onLoginWithPasscode: (String, String) -> PasscodeAuthResult,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var authMode by remember { mutableStateOf(AuthMode.LOG_IN) }
    var loginMethod by remember { mutableStateOf(LoginMethod.OTP) }

    // Inputs
    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passcodeInput by remember { mutableStateOf("") }
    var confirmPasscodeInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    // UI States
    var isPasscodeVisible by remember { mutableStateOf(false) }
    var isConfirmPasscodeVisible by remember { mutableStateOf(false) }
    var isOtpStage by remember { mutableStateOf(false) }
    var isSendingOtp by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCountdown by remember { mutableIntStateOf(30) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }
    var otpExpirySeconds by remember { mutableIntStateOf(0) }

    // Lockout ticker
    LaunchedEffect(customerOtpLockoutUntilEpochMs) {
        val now = System.currentTimeMillis()
        if (customerOtpLockoutUntilEpochMs > now) {
            lockoutSeconds = (((customerOtpLockoutUntilEpochMs - now) / 1000) + 1).toInt()
        } else {
            lockoutSeconds = 0
        }
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds -= 1
        }
    }

    // OTP Expiry ticker (5-minute expiration)
    LaunchedEffect(otpExpiresEpochMs, isOtpStage) {
        val now = System.currentTimeMillis()
        if (isOtpStage && otpExpiresEpochMs > now) {
            otpExpirySeconds = (((otpExpiresEpochMs - now) / 1000) + 1).toInt()
        } else if (isOtpStage && otpExpiresEpochMs > 0 && otpExpiresEpochMs <= now) {
            otpExpirySeconds = 0
        } else if (isOtpStage) {
            otpExpirySeconds = 300
        }
    }

    LaunchedEffect(otpExpirySeconds) {
        if (isOtpStage && otpExpirySeconds > 0) {
            delay(1000L)
            otpExpirySeconds -= 1
        }
    }

    // Resend cooldown timer
    LaunchedEffect(isOtpStage, resendCountdown) {
        if (isOtpStage && resendCountdown > 0) {
            delay(1000L)
            resendCountdown -= 1
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_login_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOtpStage) "Verify Email" else if (authMode == AuthMode.LOG_IN) "Member Log In" else "Sanctuary Sign In",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isOtpStage) {
                            isOtpStage = false
                            errorMessage = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ForestGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sanctuary Brand Emblem
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ForestGreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bed,
                    contentDescription = null,
                    tint = SatinGoldAccent,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Good Dream Sanctuary",
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Restorative Sleep • Custom Handcrafted Luxury",
                fontSize = 12.sp,
                color = TextSecondaryMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Authentication Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = isOtpStage,
                        transitionSpec = {
                            if (targetState) {
                                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> -width } + fadeOut()
                                )
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                    slideOutHorizontally { width -> width } + fadeOut()
                                )
                            }
                        },
                        label = "login_stage_anim"
                    ) { inOtpStage ->
                        if (!inOtpStage) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // -------------------------------------------------------------
                                // TOP SEGMENTED SWITCHER: [LOG IN] vs [CREATE ACCOUNT (SIGN IN)]
                                // -------------------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Log In Tab
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (authMode == AuthMode.LOG_IN) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .cushionPressEffect(pressedScale = 0.95f)
                                        .clickable {
                                            authMode = AuthMode.LOG_IN
                                            errorMessage = null
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Log In",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = if (authMode == AuthMode.LOG_IN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Sign In / Create Account Tab
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (authMode == AuthMode.SIGN_IN_REGISTER) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .cushionPressEffect(pressedScale = 0.95f)
                                        .clickable {
                                            authMode = AuthMode.SIGN_IN_REGISTER
                                            errorMessage = null
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "Create Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = if (authMode == AuthMode.SIGN_IN_REGISTER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // =============================================================
                        // BOX 1: LOG IN (EXISTING MEMBERS)
                        // =============================================================
                        if (authMode == AuthMode.LOG_IN) {
                            // Sub-toggle: [Email OTP] vs [Passcode]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (loginMethod == LoginMethod.OTP) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (loginMethod == LoginMethod.OTP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .cushionPressEffect(pressedScale = 0.94f)
                                        .clickable {
                                            loginMethod = LoginMethod.OTP
                                            errorMessage = null
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = null,
                                            tint = if (loginMethod == LoginMethod.OTP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Email OTP",
                                            fontSize = 12.sp,
                                            fontWeight = if (loginMethod == LoginMethod.OTP) FontWeight.Bold else FontWeight.Medium,
                                            color = if (loginMethod == LoginMethod.OTP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (loginMethod == LoginMethod.PASSCODE) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (loginMethod == LoginMethod.PASSCODE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .cushionPressEffect(pressedScale = 0.94f)
                                        .clickable {
                                            loginMethod = LoginMethod.PASSCODE
                                            errorMessage = null
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = null,
                                            tint = if (loginMethod == LoginMethod.PASSCODE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Passcode",
                                            fontSize = 12.sp,
                                            fontWeight = if (loginMethod == LoginMethod.PASSCODE) FontWeight.Bold else FontWeight.Medium,
                                            color = if (loginMethod == LoginMethod.PASSCODE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (loginMethod == LoginMethod.OTP)
                                    "Enter your email address to receive a secure 6-digit one-time code."
                                else
                                    "Enter your registered email and your personal passcode to sign in.",
                                fontSize = 12.5.sp,
                                color = TextSecondaryMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Email Input
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. name@domain.com") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = if (loginMethod == LoginMethod.PASSCODE) ImeAction.Next else ImeAction.Done
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreenPrimary,
                                    focusedLabelColor = ForestGreenPrimary,
                                    cursorColor = ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_email_input")
                            )

                            // Passcode Input (if Passcode Login chosen)
                            AnimatedVisibility(
                                visible = loginMethod == LoginMethod.PASSCODE,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = passcodeInput,
                                        onValueChange = {
                                            passcodeInput = it
                                            errorMessage = null
                                        },
                                        label = { Text("Personal Passcode") },
                                        placeholder = { Text("Enter your passcode") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = ForestGreenPrimary
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { isPasscodeVisible = !isPasscodeVisible }) {
                                                Icon(
                                                    imageVector = if (isPasscodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (isPasscodeVisible) "Hide" else "Show",
                                                    tint = Color.Gray
                                                )
                                            }
                                        },
                                        visualTransformation = if (isPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ForestGreenPrimary,
                                            focusedLabelColor = ForestGreenPrimary,
                                            cursorColor = ForestGreenPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("login_passcode_input")
                                    )
                                }
                            }

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = StatusError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Submit Button for Log In
                            Button(
                                onClick = {
                                    val cleanEmail = emailInput.trim().lowercase()
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                        errorMessage = "Please enter a valid email address (e.g. name@domain.com)"
                                        return@Button
                                    }
                                    focusManager.clearFocus()

                                    if (loginMethod == LoginMethod.OTP) {
                                        coroutineScope.launch {
                                            isSendingOtp = true
                                            errorMessage = null
                                            try {
                                                when (val res = onRequestOtp(cleanEmail)) {
                                                    is OtpRequestResult.Success -> {
                                                        resendCountdown = 30
                                                        isOtpStage = true
                                                        errorMessage = null
                                                        Toast.makeText(context, "Verification code sent to $cleanEmail", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is OtpRequestResult.RateLimited -> {
                                                        errorMessage = res.message
                                                        if (res.remainingSeconds > 0) {
                                                            lockoutSeconds = res.remainingSeconds
                                                        }
                                                    }
                                                    is OtpRequestResult.InvalidEmail -> {
                                                        errorMessage = res.message
                                                    }
                                                    is OtpRequestResult.DeliveryFailed -> {
                                                        errorMessage = res.message
                                                    }
                                                }
                                            } finally {
                                                isSendingOtp = false
                                            }
                                        }
                                    } else {
                                        // Login with Passcode
                                        if (passcodeInput.trim().isEmpty()) {
                                            errorMessage = "Please enter your passcode"
                                            return@Button
                                        }
                                        when (val res = onLoginWithPasscode(cleanEmail, passcodeInput.trim())) {
                                            is PasscodeAuthResult.Success -> {
                                                Toast.makeText(context, "✓ ${res.message}", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                            is PasscodeAuthResult.InvalidCredentials -> {
                                                errorMessage = res.message
                                            }
                                            is PasscodeAuthResult.UserNotFound -> {
                                                errorMessage = res.message
                                            }
                                            is PasscodeAuthResult.Locked -> {
                                                lockoutSeconds = res.remainingSeconds
                                                errorMessage = res.message
                                            }
                                        }
                                    }
                                },
                                enabled = lockoutSeconds <= 0 && !isSendingOtp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .cushionPressEffect(pressedScale = 0.96f)
                                    .testTag("login_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lockoutSeconds > 0) Color(0xFFCCCCCC) else ForestGreenPrimary,
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                )
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sending Code to Email...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (loginMethod == LoginMethod.OTP) Icons.AutoMirrored.Filled.Send else Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = SatinGoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (lockoutSeconds > 0)
                                            "Locked Out (${lockoutSeconds}s)"
                                        else if (loginMethod == LoginMethod.OTP)
                                            "Send Verification Code"
                                        else
                                            "Sign In with Passcode",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lockoutSeconds > 0) Color.Gray else Color.White
                                    )
                                }
                            }
                        }

                        // =============================================================
                        // BOX 2: SIGN IN / CREATE ACCOUNT (NEW CUSTOMER REGISTRATION)
                        // =============================================================
                        else {
                            Text(
                                text = "Create your sanctuary account. Set your personal name and passcode for seamless visits.",
                                fontSize = 12.5.sp,
                                color = TextSecondaryMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Full Name Input
                            OutlinedTextField(
                                value = fullNameInput,
                                onValueChange = {
                                    fullNameInput = it
                                    errorMessage = null
                                },
                                label = { Text("Full Name") },
                                placeholder = { Text("e.g. Lakshya Chandra") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreenPrimary,
                                    focusedLabelColor = ForestGreenPrimary,
                                    cursorColor = ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_name_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Email Input
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    errorMessage = null
                                },
                                label = { Text("Email Address") },
                                placeholder = { Text("e.g. name@domain.com") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreenPrimary,
                                    focusedLabelColor = ForestGreenPrimary,
                                    cursorColor = ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_email_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Setup Passcode Input
                            OutlinedTextField(
                                value = passcodeInput,
                                onValueChange = {
                                    passcodeInput = it
                                    errorMessage = null
                                },
                                label = { Text("Create Passcode (PIN/Password)") },
                                placeholder = { Text("Minimum 4 characters") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isPasscodeVisible = !isPasscodeVisible }) {
                                        Icon(
                                            imageVector = if (isPasscodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isPasscodeVisible) "Hide" else "Show",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (isPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreenPrimary,
                                    focusedLabelColor = ForestGreenPrimary,
                                    cursorColor = ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_passcode_input")
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Confirm Passcode Input
                            OutlinedTextField(
                                value = confirmPasscodeInput,
                                onValueChange = {
                                    confirmPasscodeInput = it
                                    errorMessage = null
                                },
                                label = { Text("Confirm Passcode") },
                                placeholder = { Text("Re-enter passcode") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasscodeVisible = !isConfirmPasscodeVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasscodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = if (isConfirmPasscodeVisible) "Hide" else "Show",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasscodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreenPrimary,
                                    focusedLabelColor = ForestGreenPrimary,
                                    cursorColor = ForestGreenPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("signup_confirm_passcode_input")
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = StatusError,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Submit Button for Sign Up
                            Button(
                                onClick = {
                                    val cleanName = fullNameInput.trim()
                                    val cleanEmail = emailInput.trim().lowercase()
                                    val cleanPass = passcodeInput.trim()
                                    val cleanConfirm = confirmPasscodeInput.trim()

                                    if (cleanName.isBlank()) {
                                        errorMessage = "Please enter your full name"
                                        return@Button
                                    }
                                    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                        errorMessage = "Please enter a valid email address"
                                        return@Button
                                    }
                                    if (cleanPass.length < 4) {
                                        errorMessage = "Passcode must be at least 4 characters"
                                        return@Button
                                    }
                                    if (cleanPass != cleanConfirm) {
                                        errorMessage = "Passcodes do not match. Please re-enter."
                                        return@Button
                                    }

                                    focusManager.clearFocus()
                                    coroutineScope.launch {
                                        isSendingOtp = true
                                        errorMessage = null
                                        try {
                                            when (val res = onRequestOtp(cleanEmail)) {
                                                is OtpRequestResult.Success -> {
                                                    resendCountdown = 30
                                                    isOtpStage = true
                                                    errorMessage = null
                                                    Toast.makeText(context, "Verification code sent to $cleanEmail", Toast.LENGTH_SHORT).show()
                                                }
                                                is OtpRequestResult.RateLimited -> {
                                                    errorMessage = res.message
                                                    if (res.remainingSeconds > 0) {
                                                        lockoutSeconds = res.remainingSeconds
                                                    }
                                                }
                                                is OtpRequestResult.InvalidEmail -> {
                                                    errorMessage = res.message
                                                }
                                                is OtpRequestResult.DeliveryFailed -> {
                                                    errorMessage = res.message
                                                }
                                            }
                                        } finally {
                                            isSendingOtp = false
                                        }
                                    }
                                },
                                enabled = lockoutSeconds <= 0 && !isSendingOtp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .cushionPressEffect(pressedScale = 0.96f)
                                    .testTag("signup_submit_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (lockoutSeconds > 0) Color(0xFFCCCCCC) else ForestGreenPrimary,
                                    disabledContainerColor = Color(0xFFE0E0E0)
                                )
                            ) {
                                if (isSendingOtp) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Verifying & Sending Code...",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = SatinGoldAccent
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (lockoutSeconds > 0) "Locked Out (${lockoutSeconds}s)" else "Create Account & Send Code",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (lockoutSeconds > 0) Color.Gray else Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                            // =============================================================
                            // STAGE 2: 6-DIGIT EMAIL OTP VERIFICATION
                            // =============================================================
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enter One-Time Verification Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "A 6-digit verification code was sent to ${emailInput.trim()}. Please check your inbox.",
                            fontSize = 12.5.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    otpInput = it.filter { char -> char.isDigit() }
                                    errorMessage = null
                                }
                            },
                            label = { Text("6-Digit Code") },
                            placeholder = { Text("••••••") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LockClock,
                                    contentDescription = null,
                                    tint = ForestGreenPrimary
                                )
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                letterSpacing = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ForestGreenPrimary,
                                focusedLabelColor = ForestGreenPrimary,
                                cursorColor = ForestGreenPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_code_input")
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = StatusError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Resend Timer Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSendingOtp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = ForestGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sending new code...",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                            } else if (resendCountdown > 0) {
                                Text(
                                    text = "Resend code in ${resendCountdown}s",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                            } else {
                                Text(
                                    text = "Resend Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 48.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .clickable {
                                        coroutineScope.launch {
                                            isSendingOtp = true
                                            errorMessage = null
                                            try {
                                                when (val res = onRequestOtp(emailInput.trim())) {
                                                    is OtpRequestResult.Success -> {
                                                        resendCountdown = 30
                                                        errorMessage = null
                                                        Toast.makeText(context, "New verification code sent to your email!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    is OtpRequestResult.RateLimited -> {
                                                        errorMessage = res.message
                                                        if (res.remainingSeconds > 0) {
                                                            lockoutSeconds = res.remainingSeconds
                                                        }
                                                    }
                                                    is OtpRequestResult.InvalidEmail -> {
                                                        errorMessage = res.message
                                                    }
                                                    is OtpRequestResult.DeliveryFailed -> {
                                                        errorMessage = res.message
                                                    }
                                                }
                                            } finally {
                                                isSendingOtp = false
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                if (otpInput.length < 6) {
                                    errorMessage = "Please enter all 6 digits of your verification code"
                                    return@Button
                                }
                                isVerifying = true
                                val customName = if (authMode == AuthMode.SIGN_IN_REGISTER) fullNameInput.trim().ifBlank { null } else null
                                val setupPass = if (authMode == AuthMode.SIGN_IN_REGISTER) passcodeInput.trim().ifBlank { null } else null

                                val result = onVerifyOtpDetailed(emailInput.trim(), otpInput.trim(), customName, setupPass)
                                isVerifying = false
                                when (result) {
                                    is OtpVerifyResult.Success -> {
                                        Toast.makeText(context, "✓ Welcome to Good Dream Sanctuary!", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    }
                                    is OtpVerifyResult.InvalidCode -> {
                                        errorMessage = result.message
                                    }
                                    is OtpVerifyResult.Expired -> {
                                        errorMessage = result.message
                                        otpExpirySeconds = 0
                                    }
                                    is OtpVerifyResult.Locked -> {
                                        lockoutSeconds = result.remainingSeconds
                                        errorMessage = result.message
                                    }
                                }
                            },
                            enabled = !isVerifying && lockoutSeconds <= 0 && otpExpirySeconds > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .cushionPressEffect(pressedScale = 0.96f)
                                .testTag("verify_otp_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (lockoutSeconds > 0 || otpExpirySeconds <= 0) Color(0xFFCCCCCC) else ForestGreenPrimary,
                                disabledContainerColor = Color(0xFFE0E0E0)
                            )
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (lockoutSeconds > 0) Icons.Default.LockClock else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (lockoutSeconds > 0 || otpExpirySeconds <= 0) Color.Gray else SatinGoldAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (lockoutSeconds > 0)
                                        "Locked Out (${lockoutSeconds}s)"
                                    else if (otpExpirySeconds <= 0)
                                        "Code Expired (Resend)"
                                    else
                                        "Verify & Complete",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lockoutSeconds > 0 || otpExpirySeconds <= 0) Color.Gray else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Change email / back button
                        TextButton(
                            onClick = {
                                isOtpStage = false
                                errorMessage = null
                            },
                            modifier = Modifier.cushionPressEffect(pressedScale = 0.95f)
                        ) {
                            Text(
                                text = "Use different email address",
                                fontSize = 12.sp,
                                color = ForestGreenPrimary
                            )
                        }
                    }
                }
            }
        }
    }

            Spacer(modifier = Modifier.height(18.dp))

            // Dignified security reassurance badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = ForestGreenPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Hardware-Backed AES-256 Encryption • Zero Plaintext Storage",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}
