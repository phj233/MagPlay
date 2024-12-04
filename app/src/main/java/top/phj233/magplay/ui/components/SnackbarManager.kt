import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SnackbarMessage(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onAction: (() -> Unit)? = null
)

class SnackbarManager {
    private val _snackbarMessage = MutableStateFlow<SnackbarMessage?>(null)
    val snackbarMessage: StateFlow<SnackbarMessage?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(message: SnackbarMessage) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

@Composable
fun CustomSnackbar(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    snackbarMessage: SnackbarMessage? = null,
    onDismiss: () -> Unit = {}
) {
    snackbarMessage?.let { message ->
        Snackbar(
            modifier = modifier.padding(16.dp),
            dismissAction = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            action = message.actionLabel?.let { label ->
                {
                    TextButton(
                        onClick = {
                            message.onAction?.invoke()
                            onDismiss()
                        }
                    ) {
                        Text(label)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Text(message.message)
        }
    }
}