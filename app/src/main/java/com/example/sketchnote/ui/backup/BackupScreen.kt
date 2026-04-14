package com.example.sketchnote.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }

    // Launcher chọn file JSON để restore
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importBackup(context, it) {
                showImportDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sao lưu & Khôi phục", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Thông tin
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    "Sao lưu sẽ lưu toàn bộ ghi chú ra file JSON trong thư mục Downloads. " +
                            "Bạn có thể dùng file này để khôi phục dữ liệu khi cần.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))

            // Nút Xuất backup
            Button(
                onClick = { viewModel.exportBackup(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Xuất Backup (JSON)")
            }

            // Nút Nhập backup
            OutlinedButton(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nhập Backup từ file")
            }
        }
    }

    // Dialog thông báo import thành công
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Khôi phục thành công!") },
            text = { Text("Dữ liệu đã được nhập vào app.") },
            confirmButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    onBack()
                }) { Text("OK") }
            }
        )
    }
}