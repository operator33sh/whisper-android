package com.whisper.android.ui.navigation

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whisper.android.ui.onboarding.OnboardingScreen
import com.whisper.android.ui.privatefeed.PrivateFeedScreen
import com.whisper.android.ui.publicfeed.PublicFeedScreen

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_FEEDS = "feeds"

@Composable
fun WhisperNavGraph() {
    val context = LocalContext.current
    val navController = rememberNavController()

    val startDestination = if (hasStoredNsec(context)) ROUTE_FEEDS else ROUTE_ONBOARDING

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

@Composable
private fun FeedsHost() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> PublicFeedScreen()
                1 -> PrivateFeedScreen()
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color.Black,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Public") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Following") },
            )
        }
    }
}

private fun hasStoredNsec(context: Context): Boolean {
    return try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "whisper_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        prefs.getString("nsec", null) != null
    } catch (_: Exception) {
        false
    }
}
