package pt.isec.diogo.safetysec.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.isec.diogo.safetysec.data.model.Alert
import pt.isec.diogo.safetysec.data.model.AlertStatus
import pt.isec.diogo.safetysec.data.model.AlertTriggerType

/**
 * Repository para gestão de alertas no Firestore.
 * no sos só são criados após o countdown
 */
class AlertsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val alertsCollection = firestore.collection("alerts")

    suspend fun createAlert(alert: Alert): Result<String> = runCatching {
        val docRef = alertsCollection.document()
        val alertWithId = alert.copy(id = docRef.id)
        docRef.set(alertToMap(alertWithId)).await()
        docRef.id
    }

    suspend fun getAlertsForMonitor(protectedUserIds: List<String>): Result<List<Alert>> = runCatching {
        if (protectedUserIds.isEmpty()) return@runCatching emptyList()
        
        alertsCollection
            .whereIn("protectedUserId", protectedUserIds)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { mapToAlert(it.data) }
    }

    suspend fun getAlertHistory(protectedUserId: String): Result<List<Alert>> = runCatching {
        alertsCollection
            .whereEqualTo("protectedUserId", protectedUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { mapToAlert(it.data) }
    }

    suspend fun resolveAlert(
        alertId: String, 
        resolvedBy: String, 
        resolvedByName: String
    ): Result<Unit> = runCatching {
        alertsCollection.document(alertId).update(
            mapOf(
                "status" to AlertStatus.RESOLVED.name,
                "resolvedAt" to System.currentTimeMillis(),
                "resolvedBy" to resolvedBy,
                "resolvedByName" to resolvedByName
            )
        ).await()
    }

    suspend fun getAlertById(alertId: String): Result<Alert?> = runCatching {
        val doc = alertsCollection.document(alertId).get().await()
        mapToAlert(doc.data)
    }

    private fun alertToMap(alert: Alert): Map<String, Any?> = mapOf(
        "id" to alert.id,
        "protectedUserId" to alert.protectedUserId,
        "protectedUserName" to alert.protectedUserName,
        "triggerType" to alert.triggerType.name,
        "status" to alert.status.name,
        "latitude" to alert.latitude,
        "longitude" to alert.longitude,
        "videoUrl" to alert.videoUrl,
        "createdAt" to alert.createdAt,
        "resolvedAt" to alert.resolvedAt,
        "resolvedBy" to alert.resolvedBy,
        "resolvedByName" to alert.resolvedByName
    )

    private fun mapToAlert(data: Map<String, Any?>?): Alert? {
        if (data == null) return null
        return Alert(
            id = data["id"] as? String ?: "",
            protectedUserId = data["protectedUserId"] as? String ?: "",
            protectedUserName = data["protectedUserName"] as? String ?: "",
            triggerType = try {
                AlertTriggerType.valueOf(data["triggerType"] as? String ?: "MANUAL_SOS")
            } catch (e: Exception) {
                AlertTriggerType.MANUAL_SOS
            },
            status = try {
                AlertStatus.valueOf(data["status"] as? String ?: "ACTIVE")
            } catch (e: Exception) {
                AlertStatus.ACTIVE
            },
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble(),
            videoUrl = data["videoUrl"] as? String,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            resolvedAt = (data["resolvedAt"] as? Number)?.toLong(),
            resolvedBy = data["resolvedBy"] as? String,
            resolvedByName = data["resolvedByName"] as? String
        )
    }
}
