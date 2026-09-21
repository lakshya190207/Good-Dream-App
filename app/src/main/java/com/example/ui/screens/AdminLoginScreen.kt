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
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminAuthResult
import kotlinx.coroutines.delay

/**
 * Dedicated Executive Administrator Login Portal.
 * Segregates CMS & Catalog Management from customer workflows.
 * Requires authorized Administrator credentials.
 * Enforces security rate limiting and lockout on brute-force attempts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLoginScreen(
    adminLockoutUntilEpochMs: Long = 0L,
    onLoginAdmin: (String, String) -> AdminAuthResult,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var adminId by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var lockoutSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(adminLockoutUntilEpochMs) {
        val now = System.currentTimeMillis()
        if (adminLockoutUntilEpochMs > now) {
            lockoutSeconds = (((adminLockoutUntilEpochMs - now) / 1000) + 1).toInt()
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_login_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Executive Admin Security Gate",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SatinGoldAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ForestGreenDark
                )
            )
        },
        containerColor = Color(0xFF0F1E17) // Deep executive forest background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Executive Security Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B3B2B))
                    .border(2.dp, SatinGoldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = SatinGoldAccent,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Good Dream™ Executive CMS",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Authorized Personnel Only • Catalog & Lead Management",
                fontSize = 12.5.sp,
                color = SatinGoldLight.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Main Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF172D22)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SatinGoldAccent.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = SatinGoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin ID & Master Password",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter administrative credentials to unlock Catalog Studio, edit pricing, or view customer inquiry leads.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 17.sp
                    )

                    if (lockoutSeconds > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3B1616),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5252)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockClock,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Rate-Limit Lockout Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Access locked for $lockoutSeconds seconds due to repeated failed attempts.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Admin ID / Email Field
                    OutlinedTextField(
                        value = adminId,
                        onValueChange = {
                            adminId = it
                            errorMessage = null
                        },
                        label = { Text("Admin ID / Email", color = SatinGoldLight) },
                        placeholder = { Text("admin@domain.com", color = Color.White.copy(alpha = 0.35f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = SatinGoldAccent
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SatinGoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            cursorColor = SatinGoldAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_id_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Admin Password Field
                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = {
                            adminPassword = it
                            errorMessage = null
                        },
                        label = { Text("Master Password", color = SatinGoldLight) },
                        placeholder = { Text("Enter master password", color = Color.White.copy(alpha = 0.35f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SatinGoldAccent
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                    tint = SatinGoldAccent
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SatinGoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                            cursorColor = SatinGoldAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_password_input")
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusError.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFFF8B8B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFFF8B8B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (adminId.isBlank() || adminPassword.isBlank()) {
                                errorMessage = "Please enter both Admin ID and Master Password"
                                return@Button
                            }
                            focusManager.clearFocus()
                            isAuthenticating = true
                            val result = onLoginAdmin(adminId.trim(), adminPassword.trim())
                            isAuthenticating = false
                            when (result) {
                                is AdminAuthResult.Success -> {
                                    Toast.makeText(context, "✓ Executive Studio Unlocked", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                }
                                is AdminAuthResult.Locked -> {
                                    lockoutSeconds = result.remainingSeconds
                                    errorMessage = result.message
                                }
                                is AdminAuthResult.InvalidCredentials -> {
                                    errorMessage = result.message
                                }
                            }
                        },
                        enabled = !isAuthenticating && lockoutSeconds <= 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("admin_authenticate_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (lockoutSeconds > 0) Color(0xFF2E3A33) else SatinGoldAccent,
                            disabledContainerColor = Color(0xFF2E3A33)
                        )
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = ForestGreenDark,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (lockoutSeconds > 0) Icons.Default.LockClock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (lockoutSeconds > 0) Color.White.copy(alpha = 0.5f) else ForestGreenDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lockoutSeconds > 0) "Locked Out (${lockoutSeconds}s)" else "Authenticate & Unlock Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lockoutSeconds > 0) Color.White.copy(alpha = 0.5f) else ForestGreenDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Subtle Security Notice
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = SatinGoldLight.copy(alpha = 0.5f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Good Dream Administrative System • Encrypted Transmission",
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
