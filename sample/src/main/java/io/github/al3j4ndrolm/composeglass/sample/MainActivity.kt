package io.github.al3j4ndrolm.composeglass.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.al3j4ndrolm.composeglass.glassMorphism

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { GlassDemo() } }
    }
}

@Composable
private fun GlassDemo() {
    Box(Modifier.fillMaxSize()) {
        ColorFeed()

        // Floating top bar: default style (Liquid Glass)
        Box(
            Modifier
                .statusBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .height(64.dp)
                .glassMorphism(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text("ComposeGlass", Modifier.padding(horizontal = 20.dp), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }

        // Floating player card: default style with a rounder shape
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .glassMorphism(shape = RoundedCornerShape(32.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(48.dp).background(Color.White.copy(alpha = 0.35f), CircleShape))
            Column {
                Text("Now playing", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                Text("Glass, frosted", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** A scrolling, colorful backdrop so the blur is easy to see. */
@Composable
private fun ColorFeed() {
    val palettes = listOf(
        listOf(Color(0xFFFF6B6B), Color(0xFFFFD93D)),
        listOf(Color(0xFF6BCB77), Color(0xFF4D96FF)),
        listOf(Color(0xFF9D4EDD), Color(0xFFFF6FB5)),
        listOf(Color(0xFF00C2D1), Color(0xFF3A0CA3)),
        listOf(Color(0xFFFF9F1C), Color(0xFFE71D36)),
    )
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFF0E0E12)),
        contentPadding = PaddingValues(top = 120.dp, bottom = 160.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(30) { i ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.linearGradient(palettes[i % palettes.size]), RoundedCornerShape(28.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Text("Card ${i + 1}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
