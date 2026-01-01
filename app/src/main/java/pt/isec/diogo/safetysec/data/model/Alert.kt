package pt.isec.diogo.safetysec.data.model

/**
 * alerta de segurança
 */
data class Alert(
    val id: String = "",
    val protectedUserId: String = "",
    val protectedUserName: String = "",
    val triggerType: AlertTriggerType = AlertTriggerType.MANUAL_SOS,
    val status: AlertStatus = AlertStatus.ACTIVE,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val videoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolvedBy: String? = null,
    val resolvedByName: String? = null
)

enum class AlertTriggerType {
    MANUAL_SOS,          // Botão de pânico
    FALL_DETECTION,      // Deteção automática de queda (sensor)
    ACCIDENT_DETECTION,  // Deteção automática de acidente (sensor)
    SPEED_LIMIT,         // Limite de velocidade
    INACTIVITY,          // Inatividade
    GEOFENCE_VIOLATION   // Geofencing
}

enum class AlertStatus {
    ACTIVE,    // Alerta ativo, monitors notificados
    RESOLVED,  // Resolvido por um monitor
    CANCELLED  // Cancelado (apenas para histórico local)
}
