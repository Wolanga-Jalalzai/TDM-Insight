package com.tdminsight.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tdminsight.app.engine.TARGETS
import com.tdminsight.app.ui.AppViewModel
import com.tdminsight.app.ui.components.ConcentrationTimeChart
import com.tdminsight.app.ui.components.ExplanationStepper
import com.tdminsight.app.ui.components.ParamGrid
import com.tdminsight.app.ui.components.RangeComparisonBar
import com.tdminsight.app.ui.components.RecommendationCard
import com.tdminsight.app.ui.theme.Slate500
import com.tdminsight.app.ui.theme.Slate900
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val entry by viewModel.lastResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Results") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val e = entry
        if (e == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No result to show.", color = Slate500)
            }
            return@Scaffold
        }
        val result = e.result

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(result.workflow.label, style = MaterialTheme.typography.headlineMedium, color = Slate900)
            Spacer(Modifier.height(16.dp))

            Text("Key results", fontWeight = FontWeight.SemiBold, color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            ParamGrid(result.pk, highlightKeys = setOf("cmin", "auc24"))

            Spacer(Modifier.height(20.dp))
            Text("Concentration-time profile", fontWeight = FontWeight.SemiBold, color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            ConcentrationTimeChart(result)

            Spacer(Modifier.height(20.dp))
            Text("Target comparison", fontWeight = FontWeight.SemiBold, color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(10.dp))
            val cminVal = result.pk["cmin"]?.value
            val aucVal = result.pk["auc24"]?.value
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (cminVal != null && !cminVal.isNaN()) {
                    RangeComparisonBar(
                        label = "Trough (Cmin)",
                        value = cminVal,
                        unit = "mg/L",
                        rangeMin = TARGETS.troughMin,
                        rangeMax = TARGETS.troughMax,
                        axisMax = max(cminVal, TARGETS.troughMax) * 1.4,
                    )
                }
                if (aucVal != null && !aucVal.isNaN()) {
                    RangeComparisonBar(
                        label = "AUC24",
                        value = aucVal,
                        unit = "mg\u00b7h/L",
                        rangeMin = TARGETS.aucMin,
                        rangeMax = TARGETS.aucMax,
                        axisMax = max(aucVal, TARGETS.aucMax) * 1.3,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Pharmacokinetic parameters", fontWeight = FontWeight.SemiBold, color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            ParamGrid(result.intermediate)

            Spacer(Modifier.height(20.dp))
            RecommendationCard(note = result.recommendation.note, targetRange = result.recommendation.target)

            Spacer(Modifier.height(24.dp))
            Text("Calculation explanation", fontWeight = FontWeight.SemiBold, color = Slate500, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "Input Values → Intermediate Values → Pharmacokinetic Parameters → Final Result",
                color = Slate500,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(Modifier.height(10.dp))
            ExplanationStepper(result.explanation)

            Spacer(Modifier.height(50.dp))
        }
    }
}
