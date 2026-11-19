package com.example.util

import com.example.data.model.AccountStatementReport
import com.example.data.model.Expense
import com.example.data.model.OtherIncome
import com.example.data.model.Product
import com.example.data.model.Purchase
import com.example.data.model.PurchaseReturn
import com.example.data.model.Sale
import com.example.data.model.Store
import com.example.domain.CustomerStatementReport
import com.example.domain.DayBookEntry
import com.example.domain.LedgerEntryType
import com.example.domain.MonthlyReportData
import com.example.domain.ProfitAndLossStatement
import com.example.domain.StaffReportSummary
import com.example.domain.SupplierStatementReport

object ReportDataBuilders {

    private fun nowFormatted(lang: AppLanguage): String =
        ShopLocale.formatDateLong(System.currentTimeMillis(), lang)

    fun buildMonthlyReportTable(
        reportData: MonthlyReportData,
        year: Int,
        month: Int,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN,
        includeSupplierDebt: Boolean = true
    ): ReportTableData {
        val isBn = lang == AppLanguage.BN
        val currentMonthLabel = ShopLocale.formatMonthYear(year, month, lang)
        val noDataLabel = if (isBn) "এই মাসে কোনো তথ্য নেই" else "No data this month"

        val headers = if (isBn) {
            listOf("বিভাগ", "বিবরণ / আইটেম", "বিস্তারিত", "পরিমাণ")
        } else {
            listOf("Section", "Metric / Item", "Detail", "Amount")
        }

        val rows = mutableListOf<List<String>>()
        val rawRows = mutableListOf<List<Double?>>()

        fun addRow(section: String, item: String, detail: String, amountText: String, rawAmount: Double? = null) {
            rows.add(listOf(section, item, detail, amountText))
            rawRows.add(listOf(null, null, null, rawAmount))
        }

        // 1. SUMMARY
        val sSummary = if (isBn) "১. সারসংক্ষেপ" else "1. Summary"
        addRow(sSummary, if (isBn) "নিট বিক্রয়" else "Net Sales", ShopLocale.format("total_orders_row", lang, reportData.totalOrders), ShopLocale.formatMoney(reportData.netSales, lang), reportData.netSales)
        if (canSeeProfit) {
            addRow(sSummary, if (isBn) "মোট লাভ" else "Gross Profit", "", ShopLocale.formatMoney(reportData.grossProfit, lang), reportData.grossProfit)
        }
        addRow(sSummary, if (isBn) "মোট খরচ" else "Total Expenses", "", ShopLocale.formatMoney(reportData.totalExpenses, lang), reportData.totalExpenses)
        if (canSeeProfit) {
            addRow(sSummary, if (isBn) "নিট লাভ" else "Net Profit", "", ShopLocale.formatMoney(reportData.netProfit, lang), reportData.netProfit)
        }
        addRow(sSummary, if (isBn) "ক্যাশ ও ব্যাংক শেষে আছে" else "Cash Closing Balance", "", ShopLocale.formatMoney(reportData.cashClosingBalance, lang), reportData.cashClosingBalance)
        addRow(sSummary, if (isBn) "মোট বাকি" else "Total Baki to Receive", "", ShopLocale.formatMoney(reportData.bakiClosingReceivable, lang), reportData.bakiClosingReceivable)
        addRow(sSummary, if (isBn) "মহাজনকে দেব" else "Total Payable to Suppliers", "", ShopLocale.formatMoney(reportData.supplierClosingPayable, lang), reportData.supplierClosingPayable)

        if (includeSupplierDebt) {
            // 2. SALES
            val sSales = if (isBn) "২. বিক্রয়" else "2. Sales"
            if (reportData.totalOrders == 0L) {
                addRow(sSales, noDataLabel, "", "")
            } else {
                addRow(sSales, if (isBn) "মোট বিক্রয় (ছাড়ের আগে)" else "Gross Sales", "", ShopLocale.formatMoney(reportData.grossSales, lang), reportData.grossSales)
                addRow(sSales, if (isBn) "ছাড়" else "Discounts", "", ShopLocale.formatMoney(reportData.totalDiscounts, lang), reportData.totalDiscounts)
                addRow(sSales, if (isBn) "নিট বিক্রয়" else "Net Sales", "", ShopLocale.formatMoney(reportData.netSales, lang), reportData.netSales)
                addRow(sSales, if (isBn) "অর্ডার সংখ্যা" else "Order Count", "", ShopLocale.formatNumber(reportData.totalOrders, lang), reportData.totalOrders.toDouble())
                addRow(sSales, if (isBn) "গড় অর্ডার মূল্য" else "Average Order Value", "", ShopLocale.formatMoney(reportData.averageOrderValue, lang), reportData.averageOrderValue)
                addRow(sSales, if (isBn) "নগদ প্রাপ্ত বিক্রয়" else "Cash Received Sales", "", ShopLocale.formatMoney(reportData.cashReceivedSales, lang), reportData.cashReceivedSales)
                addRow(sSales, if (isBn) "বাকিতে বিক্রয়" else "Sold on Credit (Baki)", "", ShopLocale.formatMoney(reportData.bakiCreditSales, lang), reportData.bakiCreditSales)

                reportData.topProductsByAmount.forEachIndexed { idx, prod ->
                    val rank = ShopLocale.formatNumber(idx + 1, lang)
                    val qtyDetail = "${ShopLocale.formatNumber(prod.quantitySold, lang)} ${ShopLocale.displayUnit(prod.unit, lang)}"
                    addRow(
                        sSales,
                        if (isBn) "শীর্ষ পণ্য #$rank: ${prod.productName}" else "Top Product #$rank: ${prod.productName}",
                        qtyDetail,
                        ShopLocale.formatMoney(prod.totalSalesAmount, lang),
                        prod.totalSalesAmount
                    )
                }
                reportData.topCustomersByAmount.forEachIndexed { idx, cust ->
                    val rank = ShopLocale.formatNumber(idx + 1, lang)
                    val custDisplay = ShopLocale.displayCustomerName(cust.customerName, lang)
                    val split = if (isBn) {
                        "নগদ ${ShopLocale.formatMoney(cust.paidAmount, lang)} • বাকি ${ShopLocale.formatMoney(cust.bakiAmount, lang)}"
                    } else {
                        "Paid ${ShopLocale.formatMoney(cust.paidAmount, lang)} • Baki ${ShopLocale.formatMoney(cust.bakiAmount, lang)}"
                    }
                    addRow(
                        sSales,
                        if (isBn) "শীর্ষ ক্রেতা #$rank: $custDisplay" else "Top Customer #$rank: $custDisplay",
                        split,
                        ShopLocale.formatMoney(cust.totalBought, lang),
                        cust.totalBought
                    )
                }
            }

            // 3. BAKI / CREDIT
            val sBaki = if (isBn) "৩. বাকি / ক্রেডিট" else "3. Baki / Credit"
            if (!reportData.bakiHasActivityOrDue) {
                addRow(sBaki, noDataLabel, "", "")
            } else {
                addRow(sBaki, if (isBn) "শুরুতে বাকি" else "Opening Receivable", "", ShopLocale.formatMoney(reportData.bakiOpeningReceivable, lang), reportData.bakiOpeningReceivable)
                addRow(sBaki, if (isBn) "নতুন বাকি" else "New Credit Given", "", ShopLocale.formatMoney(reportData.bakiNewCreditGiven, lang), reportData.bakiNewCreditGiven)
                addRow(sBaki, if (isBn) "এই মাসে আদায়" else "Collected This Month", "", ShopLocale.formatMoney(reportData.bakiCollectedThisMonth, lang), reportData.bakiCollectedThisMonth)
                addRow(sBaki, if (isBn) "শেষে বাকি আছে" else "Closing Receivable", "", ShopLocale.formatMoney(reportData.bakiClosingReceivable, lang), reportData.bakiClosingReceivable)
                addRow(sBaki, if (isBn) "বকেয়া বয়স: ০-৩০ দিন" else "Ageing: 0-30 Days", "", ShopLocale.formatMoney(reportData.bakiAgeing.bucket0To30, lang), reportData.bakiAgeing.bucket0To30)
                addRow(sBaki, if (isBn) "বকেয়া বয়স: ৩১-৬০ দিন" else "Ageing: 31-60 Days", "", ShopLocale.formatMoney(reportData.bakiAgeing.bucket31To60, lang), reportData.bakiAgeing.bucket31To60)
                addRow(sBaki, if (isBn) "বকেয়া বয়স: ৬০+ দিন" else "Ageing: 60+ Days", "", ShopLocale.formatMoney(reportData.bakiAgeing.bucket60Plus, lang), reportData.bakiAgeing.bucket60Plus)

                reportData.debtorRows.forEach { d ->
                    val overdueStr = if (isBn) "${ShopLocale.formatNumber(d.daysOverdue, lang)} দিন বকেয়া" else "${ShopLocale.formatNumber(d.daysOverdue, lang)} days overdue"
                    addRow(sBaki, d.customerName, overdueStr, ShopLocale.formatMoney(d.amountOwed, lang), d.amountOwed)
                }
            }

            // 4. SUPPLIERS
            val sSup = if (isBn) "৪. মহাজন / সাপ্লায়ার" else "4. Suppliers"
            if (!reportData.supplierHasActivityOrDue) {
                addRow(sSup, noDataLabel, "", "")
            } else {
                addRow(sSup, if (isBn) "শুরুতে দেব" else "Opening Payable", "", ShopLocale.formatMoney(reportData.supplierOpeningPayable, lang), reportData.supplierOpeningPayable)
                addRow(sSup, if (isBn) "এই মাসের ক্রয়" else "Purchases This Month", "", ShopLocale.formatMoney(reportData.supplierPurchasesThisMonth, lang), reportData.supplierPurchasesThisMonth)
                addRow(sSup, if (isBn) "এই মাসে পরিশোধ" else "Paid This Month", "", ShopLocale.formatMoney(reportData.supplierPaidThisMonth, lang), reportData.supplierPaidThisMonth)
                if (reportData.supplierReturnsThisMonth > 0.001) {
                    addRow(sSup, if (isBn) "ক্রয় ফেরত" else "Purchase Returns", "", ShopLocale.formatMoney(reportData.supplierReturnsThisMonth, lang), reportData.supplierReturnsThisMonth)
                }
                addRow(sSup, if (isBn) "শেষে দেব" else "Closing Payable", "", ShopLocale.formatMoney(reportData.supplierClosingPayable, lang), reportData.supplierClosingPayable)

                reportData.supplierOwedRows.forEach { sup ->
                    addRow(sSup, sup.supplierName, sup.supplierPhone, ShopLocale.formatMoney(sup.amountOwed, lang), sup.amountOwed)
                }
            }

            // 5. STOCK
            val sStock = if (isBn) "৫. স্টক" else "5. Stock"
            if (reportData.openingStockValue <= 0.001 && reportData.purchasedStockValue <= 0.001 && reportData.soldStockAtCost <= 0.001 && reportData.closingStockValue <= 0.001 && reportData.lowStockItems.isEmpty() && reportData.slowStock30DayItems.isEmpty()) {
                addRow(sStock, noDataLabel, "", "")
            } else {
                if (canSeeProfit) {
                    addRow(sStock, if (isBn) "প্রারম্ভিক স্টক মূল্য" else "Opening Stock (at Cost)", "", ShopLocale.formatMoney(reportData.openingStockValue, lang), reportData.openingStockValue)
                    addRow(sStock, if (isBn) "ক্রয়" else "Purchased", "", ShopLocale.formatMoney(reportData.purchasedStockValue, lang), reportData.purchasedStockValue)
                    addRow(sStock, if (isBn) "বিক্রিত পণ্যের ক্রয়মূল্য" else "Sold (at Cost)", "", ShopLocale.formatMoney(reportData.soldStockAtCost, lang), reportData.soldStockAtCost)
                    addRow(sStock, if (isBn) "শেষে স্টক মূল্য" else "Closing Stock (at Cost)", "", ShopLocale.formatMoney(reportData.closingStockValue, lang), reportData.closingStockValue)
                }
                reportData.lowStockItems.forEach { item ->
                    val qtyTxt = "${ShopLocale.formatNumber(item.stock, lang)} / ${ShopLocale.formatNumber(item.lowStockLimit, lang)} ${ShopLocale.displayUnit(item.unit, lang)}"
                    addRow(
                        sStock,
                        if (isBn) "কম স্টক: ${item.productName}" else "Low Stock: ${item.productName}",
                        qtyTxt,
                        if (canSeeProfit) ShopLocale.formatMoney(item.stockValueAtCost, lang) else qtyTxt,
                        if (canSeeProfit) item.stockValueAtCost else item.stock
                    )
                }
                reportData.slowStock30DayItems.forEach { item ->
                    val qtyTxt = "${ShopLocale.formatNumber(item.stock, lang)} ${ShopLocale.displayUnit(item.unit, lang)}"
                    addRow(
                        sStock,
                        if (isBn) "৩০ দিনে বিক্রি হয়নি: ${item.productName}" else "No Sale 30d: ${item.productName}",
                        qtyTxt,
                        if (canSeeProfit) ShopLocale.formatMoney(item.moneyStuck, lang) else qtyTxt,
                        if (canSeeProfit) item.moneyStuck else item.stock
                    )
                }
            }

            // 6. EXPENSES
            val sExp = if (isBn) "৬. খরচ" else "6. Expenses"
            if (reportData.expensesByCategory.isEmpty()) {
                addRow(sExp, noDataLabel, "", "")
            } else {
                reportData.expensesByCategory.forEach { exp ->
                    val pctTxt = ShopLocale.formatPercent(exp.percentage, 1, lang)
                    addRow(sExp, ShopLocale.displayCategory(exp.category, lang), pctTxt, ShopLocale.formatMoney(exp.amount, lang), exp.amount)
                }
                addRow(sExp, if (isBn) "মোট খরচ" else "Total Expenses", "", ShopLocale.formatMoney(reportData.totalExpenses, lang), reportData.totalExpenses)
            }

            // 7. CASH & BANK
            val sCash = if (isBn) "৭. ক্যাশ ও ব্যাংক" else "7. Cash & Bank"
            if (reportData.accountCashRows.isEmpty()) {
                addRow(sCash, noDataLabel, "", "")
            } else {
                reportData.accountCashRows.forEach { acc ->
                    val dispName = ShopLocale.displayAccountName(acc.accountName, acc.accountId, lang)
                    val flowDetail = if (isBn) {
                        "শুরু ${ShopLocale.formatMoney(acc.opening, lang)} | টাকা এসেছে +${ShopLocale.formatMoney(acc.moneyIn, lang)} | টাকা গেছে -${ShopLocale.formatMoney(acc.moneyOut, lang)}"
                    } else {
                        "Open ${ShopLocale.formatMoney(acc.opening, lang)} | In +${ShopLocale.formatMoney(acc.moneyIn, lang)} | Out -${ShopLocale.formatMoney(acc.moneyOut, lang)}"
                    }
                    addRow(sCash, dispName, flowDetail, ShopLocale.formatMoney(acc.closing, lang), acc.closing)
                }
                val totDetail = if (isBn) {
                    "শুরু ${ShopLocale.formatMoney(reportData.cashOpeningTotal, lang)} | টাকা এসেছে +${ShopLocale.formatMoney(reportData.cashInTotal, lang)} | টাকা গেছে -${ShopLocale.formatMoney(reportData.cashOutTotal, lang)}"
                } else {
                    "Open ${ShopLocale.formatMoney(reportData.cashOpeningTotal, lang)} | In +${ShopLocale.formatMoney(reportData.cashInTotal, lang)} | Out -${ShopLocale.formatMoney(reportData.cashOutTotal, lang)}"
                }
                addRow(sCash, if (isBn) "মোট শেষে আছে" else "Total Cash Closing", totDetail, ShopLocale.formatMoney(reportData.cashClosingBalance, lang), reportData.cashClosingBalance)
            }

            // 8. PROFIT & LOSS
            val sPnl = if (isBn) "৮. লাভ ও ক্ষতি" else "8. Profit & Loss"
            if (!canSeeProfit || (reportData.netSales == 0.0 && reportData.costOfGoodsSold == 0.0 && reportData.totalExpenses == 0.0 && reportData.otherIncome == 0.0)) {
                addRow(sPnl, noDataLabel, "", "")
            } else {
                addRow(sPnl, if (isBn) "নিট বিক্রয়" else "Net Sales", "", ShopLocale.formatMoney(reportData.netSales, lang), reportData.netSales)
                addRow(sPnl, if (isBn) "(-) বিক্রিত পণ্যের ক্রয়মূল্য" else "(-) Cost of Goods Sold", "", ShopLocale.formatMoney(reportData.costOfGoodsSold, lang), reportData.costOfGoodsSold)
                addRow(sPnl, if (isBn) "(=) মোট লাভ" else "(=) Gross Profit", "", ShopLocale.formatMoney(reportData.grossProfit, lang), reportData.grossProfit)
                if (reportData.otherIncome > 0.001) {
                    addRow(sPnl, if (isBn) "(+) অন্যান্য আয়" else "(+) Other Income", "", ShopLocale.formatMoney(reportData.otherIncome, lang), reportData.otherIncome)
                }
                addRow(sPnl, if (isBn) "(-) মোট খরচ" else "(-) Expenses", "", ShopLocale.formatMoney(reportData.totalExpenses, lang), reportData.totalExpenses)
                addRow(sPnl, if (isBn) "(=) নিট লাভ" else "(=) Net Profit", "", ShopLocale.formatMoney(reportData.netProfit, lang), reportData.netProfit)
            }

            // RECONCILIATION CHECK
            val sCheck = if (isBn) "হিসাব যাচাই" else "Reconciliation"
            listOf(
                reportData.cashCheck to (if (isBn) "ক্যাশ ও ব্যাংক" else "Cash & Bank"),
                reportData.bakiCheck to (if (isBn) "বাকি / ক্রেডিট" else "Baki / Credit"),
                reportData.supplierCheck to (if (isBn) "মহাজন / সাপ্লায়ার" else "Suppliers")
            ).forEach { (chk, name) ->
                val statusTxt = if (chk.matches) {
                    if (isBn) "সঠিক (শেষে আছে = শুরু + টাকা এসেছে - টাকা গেছে)" else "Matched (Closing = Opening + In - Out)"
                } else {
                    if (isBn) {
                        "সতর্কতা: অমিল ${ShopLocale.formatMoney(chk.difference, lang)} (প্রত্যাশিত ${ShopLocale.formatMoney(chk.expectedClosing, lang)})"
                    } else {
                        "Warning: Diff ${ShopLocale.formatMoney(chk.difference, lang)} (Expected ${ShopLocale.formatMoney(chk.expectedClosing, lang)})"
                    }
                }
                addRow(sCheck, name, statusTxt, ShopLocale.formatMoney(chk.actualClosing, lang), chk.actualClosing)
            }
        }

        val title = ShopLocale.format("rep_monthly_title", lang, currentMonthLabel)

        val summaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = if (isBn) "নিট বিক্রয়" else "Net Sales",
                    value = ShopLocale.formatMoney(reportData.netSales, lang),
                    detail = ShopLocale.format("total_orders_row", lang, reportData.totalOrders)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (isBn) "মোট লাভ" else "Gross Profit",
                        value = ShopLocale.formatMoney(reportData.grossProfit, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (isBn) "মোট খরচ" else "Total Expenses",
                    value = ShopLocale.formatMoney(reportData.totalExpenses, lang)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (isBn) "নিট লাভ" else "Net Profit",
                        value = ShopLocale.formatMoney(reportData.netProfit, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (isBn) "ক্যাশ শেষে আছে" else "Cash Closing",
                    value = ShopLocale.formatMoney(reportData.cashClosingBalance, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (isBn) "মোট বাকি" else "Baki to Receive",
                    value = ShopLocale.formatMoney(reportData.bakiClosingReceivable, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (isBn) "মহাজনকে দেব" else "Supplier Payable",
                    value = ShopLocale.formatMoney(reportData.supplierClosingPayable, lang)
                )
            )
        }

        val checksNote = if (!includeSupplierDebt) {
            if (isBn) {
                "সারাংশ: নিট বিক্রয় ${ShopLocale.formatMoney(reportData.netSales, lang)} | ক্যাশ ${ShopLocale.formatMoney(reportData.cashClosingBalance, lang)} | পাব ${ShopLocale.formatMoney(reportData.bakiClosingReceivable, lang)}"
            } else {
                "Summary: Net Sales ${ShopLocale.formatMoney(reportData.netSales, lang)} | Cash ${ShopLocale.formatMoney(reportData.cashClosingBalance, lang)} | Baki ${ShopLocale.formatMoney(reportData.bakiClosingReceivable, lang)}"
            }
        } else {
            val allOk = reportData.cashCheck.matches && reportData.bakiCheck.matches && reportData.supplierCheck.matches
            if (allOk) {
                if (isBn) "শেষে আছে = শুরু + টাকা এসেছে - টাকা গেছে যাচাই: ক্যাশ, বাকি ও মহাজন হিসাব সম্পূর্ণ সঠিক।" else "Check (Closing = Opening + In - Out): Cash, Baki & Suppliers all match."
            } else {
                val mismatches = buildList {
                    if (!reportData.cashCheck.matches) add(if (isBn) "ক্যাশ (${ShopLocale.formatMoney(reportData.cashCheck.difference, lang)})" else "Cash (${ShopLocale.formatMoney(reportData.cashCheck.difference, lang)})")
                    if (!reportData.bakiCheck.matches) add(if (isBn) "বাকি (${ShopLocale.formatMoney(reportData.bakiCheck.difference, lang)})" else "Baki (${ShopLocale.formatMoney(reportData.bakiCheck.difference, lang)})")
                    if (!reportData.supplierCheck.matches) add(if (isBn) "মহাজন (${ShopLocale.formatMoney(reportData.supplierCheck.difference, lang)})" else "Suppliers (${ShopLocale.formatMoney(reportData.supplierCheck.difference, lang)})")
                }.joinToString(", ")
                if (isBn) "সতর্কতা — হিসাব অমিল: $mismatches" else "Check warning — mismatch in: $mismatches"
            }
        }

        return ReportTableData(
            title = title,
            storeName = store?.name?.ifBlank { ShopLocale.get("receipt_header_store_default", lang) } ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = currentMonthLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = setOf(3),
            rawNumbers = rawRows,
            totals = null,
            rawTotals = emptyList(),
            notes = checksNote,
            summaryMetrics = summaryMetrics
        )
    }

    fun buildSalesReportTable(
        sales: List<Sale>,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalAmount = sales.sumOf { it.grandTotal }
        val totalDiscount = sales.sumOf { it.discount }
        val totalProfit = sales.sumOf { it.profit }

        val rows = sales.map { s ->
            val dateStr = s.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (s.paymentType == "baki") ShopLocale.get("status_baki", lang) else ShopLocale.get("status_paid", lang)
            val custName = ShopLocale.displayCustomerName(s.customerName, lang)
            if (canSeeProfit) {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    ShopLocale.formatMoney(s.profit, lang),
                    status
                )
            } else {
                listOf(
                    s.invoiceNo,
                    dateStr,
                    custName,
                    ShopLocale.formatMoney(s.grandTotal, lang),
                    ShopLocale.formatMoney(s.discount, lang),
                    status
                )
            }
        }
        val rawRows = sales.map { s ->
            if (canSeeProfit) {
                listOf(null, null, null, s.grandTotal, s.discount, s.profit, null)
            } else {
                listOf(null, null, null, s.grandTotal, s.discount, null)
            }
        }
        val totalLabel = ShopLocale.format("total_orders_row", lang, sales.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ShopLocale.formatMoney(totalProfit, lang),
                ""
            )
        } else {
            listOf(
                totalLabel,
                "",
                "",
                ShopLocale.formatMoney(totalAmount, lang),
                ShopLocale.formatMoney(totalDiscount, lang),
                ""
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, null, totalAmount, totalDiscount, totalProfit, null)
        } else {
            listOf(null, null, null, totalAmount, totalDiscount, null)
        }

        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_status", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_invoice", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_customer", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_discount", lang),
                ShopLocale.get("col_status", lang)
            )
        }

        val paidTotal = sales.filter { it.paymentType != "baki" }.sumOf { it.grandTotal }
        val bakiTotal = sales.filter { it.paymentType == "baki" }.sumOf { it.grandTotal }
        val salesSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট বিক্রয়" else "Total Sales",
                    value = ShopLocale.formatMoney(totalAmount, lang),
                    detail = totalLabel
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "নগদ আদায়" else "Paid Sales",
                    value = ShopLocale.formatMoney(paidTotal, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বাকি বিক্রয়" else "Baki (Due)",
                    value = ShopLocale.formatMoney(bakiTotal, lang)
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "মোট লাভ" else "Total Profit",
                        value = ShopLocale.formatMoney(totalProfit, lang),
                        detail = if (lang == AppLanguage.BN) {
                            "ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        } else {
                            "Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
                        }
                    )
                )
            }
        }

        return ReportTableData(
            title = ShopLocale.get("rep_sales_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(3, 4, 5) else setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট অর্ডার: ${ShopLocale.formatNumber(sales.size, lang)}টি • নগদ: ${ShopLocale.formatMoney(paidTotal, lang)} • বাকি: ${ShopLocale.formatMoney(bakiTotal, lang)} • মোট ছাড়: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            } else {
                "Orders: ${sales.size} • Paid: ${ShopLocale.formatMoney(paidTotal, lang)} • Baki: ${ShopLocale.formatMoney(bakiTotal, lang)} • Total Discount: ${ShopLocale.formatMoney(totalDiscount, lang)}"
            },
            summaryMetrics = salesSummary
        )
    }

    fun buildPurchaseReportTable(
        purchases: List<Purchase>,
        returns: List<PurchaseReturn>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalPurchases = purchases.sumOf { it.totalAmount }
        val totalPaid = purchases.sumOf { it.paidAmount }
        val totalDue = purchases.sumOf { it.dueAmount }

        val rows = purchases.map { p ->
            val dateStr = p.createdAt?.toDate()?.time?.let { ShopLocale.formatDateShort(it, lang) } ?: "—"
            val status = if (p.paymentType == "credit") ShopLocale.get("status_credit_due", lang) else ShopLocale.get("status_paid", lang)
            listOf(
                p.purchaseNo,
                dateStr,
                p.supplierName,
                ShopLocale.formatMoney(p.totalAmount, lang),
                ShopLocale.formatMoney(p.paidAmount, lang),
                ShopLocale.formatMoney(p.dueAmount, lang),
                status
            )
        }
        val rawRows = purchases.map { p ->
            listOf(null, null, null, p.totalAmount, p.paidAmount, p.dueAmount, null)
        }
        val totals = listOf(
            ShopLocale.format("total_orders_row", lang, purchases.size),
            "",
            "",
            ShopLocale.formatMoney(totalPurchases, lang),
            ShopLocale.formatMoney(totalPaid, lang),
            ShopLocale.formatMoney(totalDue, lang),
            ""
        )
        val rawTotals = listOf<Double?>(null, null, null, totalPurchases, totalPaid, totalDue, null)
        val totalReturns = returns.sumOf { it.amount }
        val purchaseSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(totalPurchases, lang),
                detail = ShopLocale.format("total_orders_row", lang, purchases.size)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পরিশোধিত" else "Total Paid",
                value = ShopLocale.formatMoney(totalPaid, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "সরবরাহকারী বকেয়া" else "Total Due",
                value = ShopLocale.formatMoney(totalDue, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Purchase Returns",
                value = ShopLocale.formatMoney(totalReturns, lang),
                detail = if (lang == AppLanguage.BN) "${returns.size}টি ফেরত" else "${returns.size} returns"
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_purchase_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_order_no", lang),
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_supplier", lang),
                ShopLocale.get("col_total", lang),
                ShopLocale.get("col_paid", lang),
                ShopLocale.get("col_due", lang),
                ShopLocale.get("col_status", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ক্রয়: ${ShopLocale.formatMoney(totalPurchases, lang)} | পরিশোধিত: ${ShopLocale.formatMoney(totalPaid, lang)} | বকেয়া: ${ShopLocale.formatMoney(totalDue, lang)} | ফেরত: ${ShopLocale.formatMoney(totalReturns, lang)}"
            } else {
                "Total Purchases: ${ShopLocale.formatMoney(totalPurchases, lang)} | Paid: ${ShopLocale.formatMoney(totalPaid, lang)} | Due: ${ShopLocale.formatMoney(totalDue, lang)} | Returns: ${ShopLocale.formatMoney(totalReturns, lang)}"
            },
            summaryMetrics = purchaseSummary
        )
    }

    fun buildDayBookReportTable(
        entries: List<DayBookEntry>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIn = entries.filter { it.isIncome }.sumOf { it.amount }
        val totalOut = entries.filter { !it.isIncome }.sumOf { it.amount }

        val rows = entries.map { e ->
            val dateStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val direction = if (e.isIncome) ShopLocale.get("flow_in", lang) else ShopLocale.get("flow_out", lang)
            val typeStr = ShopLocale.displayEntryType(e.type, lang)
            val partyStr = ShopLocale.displayCustomerName(e.partyName, lang)
            listOf(
                dateStr,
                typeStr,
                e.referenceNo,
                partyStr,
                direction,
                ShopLocale.formatMoney(e.amount, lang)
            )
        }
        val rawRows = entries.map { e ->
            listOf(null, null, null, null, null, if (e.isIncome) e.amount else -e.amount)
        }
        val totals = listOf(
            ShopLocale.get("net_flow_row", lang),
            "",
            "",
            ShopLocale.format(
                "daybook_totals_note",
                lang,
                ShopLocale.formatMoney(totalIn, lang),
                ShopLocale.formatMoney(totalOut, lang)
            ),
            "",
            ShopLocale.formatMoney(totalIn - totalOut, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, null, totalIn - totalOut)
        val netFlow = totalIn - totalOut
        val dayBookSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "টাকা এসেছে" else "Total Cash In",
                value = ShopLocale.formatMoney(totalIn, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "টাকা গেছে" else "Total Cash Out",
                value = ShopLocale.formatMoney(totalOut, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "নিট প্রবাহ" else "Net Cash Flow",
                value = ShopLocale.formatMoney(netFlow, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট লেনদেন" else "Transactions",
                value = ShopLocale.formatNumber(entries.size, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.get("rep_day_book_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_datetime", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_party", lang),
                ShopLocale.get("col_flow", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = totals[3],
            summaryMetrics = dayBookSummary
        )
    }

    fun buildProfitLossReportTable(
        pnl: ProfitAndLossStatement,
        isStockBased: Boolean,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN,
        isFatherMode: Boolean = false
    ): ReportTableData {
        val effectiveStockBased = if (isFatherMode) true else isStockBased
        val cogsMethod = if (effectiveStockBased) ShopLocale.get("pnl_stock_based", lang) else ShopLocale.get("pnl_purchases_based", lang)
        val rows = mutableListOf(
            listOf(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
            listOf(
                ShopLocale.format("pnl_cogs_label", lang, cogsMethod),
                "-${ShopLocale.formatMoney(pnl.costOfGoodsSold, lang)}"
            )
        )
        val rawRows = mutableListOf(
            listOf<Double?>(null, pnl.netSales),
            listOf<Double?>(null, -pnl.costOfGoodsSold)
        )
        if (isFatherMode) {
            rows.add(listOf(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
        } else {
            rows.add(listOf(ShopLocale.get("pnl_gross_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang)))
            rows.add(listOf(ShopLocale.get("pnl_other_income", lang), "+${ShopLocale.formatMoney(pnl.otherIncome, lang)}"))
            rows.add(listOf(ShopLocale.get("pnl_operating_expenses", lang), "-${ShopLocale.formatMoney(pnl.expenses, lang)}"))
            rawRows.add(listOf<Double?>(null, pnl.grossProfit))
            rawRows.add(listOf<Double?>(null, pnl.otherIncome))
            rawRows.add(listOf<Double?>(null, -pnl.expenses))
            pnl.expensesByCategory.forEach { (cat, amt) ->
                rows.add(listOf("  • ${ShopLocale.displayCategory(cat, lang)}", "-${ShopLocale.formatMoney(amt, lang)}"))
                rawRows.add(listOf<Double?>(null, -amt))
            }
            rows.add(listOf(ShopLocale.get("pnl_net_profit", lang), ShopLocale.formatMoney(pnl.netProfit, lang)))
            rawRows.add(listOf<Double?>(null, pnl.netProfit))
        }

        val totalAmount = if (isFatherMode) pnl.grossProfit else pnl.netProfit
        val totalLabel = if (isFatherMode) ShopLocale.get("col_profit", lang).uppercase() else ShopLocale.get("pnl_net_profit", lang).uppercase()

        val grossMarginPct = if (pnl.netSales > 0.0) (pnl.grossProfit / pnl.netSales) * 100.0 else 0.0
        val netMarginPct = if (pnl.netSales > 0.0) (pnl.netProfit / pnl.netSales) * 100.0 else 0.0
        val pnlSummary = if (isFatherMode) {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(ShopLocale.get("col_profit", lang), ShopLocale.formatMoney(pnl.grossProfit, lang))
            )
        } else {
            listOf(
                ReportSummaryMetric(ShopLocale.get("pnl_net_sales", lang), ShopLocale.formatMoney(pnl.netSales, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_gross_profit", lang),
                    ShopLocale.formatMoney(pnl.grossProfit, lang),
                    ShopLocale.formatPercent(grossMarginPct, 1, lang)
                ),
                ReportSummaryMetric(ShopLocale.get("pnl_operating_expenses", lang), ShopLocale.formatMoney(pnl.expenses, lang)),
                ReportSummaryMetric(
                    ShopLocale.get("pnl_net_profit", lang),
                    ShopLocale.formatMoney(pnl.netProfit, lang),
                    ShopLocale.formatPercent(netMarginPct, 1, lang)
                )
            )
        }

        return ReportTableData(
            title = if (isFatherMode) ShopLocale.get("nav_business", lang) else ShopLocale.get("rep_pnl_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(ShopLocale.get("col_line_item", lang), ShopLocale.get("col_amount", lang)),
            rows = rows,
            numericColumns = setOf(1),
            rawNumbers = rawRows,
            totals = listOf(totalLabel, ShopLocale.formatMoney(totalAmount, lang)),
            rawTotals = listOf(null, totalAmount),
            notes = if (effectiveStockBased) ShopLocale.get("pnl_note_stock", lang) else ShopLocale.get("pnl_note_purchase", lang),
            summaryMetrics = pnlSummary
        )
    }

    fun buildExpenseReportTable(
        expenses: List<Expense>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalExpenses = expenses.sumOf { it.amount }
        val rows = expenses.map { exp ->
            listOf(
                ShopLocale.formatDateLongFullMonth(exp.date, lang),
                ShopLocale.displayCategory(exp.category, lang),
                ShopLocale.displayAccountName(exp.accountName, exp.accountId, lang),
                exp.note,
                ShopLocale.formatMoneyFixed2(exp.amount, lang)
            )
        }
        val rawNumbers = expenses.map { exp ->
            listOf<Double?>(null, null, null, null, exp.amount)
        }
        val totalFormatted = ShopLocale.formatMoneyFixed2(totalExpenses, lang)
        val totals = listOf(
            ShopLocale.get("total_expenses_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, expenses.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalExpenses)
        val byCat = expenses.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgExp = if (expenses.isNotEmpty()) totalExpenses / expenses.size else 0.0
        val expenseSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট খরচ" else "Total Expenses",
                    value = totalFormatted,
                    detail = ShopLocale.format("entries_count", lang, expenses.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalExpenses > 0.0) (topCat.second / totalExpenses) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "সর্বোচ্চ খাত" else "Top Category",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${ShopLocale.formatPercent(pct, 1, lang)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় খরচ" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgExp, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted | $topText"
        } else {
            "${ShopLocale.get("total_expenses_row", lang)}: $totalFormatted"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_expense_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = expenseSummary
        )
    }

    fun buildOtherIncomeReportTable(
        incomes: List<OtherIncome>,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val totalIncome = incomes.sumOf { it.amount }
        val rows = incomes.map { inc ->
            listOf(
                ShopLocale.formatDateLongFullMonth(inc.date, lang),
                ShopLocale.displayCategory(inc.category, lang),
                ShopLocale.displayAccountName(inc.accountName, inc.accountId, lang),
                inc.note,
                ShopLocale.formatMoneyFixed2(inc.amount, lang)
            )
        }
        val rawNumbers = incomes.map { inc ->
            listOf<Double?>(null, null, null, null, inc.amount)
        }
        val totalFormatted = "+${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        val totals = listOf(
            ShopLocale.get("total_other_income_row", lang),
            "",
            "",
            ShopLocale.format("entries_count", lang, incomes.size),
            totalFormatted
        )
        val rawTotals = listOf<Double?>(null, null, null, null, totalIncome)
        val byCat = incomes.groupBy { ShopLocale.displayCategory(it.category, lang) }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val topCat = byCat.firstOrNull()
        val avgInc = if (incomes.isNotEmpty()) totalIncome / incomes.size else 0.0
        val incomeSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট অন্যান্য আয়" else "Total Other Income",
                    value = ShopLocale.formatMoneyFixed2(totalIncome, lang),
                    detail = ShopLocale.format("entries_count", lang, incomes.size)
                )
            )
            if (topCat != null) {
                val pct = if (totalIncome > 0.0) (topCat.second / totalIncome) * 100.0 else 0.0
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "প্রধান উৎস" else "Top Source",
                        value = topCat.first,
                        detail = "${ShopLocale.formatMoneyFixed2(topCat.second, lang)} (${ShopLocale.formatPercent(pct, 1, lang)})"
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "গড় আয়" else "Average Entry",
                    value = ShopLocale.formatMoneyFixed2(avgInc, lang)
                )
            )
        }
        val catBreakdownNote = if (byCat.isNotEmpty()) {
            val topText = byCat.take(4).joinToString(" • ") { "${it.first}: ${ShopLocale.formatMoneyFixed2(it.second, lang)}" }
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)} | $topText"
        } else {
            "${ShopLocale.get("total_other_income_row", lang)}: ${ShopLocale.formatMoneyFixed2(totalIncome, lang)}"
        }

        return ReportTableData(
            title = ShopLocale.get("rep_other_income_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_category", lang),
                ShopLocale.get("col_account", lang),
                ShopLocale.get("col_note", lang),
                ShopLocale.get("col_amount", lang)
            ),
            rows = rows,
            numericColumns = setOf(4),
            rawNumbers = rawNumbers,
            totals = totals,
            rawTotals = rawTotals,
            notes = catBreakdownNote,
            summaryMetrics = incomeSummary
        )
    }

    fun buildInventoryReportTable(
        products: List<Product>,
        store: Store?,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val now = nowFormatted(lang)
        val totalItemsCount = products.sumOf { it.stock }
        val totalCostValue = products.sumOf { it.stock * it.buyPrice }
        val totalSaleValue = products.sumOf { it.stock * it.sellPrice }

        val rows = products.map { p ->
            val stockText = "${ShopLocale.formatNumber(p.stock, lang)} ${ShopLocale.displayUnit(p.unit, lang)}"
            if (canSeeProfit) {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.buyPrice, lang),
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.buyPrice, lang)
                )
            } else {
                listOf(
                    p.name,
                    stockText,
                    ShopLocale.formatMoney(p.sellPrice, lang),
                    ShopLocale.formatMoney(p.stock * p.sellPrice, lang)
                )
            }
        }
        val rawRows = products.map { p ->
            if (canSeeProfit) {
                listOf(null, p.stock.toDouble(), p.buyPrice, p.sellPrice, p.stock * p.buyPrice)
            } else {
                listOf(null, p.stock.toDouble(), p.sellPrice, p.stock * p.sellPrice)
            }
        }
        val totalProductsLabel = ShopLocale.format("total_products_row", lang, products.size)
        val totalItemsLabel = ShopLocale.format("items_count_label", lang, totalItemsCount)
        val totals = if (canSeeProfit) {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                "",
                ShopLocale.formatMoney(totalCostValue, lang)
            )
        } else {
            listOf(
                totalProductsLabel,
                totalItemsLabel,
                "",
                ShopLocale.formatMoney(totalSaleValue, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, totalItemsCount.toDouble(), null, null, totalCostValue)
        } else {
            listOf(null, totalItemsCount.toDouble(), null, totalSaleValue)
        }
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_buy_price", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_cost_value", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_product", lang),
                ShopLocale.get("col_stock", lang),
                ShopLocale.get("col_sell_price", lang),
                ShopLocale.get("col_sale_value", lang)
            )
        }
        val invSummary = buildList {
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট পণ্য" else "Total Products",
                    value = ShopLocale.formatNumber(products.size, lang),
                    detail = totalItemsLabel
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = if (lang == AppLanguage.BN) "ক্রয় মূল্য (মজুদ)" else "Total Cost Value",
                        value = ShopLocale.formatMoney(totalCostValue, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "বিক্রয় মূল্য (সম্ভাব্য)" else "Total Sale Value",
                    value = ShopLocale.formatMoney(totalSaleValue, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_inventory_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = ShopLocale.format("as_of_date", lang, now),
            generatedAt = now,
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(1, 2, 3, 4) else setOf(1, 2, 3),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (canSeeProfit) {
                if (lang == AppLanguage.BN) {
                    "সম্ভাব্য বিক্রয় মূল্য: ${ShopLocale.formatMoney(totalSaleValue, lang)} • সম্ভাব্য লাভ: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                } else {
                    "Potential Retail Value: ${ShopLocale.formatMoney(totalSaleValue, lang)} • Potential Gross Profit: ${ShopLocale.formatMoney(totalSaleValue - totalCostValue, lang)}"
                }
            } else null,
            summaryMetrics = invSummary
        )
    }

    fun buildCustomerStatementReportTable(
        rep: CustomerStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            val typeStr = if (isBakiSale) ShopLocale.get("entry_baki_sale", lang) else ShopLocale.get("entry_payment_in", lang)
            val signedAmt = if (isBakiSale) "+${ShopLocale.formatMoney(e.amount, lang)}" else "-${ShopLocale.formatMoney(e.amount, lang)}"
            listOf(
                dStr,
                typeStr,
                e.referenceNo,
                signedAmt,
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            val isBakiSale = e.type == LedgerEntryType.BAKI_SALE
            listOf<Double?>(null, null, null, if (isBakiSale) e.amount else -e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "cust_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalBakiSales, lang),
                ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val custSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট বাকি বিক্রয়" else "Baki Sales",
                value = ShopLocale.formatMoney(rep.totalBakiSales, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট আদায়" else "Payments Received",
                value = ShopLocale.formatMoney(rep.totalPayments, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "শেষে বাকি আছে" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_customer_stmt_title", lang, rep.customer.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = custSummary
        )
    }

    fun buildSupplierStatementReportTable(
        rep: SupplierStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { e ->
            val dStr = ShopLocale.formatDateShort(e.timestamp, lang)
            listOf(
                dStr,
                ShopLocale.displayEntryType(e.type, lang),
                e.referenceNo,
                ShopLocale.formatMoney(e.amount, lang),
                ShopLocale.formatMoney(e.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { e ->
            listOf<Double?>(null, null, null, e.amount, e.runningBalance)
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.format(
                "sup_stmt_totals_note",
                lang,
                ShopLocale.formatMoney(rep.totalPurchases, lang),
                ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ShopLocale.formatMoney(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, null, rep.closingBalance)
        val supSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoney(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "মোট ক্রয়" else "Total Purchases",
                value = ShopLocale.formatMoney(rep.totalPurchases, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "ক্রয় ফেরত" else "Total Returns",
                value = ShopLocale.formatMoney(rep.totalReturns, lang)
            ),
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "শেষে দেব" else "Closing Balance",
                value = ShopLocale.formatMoney(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_supplier_stmt_title", lang, rep.supplier.name),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_amount", lang),
                ShopLocale.get("col_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoney(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoney(rep.closingBalance, lang)}",
            summaryMetrics = supSummary
        )
    }

    fun buildAccountStatementReportTable(
        rep: AccountStatementReport,
        store: Store?,
        periodLabel: String,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val rows = rep.entries.map { entry ->
            listOf(
                ShopLocale.formatDateShort(entry.timestamp, lang),
                ShopLocale.displayEntryType(entry.type, lang),
                entry.referenceNo,
                if (entry.moneyIn > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyIn, lang) else "—",
                if (entry.moneyOut > 0.0) ShopLocale.formatMoneyFixed2(entry.moneyOut, lang) else "—",
                ShopLocale.formatMoneyFixed2(entry.runningBalance, lang)
            )
        }
        val rawRows = rep.entries.map { entry ->
            listOf(
                null,
                null,
                null,
                if (entry.moneyIn > 0.0) entry.moneyIn else null,
                if (entry.moneyOut > 0.0) entry.moneyOut else null,
                entry.runningBalance
            )
        }
        val totals = listOf(
            ShopLocale.get("closing_balance_row", lang),
            "",
            "",
            ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang),
            ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang),
            ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
        )
        val rawTotals = listOf<Double?>(null, null, null, rep.totalMoneyIn, rep.totalMoneyOut, rep.closingBalance)
        val accDisplay = ShopLocale.displayAccountName(rep.account.name, rep.account.id, lang)
        val acctSummary = listOf(
            ReportSummaryMetric(
                label = if (lang == AppLanguage.BN) "পূর্বের জের" else "Opening Balance",
                value = ShopLocale.formatMoneyFixed2(rep.openingBalance, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_in", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyIn, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("col_money_out", lang),
                value = ShopLocale.formatMoneyFixed2(rep.totalMoneyOut, lang)
            ),
            ReportSummaryMetric(
                label = ShopLocale.get("closing_balance_row", lang),
                value = ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)
            )
        )
        return ReportTableData(
            title = ShopLocale.format("rep_account_stmt_title", lang, accDisplay),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = listOf(
                ShopLocale.get("col_date", lang),
                ShopLocale.get("col_type", lang),
                ShopLocale.get("col_ref", lang),
                ShopLocale.get("col_money_in", lang),
                ShopLocale.get("col_money_out", lang),
                ShopLocale.get("col_running_balance", lang)
            ),
            rows = rows,
            numericColumns = setOf(3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = "${ShopLocale.format("opening_balance_note", lang, ShopLocale.formatMoneyFixed2(rep.openingBalance, lang))} • ${ShopLocale.get("closing_balance_row", lang)}: ${ShopLocale.formatMoneyFixed2(rep.closingBalance, lang)}",
            summaryMetrics = acctSummary
        )
    }

    fun buildStaffReportTable(
        staffSummary: StaffReportSummary,
        store: Store?,
        periodLabel: String,
        canSeeProfit: Boolean,
        lang: AppLanguage = AppLanguage.EN
    ): ReportTableData {
        val headers = if (canSeeProfit) {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_profit", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        } else {
            listOf(
                ShopLocale.get("col_staff_member", lang),
                ShopLocale.get("col_role", lang),
                ShopLocale.get("col_orders", lang),
                ShopLocale.get("col_qty_sold", lang),
                ShopLocale.get("col_sales_total", lang),
                ShopLocale.get("col_payments_in", lang)
            )
        }
        val rows = staffSummary.rows.map { row ->
            val roleDisplay = ShopLocale.displayStaffRole(row.roleLabel, lang)
            val displayMember = if (row.memberName == "Owner") ShopLocale.get("role_owner", lang) else row.memberName
            if (canSeeProfit) {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.profit, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            } else {
                listOf(
                    displayMember,
                    roleDisplay,
                    ShopLocale.formatNumber(row.salesCount, lang),
                    ShopLocale.formatNumber(row.itemsSold, lang),
                    ShopLocale.formatMoney(row.totalSales, lang),
                    ShopLocale.formatMoney(row.paymentsReceived, lang)
                )
            }
        }
        val rawRows: List<List<Double?>> = staffSummary.rows.map { row ->
            if (canSeeProfit) {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.profit, row.paymentsReceived)
            } else {
                listOf(null, null, row.salesCount.toDouble(), row.itemsSold.toDouble(), row.totalSales, row.paymentsReceived)
            }
        }
        val totalStaffLabel = ShopLocale.format("total_staff_row", lang, staffSummary.rows.size)
        val totals = if (canSeeProfit) {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalProfit, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        } else {
            listOf(
                totalStaffLabel,
                "",
                ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                ShopLocale.formatNumber(staffSummary.totalItemsSold, lang),
                ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
            )
        }
        val rawTotals = if (canSeeProfit) {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalProfit, staffSummary.totalPaymentsReceived)
        } else {
            listOf(null, null, staffSummary.totalSalesCount.toDouble(), staffSummary.totalItemsSold.toDouble(), staffSummary.totalSalesAmount, staffSummary.totalPaymentsReceived)
        }
        val staffSummaryMetrics = buildList {
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("sales_recorded_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalSalesAmount, lang),
                    detail = ShopLocale.format(
                        "staff_orders_items_val",
                        lang,
                        ShopLocale.formatNumber(staffSummary.totalSalesCount, lang),
                        ShopLocale.formatNumber(staffSummary.totalItemsSold, lang)
                    )
                )
            )
            if (canSeeProfit) {
                add(
                    ReportSummaryMetric(
                        label = ShopLocale.get("profit_generated_lbl", lang),
                        value = ShopLocale.formatMoney(staffSummary.totalProfit, lang)
                    )
                )
            }
            add(
                ReportSummaryMetric(
                    label = ShopLocale.get("cust_payments_received_lbl", lang),
                    value = ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)
                )
            )
            add(
                ReportSummaryMetric(
                    label = if (lang == AppLanguage.BN) "মোট স্টাফ" else "Team Members",
                    value = ShopLocale.formatNumber(staffSummary.rows.size, lang)
                )
            )
        }
        return ReportTableData(
            title = ShopLocale.get("rep_staff_title", lang),
            storeName = store?.name ?: ShopLocale.get("receipt_header_store_default", lang),
            storePhone = store?.phone ?: "",
            storeAddress = store?.address ?: "",
            periodLabel = periodLabel,
            generatedAt = nowFormatted(lang),
            headers = headers,
            rows = rows,
            numericColumns = if (canSeeProfit) setOf(2, 3, 4, 5, 6) else setOf(2, 3, 4, 5),
            rawNumbers = rawRows,
            totals = totals,
            rawTotals = rawTotals,
            notes = if (lang == AppLanguage.BN) {
                "মোট ${ShopLocale.formatNumber(staffSummary.rows.size, lang)} জন স্টাফ • মোট অর্ডার: ${ShopLocale.formatNumber(staffSummary.totalSalesCount, lang)} • ক্রেতার বাকি আদায়: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            } else {
                "Active Team: ${staffSummary.rows.size} staff • Total Orders: ${staffSummary.totalSalesCount} • Customer Payments Collected: ${ShopLocale.formatMoney(staffSummary.totalPaymentsReceived, lang)}"
            },
            summaryMetrics = staffSummaryMetrics
        )
    }
}
