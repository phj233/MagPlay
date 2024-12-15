package top.phj233.magplay.ui.screens.start

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen() {
    val viewModel: StartViewModel = koinViewModel()
    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    val storagePermissionGranted by viewModel.storagePermissionGranted.collectAsState()
    val selectedUri by viewModel.selectedUri.collectAsState()

    // Animation states
    val iconRotation by rememberInfiniteTransition(label = "").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    // 注册权限检查的结果处理器
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkStoragePermission()
    }

    // 注册目录选择的结果处理器
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val documentFile = DocumentFile.fromTreeUri(context, it)
                if (documentFile?.exists() == true && documentFile.canRead()) {
                    viewModel.setSelectedUri(it)
                    viewModel.resetPermissionRequest()
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "无法获取目录权限", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 检查权限状态
    LaunchedEffect(Unit) {
        viewModel.checkStoragePermission()
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要存储权限") },
            text = { Text("为了正常使用应用，需要获取存储权限。请在设置中授予权限。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                        } else {
                            permissionLauncher.launch(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                ) {
                    Text("前往设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "MagPlay",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer { 
                                if (!storagePermissionGranted) {
                                    rotationZ = iconRotation
                                }
                            },
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "欢迎使用 MagPlay",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "使用需要设置存储权限和目录",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "存储权限",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Icon(
                                imageVector = if (storagePermissionGranted) 
                                    Icons.Rounded.CheckCircle 
                                else 
                                    Icons.Rounded.Error,
                                contentDescription = null,
                                tint = if (storagePermissionGranted)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "存储目录",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Icon(
                                imageVector = if (selectedUri != null) 
                                    Icons.Rounded.CheckCircle 
                                else 
                                    Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = if (selectedUri != null)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selectedUri != null) {
                            Text(
                                text = selectedUri!!.path ?: "未知路径",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!storagePermissionGranted) {
                        Button(
                            onClick = { showPermissionDialog = true },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("请求存储权限")
                        }
                    }

                    if (storagePermissionGranted || selectedUri == null) {
                        Button(
                            onClick = { directoryPicker.launch(null) },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CreateNewFolder,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("选择存储目录")
                        }
                    }
                }
            }
        }
    }
}
