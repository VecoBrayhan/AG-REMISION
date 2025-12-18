package domain

import domain.model.GuideStatus
import domain.model.RemissionGuide
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BusinessLogicTest {

    // PRUEBA 1: Verificar que el sistema rechaza RUCs basura
    @Test
    fun Validation_RechazaRucInvalido() {
        val guiaCorrupta = RemissionGuide(
            guideName = "Guia_2025.pdf",
            date = "2025-10-20",
            ruc = "1045", // RUC incompleto (Error de IA)
            status = GuideStatus.PENDING_REVIEW
        )

        // El sistema debe responder FALSE (No válido)
        assertFalse(guiaCorrupta.isValid(), "El sistema debería rechazar un RUC de 4 dígitos")
    }

    // PRUEBA 2: Verificar que NO se puede aprobar una guía sin productos
    @Test
    fun Validation_ImpideAprobarGuiaVacia() {
        val guiaVacia = RemissionGuide(
            guideName = "Guia_SinProductos.pdf",
            date = "2025-10-20",
            ruc = "20123456789",
            status = GuideStatus.APPROVED, // Intento de aprobar
            extractedData = mapOf("productosJson" to "[]") // Lista vacía
        )

        // El sistema debe bloquear esto
        assertFalse(guiaVacia.isValid(), "El sistema permitió aprobar una guía sin productos")
    }

    // PRUEBA 3: Verificar que una guía correcta SÍ pasa
    @Test
    fun Validation_AceptaGuiaCorrecta() {
        val guiaPerfecta = RemissionGuide(
            guideName = "Guia_OK.pdf",
            date = "2025-10-20",
            ruc = "20607622427", // RUC válido de 11 dígitos
            status = GuideStatus.PENDING_REVIEW
        )

        // El sistema debe responder TRUE
        assertTrue(guiaPerfecta.isValid(), "El sistema rechazó una guía válida")
    }
}