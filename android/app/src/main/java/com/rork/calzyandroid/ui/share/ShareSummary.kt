package com.rork.calzyandroid.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.rork.calzyandroid.R
import com.rork.calzyandroid.data.AppData
import com.rork.calzyandroid.data.Dates
import com.rork.calzyandroid.data.NutritionTargets
import com.rork.calzyandroid.data.caloriesBurned
import com.rork.calzyandroid.data.caloriesEaten
import com.rork.calzyandroid.data.carbsOn
import com.rork.calzyandroid.data.fatOn
import com.rork.calzyandroid.data.mealsOn
import com.rork.calzyandroid.data.proteinOn
import com.rork.calzyandroid.data.streak
import com.rork.calzyandroid.data.waterOn
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders the day as a shareable summary card bitmap and fires a share intent.
 * Mirrors `web/src/lib/summaryCard.ts`, including the app icon in the footer.
 */
object ShareSummary {

    private const val W = 720
    private const val H = 900
    private const val PAD = 56f

    private val ink = Color.parseColor("#0B0B0C")
    private val inkSoft = Color.parseColor("#6B6B72")
    private val inkFaint = Color.parseColor("#A8A8AF")
    private val flame = Color.parseColor("#FF6B2C")
    private val water = Color.parseColor("#399FFF")
    private val protein = Color.parseColor("#FF5A6D")
    private val carbs = Color.parseColor("#4C8EFF")
    private val fat = Color.parseColor("#F5A524")
    private val mint = Color.parseColor("#2FBF70")

