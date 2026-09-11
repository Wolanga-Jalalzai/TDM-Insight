package com.tdminsight.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.engine.TARGETS
import com.tdminsight.app.engine.TdmResult
import com.tdminsight.app.ui.theme.*
import kotlin.math.exp
import kotlin.math.max

/**
 * Builds a projected vancomycin concentration-vs-time curve across one steady-state
 * dosing interval, using the same one-compartment model as [com.tdminsight.app.engine.TdmEngine]:
 * a rising phase during the infusion (0..tInf) and first-order exponential decay
 * afterwards (tInf..tau). The curve is anchored to the exact peak/trough values shown
 * in the results (Cmax at end of infusion, Cmin at end of the interval).
 */
private fun buildCurvePoints(
    ke: Double,
    peakEnd: Double,
    trough: Double,
    tInf: Double,
    tau: Double,
): List<Pair<Double, Double>> {
    val points = mutableListOf<Pair<Double, Double>>()
    val riseSteps = 24
    val decaySteps = 40

    if (tInf > 0) {
        val denom = 1 - exp(-ke * tInf)
        for (s in 0..riseSteps) {
            val t = tInf * s / riseSteps
            val frac = if (denom > 1e-9) (1 - exp(-ke * t)) / denom else t / tInf
            points += t to (trough + (peakEnd - trough) * frac)
        }
    } else {
        points += 0.0 to trough
    }

    val decayDuration = tau - tInf
    if (decayDuration > 0) {
        for (s in 1..decaySteps) {
            val dt = decayDuration * s / decaySteps
            points += (tInf + dt) to (peakEnd * exp(-ke * dt))
        }
    }
    return points
}

@Composable
fun ConcentrationTimeChart(result: TdmResult, modifier: Modifier = Modifier) {
    val ke = result.intermediate["ke"]?.value
    val peakEnd = result.intermediate["cmaxTrue"]?.value ?: result.pk["cmax"]?.value
    val trough = result.pk["cmin"]?.value
    val tau = result.inputs.tau
    val tInf = result.inputs.tInf

    if (ke == null || peakEnd == null || trough == null ||
        ke.isNaN() || peakEnd.isNaN() || trough.isNaN() || tau <= 0
    ) {
        return
    }

    val points = remember(ke, peakEnd, trough, tInf, tau) {
        buildCurvePoints(ke, peakEnd, trough, tInf, tau)
    }
    val yMax = remember(peakEnd) { max(peakEnd, TARGETS.troughMax) * 1.2 }

    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("%.1f mg/L".format(peakEnd), color = Slate500, style = MaterialTheme.typography.labelSmall)
            Text("peak (end of infusion)", color = Slate500, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            val w = size.width
            val h = size.height

            fun xOf(t: Double) = (t / tau * w).toFloat()
            fun yOf(c: Double) = (h - (c / yMax * h)).toFloat().coerceIn(0f, h)

            // Target trough band (shaded)
            val bandTop = yOf(TARGETS.troughMax)
            val bandBottom = yOf(TARGETS.troughMin)
            drawRect(
                color = Emerald50,
                topLeft = Offset(0f, bandTop),
                size = androidx.compose.ui.geometry.Size(w, bandBottom - bandTop)
            )

            // Dashed lines at target min/max
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            drawLine(
                color = Emerald600.copy(alpha = 0.6f),
                start = Offset(0f, bandBottom),
                end = Offset(w, bandBottom),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )
            drawLine(
                color = Emerald600.copy(alpha = 0.6f),
                start = Offset(0f, bandTop),
                end = Offset(w, bandTop),
                strokeWidth = 1.5f,
                pathEffect = dashEffect
            )

            // Concentration curve
            for (idx in 0 until points.size - 1) {
                val (t1, c1) = points[idx]
                val (t2, c2) = points[idx + 1]
                drawLine(
                    color = Slate900,
                    start = Offset(xOf(t1), yOf(c1)),
                    end = Offset(xOf(t2), yOf(c2)),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            // Infusion-end marker (peak)
            drawCircle(color = Slate900, radius = 6f, center = Offset(xOf(tInf), yOf(peakEnd)))
            // Trough markers (start and end of interval)
            drawCircle(color = Emerald600, radius = 6f, center = Offset(xOf(0.0), yOf(trough)))
            drawCircle(color = Emerald600, radius = 6f, center = Offset(xOf(tau), yOf(trough)))

            // Baseline axis
            drawLine(
                color = Slate200,
                start = Offset(0f, h),
                end = Offset(w, h),
                strokeWidth = Stroke.HairlineWidth
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("0 h", color = Slate500, style = MaterialTheme.typography.labelSmall)
            Text("dose interval (\u03c4 = ${tau.toInt()} h)", color = Slate500, style = MaterialTheme.typography.labelSmall)
            Text("${tau.toInt()} h", color = Slate500, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(Slate900)
            Spacer(Modifier.width(6.dp))
            Text("Predicted concentration", color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(14.dp))
            Box(
                Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Emerald50)
            )
            Spacer(Modifier.width(6.dp))
            Text("Target trough range", color = Slate500, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

/**
 * Horizontal bar showing where [value] falls relative to the target range
 * [rangeMin]..[rangeMax], scaled against [axisMax].
 */
@Composable
fun RangeComparisonBar(
    label: String,
    value: Double,
    unit: String,
    rangeMin: Double,
    rangeMax: Double,
    axisMax: Double,
    modifier: Modifier = Modifier,
) {
    val inRange = value in rangeMin..rangeMax
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontWeight = FontWeight.SemiBold, color = Slate900, style = MaterialTheme.typography.bodyMedium)
            Text(
                "%.1f %s".format(value, unit),
                color = if (inRange) Emerald600 else Red600,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(6.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Slate100)
        ) {
            val trackWidth = maxWidth
            val zoneStartFrac = (rangeMin / axisMax).coerceIn(0.0, 1.0)
            val zoneEndFrac = (rangeMax / axisMax).coerceIn(0.0, 1.0)
            val zoneWidth = (trackWidth * (zoneEndFrac - zoneStartFrac).toFloat())
                .coerceAtLeast(0.dp)

            Box(
                Modifier
                    .offset(x = trackWidth * zoneStartFrac.toFloat())
                    .width(zoneWidth)
                    .fillMaxHeight()
                    .background(Emerald50)
            )

            val valueFrac = (value / axisMax).coerceIn(0.0, 1.0)
            val markerOffset = (trackWidth * valueFrac.toFloat() - 2.dp)
                .coerceAtLeast(0.dp)
            Box(
                Modifier
                    .offset(x = markerOffset)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (inRange) Emerald600 else Red600)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Target: ${rangeMin.toInt()}\u2013${rangeMax.toInt()} $unit",
            color = Slate500,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
