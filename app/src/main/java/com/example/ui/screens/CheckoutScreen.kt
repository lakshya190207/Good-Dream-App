package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.example.data.local.SavedAddress
import com.example.data.model.PendingPaymentOrderDraft
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItemWithProduct
import java.text.NumberFormat
import java.util.Locale

enum class PaymentCategory {
    UPI,
    CARD,
    NET_BANKING,
    CASH_ON_DELIVERY,
    EMI
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItemWithProduct>,
    totalPrice: Double,
    initialCustomerEmail: String = "",
    initialCustomerName: String = "",
    savedAddresses: List<SavedAddress> = emptyList(),
    onSaveAddress: ((SavedAddress) -> Unit)? = null,
    onDeleteAddress: ((String) -> Unit)? = null,
    appliedCouponCode: String? = null,
    discountPercent: Int = 0,
    discountAmount: Double = 0.0,
    finalPayablePrice: Double = totalPrice,
    isPaymentProcessing: Boolean = false,
    paymentErrorMessage: String? = null,
    onClearPaymentError: (() -> Unit)? = null,
    onInitiateOnlinePayment: ((PendingPaymentOrderDraft) -> Unit)? = null,
    onPlaceOrder: (
        name: String,
        phone: String,
        email: String,
        address: String,
        city: String,
        state: String,
        pincode: String,
        slot: String,
        floorElevator: String,
        paymentMethod: String
    ) -> Unit,
    onOpenTerms: (() -> Unit)? = null,
    onOpenPrivacy: (() -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
    }

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val currentView = LocalView.current

    // Hardcore Security: Tapjacking & Overlay Attack Protection
    // Enforces OS-level touch rejection if another application or floating overlay obscures the checkout view
    DisposableEffect(currentView) {
        val previousFilter = currentView.filterTouchesWhenObscured
        currentView.filterTouchesWhenObscured = true
        onDispose {
            currentView.filterTouchesWhenObscured = previousFilter
        }
    }
    val effectivePayablePrice = remember(totalPrice, discountAmount, finalPayablePrice) {
        if (finalPayablePrice > 0) finalPayablePrice else (totalPrice - discountAmount).coerceAtLeast(0.0)
    }
    val codAdvanceAmount = remember(effectivePayablePrice) { (effectivePayablePrice * 0.20).toLong() }
    val codBalanceAmount = remember(effectivePayablePrice, codAdvanceAmount) { effectivePayablePrice.toLong() - codAdvanceAmount }

    // Step 1: User & Delivery Information ("All Users Detail First")
    var fullName by remember(initialCustomerName) { mutableStateOf(initialCustomerName) }
    var phoneNumber by remember { mutableStateOf("") }
    var emailAddress by remember(initialCustomerEmail) { mutableStateOf(initialCustomerEmail) }
    var flatHouseNo by remember { mutableStateOf("") }
    var streetLocality by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Jaipur") }
    var state by remember { mutableStateOf("Rajasthan") }
    var pincode by remember { mutableStateOf("302039") }

    // Address Book Selection & Persistence State
    var selectedAddressId by remember(savedAddresses) {
        mutableStateOf(savedAddresses.firstOrNull { it.isDefault }?.id ?: savedAddresses.firstOrNull()?.id)
    }
    var isEnteringNewAddress by remember(savedAddresses) {
        mutableStateOf(savedAddresses.isEmpty())
    }
    var shouldSaveToAddressBook by remember { mutableStateOf(false) }
    var newAddressTag by remember { mutableStateOf("Home") }

    fun populateFromAddress(addr: SavedAddress) {
        fullName = addr.fullName
        phoneNumber = addr.phoneNumber
        flatHouseNo = addr.flatHouseNo
        streetLocality = addr.streetLocality
        landmark = addr.landmark
        city = addr.city
        state = addr.state
        pincode = addr.pincode
    }

