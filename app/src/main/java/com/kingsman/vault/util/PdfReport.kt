package com.kingsman.vault.util

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.kingsman.vault.R
import com.kingsman.vault.data.Direction
import com.kingsman.vault.data.Kind
import java.io.File

/** Renders a ReportData to an A4 PDF using Android's built-in PdfDocument. */
object PdfReport {

    private const val W = 595
    private const val H = 842
    private const val M = 40f

    private const val INK = 0xFF1A1410.toInt()
    private const val MUTED = 0xFF6B5F53.toInt()
    private const val LINE = 0xFFE9E3DA.toInt()
    private const val AMBER = 0xFFF59E0B.toInt()
    private const val GREEN = 0xFF15803D.toInt()
    private const val RED = 0xFFB91C1C.toInt()

    private class Writer(val context: Context, val footer: String) {
        val doc = PdfDocument()
        private var pageNo = 0
        private var page: PdfDocument.Page? = null
        lateinit var c: Canvas
        var y = M

        val h1 = paint(20f, true, INK)
        val h2 = paint(13f, true, INK)
        val body = paint(10f, false, INK)
        val bold = paint(10f, true, INK)
        val muted = paint(9f, false, MUTED)
        val line = Paint().apply { color = LINE; strokeWidth = 0.8f }

        fun paint(size: Float, isBold: Boolean, color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        }

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            page = p
            c = p.canvas
            y = M
            c.drawText("$footer  ·  Page $pageNo", M, H - 20f, muted)
        }

        fun ensure(height: Float) {
            if (y + height > H - M) newPage()
        }

        fun text(s: String, x: Float, p: Paint, right: Boolean = false) {
            p.textAlign = if (right) Paint.Align.RIGHT else Paint.Align.LEFT
            c.drawText(s, x, y, p)
            p.textAlign = Paint.Align.LEFT
        }

        fun fit(s: String, maxWidth: Float, p: Paint): String {
            if (p.measureText(s) <= maxWidth) return s
            var t = s
            while (t.isNotEmpty() && p.measureText("$t…") > maxWidth) t = t.dropLast(1)
            return "$t…"
        }

        fun section(title: String) {
            ensure(44f)
            y += 18f
            text(title, M, h2)
            y += 8f
            c.drawLine(M, y, W - M, y, line)
            y += 14f
        }

        fun row(left: String, right: String, rightPaint: Paint = bold) {
            ensure(16f)
            text(fit(left, 330f, body), M, body)
            text(right, W - M, rightPaint, right = true)
            y += 16f
        }

