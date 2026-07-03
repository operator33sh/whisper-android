package com.whisper.android.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.whisper.android.ui.theme.interFamily
import com.whisper.android.ui.theme.playfairDisplayFamily
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whisper.android.WhisperApplication
import com.whisper.android.ui.onboarding.OnboardingScreen
import com.whisper.android.ui.privatefeed.PrivateFeedScreen
import com.whisper.android.ui.publicfeed.PublicFeedScreen

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_FEEDS = "feeds"

@Composable
fun WhisperNavGraph() {
    val app = LocalContext.current.applicationContext as WhisperApplication
    val navController = rememberNavController()

    val startDestination = if (app.securePrefs.getString("nsec", null) != null) ROUTE_FEEDS else ROUTE_ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_ONBOARDING) {
            OnboardingScreen(
                onNavigateToFeed = {
                    navController.navigate(ROUTE_FEEDS) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_FEEDS) {
            FeedsHost()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedsHost() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Whisper",
                        fontFamily = playfairDisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.Black,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9F7)),
            )
        },
        containerColor = Color(0xFFF9F9F7),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFFF9F9F7),
                contentColor = Color(0xFF2D2D2D),
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Feed", fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Following", fontFamily = interFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp) },
                )
            }
            when (selectedTab) {
                0 -> PublicFeedScreen()
                1 -> PrivateFeedScreen()
            }
        }
    }
}
