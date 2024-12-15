package top.phj233.magplay.ui.screens.main

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navDownloadManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Main(){
    val nav = LocalNavController.current
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var selectedTab by rememberSaveable { mutableStateOf("Home") }
    var expanded by remember { mutableStateOf(false) }
    
    selectedTab = when (currentRoute) {
        "Home" -> "Home"
        "Search" -> "Search"
        "Work" -> "Work"
        "Account" -> "Account"
        "Setting" -> "Setting"
        else -> selectedTab
    }
    
    val transition = updateTransition(targetState = expanded, label = "FAB Rotation")
    val rotation by transition.animateFloat(label = "FAB Rotation") { state ->
        if (state) 45f else 0f
    }
    
    Scaffold(
        topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "MagPlay", fontSize = 25.sp, fontWeight = FontWeight.Bold
                        )
                    }
                )
            },
        bottomBar = {
                NavigationBar(modifier = Modifier.fillMaxWidth()) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Localized description"
                            )
                        },
                        label = {
                            Text(
                                "Home",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selected = selectedTab == "Home",
                        onClick = {
                            selectedTab = "Home"
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Localized description"
                            )
                        },
                        label = {
                            Text(
                                "Search",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selected = selectedTab == "Search",
                        onClick = {
                            selectedTab = "Search"
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            //calculate
                            Icon(
                                imageVector = Icons.Outlined.AddCircle,
                                contentDescription = "Localized description"
                            )
                        },
                        label = {
                            Text(
                                "Work",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selected = selectedTab == "Work",
                        onClick = {
                            selectedTab = "Work"
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Localized description"
                            )
                        },
                        label = {
                            Text(
                                "Account",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selected = selectedTab == "Account",
                        onClick = {
                            selectedTab = "Account"
                        }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Localized description"
                            )
                        },
                        label = {
                            Text(
                                "Setting",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selected = selectedTab == "Setting",
                        onClick = {
                            selectedTab = "Setting"
                        }
                    )
                }
            },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (expanded) {
                    SmallFloatingActionButton(
                        onClick = { 
                            nav.navDownloadManager()
                            expanded = false
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("下载管理")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Download, contentDescription = "下载管理")
                        }
                    }
                    // Add more SmallFloatingActionButtons here for other options
                }
                FloatingActionButton(
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "展开",
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }
    ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (selectedTab) {
                    "Home" -> {
                        HomeScreen()
                    }

                    "Search" -> {
                        SearchScreen()
                    }

                    "Work" -> {
                        WorkScreen()
                    }

                    "Account" -> {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "This is the Account Screen"
                        )
                    }

                    "Setting" -> {
                        SettingsScreen()
                    }
                }
    }
    }
}