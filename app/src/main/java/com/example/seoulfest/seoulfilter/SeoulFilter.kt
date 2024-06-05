package com.example.seoulfest.seoulfilter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeoulFilter(navController: NavHostController, initialSelectedDistricts: List<String>) {
    var selectedDistricts by remember { mutableStateOf(initialSelectedDistricts.toMutableList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with "서울특별시" text and X button
        TopAppBar(
            title = { Text("서울특별시") },
            actions = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )

        // Grid of district names
        val districts = listOf(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구",
            "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구",
            "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구",
            "중구", "중랑구"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(districts) { district ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(8.dp)
                        .width(80.dp) // 고정된 너비 설정
                        .height(40.dp) // 고정된 높이 설정
                        .clickable {
                            selectedDistricts = if (selectedDistricts.contains(district)) {
                                selectedDistricts
                                    .toMutableList()
                                    .apply { remove(district) }
                            } else {
                                selectedDistricts
                                    .toMutableList()
                                    .apply { add(district) }
                            }
                        }
                        .background(if (selectedDistricts.contains(district)) Color.Gray else Color.Transparent)
                        .padding(8.dp)
                ) {
                    Text(
                        text = district,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp // 텍스트 크기 조정
                    )
                }
            }
        }

        // Confirm button at the bottom
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = {
                val selectedDistrictsStr = selectedDistricts.joinToString(",")
                navController.navigate("main?selectedDistricts=$selectedDistrictsStr") {
                    popUpTo("main") { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("확인")
        }
    }
}
