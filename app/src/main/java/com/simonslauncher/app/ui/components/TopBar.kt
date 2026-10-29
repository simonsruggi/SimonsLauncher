package com.simonslauncher.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.simonslauncher.app.ui.theme.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simonslauncher.app.model.Weather
import com.simonslauncher.app.ui.theme.Amber
import com.simonslauncher.app.ui.theme.TextPrimary
import com.simonslauncher.app.ui.theme.TextSecondary

@Composable
fun TopBar(
    clock: String,
    weather: Weather?,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 34.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Simons") }
                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("Launcher") }
            },
            color = Amber,
            fontSize = 28.sp,
            letterSpacing = (-0.3).sp
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = clock,
            fontSize = 27.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary
        )

        if (weather != null) {
            Spacer(Modifier.width(22.dp))
            Spacer(
                Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(Color(0x55FFFFFF))
            )
            Spacer(Modifier.width(22.dp))
            WeatherIcon(weather = weather, modifier = Modifier.size(width = 30.dp, height = 24.dp))
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${weather.temperature}°",
                    fontSize = 21.sp,
                    color = TextPrimary
                )
                Text(
                    text = weather.city,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        SettingsButton(onClick = onOpenSettings)
    }
}
