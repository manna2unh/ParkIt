package com.unh.parkit_android26_myles_syed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unh.parkit_android26_myles_syed.ui.parking.ParkingRepository

private val SpotBg = Color(0xFFF1F6FA)
private val VacantGreen = Color(0xFF2E7D5B)
private val TakenRed = Color(0xFFC0392B)
private val SpotGray = Color(0xFF6B7C8C)

@Composable
fun SpotPickerScreen(lot: Lot, onBack: () -> Unit, onSpotChosen: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().background(SpotBg).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black,
                modifier = Modifier.size(28.dp).clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Choose a spot", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                Text(
                    "${lot.name} · ${ParkingRepository.openCount(lot)} open",
                    fontSize = 14.sp, color = SpotGray
                )
            }
        }

        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(VacantGreen, "Vacant")
            LegendDot(TakenRed, "Taken")
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(lot.spots) { i ->
                val spot = i + 1
                val taken = ParkingRepository.isTaken(lot, spot)
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (taken) TakenRed else VacantGreen)
                        .clickable(enabled = !taken) { onSpotChosen(spot) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        ParkingRepository.spotLabel(spot),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, color = SpotGray)
    }
}