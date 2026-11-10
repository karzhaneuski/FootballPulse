package com.artsiom.footballpulse.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artsiom.footballpulse.R
import com.artsiom.footballpulse.domain.model.League

@Composable
fun LeagueSelector(
    leagues: List<League>,
    selectedLeagueCode: String,
    onLeagueSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.selectableGroup(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 10.dp)
    ) {
        items(leagues, key = { it.code }) { league ->
            val selected = league.code == selectedLeagueCode
            BasicText(
                league.shortName,
                modifier = Modifier.padding(horizontal = 4.dp)
                    .background(
                        colorResource(if (selected) R.color.color_tab_selected_bg else R.color.color_tab_unselected_bg),
                        RoundedCornerShape(20.dp)
                    )
                    .selectable(selected, role = Role.Tab, onClick = { onLeagueSelected(league.code) })
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorResource(
                        if (selected) R.color.color_tab_selected_text else R.color.color_tab_unselected_text
                    ),
                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                )
            )
        }
    }
}
