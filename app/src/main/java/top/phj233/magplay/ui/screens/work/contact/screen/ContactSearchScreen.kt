package top.phj233.magplay.ui.screens.work.contact.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.phj233.magplay.ui.components.MagPlayTopBar
import top.phj233.magplay.ui.screens.work.ContactViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSearchScreen() {
    val viewModel: ContactViewModel = viewModel()
    var searchQuery by remember { mutableStateOf("") }
    val contacts by viewModel.contacts.collectAsState()
    val filteredContacts = contacts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
    }

    Scaffold(
        topBar = { MagPlayTopBar(title = "搜索联系人") }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(fontSize = 20.sp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) { innerTextField ->
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "搜索联系人...",
                        style = TextStyle(fontSize = 20.sp, color = androidx.compose.ui.graphics.Color.Gray)
                    )
                }
                innerTextField()
            }
            LazyColumn {
                items(filteredContacts) { contact ->
                    ContactCard(contact, onClick = {}, onDelete ={} )
                }
            }
        }
    }
}