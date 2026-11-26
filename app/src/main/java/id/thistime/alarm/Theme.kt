package id.thistime.alarm

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Palet "subuh": malam biru tua, matahari kuning sebagai satu-satunya aksen.
val Night = Color(0xFF0F1535)
val Raised = Color(0xFF1A2250)
val RaisedHi = Color(0xFF232C63)
val Ice = Color(0xFFE8EEFF)
val Dim = Color(0xFF9AA6D4)
val Sun = Color(0xFFFFC845)
val OnSun = Color(0xFF1B1400)
val Dusk = Color(0xFF5B3A8C)
val Dawn = Color(0xFFD4703A)

private val Scheme = darkColorScheme(
    primary = Sun, onPrimary = OnSun,
    secondaryContainer = Sun, onSecondaryContainer = OnSun,
    background = Night, onBackground = Ice,
    surface = Raised, onSurface = Ice,
    surfaceVariant = RaisedHi, onSurfaceVariant = Dim,
    outline = Color(0xFF3A4585), outlineVariant = Color(0xFF3A4585),
    surfaceContainerLowest = Night, surfaceContainerLow = Raised, surfaceContainer = Raised,
    surfaceContainerHigh = RaisedHi, surfaceContainerHighest = Color(0xFF2B3575)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(32.dp)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, shapes = AppShapes, content = content)
