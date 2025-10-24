package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun ExcelViewer(modifier: Modifier, excelBytes: ByteArray)