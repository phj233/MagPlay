package top.phj233.magplay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import top.phj233.magplay.repository.DBUtil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DBUtil.initializeDB(applicationContext)
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}