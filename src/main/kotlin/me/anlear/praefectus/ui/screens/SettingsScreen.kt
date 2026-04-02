package me.anlear.praefectus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.anlear.praefectus.data.repository.HeroRepository
import me.anlear.praefectus.domain.models.RankBracket
import me.anlear.praefectus.ui.components.RankSelector
import me.anlear.praefectus.ui.theme.DotaColors
import me.anlear.praefectus.util.Config
import me.anlear.praefectus.util.Lang
import me.anlear.praefectus.util.Strings

@Composable
fun SettingsScreen(
    heroRepository: HeroRepository,
    currentLang: Lang,
    currentBracket: RankBracket,
    onLangChanged: (Lang) -> Unit,
    onBracketChanged: (RankBracket) -> Unit,
    supportBonus: Boolean,
    onSupportBonusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(Config.apiToken) }
    var updating by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            Strings.get("settings", currentLang),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = DotaColors.TextPrimary
        )

        // API Token
        SettingsSection(Strings.get("api_token", currentLang)) {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DotaColors.TextPrimary,
                    unfocusedTextColor = DotaColors.TextPrimary,
                    focusedBorderColor = DotaColors.Accent,
                    unfocusedBorderColor = DotaColors.SurfaceBorder,
                    cursorColor = DotaColors.Accent,
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        Config.apiToken = token
                        statusMessage = Strings.get("save", currentLang) + " ✓"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DotaColors.Accent)
                ) {
                    Text(Strings.get("save", currentLang))
                }
                Text(
                    "https://stratz.com/api",
                    fontSize = 12.sp,
                    color = DotaColors.Accent,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }

        // Language
        SettingsSection(Strings.get("language", currentLang)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lang.entries.forEach { lang ->
                    Button(
                        onClick = {
                            Config.language = lang.name
                            onLangChanged(lang)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentLang == lang) DotaColors.Accent else DotaColors.Surface
                        )
                    ) {
                        Text(lang.name)
                    }
                }
            }
        }

        // Rank Bracket
        SettingsSection(Strings.get("rank_bracket", currentLang)) {
            RankSelector(
                selected = currentBracket,
                onSelect = {
                    Config.rankBracket = it.apiName
                    onBracketChanged(it)
                },
                lang = currentLang
            )
        }

        // Update Data
        SettingsSection(Strings.get("update_data", currentLang)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            updating = true
                            statusMessage = null
                            try {
                                heroRepository.getHeroes(forceRefresh = true)
                                heroRepository.getHeroStats(currentBracket, forceRefresh = true)
                                statusMessage = Strings.get("data_updated", currentLang)
                            } catch (e: Exception) {
                                statusMessage = "${Strings.get("error", currentLang)}: ${e.message}"
                            }
                            updating = false
                        }
                    },
                    enabled = !updating,
                    colors = ButtonDefaults.buttonColors(containerColor = DotaColors.Accent)
                ) {
                    Text(if (updating) Strings.get("updating", currentLang) else Strings.get("update_data", currentLang))
                }

                if (updating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = DotaColors.Accent)
                }

                statusMessage?.let {
                    Text(it, fontSize = 12.sp, color = DotaColors.TextSecondary)
                }
            }
        }

        // Support Bonus
        SettingsSection(Strings.get("support_bonus", currentLang)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = supportBonus,
                    onCheckedChange = {
                        Config.supportBonus = it
                        onSupportBonusChanged(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DotaColors.Accent,
                        checkedTrackColor = DotaColors.Accent.copy(alpha = 0.3f),
                        uncheckedThumbColor = DotaColors.TextSecondary,
                        uncheckedTrackColor = DotaColors.Surface
                    )
                )
                Text(
                    Strings.get("support_bonus_desc", currentLang),
                    fontSize = 13.sp,
                    color = DotaColors.TextSecondary
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DotaColors.BackgroundSecondary)
            .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = DotaColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
