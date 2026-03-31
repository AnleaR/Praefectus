package me.anlear.praefectus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import me.anlear.praefectus.data.api.StratzApiClient
import me.anlear.praefectus.data.cache.DatabaseFactory
import me.anlear.praefectus.data.repository.HeroRepository
import me.anlear.praefectus.domain.models.RankBracket
import me.anlear.praefectus.ui.components.RankSelector
import me.anlear.praefectus.ui.navigation.NavigationRail
import me.anlear.praefectus.ui.navigation.Screen
import me.anlear.praefectus.ui.screens.DraftScreen
import me.anlear.praefectus.ui.screens.SettingsScreen
import me.anlear.praefectus.ui.screens.TierListScreen
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.ui.theme.PraefectusTheme
import me.anlear.praefectus.util.Config
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

fun main() = application {
    DatabaseFactory.init()

    val apiClient = remember { StratzApiClient { Config.apiToken } }
    val heroRepository = remember { HeroRepository(apiClient) }

    var lang by remember { mutableStateOf(Lang.valueOf(Config.language.uppercase().let { if (it == "RU" || it == "EN") it else "EN" })) }
    var bracket by remember { mutableStateOf(RankBracket.fromString(Config.rankBracket)) }
    var currentScreen by remember { mutableStateOf(Screen.DRAFT) }
    var showTokenDialog by remember { mutableStateOf(Config.apiToken.isBlank()) }

    Window(
        onCloseRequest = {
            apiClient.close()
            exitApplication()
        },
        title = Strings.get("app_title", lang),
        state = rememberWindowState(size = DpSize(1440.dp, 900.dp))
    ) {
        window.minimumSize = java.awt.Dimension(1280, 720)

        PraefectusTheme {
            // Token prompt dialog
            if (showTokenDialog) {
                TokenDialog(
                    lang = lang,
                    onTokenSaved = { token ->
                        Config.apiToken = token
                        showTokenDialog = false
                    },
                    onDismiss = { showTokenDialog = false }
                )
            }

            Row(modifier = Modifier.fillMaxSize().background(DotaColors.Background)) {
                // Navigation rail
                NavigationRail(
                    currentScreen = currentScreen,
                    lang = lang,
                    onNavigate = { currentScreen = it }
                )

                // Main content
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top bar with rank selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DotaColors.BackgroundSecondary)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.get(currentScreen.locKey, lang),
                            fontSize = 16.sp,
                            color = DotaColors.TextPrimary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )

                        if (currentScreen != Screen.SETTINGS) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    Strings.get("rank_bracket", lang) + ":",
                                    fontSize = 12.sp,
                                    color = DotaColors.TextSecondary
                                )
                                RankSelector(
                                    selected = bracket,
                                    onSelect = {
                                        bracket = it
                                        Config.rankBracket = it.apiName
                                    }
                                )
                            }
                        }
                    }

                    // Screen content
                    when (currentScreen) {
                        Screen.DRAFT -> DraftScreen(
                            heroRepository = heroRepository,
                            bracket = bracket,
                            lang = lang
                        )
                        Screen.TIER_LIST -> TierListScreen(
                            heroRepository = heroRepository,
                            bracket = bracket,
                            lang = lang
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            heroRepository = heroRepository,
                            currentLang = lang,
                            currentBracket = bracket,
                            onLangChanged = { lang = it },
                            onBracketChanged = { bracket = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TokenDialog(lang: Lang, onTokenSaved: (String) -> Unit, onDismiss: () -> Unit) {
    var tokenInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                Strings.get("token_prompt_title", lang),
                color = DotaColors.TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    Strings.get("token_prompt_text", lang),
                    color = DotaColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "https://stratz.com/api",
                    color = DotaColors.Accent,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    singleLine = true,
                    label = { Text(Strings.get("api_token", lang)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DotaColors.TextPrimary,
                        unfocusedTextColor = DotaColors.TextPrimary,
                        focusedBorderColor = DotaColors.Accent,
                        unfocusedBorderColor = DotaColors.SurfaceBorder,
                        cursorColor = DotaColors.Accent,
                        focusedLabelColor = DotaColors.Accent,
                        unfocusedLabelColor = DotaColors.TextSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (tokenInput.isNotBlank()) onTokenSaved(tokenInput) },
                colors = ButtonDefaults.buttonColors(containerColor = DotaColors.Accent)
            ) {
                Text(Strings.get("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("settings", lang), color = DotaColors.TextSecondary)
            }
        },
        containerColor = DotaColors.BackgroundSecondary,
        titleContentColor = DotaColors.TextPrimary,
        textContentColor = DotaColors.TextSecondary
    )
}
