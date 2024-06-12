package com.example.seoulfest.main_screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.seoulfest.R

@Composable
fun TopBar(
    navController: NavHostController,
    selectedDistricts: List<String>,
    selectedStartDate: String,
    selectedEndDate: String,
    notificationsEnabled: Boolean,
    upcomingEventCount: Int
) {
    var showDateRangePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.colorPrimary))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = {
                val selectedDistrictsStr = selectedDistricts.joinToString(",")
                navController.navigate("seoul?selectedDistricts=$selectedDistrictsStr")
            }) {
                Text("지역", color = colorResource(id = R.color.colorTextPrimary))
            }
            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                Log.d("TopBar", "날짜 선택 버튼 클릭")
                showDateRangePicker = true
            }) {
                Text("날짜 선택", color = colorResource(id = R.color.colorTextPrimary))
            }

            Spacer(modifier = Modifier.weight(1f)) // 애니메이션과 버튼 사이에 충분한 공간 확보

            LaunchLottieAnimation()

            if (notificationsEnabled) {
                NotificationIcon(navController, upcomingEventCount)
            }
        }
    }

    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDismissRequest = {
                Log.d("TopBar", "DateRangePickerDialog dismissed")
                showDateRangePicker = false
            },
            onDateRangeSelected = { startDate, endDate ->
                Log.d("TopBar", "Selected date range: $startDate to $endDate")
                navController.navigate("main?selectedDistricts=${selectedDistricts.joinToString(",")}&selectedStartDate=$startDate&selectedEndDate=$endDate") {
                    popUpTo("main") { inclusive = true }
                }
                showDateRangePicker = false
            }
        )
    }
}
