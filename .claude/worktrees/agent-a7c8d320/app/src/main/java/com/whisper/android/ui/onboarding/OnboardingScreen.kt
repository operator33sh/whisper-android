package com.whisper.android.ui.onboarding

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun OnboardingScreen(
    onNavigateToFeed: () -> Unit,
    vm: OnboardingViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.navigateToFeed) {
        if (state.navigateToFeed) onNavigateToFeed()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            text = "Whisper",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        Text(
            text = "Sovereign communication",
            fontSize = 14.sp,
            color = Color(0xFF666666),
        )

        Spacer(Modifier.height(40.dp))

        TabRow(
            selectedTabIndex = state.tab.ordinal,
            containerColor = Color.White,
            contentColor = Color.Black,
        ) {
            Tab(
                selected = state.tab == OnboardingTab.IMPORT,
                onClick = { vm.selectTab(OnboardingTab.IMPORT) },
                text = { Text("Import Key") },
            )
            Tab(
                selected = state.tab == OnboardingTab.GENERATE,
                onClick = { vm.selectTab(OnboardingTab.GENERATE) },
                text = { Text("Generate Key") },
            )
        }

        Spacer(Modifier.height(32.dp))

        when (state.tab) {
            OnboardingTab.IMPORT -> ImportKeyTab(state = state, vm = vm)
            OnboardingTab.GENERATE -> GenerateKeyTab(state = state, vm = vm)
        }
    }
}

@Composable
private fun ImportKeyTab(state: OnboardingUiState, vm: OnboardingViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.importNsecInput,
            onValueChange = vm::onNsecInputChanged,
            label = { Text("Your nsec key") },
            placeholder = { Text("nsec1...") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.error != null,
            supportingText = state.error?.let { { Text(it, color = Color.Red) } },
            singleLine = true,
        )

        Button(
            onClick = { vm.importNsec(state.importNsecInput) },
            enabled = state.importNsecInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
            ),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun GenerateKeyTab(state: OnboardingUiState, vm: OnboardingViewModel) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.generatedNsec == null) {
            Text(
                text = "Generate a new Nostr identity. Make sure to save your nsec — it is the only way to access your account.",
                color = Color(0xFF444444),
                fontSize = 14.sp,
            )
            Button(
                onClick = vm::generateKeypair,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            ) {
                Text("Generate")
            }
        } else {
            Text(
                text = "Save your nsec somewhere safe. You cannot recover it.",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(8.dp))

            CopyableField(label = "Public key (npub)", value = state.generatedNpub ?: "", context = context)
            CopyableField(label = "Private key (nsec) — keep secret", value = state.generatedNsec, context = context)

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = vm::confirmKeypair,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            ) {
                Text("I saved my nsec — Continue")
            }
        }
    }
}

@Composable
private fun CopyableField(label: String, value: String, context: Context) {
    Column {
        Text(label, fontSize = 12.sp, color = Color(0xFF888888))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value.take(24) + "...",
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
            ) {
                Text("Copy")
            }
        }
    }
}
