package com.just_for_fun.synctax.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.core.utils.AppConfig

@Composable
fun WelcomeScreen(
    onNameSubmit: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showSpecialWelcome by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    // Theme-aware colors from AppColors
    val backgroundColor = AppColors.welcomeBackground
    val textPrimaryColor = AppColors.welcomeTitle
    val textSecondaryColor = AppColors.welcomeSubtitle
    val textHintColor = AppColors.textHint
    val accentColor = AppColors.accentPrimary
    val cardBackgroundColor = AppColors.welcomeCardBackground
    val inputBorderColor = AppColors.inputBorder
    val inputFocusedBorderColor = AppColors.inputFocusedBorder
    val errorColor = AppColors.error
    val bottomTextColor = AppColors.textHint

    // Background bubble colors 
    val bubble1Color = AppColors.accentPrimary.copy(alpha = 0.15f)
    val bubble2Color = AppColors.accentSecondary.copy(alpha = 0.15f)
    val bubble3Color = Color.White.copy(alpha = 0.15f)

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Check if name is a creator name using AppConfig
    val trimmedName = name.trim()
    val isCreator = AppConfig.isCreator(context, trimmedName)

    // Show special welcome screen for creators
    if (showSpecialWelcome && isCreator) {
        // *** CHANGE THIS LINE ***
        SpecialCreatorWelcomeScreenTwo(
            creatorName = trimmedName,
            onContinue = {
                showSpecialWelcome = false
                onNameSubmit(trimmedName)
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Background circles for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Red bubble
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = (-100).dp, y = (-150).dp)
                    .background(
                        bubble1Color,
                        shape = RoundedCornerShape(50)
                    )
                    .blur(100.dp)
            )
            // Black bubble
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 100.dp, y = 150.dp)
                    .background(
                        bubble2Color,
                        shape = RoundedCornerShape(50)
                    )
                    .blur(100.dp)
            )
            // White bubble
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-50).dp, y = 200.dp)
                    .background(
                        bubble3Color,
                        shape = RoundedCornerShape(50)
                    )
                    .blur(100.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Top Section with Image and Title
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000)) +
                        slideInVertically(
                            animationSpec = tween(1000, easing = FastOutSlowInEasing),
                            initialOffsetY = { -it / 3 }
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Image from assets
                    val context = LocalContext.current
                    val imageBitmap = remember {
                        try {
                            context.assets.open("app_icon.jpg").use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = "Synctax Logo",
                            modifier = Modifier
                                .size(180.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // App Title
                    Text(
                        text = "Synctax",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Your Personal Music Experience",
                        style = MaterialTheme.typography.bodyLarge,
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Input Section
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 300)) +
                        slideInVertically(
                            animationSpec = tween(1000, delayMillis = 300),
                            initialOffsetY = { it / 2 }
                        )
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardBackgroundColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "What should we call you?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = textPrimaryColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it.trimStart() // Trim leading spaces only while typing
                                isError = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text(
                                    "Your Name",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            placeholder = {
                                Text(
                                    "Enter your name",
                                    color = textHintColor
                                )
                            },
                            isError = isError,
                            supportingText = {
                                if (isError) {
                                    Text(
                                        text = "Please enter your name",
                                        color = errorColor
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    keyboardController?.hide()
                                    val finalName = name.trim()
                                    if (finalName.isNotEmpty()) {
                                        if (isCreator) {
                                            showSpecialWelcome = true
                                        } else {
                                            onNameSubmit(finalName)
                                        }
                                    } else {
                                        isError = true
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = inputFocusedBorderColor,
                                unfocusedBorderColor = inputBorderColor,
                                focusedTextColor = textPrimaryColor,
                                unfocusedTextColor = textPrimaryColor,
                                cursorColor = textPrimaryColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val finalName = name.trim()
                                if (finalName.isNotEmpty()) {
                                    if (isCreator) {
                                        showSpecialWelcome = true
                                    } else {
                                        onNameSubmit(finalName)
                                    }
                                } else {
                                    isError = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Text(
                                text = "Get Started",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Bottom text
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1000, delayMillis = 600))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "made by Krit",
                        style = MaterialTheme.typography.bodySmall,
                        color = bottomTextColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}