        fun finish(file: File) {
            page?.let { doc.finishPage(it) }
            file.outputStream().use { doc.writeTo(it) }
            doc.close()
        }
    }

    fun write(context: Context, r: ReportData): File {
        val cur = r.currency
        fun m(v: Long) = Money.format(v, cur)
        val w = Writer(context, "Vault · ${r.scopeName} · ${r.periodLabel}")
        w.newPage()

        // Header with logo
        runCatching {
            val logo = BitmapFactory.decodeResource(context.resources, R.drawable.vault_logo)
            w.c.drawBitmap(logo, null, RectF(M, M - 8f, M + 40f, M + 32f), null)
        }
        w.y = M + 12f
        w.text("Vault Report", M + 52f, w.h1)
        w.y += 16f
        w.text("${r.scopeName} · ${r.periodLabel} · ${Money.name(cur)} ($cur)", M + 52f, w.muted)
        w.y += 12f
        w.text("${Dates.fmt(r.from)} – ${Dates.fmt(r.to - 1)}", M + 52f, w.muted)
        w.y += 12f

        // Summary boxes
        w.section("Summary")
        val boxW = (W - 2 * M - 24f) / 4
        val labels = listOf("Income", "Expenses", "Net", "Kept")
        val values = listOf(m(r.totals.income), m(r.totals.expense), m(r.totals.net), "${r.savingsRate}%")
        val colors = listOf(GREEN, RED, INK, AMBER)
        w.ensure(52f)
        for (i in 0..3) {
            val x = M + i * (boxW + 8f)
            val box = RectF(x, w.y - 10f, x + boxW, w.y + 36f)
            w.c.drawRoundRect(box, 8f, 8f, Paint().apply { color = 0xFFF7F5F2.toInt() })
            w.c.drawText(labels[i], x + 8f, w.y + 4f, w.muted)
            val vp = w.paint(11f, true, colors[i])
            w.c.drawText(w.fit(values[i], boxW - 12f, vp), x + 8f, w.y + 24f, vp)
        }
        w.y += 50f

        fun categoryBlock(title: String, list: List<Pair<String, Long>>, total: Long, color: Int) {
            if (list.isEmpty()) return
            w.section(title)
            val barPaint = Paint().apply { this.color = color }
            val track = Paint().apply { this.color = 0xFFF1ECE4.toInt() }
            list.forEach { (name, amt) ->
                w.ensure(22f)
                val pct = if (total > 0) (amt * 100 / total) else 0L
                w.text(w.fit(name, 180f, w.body), M, w.body)
                val bx = M + 190f
                val bw = 200f
                w.c.drawRoundRect(RectF(bx, w.y - 7f, bx + bw, w.y - 1f), 3f, 3f, track)
                w.c.drawRoundRect(RectF(bx, w.y - 7f, bx + bw * pct / 100f, w.y - 1f), 3f, 3f, barPaint)
                w.text("$pct%", bx + bw + 30f, w.muted, right = true)
                w.text(m(amt), W - M, w.bold, right = true)
                w.y += 18f
            }
        }
        categoryBlock("Expenses by category", r.expenseByCat, r.totals.expense, RED)
        categoryBlock("Income by category", r.incomeByCat, r.totals.income, GREEN)

        if (r.monthly.isNotEmpty()) {
            w.section("Month by month")
            w.ensure(16f)
            w.text("Month", M, w.muted)
            w.text("In", M + 260f, w.muted, right = true)
            w.text("Out", M + 380f, w.muted, right = true)
            w.text("Net", W - M, w.muted, right = true)
            w.y += 16f
            r.monthly.forEach { (ym, inc, exp) ->
                w.ensure(16f)
                w.text(Dates.fmtMonth(ym), M, w.body)
                w.text(m(inc), M + 260f, w.body, right = true)
                w.text(m(exp), M + 380f, w.body, right = true)
                w.text(m(inc - exp), W - M, w.bold, right = true)
                w.y += 16f
            }
        }

        if (r.goals.isNotEmpty()) {
            w.section("Savings goals  ·  total saved ${m(r.savedTotal)}")
            r.goals.forEach {
                val pct = if (it.target > 0) it.saved * 100 / it.target else 0L
                w.row(
                    "${it.name}  (${pct}% of ${m(it.target)}; this period ${Money.format(it.inPeriod, cur, signed = true)})",
                    m(it.saved)
                )
            }
        }

        if (r.investments.isNotEmpty()) {
            w.section("Investments  ·  value ${m(r.investmentValue)}")
            r.investments.forEach {
                val gain = it.current - it.invested
                w.row(
                    "${it.name} · ${it.type}  (put in ${m(it.invested)}, gain ${Money.format(gain, cur, signed = true)})",
                    m(it.current)
                )
            }
        }

        if (r.debts.isNotEmpty()) {
            w.section("Debts  ·  owed to me ${m(r.owedToMe)}  ·  I owe ${m(r.iOwe)}")
            r.debts.forEach {
                val dir = if (it.direction == Direction.OWED_TO_ME) "owes me" else "I owe"
                val due = it.due?.let { d -> " · due ${Dates.fmt(d)}" } ?: ""
                w.row("${it.person} — $dir$due", m(it.remaining), w.paint(10f, true, if (it.direction == Direction.OWED_TO_ME) GREEN else RED))
            }
        }

        if (r.owedByBusinesses > 0) {
            w.section("Businesses owe me (paid personally, not yet reimbursed)")
            w.row("Outstanding", m(r.owedByBusinesses))
        }

        if (r.txns.isNotEmpty()) {
            w.section("Transactions (${r.txns.size})")
            r.txns.forEach { t ->
                w.ensure(16f)
                w.text(Dates.fmtShort(t.date), M, w.muted)
                val desc = buildString {
                    append(t.categoryName)
                    if (t.note.isNotBlank()) append(" — ").append(t.note)
                    if (t.tag.isNotBlank()) append("  #").append(t.tag)
                }
                w.text(w.fit(desc, 330f, w.body), M + 50f, w.body)
                val p = w.paint(10f, true, if (t.kind == Kind.INCOME) GREEN else INK)
                val sign = if (t.kind == Kind.INCOME) "+" else "−"
                w.text(sign + m(t.amount), W - M, p, right = true)
                w.y += 16f
            }
        }

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.filter { it.name.endsWith(".pdf") }?.forEach { it.delete() }
        val safeScope = r.scopeName.replace(Regex("[^A-Za-z0-9]+"), "-")
        val file = File(dir, "Vault-Report-$safeScope-${Dates.stamp()}.pdf")
        w.finish(file)
        return file
    }
}