    fun share(context: Context, data: AppData, date: LocalDate, targets: NutritionTargets) {
        val bitmap = render(context, data, date, targets)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "modernbody-summary.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share your day"))
    }

    private fun render(
        context: Context,
        data: AppData,
        date: LocalDate,
        targets: NutritionTargets,
    ): Bitmap {
        val eaten = data.caloriesEaten(date)
        val burned = data.caloriesBurned(date)
        val budget = targets.calories + burned
        val meals = data.mealsOn(date).size
        val waterMl = data.waterOn(date)
        val streak = data.streak()

        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val nunito = ResourcesCompat.getFont(context, R.font.nunito_extrabold)
            ?: Typeface.DEFAULT_BOLD
        val body = Typeface.create("sans-serif", Typeface.NORMAL)
        val bodyBold = Typeface.create("sans-serif-medium", Typeface.BOLD)

        // Background: off-white with pastel mists.
        canvas.drawColor(Color.parseColor("#F6F5F8"))
        fun mist(cx: Float, cy: Float, radius: Float, color: String, alpha: Int) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(
                        Color.argb(alpha, Color.red(Color.parseColor(color)), Color.green(Color.parseColor(color)), Color.blue(Color.parseColor(color))),
                        Color.TRANSPARENT,
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawCircle(cx, cy, radius, paint)
        }
        mist(30f, -20f, 520f, "#E6DCF7", 235)
        mist(W - 20f, 60f, 470f, "#FFE2E0", 220)
        mist(-90f, H * 0.5f, 450f, "#DCE9FF", 190)
        mist(W + 60f, H * 0.66f, 450f, "#FFEADC", 165)
        mist(W * 0.4f, H + 40f, 500f, "#E6DCF7", 175)

        val text = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        text.typeface = nunito
        text.textSize = 46f
        text.color = ink
        canvas.drawText("ModernBody", PAD, 118f, text)

        text.typeface = body
        text.textSize = 26f
        text.color = inkSoft
        canvas.drawText(Dates.abbreviatedDate(date), PAD, 160f, text)

        if (streak > 0) {
            val chip = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(31, Color.red(flame), Color.green(flame), Color.blue(flame))
            }
            val chipRect = RectF(W - PAD - 160f, 86f, W - PAD, 138f)
            canvas.drawRoundRect(chipRect, 26f, 26f, chip)
            text.typeface = nunito
            text.textSize = 28f
            text.color = flame
            text.textAlign = Paint.Align.CENTER
            canvas.drawText("\uD83D\uDD25 $streak", chipRect.centerX(), 122f, text)
            text.textAlign = Paint.Align.LEFT
        }

        // Calorie ring
        val ringCx = W / 2f
        val ringCy = 360f
        val ringR = 130f
        val ringStroke = 26f
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = ringStroke
            strokeCap = Paint.Cap.ROUND
            color = Color.argb(20, 0, 0, 0)
        }
        canvas.drawCircle(ringCx, ringCy, ringR, ring)
        val progress = if (budget > 0) min(1f, eaten.toFloat() / budget) else 0f
        ring.color = ink
        canvas.drawArc(
            RectF(ringCx - ringR, ringCy - ringR, ringCx + ringR, ringCy + ringR),
            -90f,
            360f * progress,
            false,
            ring,
        )

        text.textAlign = Paint.Align.CENTER
        text.typeface = nunito
        text.textSize = 84f
        text.color = ink
        canvas.drawText("$eaten", ringCx, ringCy + 12f, text)
        text.typeface = body
        text.textSize = 26f
        text.color = inkFaint
        canvas.drawText("of $budget kcal", ringCx, ringCy + 58f, text)

        // Macro bars
        var y = 570f
        data class Macro(val name: String, val value: Double, val goal: Int, val tint: Int)
        listOf(
            Macro("Protein", data.proteinOn(date), targets.protein, protein),
            Macro("Carbs", data.carbsOn(date), targets.carbs, carbs),
            Macro("Fat", data.fatOn(date), targets.fat, fat),
        ).forEach { macro ->
            text.textAlign = Paint.Align.LEFT
            text.typeface = bodyBold
            text.textSize = 26f
            text.color = inkSoft
            canvas.drawText(macro.name, PAD, y, text)

            text.textAlign = Paint.Align.RIGHT
            text.typeface = nunito
            text.color = ink
            canvas.drawText(
                "${macro.value.roundToInt()} / ${macro.goal} g",
                W - PAD,
                y,
                text,
            )

            val barTop = y + 16f
            val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(
                    36,
                    Color.red(macro.tint),
                    Color.green(macro.tint),
                    Color.blue(macro.tint),
                )
            }
            canvas.drawRoundRect(RectF(PAD, barTop, W - PAD, barTop + 16f), 8f, 8f, track)
            val fillRatio = if (macro.goal > 0) {
                min(1f, (macro.value / macro.goal).toFloat())
            } else {
                0f
            }
            if (fillRatio > 0f) {
                track.color = macro.tint
                canvas.drawRoundRect(
                    RectF(PAD, barTop, PAD + (W - 2 * PAD) * fillRatio, barTop + 16f),
                    8f,
                    8f,
                    track,
                )
            }
            y += 88f
        }

        // Stats row
        y += 8f
        val statW = (W - 2 * PAD - 2 * 16f) / 3f
        data class Stat(val label: String, val value: String, val tint: Int)
        listOf(
            Stat("Meals", "$meals", mint),
            Stat("Water", "${waterMl}ml", water),
            Stat("Burned", "$burned", flame),
        ).forEachIndexed { index, stat ->
            val left = PAD + index * (statW + 16f)
            val rect = RectF(left, y, left + statW, y + 108f)
            val card = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 255, 255, 255)
            }
            canvas.drawRoundRect(rect, 28f, 28f, card)
            text.textAlign = Paint.Align.CENTER
            text.typeface = nunito
            text.textSize = 34f
            text.color = stat.tint
            canvas.drawText(stat.value, rect.centerX(), y + 52f, text)
            text.typeface = body
            text.textSize = 22f
            text.color = inkSoft
            canvas.drawText(stat.label, rect.centerX(), y + 88f, text)
        }

        // Footer: app icon + wordmark
        val footerY = H - 72f
        val iconSize = 56f
        val appIcon = runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
        }.getOrNull()
        if (appIcon != null) {
            val iconBitmap = Bitmap.createBitmap(
                iconSize.toInt(),
                iconSize.toInt(),
                Bitmap.Config.ARGB_8888,
            )
            val iconCanvas = Canvas(iconBitmap)
            appIcon.setBounds(0, 0, iconSize.toInt(), iconSize.toInt())
            appIcon.draw(iconCanvas)

            val rounded = Bitmap.createBitmap(
                iconSize.toInt(),
                iconSize.toInt(),
                Bitmap.Config.ARGB_8888,
            )
            val roundedCanvas = Canvas(rounded)
            val clip = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, iconSize, iconSize),
                    16f,
                    16f,
                    Path.Direction.CW,
                )
            }
            roundedCanvas.clipPath(clip)
            roundedCanvas.drawBitmap(iconBitmap, 0f, 0f, null)
            canvas.drawBitmap(rounded, PAD, footerY - iconSize + 16f, null)
        }

        text.textAlign = Paint.Align.LEFT
        text.typeface = bodyBold
        text.textSize = 26f
        text.color = inkSoft
        canvas.drawText("Tracked with ModernBody", PAD + iconSize + 18f, footerY - 4f, text)

        // Gradient underline accent
        val underline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                PAD, 0f, W - PAD, 0f,
                intArrayOf(flame, protein),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(RectF(PAD, H - 36f, W - PAD, H - 28f), 4f, 4f, underline)

        return bitmap
    }
}
