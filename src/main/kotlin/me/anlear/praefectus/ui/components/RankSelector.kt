package me.anlear.praefectus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.anlear.praefectus.domain.models.RankBracket
import me.anlear.praefectus.ui.theme.DotaColors

@Composable
fun RankSelector(
    selected: RankBracket,
    onSelect: (RankBracket) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(DotaColors.Surface)
                .border(1.dp, DotaColors.SurfaceBorder, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(selected.display, fontSize = 12.sp, color = DotaColors.TextPrimary)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DotaColors.Surface)
        ) {
            RankBracket.entries.forEach { bracket ->
                DropdownMenuItem(
                    text = { Text(bracket.display, color = DotaColors.TextPrimary, fontSize = 12.sp) },
                    onClick = {
                        onSelect(bracket)
                        expanded = false
                    }
                )
            }
        }
    }
}
