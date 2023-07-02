package ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import platform.font

object Fonts {
    @Composable
    fun publicSans() = FontFamily(
        font(
            "PublicSans",
            "PublicSans-Regular",
            FontWeight.Normal,
            FontStyle.Normal
        ),
        font(
            "PublicSans",
            "PublicSans-Italic",
            FontWeight.Normal,
            FontStyle.Italic
        ),

        font(
            "PublicSans",
            "PublicSans-Bold",
            FontWeight.Bold,
            FontStyle.Normal
        ),
        font(
            "PublicSans",
            "PublicSans-BoldItalic",
            FontWeight.Bold,
            FontStyle.Italic
        ),

        font(
            "PublicSans",
            "PublicSans-ExtraBold",
            FontWeight.ExtraBold,
            FontStyle.Normal
        ),
        font(
            "PublicSans",
            "PublicSans-ExtraBoldItalic",
            FontWeight.ExtraBold,
            FontStyle.Italic
        ),

        font(
            "PublicSans",
            "PublicSans-Medium",
            FontWeight.Medium,
            FontStyle.Normal
        ),
        font(
            "PublicSans",
            "PublicSans-MediumItalic",
            FontWeight.Medium,
            FontStyle.Italic
        )
    )
}
