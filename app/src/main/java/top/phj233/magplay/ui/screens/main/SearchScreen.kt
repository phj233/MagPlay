package top.phj233.magplay.ui.screens.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navParse
import top.phj233.magplay.torrent.TorrentState

@Composable
fun SearchScreen() {
    val viewModel = viewModel<SearchViewModel>()
    val context = LocalContext.current
    val nav = LocalNavController.current
    val torrentState by viewModel.torrentState.collectAsState()
    
    LaunchedEffect(Unit) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData? = clipboardManager.primaryClip
        val clipText = clipData?.getItemAt(0)?.text?.toString() ?: ""
        
        if (clipText.startsWith("magnet:")) {
            viewModel.parseMagnet(clipText)
        }
    }

    when (val state = torrentState) {
        is TorrentState.Parsing -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is TorrentState.Success -> {
            LaunchedEffect(Unit) {
                nav.navParse(state.info.infoHash)
            }
        }
        is TorrentState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            SearchInput(context,onSubmit = { magnetLink ->
                viewModel.parseMagnet(magnetLink)
            })
        }
        else -> {
            SearchInput(context,onSubmit = { magnetLink ->
                viewModel.parseMagnet(magnetLink)
            })
        }
    }
}

@Composable
private fun SearchInput(context: Context,onSubmit: (String) -> Unit) {
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "请输入磁力链接：",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                textStyle = TextStyle(fontSize = 16.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(96.dp)
                    .background(color = Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = Color.Gray,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Button(
                onClick = {
                    Log.d("SearchScreen", "onSubmit: ${inputText.text}")
                    if (inputText.text.startsWith("magnet:")) {
                        onSubmit(inputText.text)
                    }else {
                        Toast.makeText(context, "请输入正确的磁力链接", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "提交")
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "提交")
            }
        }
    }
}