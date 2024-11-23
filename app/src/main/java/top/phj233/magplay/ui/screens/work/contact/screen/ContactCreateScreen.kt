package top.phj233.magplay.ui.screens.work.contact.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.phj233.magplay.entity.Contact
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navContactsSearch
import top.phj233.magplay.ui.components.MagPlayTopBar
import top.phj233.magplay.ui.screens.work.ContactViewModel

@Composable
fun ContactCreateScreen() {
    val viewModel: ContactViewModel = viewModel()
    val nav = LocalNavController.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            MagPlayTopBar(
                title = "新建联系人",
                actions = {
                    IconButton(onClick = { nav.navContactsSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            TextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                coroutineScope.launch {
                    viewModel.insertContact(Contact(null, name = name, phone = phone))
                    nav.navigateUp()
                }

            }) {
                Text("Create Contact")
            }
        }
    }
}