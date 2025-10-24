package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (FileData?) -> Unit
) {
    val filePicker = remember {
        FilePickerLauncher(fileExtensions, onFileSelected)
    }

    if (show) {
        filePicker.launch()
    }
}

class FilePickerLauncher(
    private val fileExtensions: List<String>,
    private val onFileSelected: (FileData?) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {

    fun launch() {
        val documentPicker = UIDocumentPickerViewController(forOpeningContentTypes = emptyList(), asCopy = true)
        documentPicker.delegate = this
        // Lógica para presentar el ViewController
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>
    ) {
        // Lógica para manejar el archivo seleccionado
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onFileSelected(null)
    }
}