    LaunchedEffect(savedAddresses) {
        if (savedAddresses.isNotEmpty() && !isEnteringNewAddress) {
            val target = savedAddresses.firstOrNull { it.id == selectedAddressId } ?: savedAddresses.first()
            selectedAddressId = target.id
            populateFromAddress(target)
        }
    }

    // Delivery & Installation Options
    var elevatorAccess by remember { mutableStateOf("Elevator Available") }
    var selectedDeliverySlot by remember { mutableStateOf("Morning (9 AM - 1 PM)") }
    var oldMattressRemoval by remember { mutableStateOf(false) }

    // Step 2: Payment Method
    var selectedPaymentCategory by remember { mutableStateOf(PaymentCategory.UPI) }
    var selectedUpiApp by remember { mutableStateOf("Google Pay") }
    var customUpiId by remember { mutableStateOf("") }

    var selectedBank by remember { mutableStateOf("HDFC Bank") }
    var selectedEmiTenure by remember { mutableStateOf("6 Months (No-Cost)") }
    var selectedCodAdvanceMethod by remember { mutableStateOf("Instant UPI (GPay / PhonePe)") }

    // Validation State
    var validationError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val areUserDetailsValid = remember(fullName, phoneNumber, emailAddress, flatHouseNo, streetLocality, pincode) {
        fullName.trim().length >= 2 &&
                phoneNumber.trim().filter { it.isDigit() }.length >= 10 &&
                emailAddress.contains("@") && emailAddress.contains(".") &&
                flatHouseNo.isNotBlank() &&
                streetLocality.isNotBlank() &&
                pincode.trim().filter { it.isDigit() }.length == 6
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_checkout"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Checkout",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Step 1: User & Address • Step 2: Payment",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Cart",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = "Secure", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("256-Bit SSL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    // DPDP Act 2023 & Indian Contract Act Affirmative Consent Notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "By placing order, you agree to ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Terms of Service",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    haptics.performLuxuryClick()
                                    onOpenTerms?.invoke()
                                }
                            )
                            Text(
                                text = " & ",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Privacy Policy",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                modifier = Modifier.clickable {
                                    haptics.performLuxuryClick()
                                    onOpenPrivacy?.invoke()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            if (selectedPaymentCategory == PaymentCategory.CASH_ON_DELIVERY) {
                                Text("20% Advance Due Today", fontSize = 11.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = currencyFormatter.format(codAdvanceAmount),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "80% (${currencyFormatter.format(codBalanceAmount)}) on delivery",
                                    fontSize = 11.sp,
                                    color = SatinGoldDark,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total Payable", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!appliedCouponCode.isNullOrBlank() && discountPercent > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(
                                                text = "$discountPercent% OFF",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = currencyFormatter.format(effectivePayablePrice),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (!areUserDetailsValid) {
                                    validationError = "Please fill in all customer details & delivery address first."
                                    return@Button
                                }
                                validationError = null
                                isSubmitting = true
                                haptics.performLuxurySuccess()

                                if (isEnteringNewAddress && shouldSaveToAddressBook) {
                                    onSaveAddress?.invoke(
                                        SavedAddress(
                                            tag = newAddressTag,
                                            fullName = fullName.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            flatHouseNo = flatHouseNo.trim(),
                                            streetLocality = streetLocality.trim(),
                                            landmark = landmark.trim(),
                                            city = city.trim(),
                                            state = state.trim(),
                                            pincode = pincode.trim(),
                                            isDefault = savedAddresses.isEmpty()
                                        )
                                    )
                                }

                                val fullAddress = "${flatHouseNo.trim()}, ${streetLocality.trim()}" +
                                        if (landmark.isNotBlank()) " (Near ${landmark.trim()})" else ""

                                val paymentDescription = when (selectedPaymentCategory) {
                                    PaymentCategory.UPI -> "UPI ($selectedUpiApp${if (customUpiId.isNotBlank()) " - $customUpiId" else ""})"
                                    PaymentCategory.CARD -> "Credit / Debit Card (Processed securely via Razorpay PCI-DSS)"
                                    PaymentCategory.NET_BANKING -> "Net Banking ($selectedBank)"
                                    PaymentCategory.CASH_ON_DELIVERY -> "Cash on Delivery (20% Advance ${currencyFormatter.format(codAdvanceAmount)} paid via $selectedCodAdvanceMethod • 80% Balance ${currencyFormatter.format(codBalanceAmount)} due on delivery)"
                                    PaymentCategory.EMI -> "No-Cost EMI ($selectedEmiTenure)"
                                }

                                val floorInfo = "$elevatorAccess • Slot: $selectedDeliverySlot" +
                                        if (oldMattressRemoval) " • Old Mattress Removal Requested" else ""

                                if (selectedPaymentCategory != PaymentCategory.CASH_ON_DELIVERY && onInitiateOnlinePayment != null) {
                                    val draft = PendingPaymentOrderDraft(
                                        customerName = fullName.trim(),
                                        customerPhone = phoneNumber.trim(),
                                        customerEmail = emailAddress.trim(),
                                        deliveryAddress = fullAddress,
                                        city = city.trim(),
                                        state = state.trim(),
                                        pincode = pincode.trim(),
                                        deliverySlot = selectedDeliverySlot,
                                        floorElevator = floorInfo,
                                        paymentCategory = "ONLINE",
                                        paymentMethodDetail = paymentDescription,
                                        payableAmount = effectivePayablePrice,
                                        isCod = false
                                    )
                                    isSubmitting = false
                                    onInitiateOnlinePayment(draft)
                                    return@Button
                                }

                                if (selectedPaymentCategory == PaymentCategory.CASH_ON_DELIVERY && onInitiateOnlinePayment != null) {
                                    val draft = PendingPaymentOrderDraft(
                                        customerName = fullName.trim(),
                                        customerPhone = phoneNumber.trim(),
                                        customerEmail = emailAddress.trim(),
                                        deliveryAddress = fullAddress,
                                        city = city.trim(),
                                        state = state.trim(),
                                        pincode = pincode.trim(),
                                        deliverySlot = selectedDeliverySlot,
                                        floorElevator = floorInfo,
                                        paymentCategory = "COD",
                                        paymentMethodDetail = "Cash on Delivery (20% Advance via Razorpay)",
                                        payableAmount = codAdvanceAmount.toDouble(),
                                        isCod = true,
                                        codAdvanceAmount = codAdvanceAmount.toDouble(),
                                        codBalanceAmount = codBalanceAmount.toDouble()
                                    )
                                    isSubmitting = false
                                    onInitiateOnlinePayment(draft)
                                    return@Button
                                }

                                onPlaceOrder(
                                    fullName.trim(),
                                    phoneNumber.trim(),
                                    emailAddress.trim(),
                                    fullAddress,
                                    city.trim(),
                                    state.trim(),
                                    pincode.trim(),
                                    selectedDeliverySlot,
                                    floorInfo,
                                    paymentDescription
                                )
                            },
                            enabled = !isSubmitting && !isPaymentProcessing,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("button_place_order_now"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            if (isSubmitting || isPaymentProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (selectedPaymentCategory == PaymentCategory.CASH_ON_DELIVERY)
                                        "Pay 20% (${currencyFormatter.format(codAdvanceAmount)}) & Confirm COD →"
                                    else
                                        "Pay ${currencyFormatter.format(effectivePayablePrice)} via Razorpay →",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.performLuxurySuccess()
                            val fullAddress = "${flatHouseNo.trim().ifBlank { "D-4, Vijay Vihar Colony" }}, ${streetLocality.trim().ifBlank { "Naya Kheda" }}" +
                                    if (landmark.isNotBlank()) " (Near ${landmark.trim()})" else ""
                            val floorInfo = "$elevatorAccess • Slot: $selectedDeliverySlot"
                            onPlaceOrder(
                                fullName.trim().ifBlank { "Sanctuary Patron" },
                                phoneNumber.trim().ifBlank { "+91 70149 83696" },
                                emailAddress.trim().ifBlank { "patron@gooddream.com" },
                                fullAddress,
                                city.trim().ifBlank { "Jaipur" },
                                state.trim().ifBlank { "Rajasthan" },
                                pincode.trim().ifBlank { "302039" },
                                selectedDeliverySlot,
                                floorInfo,
                                "⚡ Instant Test Order (Simulated Approval)"
                            )
                        },
                        enabled = !isSubmitting && !isPaymentProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("button_quick_test_order"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreenPrimary.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "⚡ Instant Test Order (Bypass Payment Gateway)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForestGreenPrimary
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // STEP 1 HEADER: ALL USERS DETAIL FIRST
            item {
                SectionHeaderCard(
                    stepNumber = "1",
                    title = "Customer Details & Delivery Address",
                    subtitle = "Required for order tracking, tax invoice, and delivery scheduling"
                )
            }

            // Customer Contact Info Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (initialCustomerEmail.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Verified Member Account",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Gaurav Chandra") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_checkout_name")
                        )

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it.filter { ch -> ch.isDigit() }.take(10) },
                            label = { Text("10-Digit Mobile Number *") },
                            placeholder = { Text("e.g. 9876543210") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_checkout_phone")
                        )

                        OutlinedTextField(
                            value = emailAddress,
                            onValueChange = { emailAddress = it },
                            label = { Text("Email Address (For Tax Invoice) *") },
                            placeholder = { Text("e.g. customer@example.com") },
                            leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_checkout_email")
                        )
                    }
                }
            }

            // Saved Sanctuary Addresses Card (if available)
            if (savedAddresses.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.BookmarkBorder,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Saved Sanctuary Addresses",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isEnteringNewAddress) {
                                    TextButton(
                                        onClick = {
                                            isEnteringNewAddress = false
                                            haptics.performLuxuryClick()
                                            val target = savedAddresses.firstOrNull { it.id == selectedAddressId } ?: savedAddresses.first()
                                            selectedAddressId = target.id
                                            populateFromAddress(target)
                                        }
                                    ) {
                                        Text("Use Saved", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                                    }
                                }
                            }

                            if (!isEnteringNewAddress) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    savedAddresses.forEach { addr ->
                                        val isSelected = addr.id == selectedAddressId
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) SatinGoldAccent else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    haptics.performLuxuryClick()
                                                    selectedAddressId = addr.id
                                                    populateFromAddress(addr)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = {
                                                        haptics.performLuxuryClick()
                                                        selectedAddressId = addr.id
                                                        populateFromAddress(addr)
                                                    },
                                                    colors = RadioButtonDefaults.colors(
                                                        selectedColor = ForestGreenPrimary,
                                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = MaterialTheme.colorScheme.secondaryContainer
                                                        ) {
                                                            Text(
                                                                text = addr.tag.uppercase(),
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = addr.fullName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "${addr.flatHouseNo}, ${addr.streetLocality}${if (addr.landmark.isNotBlank()) " (Near ${addr.landmark})" else ""}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 16.sp
                                                    )
                                                    Text(
                                                        text = "${addr.city}, ${addr.state} - ${addr.pincode} • Phone: ${addr.phoneNumber}",
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        haptics.performLuxuryAdjustment()
                                                        onDeleteAddress?.invoke(addr.id)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.DeleteOutline,
                                                        contentDescription = "Delete address",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            haptics.performLuxuryClick()
                                            isEnteringNewAddress = true
                                            flatHouseNo = ""
                                            streetLocality = ""
                                            landmark = ""
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Deliver to a Different / New Address", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delivery Address Card (Shown when entering new address or no saved addresses)
            if (isEnteringNewAddress || savedAddresses.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (savedAddresses.isNotEmpty()) "New Delivery Address Details" else "Delivery Address",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (savedAddresses.isNotEmpty()) {
                                    TextButton(
                                        onClick = {
                                            isEnteringNewAddress = false
                                            haptics.performLuxuryClick()
                                            val target = savedAddresses.firstOrNull { it.id == selectedAddressId } ?: savedAddresses.first()
                                            populateFromAddress(target)
                                        }
                                    ) {
                                        Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = flatHouseNo,
                                onValueChange = { flatHouseNo = it },
                                label = { Text("Flat / House No. & Building Name *") },
                                placeholder = { Text("e.g. Villa 14, Silver Oak Sanctuary") },
                                leadingIcon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_checkout_flat")
                            )

                            OutlinedTextField(
                                value = streetLocality,
                                onValueChange = { streetLocality = it },
                                label = { Text("Street, Area & Locality *") },
                                placeholder = { Text("e.g. D-4, Amba Bari or Tonk Road") },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_checkout_street")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = pincode,
                                    onValueChange = { pincode = it.filter { ch -> ch.isDigit() }.take(6) },
                                    label = { Text("PIN Code *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("input_checkout_pincode")
                                )
                            }

                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = landmark,
                                onValueChange = { landmark = it },
                                label = { Text("Nearby Landmark (Optional)") },
                                placeholder = { Text("e.g. Opposite Club House") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Save address to sanctuary book option
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        shouldSaveToAddressBook = !shouldSaveToAddressBook
                                        haptics.performLuxuryAdjustment()
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = shouldSaveToAddressBook,
                                    onCheckedChange = {
                                        shouldSaveToAddressBook = it
                                        haptics.performLuxuryAdjustment()
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ForestGreenPrimary,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Save to Sanctuary Address Book",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Keep this address securely encrypted for 1-tap orders",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (shouldSaveToAddressBook) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Address Label:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    listOf("Home", "Office", "Sanctuary", "Other").forEach { tag ->
                                        FilterChip(
                                            selected = newAddressTag == tag,
                                            onClick = {
                                                newAddressTag = tag
                                                haptics.performLuxuryClick()
                                            },
                                            label = { Text(tag, fontSize = 11.5.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Installation & Slot Preferences Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Delivery & Installation Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text("Floor / Building Access:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Elevator Available", "Ground Floor", "Stairs (Up to 3rd)").forEach { opt ->
                                FilterChip(
                                    selected = elevatorAccess == opt,
                                    onClick = { elevatorAccess = opt },
                                    label = { Text(opt, fontSize = 11.sp) }
                                )
                            }
                        }

                        Text("Preferred Delivery Slot:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Morning (9 AM - 1 PM)", "Afternoon (1 PM - 5 PM)", "Evening (5 PM - 8 PM)").forEach { slot ->
                                FilterChip(
                                    selected = selectedDeliverySlot == slot,
                                    onClick = { selectedDeliverySlot = slot },
                                    label = { Text(slot.substringBefore(" "), fontSize = 11.sp) }
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { oldMattressRemoval = !oldMattressRemoval }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(checked = oldMattressRemoval, onCheckedChange = { oldMattressRemoval = it })
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Request Old Mattress Removal & Disposal", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Our delivery team will assist in eco-friendly donation or recycling.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // STEP 2 HEADER: PAYMENT METHOD SELECTION
            item {
                SectionHeaderCard(
                    stepNumber = "2",
                    title = "Select Payment Method",
                    subtitle = if (areUserDetailsValid) "Choose your preferred payment gateway" else "Complete customer & address details above first"
                )
            }

            // Payment Category Selector
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PaymentOptionTile(
                            title = "Instant UPI (Google Pay, PhonePe, Paytm)",
                            subtitle = "Zero convenience fee • Instant verification",
                            icon = Icons.Outlined.QrCodeScanner,
                            isSelected = selectedPaymentCategory == PaymentCategory.UPI,
                            onClick = { selectedPaymentCategory = PaymentCategory.UPI }
                        )

                        if (selectedPaymentCategory == PaymentCategory.UPI) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Select UPI App:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Google Pay", "PhonePe", "Paytm", "BHIM").forEach { app ->
                                        FilterChip(
                                            selected = selectedUpiApp == app,
                                            onClick = { selectedUpiApp = app },
                                            label = { Text(app, fontSize = 11.sp) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = customUpiId,
                                    onValueChange = { customUpiId = it },
                                    label = { Text("Or Enter UPI ID") },
                                    placeholder = { Text("e.g. mobile@upi") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        PaymentOptionTile(
                            title = "Credit / Debit Card (Visa, MasterCard, RuPay)",
                            subtitle = "Bank-grade tokenization & 3D Secure OTP",
                            icon = Icons.Outlined.CreditCard,
                            isSelected = selectedPaymentCategory == PaymentCategory.CARD,
                            onClick = { selectedPaymentCategory = PaymentCategory.CARD }
                        )

                        if (selectedPaymentCategory == PaymentCategory.CARD) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "PCI-DSS Level 1 Encrypted Payment",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Card details are entered directly on Razorpay's RBI-compliant secure gateway with 3D Secure OTP verification. No card details are ever stored on your device.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        PaymentOptionTile(
                            title = "Net Banking (All Major Indian Banks)",
                            subtitle = "HDFC, ICICI, SBI, Axis, Kotak",
                            icon = Icons.Outlined.AccountBalance,
                            isSelected = selectedPaymentCategory == PaymentCategory.NET_BANKING,
                            onClick = { selectedPaymentCategory = PaymentCategory.NET_BANKING }
                        )

                        if (selectedPaymentCategory == PaymentCategory.NET_BANKING) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Choose Your Bank:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("HDFC Bank", "ICICI Bank", "SBI", "Axis Bank").forEach { bank ->
                                        FilterChip(
                                            selected = selectedBank == bank,
                                            onClick = { selectedBank = bank },
                                            label = { Text(bank, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        PaymentOptionTile(
                            title = "Cash / Card on Delivery (COD)",
                            subtitle = "Pay 20% advance now • Pay 80% later upon delivery & inspection",
                            icon = Icons.Outlined.LocalShipping,
                            isSelected = selectedPaymentCategory == PaymentCategory.CASH_ON_DELIVERY,
                            onClick = {
                                selectedPaymentCategory = PaymentCategory.CASH_ON_DELIVERY
                                Toast.makeText(
                                    context,
                                    "COD Policy: Pay 20% now and 80% later when delivered",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )

                        if (selectedPaymentCategory == PaymentCategory.CASH_ON_DELIVERY) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    ),
                                    border = BorderStroke(1.5.dp, SatinGoldAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Header
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(SatinGoldAccent.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = SatinGoldDark,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Cash on Delivery (COD) Policy",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = SatinGoldAccent.copy(alpha = 0.2f)
                                                    ) {
                                                        Text(
                                                            text = "MANDATORY",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SatinGoldDark,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Pay 20% now and 80% later when delivered",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = ForestGreenPrimary
                                                )
                                            }
                                        }

                                        // Split Breakdown Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // 20% Now
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(ForestGreenPrimary)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = "Pay 20% Now (Advance Booking)",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Locks factory tailoring & freight allocation",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = currencyFormatter.format(codAdvanceAmount),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = ForestGreenPrimary
                                                    )
                                                }

                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                // 80% Later
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(SatinGoldAccent)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(
                                                                text = "Pay 80% Later (When Delivered)",
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Pay via Cash / UPI / Card after room inspection",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = currencyFormatter.format(codBalanceAmount),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = SatinGoldDark
                                                    )
                                                }
                                            }
                                        }

                                        // Official policy description
                                        Text(
                                            text = "• Why 20% advance? Good Dream mattresses are handcrafted on-demand and shipped via dedicated delivery teams. The 20% booking deposit confirms your manufacturing slot and prevents bogus bookings.\n• In-Room Inspection: When our technician unboxes the mattress in your bedroom, you personally inspect the comfort and craftsmanship before settling the remaining 80% balance.\n• 25-Year Guarantee: Backed by our 25-Year SpringHaven™ Structural Warranty with complete craftsmanship coverage.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                        // Mode to pay 20% advance
                                        Text(
                                            text = "Select Advance Payment Mode (To Pay 20% Today):",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("Instant UPI", "Credit/Debit Card", "Net Banking").forEach { mode ->
                                                FilterChip(
                                                    selected = selectedCodAdvanceMethod == mode,
                                                    onClick = { selectedCodAdvanceMethod = mode },
                                                    label = { Text(mode, fontSize = 10.sp) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = SatinGoldAccent.copy(alpha = 0.3f),
                                                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))

                        PaymentOptionTile(
                            title = "No-Cost EMI (0% Interest)",
                            subtitle = "Available for orders above ₹10,000",
                            icon = Icons.Outlined.Payment,
                            isSelected = selectedPaymentCategory == PaymentCategory.EMI,
                            onClick = { selectedPaymentCategory = PaymentCategory.EMI }
                        )

                        if (selectedPaymentCategory == PaymentCategory.EMI) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 8.dp, bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Select No-Cost Tenure:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("3 Months", "6 Months", "12 Months").forEach { tenure ->
                                        FilterChip(
                                            selected = selectedEmiTenure.startsWith(tenure),
                                            onClick = { selectedEmiTenure = "$tenure (No-Cost)" },
                                            label = { Text(tenure, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Razorpay Security Trust Badge
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ForestGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = ForestGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Secured by Razorpay • Bank-Grade 256-Bit SSL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "PCI-DSS Level 1 Compliant • RBI Certified • Turbo UPI, Cards, Netbanking & EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Order Items Mini Preview
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Order Items (${cartItems.sumOf { it.quantity }})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.product.title} (${item.quantity}x)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = currencyFormatter.format(item.product.price * item.quantity),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cart Subtotal", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currencyFormatter.format(totalPrice), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }

                        if (!appliedCouponCode.isNullOrBlank() && discountAmount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocalOffer, contentDescription = null, tint = ForestGreenPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Privilege Voucher ($appliedCouponCode)", fontSize = 12.sp, color = ForestGreenPrimary, fontWeight = FontWeight.SemiBold)
                                }
                                Text("-${currencyFormatter.format(discountAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("In-Room Delivery & Setup", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("FREE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Final Payable Value", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(currencyFormatter.format(effectivePayablePrice), fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (paymentErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { onClearPaymentError?.invoke() },
            title = {
                Text(
                    text = "Payment Notice",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = paymentErrorMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "💡 In test mode, real bank cards and live UPI apps are declined by Razorpay to prevent accidental charges. Use test card 4111 1111 1111 1111 (CVV 123) or test UPI success@razorpay, or tap below to complete as a test order.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onClearPaymentError?.invoke() }) {
                        Text("Dismiss")
                    }
                    Button(
                        onClick = {
                            onClearPaymentError?.invoke()
                            val fullAddress = "${flatHouseNo.trim()}, ${streetLocality.trim()}" +
                                    if (landmark.isNotBlank()) " (Near ${landmark.trim()})" else ""
                            val floorInfo = "$elevatorAccess • Slot: $selectedDeliverySlot"
                            onPlaceOrder(
                                fullName.ifBlank { "Sanctuary Patron" },
                                phoneNumber.ifBlank { "+91 70149 83696" },
                                emailAddress.ifBlank { "patron@gooddream.com" },
                                fullAddress.ifBlank { "D-4, Vijay Vihar Colony, Naya Kheda, Jaipur" },
                                city.ifBlank { "Jaipur" },
                                state.ifBlank { "Rajasthan" },
                                pincode.ifBlank { "302039" },
                                selectedDeliverySlot,
                                floorInfo,
                                "Test Sandbox Payment (Simulated Approval)"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("⚡ Complete as Test Order")
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeaderCard(stepNumber: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(ForestGreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PaymentOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = if (isSelected) SatinGoldAccent else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
