package com.just_for_fun.synctax.ui.dynamic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun DynamicGreetingSection(
    userName: String,
    albumColors: AlbumColors,
    greetingTextColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    userNameColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    subGreetingColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
) {
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Hey there, burning the midnight oil?"
        }
    }

    val subGreeting = remember {
        when (greeting) {
            "Good morning" -> "Hope your day starts great!"
            "Good afternoon" -> "Keep going strong!"
            "Good evening" -> "Hope you had a good day so far!"
            else -> "Don't forget to rest when you can."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = greetingTextColor
                    )
                ) {
                    append("$greeting, ")
                }
                withStyle(
                    style = SpanStyle(
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = userNameColor
                    )
                ) {
                    append(userName)
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subGreeting,
                style = MaterialTheme.typography.bodyLarge,
                color = subGreetingColor
            )
        }
    }
}