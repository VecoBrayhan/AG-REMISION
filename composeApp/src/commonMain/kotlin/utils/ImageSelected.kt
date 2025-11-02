package utils

import androidx.compose.runtime.Composable

/**
 * Define la fuente de la imagen (Cámara o Galería).
 */
enum class ImageSource {
    CAMERA,
    GALLERY
}

/**
 * Un Composable 'expect' que proporciona una función para lanzar
 * un selector de imágenes (cámara o galería).
 *
 * @param onImageSelected Callback que se invoca con los bytes de la imagen
 * y un nombre de archivo (ej: "photo.jpg") si la selección es exitosa.
 * Se invoca con null si el usuario cancela.
 * @return Una función lambda `(ImageSource) -> Unit`. Llama a esta función
 * con `ImageSource.CAMERA` para abrir la cámara o `ImageSource.GALLERY`
 * para abrir la galería.
 */
@Composable
expect fun rememberImagePicker(
    onImageSelected: (FileData?) -> Unit
): (ImageSource) -> Unit
