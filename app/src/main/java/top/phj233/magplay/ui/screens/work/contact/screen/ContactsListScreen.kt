package top.phj233.magplay.ui.screens.work.contact.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import top.phj233.magplay.entity.Contact
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navContactCreate
import top.phj233.magplay.nav.navContactsSearch
import top.phj233.magplay.ui.components.MagPlayTopBar
import top.phj233.magplay.ui.screens.work.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen() {
    val viewModel: ContactViewModel = viewModel()
    val contacts by viewModel.contacts.collectAsState()
    val nav = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = SheetState(
            skipHiddenState = false,
            density = Density(1f),
            skipPartiallyExpanded = false,
        )
    )

    @Composable
    fun ContactBottomSheet(contact: Contact, onUpdate: (Contact) -> Unit) {
        var name by remember { mutableStateOf(contact.name) }
        var phone by remember { mutableStateOf(contact.phone) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "更新联系人", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名字") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("手机号") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                onUpdate(contact.copy(name = name, phone = phone))
            }) {
                Text("更新")
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetContent = {
            selectedContact?.let { contact ->
                ContactBottomSheet(contact = contact, onUpdate = { updatedContact ->
                    coroutineScope.launch {
                        viewModel.updateContact(updatedContact)
                        selectedContact = null
                        bottomSheetState.bottomSheetState.hide()
                    }
                })
            }
        },
        sheetPeekHeight = 0.dp,
        topBar = {
            MagPlayTopBar(
                title = "通讯录",
                actions = {
                    IconButton(onClick = { nav.navContactCreate() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                    IconButton(onClick = { nav.navContactsSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxHeight()) {
            LazyColumn {
                items(contacts) { contact ->
                    ContactCard(contact = contact, onClick = {
                        selectedContact = null
                        coroutineScope.launch {
                            if (bottomSheetState.bottomSheetState.isVisible) {
                                bottomSheetState.bottomSheetState.hide()
                            }
                            selectedContact = contact

                            bottomSheetState.bottomSheetState.expand()
                        }
                    }, onDelete = {
                        coroutineScope.launch {
                            viewModel.deleteContact(contact)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun ContactCard(contact: Contact, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = contact.name, style = MaterialTheme.typography.bodyMedium)
                Text(text = contact.phone, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                horizontalArrangement = Arrangement.End
            ){
                IconButton(onClick = onClick) {
                    Icon(Icons.Default.Create, contentDescription = "修改")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}