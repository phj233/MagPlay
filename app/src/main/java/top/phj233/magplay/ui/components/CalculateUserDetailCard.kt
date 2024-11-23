package top.phj233.magplay.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.phj233.magplay.entity.Calculator

@Composable
fun CalculateUserDetailCard(user: Calculator, onDelete: (Calculator) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("确认删除") },
            text = { Text("你确定要删除这个用户吗？") },
            confirmButton = {
                TextButton(onClick = {
                    isVisible = false
                    showDialog = false
                }) {
                    Text("是的")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp,
                pressedElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "用户身高体重信息",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
                    }
                }
                UserInfoRow(label1 = "名字: ", value1 = user.name, label2 = "性别: ", value2 = user.sex)
                UserInfoRow(label1 = "体重: ", value1 = user.weight.toString(), label2 = "身高: ", value2 = user.height.toString())
                UserInfoRow(label1 = "数据创建时间: ", value1 = user.createTime, label2 = "", value2 = "")
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            onDelete(user)
        }
    }
}

@Composable
fun UserInfoRow(label1: String, value1: String, label2: String, value2: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label1,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                text = value1,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (label2.isNotEmpty() && value2.isNotEmpty()) {
            Spacer(modifier = Modifier.width(16.dp)) // Space between the two columns
            Row(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label2,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.width(40.dp) // Fixed width for label2
                )
                Text(
                    text = value2,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}