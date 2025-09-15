package com.example.dpa.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
    // 다른 기본 색상들은 여기서 오버라이드할 수 있습니다.
)

@SuppressLint("ObsoleteSdkInt")
@Composable
fun DPATheme(
    darkTheme: Boolean = false,
    // Dynamic color는 Android 12+ 에서 사용할 수 있습니다.
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    @Suppress("DEPRECATION") val systemUiController = rememberSystemUiController() // SystemUiController 가져오기

    if (!view.isInEditMode) { // isInEditMode는 미리보기 상태인지 확인합니다.
        SideEffect {
            (view.context as Activity).window
            // window.statusBarColor = colorScheme.primary.toArgb() // 이 줄 또는 유사한 줄을 삭제하거나 주석 처리합니다.
            // WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme // 이 줄 또는 유사한 줄도 SystemUiController로 대체됩니다.

            // SystemUiController를 사용하여 상태 표시줄 색상 및 아이콘 색상 설정
            systemUiController.setStatusBarColor(
                color = Color.Transparent, // 또는 원하는 배경색 (예: colorScheme.background)
                darkIcons = !darkTheme // 어두운 테마일 때는 밝은 아이콘, 밝은 테마일 때는 어두운 아이콘
            )

            // 필요하다면 네비게이션 바 색상도 설정
            // systemUiController.setNavigationBarColor(
            //     color = Color.Transparent, // 또는 원하는 배경색
            //     darkIcons = !darkTheme
            // )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}