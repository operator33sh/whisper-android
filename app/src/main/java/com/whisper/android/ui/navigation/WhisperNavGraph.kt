package com.whisper.android.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.whisper.android.WhisperApplication
import com.whisper.android.ui.onboarding.OnboardingScreen
import com.whisper.android.ui.privatefeed.PrivateFeedScreen
import com.whisper.android.ui.publicfeed.PublicFeedScreen
import com.whisper.android.ui.theme.interFamily
import com.whisper.android.ui.theme.playfairDisplayFamily
import com.whisper.android.util.KeyUtils
import kotlinx.coroutines.launch

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
            FeedsHost(
                onLogout = {
                    app.securePrefs.edit()
                        .remove("nsec")
                        .remove("hex_pubkey")
                        .apply()
                    navController.navigate(ROUTE_ONBOARDING) {
                        popUpTo(ROUTE_FEEDS) { inclusive = true }
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedsHost(onLogout: () -> Unit) {
    val app = LocalContext.current.applicationContext as WhisperApplication
    var selectedTab by remember { mutableIntStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showPostModal by remember { mutableStateOf(false) }
    var postText by remember { mutableStateOf("") }
    val postSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFF9F9F7),
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    TextButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onLogout()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
                    ) {
                        Text(
                            "Logout",
                            color = Color.Black,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        },
    ) {
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
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            HamburgerIcon()
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF9F9F7)),
                )
            },
            containerColor = Color(0xFFF9F9F7),
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
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

                // Floating compose button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 24.dp)
                        .size(56.dp)
                        .background(Color.White, CircleShape)
                        .border(1.5.dp, Color.Black, CircleShape)
                        .clickable { showPostModal = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "w",
                        fontSize = 22.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }

    if (showPostModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showPostModal = false
                postText = ""
            },
            sheetState = postSheetState,
            containerColor = Color(0xFFF9F9F7),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = postText,
                    onValueChange = { postText = it },
                    placeholder = {
                        Text(
                            "What's on your mind?",
                            color = Color(0xFF888888),
                            fontFamily = interFamily,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 10,
                )
                Button(
                    onClick = {
                        val nsec = app.securePrefs.getString("nsec", null)
                        val hexPrivKey = nsec?.let { KeyUtils.nsecToHex(it) }
                        if (!hexPrivKey.isNullOrBlank() && postText.isNotBlank()) {
                            app.nostrRepository.publishTextNote(postText, hexPrivKey)
                            postText = ""
                            showPostModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "Whisper",
                        color = Color.White,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun HamburgerIcon() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val positions = listOf(size.height * 0.2f, size.height * 0.5f, size.height * 0.8f)
        positions.forEach { y ->
            drawLine(
                color = Color.Black,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}
