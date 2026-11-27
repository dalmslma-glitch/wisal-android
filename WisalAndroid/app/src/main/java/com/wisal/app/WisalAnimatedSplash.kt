package com.wisal.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WisalAnimatedSplash() {
    val transition = rememberInfiniteTransition(label = "wisalSplash")
    val scale by transition.animateFloat(
        initialValue = 0.96f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoPulse"
    )
    val glow by transition.animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "logoGlow"
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF041B25), Color(0xFF07111D), Color(0xFF02080D)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(190.dp).scale(scale), contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).background(Color(0xFF00D6A0).copy(alpha = glow * 0.16f), CircleShape))
                Box(
                    Modifier.offset(x = (-24).dp, y = (-18).dp).size(112.dp)
                        .background(Color(0xFF00B88A), RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ChatBubble, null, tint = Color.White, modifier = Modifier.size(62.dp)) }
                Box(
                    Modifier.offset(x = 28.dp, y = 25.dp).size(100.dp)
                        .background(Color(0xFF078BEA), RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.ChatBubble, null, tint = Color.White, modifier = Modifier.size(54.dp)) }
            }
            Spacer(Modifier.height(22.dp))
            Text("وِصال", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
            Text("W I S A L", color = Color(0xFF36E0C0), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text("رسائلك تقرّب المسافات", color = Color(0xFFB8D7DE), fontSize = 14.sp)
        }
    }
}
