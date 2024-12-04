package top.phj233.magplay.ui.screens.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navCalculate
import top.phj233.magplay.nav.navContacts
import top.phj233.magplay.nav.navMusicPlayer

@Composable
fun WorkScreen() {
    val nav = LocalNavController.current
    val innerPadding = 8.dp
    Surface(
        modifier = Modifier.padding(innerPadding)
    ) {
        Row {
            FunctionCard(
                text = "身高体重计算器",
                onClick = { nav.navCalculate() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            FunctionCard(
                text = "通讯录",
                onClick = { nav.navContacts() }
            )
            FunctionCard(
                text = "音乐播放器",
                onClick = { nav.navMusicPlayer() }
            )
        }
    }
}

@Composable
fun FunctionCard(text: String, onClick: () -> Unit) {
    val innerPadding = 8.dp
    Card(
        modifier = Modifier.padding(innerPadding),
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp)
        )
    }
}