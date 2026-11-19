package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.MonthlyReconciliationCheck
import com.example.ui.components.GlassCard
import com.example.ui.components.HeroGlowGlassCard
import com.example.ui.components.NoPermissionScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividGreen
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.AppLanguage
import com.example.util.ReportDataBuilders
import com.example.util.ReportExportUtil
import com.example.util.ShopLocale

private val AmberWarning = Color(0xFFFFB300)
private val CyanAccent = Color(0xFF00E5FF)

@Composable
fun MonthlyReportScreen(
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by storeViewModel.currentLanguage.collectAsState()
    val year by storeViewModel.monthlyReportYear.collectAsState()
    val month by storeViewModel.monthlyReportMonth.collectAsState()
    val reportData by storeViewModel.monthlyReportData.collectAsState()
    val store by storeViewModel.store.collectAsState()
    val canSeeProfit by storeViewModel.canSeeBuyingPriceAndProfitFlow.collectAsState()
    val canViewReports by storeViewModel.canViewReportsFlow.collectAsState()
    val isFatherMode by storeViewModel.isFatherMode.collectAsState()
    val isPrivacyHidden by storeViewModel.isPrivacyHidden.collectAsState()

    val isBn = currentLanguage == AppLanguage.BN

    if (!canSeeProfit || !canViewReports) {
        NoPermissionScreen(
            onBack = { storeViewModel.navigateBackInBusiness() },
            modifier = modifier,
            currentLanguage = currentLanguage
        )
        return
    }

    BackHandler {
        storeViewModel.navigateBackInBusiness()
    }

    var showExportLanguageModal by remember { mutableStateOf(false) }
    var selectedExportFormat by remember { mutableStateOf("PDF") }

    // Sections 2-8 start collapsed by default
    val expandedSections = remember {
        mutableStateMapOf(
            "sales" to false,
            "baki" to false,
            "suppliers" to false,
            "stock" to false,
            "expenses" to false,
            "cash" to false,
            "pnl" to false
        )
    }

    val currentMonthLabel = ShopLocale.formatMonthYear(year, month, currentLanguage)
    val noDataText = if (isBn) "এই মাসে কোনো তথ্য নেই" else "No data this month"

    fun moneyText(amount: Double): String =
        if (isPrivacyHidden) "${ShopLocale.CURRENCY}••••" else ShopLocale.formatMoney(amount, currentLanguage)

    fun signedMoneyText(amount: Double): String =
        if (isPrivacyHidden) "${ShopLocale.CURRENCY}••••" else ShopLocale.formatSignedMoney(amount, currentLanguage)

    fun numText(value: Long): String = ShopLocale.formatNumber(value, currentLanguage)
    fun numText(value: Double): String = ShopLocale.formatNumber(value, currentLanguage)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070907))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VividGreen.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.08f),
                        radius = size.width * 0.85f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("monthly_report_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { storeViewModel.navigateBackInBusiness() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("monthly_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isBn) "পিছনে" else "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBn) "মাসিক রিপোর্ট" else "Monthly Report",
                        color = TextPrimary,
                        fontSize = if (isFatherMode) 21.sp else 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = if (isFatherMode) {
                            if (isBn) "সহজ সারসংক্ষেপ" else "Simple Summary"
                        } else {
                            if (isBn) "সম্পূর্ণ মাস-শেষ হিসাব" else "Complete month-end statement"
                        },
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        softWrap = true
                    )
                }
                IconButton(
                    onClick = { storeViewModel.togglePrivacyHidden() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("monthly_privacy_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isPrivacyHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isPrivacyHidden) {
                            if (isBn) "টাকার পরিমাণ দেখান" else "Show amounts"
                        } else {
                            if (isBn) "টাকার পরিমাণ লুকান" else "Hide amounts"
                        },
                        tint = if (isPrivacyHidden) AmberWarning else TextPrimary
                    )
                }
            }

            // Month Picker Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = GlassSurface,
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val newMonth = if (month == 0) 11 else month - 1
                            val newYear = if (month == 0) year - 1 else year
                            storeViewModel.setMonthlyReportMonth(newYear, newMonth)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = if (isBn) "আগের মাস" else "Previous Month",
                            tint = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentMonthLabel,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true,
                            textAlign = TextAlign.Center
                        )
                    }

                    IconButton(
                        onClick = {
                            val newMonth = if (month == 11) 0 else month + 1
                            val newYear = if (month == 11) year + 1 else year
                            storeViewModel.setMonthlyReportMonth(newYear, newMonth)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = if (isBn) "পরের মাস" else "Next Month",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Localized Export Action Bar
            LocalizedMonthlyExportBar(
                isBn = isBn,
                onDownloadPdf = {
                    selectedExportFormat = "PDF"
                    showExportLanguageModal = true
                },
                onPrint = {
                    val data = ReportDataBuilders.buildMonthlyReportTable(
                        reportData = reportData,
                        year = year,
                        month = month,
                        store = store,
                        canSeeProfit = canSeeProfit,
                        lang = currentLanguage,
                        includeSupplierDebt = !isFatherMode
                    )
                    val file = ReportExportUtil.generatePdfReport(
                        context = context,
                        data = data,
                        fileName = "MonthlyReport_${year}_${month + 1}",
                        lang = currentLanguage
                    )
                    ReportExportUtil.printReportFile(
                        context = context,
                        file = file,
                        reportName = if (isBn) "মাসিক রিপোর্ট" else "Monthly Report",
                        lang = currentLanguage
                    )
                },
                onExportCsv = {
                    selectedExportFormat = "CSV"
                    showExportLanguageModal = true
                },
                onShare = {
                    val data = ReportDataBuilders.buildMonthlyReportTable(
                        reportData = reportData,
                        year = year,
                        month = month,
                        store = store,
                        canSeeProfit = canSeeProfit,
                        lang = currentLanguage,
                        includeSupplierDebt = !isFatherMode
                    )
                    val file = ReportExportUtil.generatePdfReport(
                        context = context,
                        data = data,
                        fileName = "MonthlyReport_${year}_${month + 1}",
                        lang = currentLanguage
                    )
                    val subject = if (isBn) "মাসিক রিপোর্ট: $currentMonthLabel" else "Monthly Report: $currentMonthLabel"
                    ReportExportUtil.shareReportFile(context, file, "application/pdf", subject, currentLanguage)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("monthly_report_list"),
                contentPadding = PaddingValues(top = 2.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. SUMMARY (Always open)
                item(key = "section_summary") {
                    HeroGlowGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "section_summary_card"
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "১. সারসংক্ষেপ" else "1. Summary",
                                    color = VividGreen,
                                    fontSize = if (isFatherMode) 17.sp else 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 2,
                                    softWrap = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = VividGreen.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, VividGreen.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = ShopLocale.format("total_orders_row", currentLanguage, reportData.totalOrders),
                                        color = VividGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val summaryItems = buildList {
                                add(
                                    SummaryKpi(
                                        label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                                        value = moneyText(reportData.netSales),
                                        color = VividGreen
                                    )
                                )
                                if (canSeeProfit) {
                                    add(
                                        SummaryKpi(
                                            label = if (isBn) "মোট লাভ" else "Gross Profit",
                                            value = moneyText(reportData.grossProfit),
                                            color = CyanAccent
                                        )
                                    )
                                }
                                add(
                                    SummaryKpi(
                                        label = if (isBn) "মোট খরচ" else "Total Expenses",
                                        value = moneyText(reportData.totalExpenses),
                                        color = AlertRed
                                    )
                                )
                                if (canSeeProfit) {
                                    add(
                                        SummaryKpi(
                                            label = if (isBn) "নিট লাভ" else "Net Profit",
                                            value = moneyText(reportData.netProfit),
                                            color = if (reportData.netProfit >= 0) VividGreen else AlertRed
                                        )
                                    )
                                }
                                add(
                                    SummaryKpi(
                                        label = if (isBn) "ক্যাশ ও ব্যাংক শেষে আছে" else "Cash Closing Balance",
                                        value = moneyText(reportData.cashClosingBalance),
                                        color = TextPrimary
                                    )
                                )
                                add(
                                    SummaryKpi(
                                        label = if (isBn) "মোট বাকি" else "Total Baki to Receive",
                                        value = moneyText(reportData.bakiClosingReceivable),
                                        color = AmberWarning
                                    )
                                )
                                add(
                                    SummaryKpi(
                                        label = if (isBn) "মহাজনকে দেব" else "Total Payable to Suppliers",
                                        value = moneyText(reportData.supplierClosingPayable),
                                        color = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary
                                    )
                                )
                            }

                            SummaryKpiGrid(items = summaryItems, isLargeText = isFatherMode)
                        }
                    }
                }

                // Father mode shows ONLY SUMMARY
                if (!isFatherMode) {
                    // 2. SALES
                    item(key = "section_sales") {
                        CollapsibleMonthlySection(
                            title = if (isBn) "২. বিক্রয়" else "2. Sales",
                            keyMetric = moneyText(reportData.netSales),
                            keyColor = VividGreen,
                            icon = Icons.Default.ShoppingBag,
                            expanded = expandedSections["sales"] == true,
                            onToggle = { expandedSections["sales"] = !(expandedSections["sales"] ?: false) },
                            testTag = "section_sales_card"
                        ) {
                            if (reportData.totalOrders == 0L) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                MetricRow(
                                    label = if (isBn) "মোট বিক্রয় (ছাড়ের আগে)" else "Gross Sales",
                                    value = moneyText(reportData.grossSales)
                                )
                                MetricRow(
                                    label = if (isBn) "ছাড়" else "Discounts",
                                    value = moneyText(reportData.totalDiscounts),
                                    valueColor = if (reportData.totalDiscounts > 0) AmberWarning else TextPrimary
                                )
                                MetricRow(
                                    label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                                    value = moneyText(reportData.netSales),
                                    valueColor = VividGreen,
                                    bold = true
                                )
                                MetricRow(
                                    label = if (isBn) "অর্ডার সংখ্যা" else "Order Count",
                                    value = numText(reportData.totalOrders)
                                )
                                MetricRow(
                                    label = if (isBn) "গড় অর্ডার মূল্য" else "Average Order Value",
                                    value = moneyText(reportData.averageOrderValue)
                                )
                                MetricRow(
                                    label = if (isBn) "নগদ প্রাপ্ত বিক্রয়" else "Cash Received Sales",
                                    value = moneyText(reportData.cashReceivedSales),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = if (isBn) "বাকিতে বিক্রয়" else "Sold on Credit (Baki)",
                                    value = moneyText(reportData.bakiCreditSales),
                                    valueColor = AmberWarning
                                )

                                if (reportData.topProductsByAmount.isNotEmpty()) {
                                    SectionSubheading(
                                        text = if (isBn) "শীর্ষ ৫ পণ্য (বিক্রয় অনুযায়ী)" else "Top 5 Products by Amount"
                                    )
                                    reportData.topProductsByAmount.forEachIndexed { idx, prod ->
                                        val rank = numText(idx + 1L)
                                        val qtyLine = "${numText(prod.quantitySold)} ${ShopLocale.displayUnit(prod.unit, currentLanguage)}"
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "$rank. ${prod.productName}",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                Text(
                                                    text = qtyLine,
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = moneyText(prod.totalSalesAmount),
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (canSeeProfit) {
                                                    Text(
                                                        text = if (isBn) "লাভ ${moneyText(prod.totalProfit)}" else "+${moneyText(prod.totalProfit)}",
                                                        color = VividGreen,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                        if (idx < reportData.topProductsByAmount.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }

                                SectionSubheading(
                                    text = if (isBn) "শীর্ষ ৫ ক্রেতা (পরিমাণ অনুযায়ী)" else "Top 5 Customers by Amount"
                                )
                                if (reportData.topCustomersByAmount.isEmpty()) {
                                    Text(
                                        text = if (isBn) "এই মাসে কোনো নিবন্ধিত ক্রেতার বিক্রয় নেই" else "No named customer sales this month",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                } else {
                                    reportData.topCustomersByAmount.forEachIndexed { idx, cust ->
                                        val rank = numText(idx + 1L)
                                        val dispName = ShopLocale.displayCustomerName(cust.customerName, currentLanguage)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "$rank. $dispName",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                Text(
                                                    text = if (isBn) {
                                                        "নগদ: ${moneyText(cust.paidAmount)} • বাকি: ${moneyText(cust.bakiAmount)}"
                                                    } else {
                                                        "Paid: ${moneyText(cust.paidAmount)} • Baki: ${moneyText(cust.bakiAmount)}"
                                                    },
                                                    color = if (cust.bakiAmount > 0) AmberWarning else TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = moneyText(cust.totalBought),
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (idx < reportData.topCustomersByAmount.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. BAKI / CREDIT
                    item(key = "section_baki") {
                        CollapsibleMonthlySection(
                            title = if (isBn) "৩. বাকি / ক্রেডিট" else "3. Baki / Credit",
                            keyMetric = moneyText(reportData.bakiClosingReceivable),
                            keyColor = AmberWarning,
                            icon = Icons.Default.People,
                            expanded = expandedSections["baki"] == true,
                            onToggle = { expandedSections["baki"] = !(expandedSections["baki"] ?: false) },
                            testTag = "section_baki_card"
                        ) {
                            if (!reportData.bakiHasActivityOrDue) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                MetricRow(
                                    label = if (isBn) "শুরুতে বাকি" else "Opening Receivable",
                                    value = moneyText(reportData.bakiOpeningReceivable)
                                )
                                MetricRow(
                                    label = if (isBn) "নতুন বাকি" else "New Credit Given",
                                    value = moneyText(reportData.bakiNewCreditGiven),
                                    valueColor = AmberWarning
                                )
                                MetricRow(
                                    label = if (isBn) "এই মাসে আদায়" else "Collected This Month",
                                    value = moneyText(reportData.bakiCollectedThisMonth),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = if (isBn) "শেষে বাকি আছে" else "Closing Receivable",
                                    value = moneyText(reportData.bakiClosingReceivable),
                                    valueColor = AmberWarning,
                                    bold = true
                                )

                                SectionSubheading(
                                    text = if (isBn) "বকেয়ার মেয়াদ (দিন)" else "Ageing Buckets"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AgeingBucketChip(
                                        label = if (isBn) "০-৩০ দিন" else "0-30 days",
                                        value = moneyText(reportData.bakiAgeing.bucket0To30),
                                        accent = VividGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AgeingBucketChip(
                                        label = if (isBn) "৩১-৬০ দিন" else "31-60 days",
                                        value = moneyText(reportData.bakiAgeing.bucket31To60),
                                        accent = AmberWarning,
                                        modifier = Modifier.weight(1f)
                                    )
                                    AgeingBucketChip(
                                        label = if (isBn) "৬০+ দিন" else "60+ days",
                                        value = moneyText(reportData.bakiAgeing.bucket60Plus),
                                        accent = AlertRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                SectionSubheading(
                                    text = if (isBn) "বাকি থাকা ক্রেতা (${numText(reportData.debtorRows.size.toLong())})"
                                    else "Customers Owing Money (${reportData.debtorRows.size})"
                                )
                                if (reportData.debtorRows.isEmpty()) {
                                    Text(
                                        text = if (isBn) "মাস শেষে কোনো বাকি নেই" else "No customers owe baki at month end",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                } else {
                                    reportData.debtorRows.forEachIndexed { idx, debtor ->
                                        val overdueText = if (isBn) {
                                            "${numText(debtor.daysOverdue)} দিন বকেয়া"
                                        } else {
                                            "${numText(debtor.daysOverdue)} days overdue"
                                        }
                                        val overdueColor = when {
                                            debtor.daysOverdue > 60 -> AlertRed
                                            debtor.daysOverdue > 30 -> AmberWarning
                                            else -> TextSecondary
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = debtor.customerName,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                Text(
                                                    text = overdueText,
                                                    color = overdueColor,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = moneyText(debtor.amountOwed),
                                                color = AmberWarning,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (idx < reportData.debtorRows.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. SUPPLIERS
                    item(key = "section_suppliers") {
                        CollapsibleMonthlySection(
                            title = if (isBn) "৪. মহাজন / সাপ্লায়ার" else "4. Suppliers",
                            keyMetric = moneyText(reportData.supplierClosingPayable),
                            keyColor = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary,
                            icon = Icons.Default.LocalShipping,
                            expanded = expandedSections["suppliers"] == true,
                            onToggle = { expandedSections["suppliers"] = !(expandedSections["suppliers"] ?: false) },
                            testTag = "section_suppliers_card"
                        ) {
                            if (!reportData.supplierHasActivityOrDue) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                MetricRow(
                                    label = if (isBn) "প্রারম্ভিক দেনা" else "Opening Payable",
                                    value = moneyText(reportData.supplierOpeningPayable)
                                )
                                MetricRow(
                                    label = if (isBn) "এই মাসের ক্রয়" else "Purchases This Month",
                                    value = moneyText(reportData.supplierPurchasesThisMonth)
                                )
                                MetricRow(
                                    label = if (isBn) "এই মাসে পরিশোধ" else "Paid This Month",
                                    value = moneyText(reportData.supplierPaidThisMonth),
                                    valueColor = VividGreen
                                )
                                if (reportData.supplierReturnsThisMonth > 0.001) {
                                    MetricRow(
                                        label = if (isBn) "ক্রয় ফেরত" else "Purchase Returns",
                                        value = moneyText(reportData.supplierReturnsThisMonth),
                                        valueColor = CyanAccent
                                    )
                                }
                                MetricRow(
                                    label = if (isBn) "শেষে দেব" else "Closing Payable",
                                    value = moneyText(reportData.supplierClosingPayable),
                                    valueColor = if (reportData.supplierClosingPayable > 0) AlertRed else TextPrimary,
                                    bold = true
                                )

                                SectionSubheading(
                                    text = if (isBn) "মহাজন অনুযায়ী দেনা (${numText(reportData.supplierOwedRows.size.toLong())})"
                                    else "Payable by Supplier (${reportData.supplierOwedRows.size})"
                                )
                                if (reportData.supplierOwedRows.isEmpty()) {
                                    Text(
                                        text = if (isBn) "মাস শেষে কোনো মহাজন দেনা নেই" else "No supplier dues at month end",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                } else {
                                    reportData.supplierOwedRows.forEachIndexed { idx, sup ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sup.supplierName,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                if (sup.supplierPhone.isNotBlank()) {
                                                    Text(
                                                        text = ShopLocale.digits(sup.supplierPhone, currentLanguage),
                                                        color = TextSecondary,
                                                        fontSize = 11.5.sp,
                                                        maxLines = 2,
                                                        softWrap = true
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = moneyText(sup.amountOwed),
                                                color = AlertRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (idx < reportData.supplierOwedRows.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. STOCK
                    item(key = "section_stock") {
                        val stockEmpty = reportData.openingStockValue <= 0.001 &&
                            reportData.purchasedStockValue <= 0.001 &&
                            reportData.soldStockAtCost <= 0.001 &&
                            reportData.closingStockValue <= 0.001 &&
                            reportData.lowStockItems.isEmpty() &&
                            reportData.slowStock30DayItems.isEmpty()
                        CollapsibleMonthlySection(
                            title = if (isBn) "৫. স্টক" else "5. Stock",
                            keyMetric = moneyText(reportData.closingStockValue),
                            keyColor = CyanAccent,
                            icon = Icons.Default.Inventory2,
                            expanded = expandedSections["stock"] == true,
                            onToggle = { expandedSections["stock"] = !(expandedSections["stock"] ?: false) },
                            testTag = "section_stock_card"
                        ) {
                            if (stockEmpty) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                MetricRow(
                                    label = if (isBn) "প্রারম্ভিক স্টক মূল্য (ক্রয়মূল্যে)" else "Opening Stock Value (at Cost)",
                                    value = moneyText(reportData.openingStockValue)
                                )
                                MetricRow(
                                    label = if (isBn) "ক্রয়কৃত স্টক" else "Purchased Stock",
                                    value = moneyText(reportData.purchasedStockValue),
                                    valueColor = VividGreen
                                )
                                MetricRow(
                                    label = if (isBn) "বিক্রিত স্টক (ক্রয়মূল্যে)" else "Sold Stock (at Cost)",
                                    value = moneyText(reportData.soldStockAtCost),
                                    valueColor = AmberWarning
                                )
                                MetricRow(
                                    label = if (isBn) "শেষে স্টক মূল্য" else "Closing Stock Value",
                                    value = moneyText(reportData.closingStockValue),
                                    valueColor = CyanAccent,
                                    bold = true
                                )

                                SectionSubheading(
                                    text = if (isBn) "কম স্টক পণ্য (${numText(reportData.lowStockItems.size.toLong())})"
                                    else "Low-Stock Items (${reportData.lowStockItems.size})"
                                )
                                if (reportData.lowStockItems.isEmpty()) {
                                    Text(
                                        text = if (isBn) "সব পণ্যের স্টক স্বাভাবিক আছে" else "No low-stock items",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                } else {
                                    reportData.lowStockItems.forEachIndexed { idx, item ->
                                        val unitStr = ShopLocale.displayUnit(item.unit, currentLanguage)
                                        val stockLine = if (isBn) {
                                            "স্টক: ${numText(item.stock)} / সীমা ${numText(item.lowStockLimit)} $unitStr"
                                        } else {
                                            "Stock: ${numText(item.stock)} / limit ${numText(item.lowStockLimit)} $unitStr"
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.productName,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                Text(
                                                    text = stockLine,
                                                    color = AmberWarning,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = moneyText(item.stockValueAtCost),
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (idx < reportData.lowStockItems.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }

                                val slowHeader = if (isBn) {
                                    "৩০ দিনে বিক্রি হয়নি (${numText(reportData.slowStock30DayItems.size.toLong())}) • আটকে থাকা টাকা: ${moneyText(reportData.totalSlowStockMoneyStuck)}"
                                } else {
                                    "No Sale in 30 Days (${reportData.slowStock30DayItems.size}) • Stuck: ${moneyText(reportData.totalSlowStockMoneyStuck)}"
                                }
                                SectionSubheading(text = slowHeader)
                                if (reportData.slowStock30DayItems.isEmpty()) {
                                    Text(
                                        text = if (isBn) "গত ৩০ দিনে সব স্টক পণ্যই বিক্রি হয়েছে" else "All in-stock items sold within 30 days",
                                        color = TextMuted,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        softWrap = true
                                    )
                                } else {
                                    reportData.slowStock30DayItems.forEachIndexed { idx, item ->
                                        val unitStr = ShopLocale.displayUnit(item.unit, currentLanguage)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.productName,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                                Text(
                                                    text = if (isBn) {
                                                        "মজুদ: ${numText(item.stock)} $unitStr"
                                                    } else {
                                                        "In stock: ${numText(item.stock)} $unitStr"
                                                    },
                                                    color = TextSecondary,
                                                    fontSize = 11.5.sp,
                                                    maxLines = 2,
                                                    softWrap = true
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = moneyText(item.moneyStuck),
                                                    color = AlertRed,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (isBn) "আটকে আছে" else "stuck",
                                                    color = TextMuted,
                                                    fontSize = 10.5.sp
                                                )
                                            }
                                        }
                                        if (idx < reportData.slowStock30DayItems.lastIndex) {
                                            HorizontalDivider(color = Color(0x14FFFFFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. EXPENSES
                    item(key = "section_expenses") {
                        CollapsibleMonthlySection(
                            title = if (isBn) "৬. খরচ" else "6. Expenses",
                            keyMetric = moneyText(reportData.totalExpenses),
                            keyColor = AlertRed,
                            icon = Icons.Default.ReceiptLong,
                            expanded = expandedSections["expenses"] == true,
                            onToggle = { expandedSections["expenses"] = !(expandedSections["expenses"] ?: false) },
                            testTag = "section_expenses_card"
                        ) {
                            if (reportData.expensesByCategory.isEmpty()) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                reportData.expensesByCategory.forEachIndexed { idx, cat ->
                                    val pctStr = ShopLocale.formatPercent(cat.percentage, 1, currentLanguage)
                                    val shareFrac = (cat.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = ShopLocale.displayCategory(cat.category, currentLanguage),
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                softWrap = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = pctStr,
                                                color = TextSecondary,
                                                fontSize = 11.5.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = moneyText(cat.amount),
                                                color = AlertRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(5.dp))
                                        LinearProgressIndicator(
                                            progress = { shareFrac },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = AlertRed,
                                            trackColor = Color(0x1FFFFFFF)
                                        )
                                    }
                                    if (idx < reportData.expensesByCategory.lastIndex) {
                                        HorizontalDivider(color = Color(0x14FFFFFF), modifier = Modifier.padding(vertical = 2.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = Color(0x28FFFFFF))
                                Spacer(modifier = Modifier.height(6.dp))
                                MetricRow(
                                    label = if (isBn) "মোট খরচ" else "Total Expenses",
                                    value = moneyText(reportData.totalExpenses),
                                    valueColor = AlertRed,
                                    bold = true
                                )
                            }
                        }
                    }

                    // 7. CASH & BANK
                    item(key = "section_cash") {
                        CollapsibleMonthlySection(
                            title = if (isBn) "৭. ক্যাশ ও ব্যাংক" else "7. Cash & Bank",
                            keyMetric = moneyText(reportData.cashClosingBalance),
                            keyColor = VividGreen,
                            icon = Icons.Default.AccountBalanceWallet,
                            expanded = expandedSections["cash"] == true,
                            onToggle = { expandedSections["cash"] = !(expandedSections["cash"] ?: false) },
                            testTag = "section_cash_card"
                        ) {
                            if (reportData.accountCashRows.isEmpty()) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                reportData.accountCashRows.forEachIndexed { idx, acc ->
                                    val accName = ShopLocale.displayAccountName(acc.accountName, acc.accountId, currentLanguage)
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0x14FFFFFF),
                                        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = accName,
                                                color = TextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                softWrap = true
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            MetricRow(
                                                label = if (isBn) "প্রারম্ভিক" else "Opening",
                                                value = moneyText(acc.opening)
                                            )
                                            MetricRow(
                                                label = if (isBn) "টাকা এসেছে" else "Money In",
                                                value = moneyText(acc.moneyIn),
                                                valueColor = VividGreen
                                            )
                                            MetricRow(
                                                label = if (isBn) "টাকা গেছে" else "Money Out",
                                                value = moneyText(acc.moneyOut),
                                                valueColor = AlertRed
                                            )
                                            HorizontalDivider(
                                                color = Color(0x1FFFFFFF),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                            MetricRow(
                                                label = if (isBn) "শেষে আছে" else "Closing",
                                                value = moneyText(acc.closing),
                                                valueColor = VividGreen,
                                                bold = true
                                            )
                                        }
                                    }
                                    if (idx < reportData.accountCashRows.lastIndex) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = Color(0x28FFFFFF))
                                Spacer(modifier = Modifier.height(6.dp))
                                MetricRow(
                                    label = if (isBn) "মোট শেষে আছে" else "Total Closing Balance",
                                    value = moneyText(reportData.cashClosingBalance),
                                    valueColor = VividGreen,
                                    bold = true
                                )
                            }
                        }
                    }

                    // 8. PROFIT & LOSS
                    item(key = "section_pnl") {
                        val pnlEmpty = reportData.netSales == 0.0 &&
                            reportData.costOfGoodsSold == 0.0 &&
                            reportData.totalExpenses == 0.0 &&
                            reportData.otherIncome == 0.0
                        CollapsibleMonthlySection(
                            title = if (isBn) "৮. লাভ ও ক্ষতি" else "8. Profit & Loss",
                            keyMetric = moneyText(reportData.netProfit),
                            keyColor = if (reportData.netProfit >= 0) VividGreen else AlertRed,
                            icon = Icons.Default.PieChart,
                            expanded = expandedSections["pnl"] == true,
                            onToggle = { expandedSections["pnl"] = !(expandedSections["pnl"] ?: false) },
                            testTag = "section_pnl_card"
                        ) {
                            if (pnlEmpty) {
                                SectionEmptyMessage(noDataText)
                            } else {
                                MetricRow(
                                    label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                                    value = moneyText(reportData.netSales),
                                    valueColor = TextPrimary
                                )
                                MetricRow(
                                    label = if (isBn) "(-) বিক্রিত পণ্যের ক্রয়মূল্য (COGS)" else "(-) Cost of Goods Sold",
                                    value = moneyText(reportData.costOfGoodsSold),
                                    valueColor = AmberWarning
                                )
                                HorizontalDivider(
                                    color = Color(0x24FFFFFF),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                MetricRow(
                                    label = if (isBn) "(=) মোট লাভ" else "(=) Gross Profit",
                                    value = moneyText(reportData.grossProfit),
                                    valueColor = if (reportData.grossProfit >= 0) CyanAccent else AlertRed,
                                    bold = true
                                )
                                if (reportData.otherIncome > 0.001) {
                                    MetricRow(
                                        label = if (isBn) "(+) অন্যান্য আয়" else "(+) Other Income",
                                        value = moneyText(reportData.otherIncome),
                                        valueColor = VividGreen
                                    )
                                }
                                MetricRow(
                                    label = if (isBn) "(-) মোট খরচ" else "(-) Expenses",
                                    value = moneyText(reportData.totalExpenses),
                                    valueColor = AlertRed
                                )
                                HorizontalDivider(
                                    color = Color(0x33FFFFFF),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                                MetricRow(
                                    label = if (isBn) "(=) নিট লাভ" else "(=) Net Profit",
                                    value = moneyText(reportData.netProfit),
                                    valueColor = if (reportData.netProfit >= 0) VividGreen else AlertRed,
                                    bold = true
                                )
                            }
                        }
                    }

                    // Bottom Check Line Card: Closing = Opening + In - Out
                    item(key = "section_reconciliation_check") {
                        MonthlyReconciliationCheckCard(
                            cashCheck = reportData.cashCheck,
                            bakiCheck = reportData.bakiCheck,
                            supplierCheck = reportData.supplierCheck,
                            isBn = isBn,
                            moneyText = ::moneyText,
                            signedMoneyText = ::signedMoneyText
                        )
                    }
                }
            }
        }
    }

    if (showExportLanguageModal) {
        Dialog(onDismissRequest = { showExportLanguageModal = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkBackground,
                border = BorderStroke(1.dp, GlassBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                var chosenLang by remember(currentLanguage) { mutableStateOf(currentLanguage) }

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = VividGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBn) "রিপোর্টের ভাষা বাছাই করুন" else "Choose Report Language",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenLang = AppLanguage.EN }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenLang == AppLanguage.EN,
                            onClick = { chosenLang = AppLanguage.EN },
                            colors = RadioButtonDefaults.colors(selectedColor = VividGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "ইংরেজি (সর্বজনীন হিসাবের জন্য)" else "English (Universal accounting)",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { chosenLang = AppLanguage.BN }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = chosenLang == AppLanguage.BN,
                            onClick = { chosenLang = AppLanguage.BN },
                            colors = RadioButtonDefaults.colors(selectedColor = VividGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বাংলা (Bengali Unicode)",
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showExportLanguageModal = false },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text(
                                text = if (isBn) "বাতিল" else "Cancel",
                                color = TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                showExportLanguageModal = false
                                val data = ReportDataBuilders.buildMonthlyReportTable(
                                    reportData = reportData,
                                    year = year,
                                    month = month,
                                    store = store,
                                    canSeeProfit = canSeeProfit,
                                    lang = chosenLang,
                                    includeSupplierDebt = !isFatherMode
                                )
                                val fileName = "MonthlyReport_${year}_${month + 1}_${chosenLang.name}"
                                val file = if (selectedExportFormat == "PDF") {
                                    ReportExportUtil.generatePdfReport(context, data, fileName, chosenLang)
                                } else {
                                    ReportExportUtil.generateCsvReport(context, data, fileName, chosenLang)
                                }
                                ReportExportUtil.downloadReportFile(context, file, chosenLang)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VividGreen),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            val exportLabel = if (isBn) {
                                if (selectedExportFormat == "PDF") "PDF ডাউনলোড" else "CSV ডাউনলোড"
                            } else {
                                "Export $selectedExportFormat"
                            }
                            Text(
                                text = exportLabel,
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class SummaryKpi(
    val label: String,
    val value: String,
    val color: Color
)

@Composable
private fun SummaryKpiGrid(
    items: List<SummaryKpi>,
    isLargeText: Boolean
) {
    val rows = remember(items) { items.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pair.forEach { kpi ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0x1AFFFFFF),
                        border = BorderStroke(1.dp, Color(0x1FFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                text = kpi.label,
                                color = TextSecondary,
                                fontSize = if (isLargeText) 13.sp else 11.5.sp,
                                lineHeight = 15.sp,
                                maxLines = 2,
                                softWrap = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = kpi.value,
                                color = kpi.color,
                                fontSize = if (isLargeText) 17.sp else 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CollapsibleMonthlySection(
    title: String,
    keyMetric: String,
    keyColor: Color,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        testTag = testTag,
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = keyColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, keyColor.copy(alpha = 0.30f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = keyColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = keyMetric,
                    color = keyColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = Color(0x1FFFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (bold) TextPrimary else TextSecondary,
            fontSize = if (bold) 13.5.sp else 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            softWrap = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            color = valueColor,
            fontSize = if (bold) 14.sp else 13.sp,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SectionSubheading(text: String) {
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = text,
        color = VividGreen,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 2,
        softWrap = true
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun SectionEmptyMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            softWrap = true
        )
    }
}

@Composable
private fun AgeingBucketChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                softWrap = true,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun MonthlyReconciliationCheckCard(
    cashCheck: MonthlyReconciliationCheck,
    bakiCheck: MonthlyReconciliationCheck,
    supplierCheck: MonthlyReconciliationCheck,
    isBn: Boolean,
    moneyText: (Double) -> String,
    signedMoneyText: (Double) -> String
) {
    val checks = listOf(
        Triple(cashCheck, if (isBn) "ক্যাশ ও ব্যাংক" else "Cash & Bank", "check_cash_row"),
        Triple(bakiCheck, if (isBn) "বাকি / ক্রেডিট" else "Baki / Credit", "check_baki_row"),
        Triple(supplierCheck, if (isBn) "মহাজন / সাপ্লায়ার" else "Suppliers", "check_suppliers_row")
    )
    val allMatched = checks.all { it.first.matches }
    val borderAccent = if (allMatched) VividGreen.copy(alpha = 0.35f) else AmberWarning.copy(alpha = 0.70f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = borderAccent,
        testTag = "section_check_card",
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (allMatched) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (allMatched) VividGreen else AmberWarning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isBn) {
                        "হিসাব যাচাই: শেষে আছে = শুরু + টাকা এসেছে - টাকা গেছে"
                    } else {
                        "Check: Closing = Opening + In - Out"
                    },
                    color = if (allMatched) TextPrimary else AmberWarning,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    softWrap = true,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!allMatched) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarning.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBn) {
                            "সতর্কতা: নিচের একটি বা একাধিক হিসাব শেষে থাকা টাকার সাথে পুরোপুরি মেলেনি।"
                        } else {
                            "Warning: One or more closing balances do not match Opening + In - Out."
                        },
                        color = AmberWarning,
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        softWrap = true,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            checks.forEachIndexed { idx, (chk, label, tag) ->
                val equationLine = if (isBn) {
                    "${moneyText(chk.opening)} + ${moneyText(chk.totalIn)} - ${moneyText(chk.totalOut)} = ${moneyText(chk.expectedClosing)}"
                } else {
                    "${moneyText(chk.opening)} + ${moneyText(chk.totalIn)} - ${moneyText(chk.totalOut)} = ${moneyText(chk.expectedClosing)}"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag(tag),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (chk.matches) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (chk.matches) VividGreen else AlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            softWrap = true
                        )
                        Text(
                            text = equationLine,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            softWrap = true
                        )
                        if (!chk.matches) {
                            Text(
                                text = if (isBn) {
                                    "প্রকৃত: ${moneyText(chk.actualClosing)} • পার্থক্য: ${signedMoneyText(chk.difference)}"
                                } else {
                                    "Actual: ${moneyText(chk.actualClosing)} • Diff: ${signedMoneyText(chk.difference)}"
                                },
                                color = AlertRed,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                softWrap = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (chk.matches) VividGreen.copy(alpha = 0.14f) else AlertRed.copy(alpha = 0.16f),
                        border = BorderStroke(
                            1.dp,
                            if (chk.matches) VividGreen.copy(alpha = 0.35f) else AlertRed.copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = if (chk.matches) {
                                if (isBn) "সঠিক" else "Match"
                            } else {
                                if (isBn) "অমিল" else "Mismatch"
                            },
                            color = if (chk.matches) VividGreen else AlertRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (idx < checks.lastIndex) {
                    HorizontalDivider(color = Color(0x14FFFFFF))
                }
            }
        }
    }
}

@Composable
private fun LocalizedMonthlyExportBar(
    isBn: Boolean,
    onDownloadPdf: () -> Unit,
    onPrint: () -> Unit,
    onExportCsv: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MonthlyExportChip(
            label = if (isBn) "PDF ডাউনলোড" else "Download PDF",
            icon = Icons.Default.Download,
            testTag = "btn_download_pdf",
            onClick = onDownloadPdf
        )
        MonthlyExportChip(
            label = if (isBn) "প্রিন্ট" else "Print",
            icon = Icons.Default.Print,
            testTag = "btn_print",
            onClick = onPrint
        )
        MonthlyExportChip(
            label = if (isBn) "Excel/CSV" else "Export Excel/CSV",
            icon = Icons.Default.TableChart,
            testTag = "btn_export_csv",
            onClick = onExportCsv
        )
        MonthlyExportChip(
            label = if (isBn) "শেয়ার" else "Share",
            icon = Icons.Default.Share,
            testTag = "btn_share",
            onClick = onShare
        )
    }
}

@Composable
private fun MonthlyExportChip(
    label: String,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x1FFFFFFF),
        border = BorderStroke(1.dp, Color(0x33FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VividGreen,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
