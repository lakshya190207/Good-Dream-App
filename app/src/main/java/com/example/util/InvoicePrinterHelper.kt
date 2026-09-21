package com.example.util

import android.content.Context
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.data.model.OrderEntity
import timber.log.Timber
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native Android Tax Invoice & Warranty Certificate Generator.
 * Employs system PrintManager and WebView PrintDocumentAdapter to allow
 * instant direct printing or 1-tap "Save as PDF".
 */
object InvoicePrinterHelper {

    fun printOrderInvoice(
        context: Context,
        order: OrderEntity,
        companyName: String = "Good Dream Home Decor Private Limited",
        supportPhone: String = "+91 80 4123 9999",
        supportEmail: String = "care@gooddreamhomedecor.com",
        storeAddress: String = "Sanctuary Flagship Experience Center, Indiranagar, Bengaluru, Karnataka 560038"
    ) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Printing service unavailable on this device", Toast.LENGTH_SHORT).show()
                return
            }

            val webView = WebView(context)
            webView.settings.javaScriptEnabled = false

            val htmlContent = generateInvoiceHtml(order, companyName, supportPhone, supportEmail, storeAddress)

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val jobName = "GoodDream_Invoice_${order.id}"
                    val printAdapter = webView.createPrintDocumentAdapter(jobName)
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()

                    printManager.print(jobName, printAdapter, printAttributes)
                    Timber.i("Print job initiated for order: %s", order.id)
                }
            }

            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Timber.e(e, "Error launching invoice print manager")
            Toast.makeText(context, "Could not open print preview: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateInvoiceHtml(
        order: OrderEntity,
        companyName: String,
        supportPhone: String,
        supportEmail: String,
        storeAddress: String
    ): String {
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
            maximumFractionDigits = 0
        }
        val dateFormatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.ENGLISH)
        val formattedDate = dateFormatter.format(Date(order.createdAt))
        val invoiceNo = "INV-GD-${order.id.takeLast(6)}"

        // Calculate tax split (18% GST embedded in luxury furniture/mattress pricing)
        val subtotalBeforeTax = order.totalAmount / 1.18
        val totalGst = order.totalAmount - subtotalBeforeTax
        val cgst = totalGst / 2.0
        val sgst = totalGst / 2.0

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #1A1A1A;
                        margin: 0;
                        padding: 30px;
                        font-size: 13px;
                        line-height: 1.5;
                        background: #FFFFFF;
                    }
                    .header {
                        border-bottom: 3px solid #102E23;
                        padding-bottom: 18px;
                        margin-bottom: 24px;
                    }
                    .brand-title {
                        color: #102E23;
                        font-size: 24px;
                        font-weight: 800;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                        margin: 0;
                    }
                    .tagline {
                        color: #C5A059;
                        font-size: 12px;
                        font-weight: 700;
                        letter-spacing: 1.5px;
                        text-transform: uppercase;
                        margin-top: 2px;
                    }
                    .badge {
                        background: #102E23;
                        color: #C5A059;
                        font-weight: 700;
                        font-size: 11px;
                        padding: 4px 10px;
                        border-radius: 4px;
                        display: inline-block;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    .two-col {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 20px;
                    }
                    .col {
                        width: 48%;
                    }
                    .section-label {
                        font-size: 11px;
                        font-weight: bold;
                        color: #718096;
                        text-transform: uppercase;
                        letter-spacing: 0.8px;
                        margin-bottom: 6px;
                    }
                    .table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 20px 0;
                    }
                    .table th {
                        background-color: #F4F7F5;
                        color: #102E23;
                        text-align: left;
                        padding: 10px;
                        font-size: 11px;
                        font-weight: bold;
                        border-bottom: 2px solid #E2E8F0;
                        text-transform: uppercase;
                    }
                    .table td {
                        padding: 12px 10px;
                        border-bottom: 1px solid #E2E8F0;
                    }
                    .total-box {
                        float: right;
                        width: 320px;
                        margin-top: 10px;
                    }
                    .total-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 5px 0;
                    }
                    .grand-total {
                        border-top: 2px solid #102E23;
                        border-bottom: 2px solid #102E23;
                        padding: 8px 0;
                        font-size: 16px;
                        font-weight: bold;
                        color: #102E23;
                    }
                    .seal-box {
                        clear: both;
                        margin-top: 40px;
                        padding: 16px;
                        border: 1.5px dashed #C5A059;
                        border-radius: 8px;
                        background: #FCFAF6;
                    }
                    .footer-note {
                        margin-top: 30px;
                        text-align: center;
                        font-size: 11px;
                        color: #718096;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                        <div>
                            <h1 class="brand-title">$companyName</h1>
                            <div class="tagline">Sanctuary Atelier & Master Craftsmen</div>
                            <div style="font-size: 11px; color: #4A5568; margin-top: 6px;">
                                $storeAddress<br>
                                GSTIN: 29AAACG8412K1Z8 • Phone: $supportPhone • Email: $supportEmail
                            </div>
                        </div>
                        <div style="text-align: right;">
                            <span class="badge">Official Tax Invoice</span>
                            <div style="font-size: 16px; font-weight: bold; color: #102E23; margin-top: 6px;">$invoiceNo</div>
                            <div style="font-size: 12px; color: #718096;">Date: $formattedDate</div>
                        </div>
                    </div>
                </div>

                <div class="two-col">
                    <div class="col">
                        <div class="section-label">Billed & Shipped To:</div>
                        <div style="font-weight: bold; font-size: 14px; color: #102E23;">${order.customerName}</div>
                        <div style="color: #4A5568; margin-top: 4px;">
                            ${order.deliveryAddress}<br>
                            ${order.city}, ${order.state} - ${order.pincode}<br>
                            Phone: ${order.customerPhone}<br>
                            Email: ${order.customerEmail}
                        </div>
                    </div>
                    <div class="col" style="text-align: right;">
                        <div class="section-label">Fulfillment & Logistics:</div>
                        <div style="font-weight: bold; color: #102E23;">White-Glove Fleet Delivery</div>
                        <div style="color: #4A5568; margin-top: 4px;">
                            Slot: ${order.deliverySlot}<br>
                            Access: ${order.floorElevator}<br>
                            Status: <strong style="color: #102E23;">${order.status}</strong><br>
                            Payment Mode: ${order.paymentMethod}
                        </div>
                    </div>
                </div>

                <table class="table">
                    <thead>
                        <tr>
                            <th>Item Description & Specifications</th>
                            <th style="text-align: center;">HSN Code</th>
                            <th style="text-align: center;">Qty</th>
                            <th style="text-align: right;">Taxable Value</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                <strong style="color: #102E23;">${order.itemsSummary}</strong><br>
                                <span style="font-size: 11px; color: #718096;">
                                    Artisan Handcrafted • European Zoned Pocket Coils • Zero VOC Certification
                                </span>
                            </td>
                            <td style="text-align: center;">9404</td>
                            <td style="text-align: center;">1</td>
                            <td style="text-align: right;">${currencyFormatter.format(subtotalBeforeTax)}</td>
                        </tr>
                    </tbody>
                </table>

                <div class="total-box">
                    <div class="total-row">
                        <span>Subtotal (Before Tax):</span>
                        <span>${currencyFormatter.format(subtotalBeforeTax)}</span>
                    </div>
                    <div class="total-row">
                        <span>CGST (9%):</span>
                        <span>${currencyFormatter.format(cgst)}</span>
                    </div>
                    <div class="total-row">
                        <span>SGST (9%):</span>
                        <span>${currencyFormatter.format(sgst)}</span>
                    </div>
                    <div class="total-row">
                        <span>White-Glove In-Room Setup:</span>
                        <span style="color: #102E23; font-weight: bold;">FREE</span>
                    </div>
                    <div class="total-row grand-total">
                        <span>Total Paid / Payable:</span>
                        <span>${currencyFormatter.format(order.totalAmount)}</span>
                    </div>
                </div>

                <div class="seal-box">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <strong style="color: #102E23; font-size: 14px;">🏛️ 25-Year Craftsmanship Guarantee & Authenticity Certificate</strong><br>
                            <span style="font-size: 11.5px; color: #4A5568;">
                                Certified genuine Good Dream SpringHaven luxury piece. Includes 100-Night Risk-Free In-Home Sleep Trial.
                                Serial Registered: GD-AUT-${order.id.takeLast(8)}
                            </span>
                        </div>
                        <div style="text-align: right; min-width: 140px;">
                            <div style="font-family: cursive; font-size: 16px; color: #102E23;">Lakshya C.</div>
                            <div style="font-size: 10px; color: #718096; border-top: 1px solid #C5A059; padding-top: 2px;">Master Artisan Seal</div>
                        </div>
                    </div>
                </div>

                <div class="footer-note">
                    This is a computer-generated tax invoice and requires no physical signature under the Information Technology Act.
                    Thank you for choosing Good Dream Sanctuary for your restorative sleep.
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
