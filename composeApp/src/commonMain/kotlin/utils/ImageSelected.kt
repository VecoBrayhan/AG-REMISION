package utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberImagePicker(
    onImageSelected: (FileData?) -> Unit
): () -> Unit