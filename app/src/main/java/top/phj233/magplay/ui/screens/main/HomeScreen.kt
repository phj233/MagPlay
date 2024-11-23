package top.phj233.magplay.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(){
    Scaffold {paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Text(
                text = "Home",
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}