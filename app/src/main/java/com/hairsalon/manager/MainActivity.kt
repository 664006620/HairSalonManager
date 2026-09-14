package com.hairsalon.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hairsalon.manager.ui.*
import com.hairsalon.manager.ui.theme.HairSalonTheme
import com.hairsalon.manager.viewmodel.MemberViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HairSalonTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MemberViewModel = viewModel()) {
    var showDetail by remember { mutableStateOf(false) }
    var selectedMemberId by remember { mutableStateOf(0L) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val message by viewModel.message.collectAsState()

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importFromTxt(it) }
    }

    val exportDbLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.exportDatabase(it) }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { viewModel.exportMembersToTxt(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showDetail) {
                MemberDetailScreen(
                    viewModel = viewModel,
                    memberId = selectedMemberId,
                    onBack = { showDetail = false }
                )
            } else {
                MemberListScreen(
                    viewModel = viewModel,
                    onMemberClick = { id ->
                        selectedMemberId = id
                        showDetail = true
                    },
                    onAddClick = { showAddDialog = true },
                    onImportClick = { showImportExport = true },
                    onExportClick = { showImportExport = true }
                )
            }
        }
    }

    if (showAddDialog) {
        AddMemberDialog(
            onConfirm = { name, phone, level, remark ->
                viewModel.addMember(name, phone, level, remark)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showImportExport) {
        ImportExportDialog(
            onImport = {
                importLauncher.launch(arrayOf("text/plain", "text/*", "*/*"))
                showImportExport = false
            },
            onExportDb = {
                exportDbLauncher.launch("hairsalon_backup.db")
                showImportExport = false
            },
            onExportTxt = {
                exportTxtLauncher.launch("members_export.txt")
                showImportExport = false
            },
            onDismiss = { showImportExport = false }
        )
    }
}

@Composable
fun ImportExportDialog(
    onImport: () -> Unit,
    onExportDb: () -> Unit,
    onExportTxt: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("数据管理", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "导入TXT格式: 姓名,电话,余额,等级",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onImport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("📥 导入TXT文件")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExportDb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("💾 导出数据库文件")
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onExportTxt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("📄 导出会员列表(TXT)")
                }
                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}
