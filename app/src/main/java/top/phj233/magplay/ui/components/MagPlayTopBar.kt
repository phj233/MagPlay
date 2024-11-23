package top.phj233.magplay.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.phj233.magplay.nav.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagPlayTopBar(title: String, actions: @Composable () -> Unit = {}) {
    val navController = LocalNavController.current
    MediumTopAppBar(
        title = {
            Text(text = title)
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            actions()
        },
        modifier = Modifier
    )
}