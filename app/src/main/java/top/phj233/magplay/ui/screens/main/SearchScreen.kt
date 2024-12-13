package top.phj233.magplay.ui.screens.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.koin.compose.viewmodel.koinViewModel
import top.phj233.magplay.nav.LocalNavController
import top.phj233.magplay.nav.navParse
import top.phj233.magplay.ui.screens.magnet.ParseViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val nav = LocalNavController.current
    val parseViewModel: ParseViewModel = koinViewModel()
    
    LaunchedEffect(Unit) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData: ClipData? = clipboardManager.primaryClip
        val clipText = clipData?.getItemAt(0)?.text?.toString() ?: ""
        
        if (clipText.startsWith("magnet:")) {
            Log.d("SearchScreen", "clipboard: $clipText")
            val encodedMagnet = URLEncoder.encode(clipText, StandardCharsets.UTF_8.toString())
            nav.navParse(encodedMagnet)
        }
    }

    SearchInput(context) { magnetLink ->
        Log.d("SearchScreen", "onSubmit: $magnetLink")
        val encodedMagnet = URLEncoder.encode(magnetLink, StandardCharsets.UTF_8.toString())
        nav.navParse(encodedMagnet)
    }
}

@Composable
private fun SearchInput(context: Context, onSubmit: (String) -> Unit) {
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
                    } else {
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