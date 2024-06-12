package com.example.seoulfest.main_screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.seoulfest.models.CulturalEvent
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ContentColumn(
    innerPadding: PaddingValues,
    selectedDistricts: List<String>,
    selectedStartDate: String,
    selectedEndDate: String,
    navController: NavHostController,
    events: List<CulturalEvent>
) {
    val displayFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val startDateDisplay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedStartDate)
        ?.let { displayFormat.format(it) }
    val endDateDisplay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedEndDate)
        ?.let { displayFormat.format(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
    ) {
        SelectedDistrictsRow(selectedDistricts, navController)
        Text("날짜: $startDateDisplay ~ $endDateDisplay", style = MaterialTheme.typography.titleSmall)
        EventList(events, navController)
    }
}
