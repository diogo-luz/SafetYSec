package pt.isec.diogo.safetysec.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.isec.diogo.safetysec.data.model.Rule
import pt.isec.diogo.safetysec.data.model.RuleAssignment

class RulesRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val rulesCollection = firestore.collection("rules")
    private val assignmentsCollection = firestore.collection("rule_assignments")

    // ========== Rules CRUD ==========

    suspend fun createRule(rule: Rule): Result<Rule> {
        return try {
            val docRef = rulesCollection.add(rule.toMap()).await()
            Result.success(rule.copy(id = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRulesByMonitor(monitorId: String): Result<List<Rule>> {
        return try {
            val snapshot = rulesCollection
                .whereEqualTo("monitorId", monitorId)
                .get()
                .await()

            val rules = snapshot.documents.map { doc ->
                Rule.fromMap(doc.id, doc.data ?: emptyMap())
            }
            Result.success(rules)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRuleById(ruleId: String): Result<Rule> {
        return try {
            val doc = rulesCollection.document(ruleId).get().await()
            if (doc.exists()) {
                Result.success(Rule.fromMap(doc.id, doc.data ?: emptyMap()))
            } else {
                Result.failure(Exception("Rule not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRule(rule: Rule): Result<Unit> {
        return try {
            rulesCollection.document(rule.id).set(rule.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRule(ruleId: String): Result<Unit> {
        return try {
            // Delete assignments first
            val assignments = assignmentsCollection
                .whereEqualTo("ruleId", ruleId)
                .get()
                .await()
            assignments.documents.forEach { doc ->
                assignmentsCollection.document(doc.id).delete().await()
            }
            // Delete rule
            rulesCollection.document(ruleId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== Assignments CRUD ==========

    suspend fun assignRule(ruleId: String, protectedId: String): Result<RuleAssignment> {
        return try {
            // Check if already assigned
            val existing = assignmentsCollection
                .whereEqualTo("ruleId", ruleId)
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()

            if (!existing.isEmpty) {
                return Result.failure(Exception("Already assigned"))
            }

            val assignment = RuleAssignment(ruleId = ruleId, protectedId = protectedId)
            val docRef = assignmentsCollection.add(assignment.toMap()).await()
            Result.success(assignment.copy(id = docRef.id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAssignmentsForProtected(protectedId: String): Result<List<Pair<Rule, RuleAssignment>>> {
        return try {
            val assignments = assignmentsCollection
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()
                .documents
                .map { doc -> RuleAssignment.fromMap(doc.id, doc.data ?: emptyMap()) }

            val rulesWithAssignments = mutableListOf<Pair<Rule, RuleAssignment>>()
            for (assignment in assignments) {
                val ruleDoc = rulesCollection.document(assignment.ruleId).get().await()
                if (ruleDoc.exists()) {
                    val rule = Rule.fromMap(ruleDoc.id, ruleDoc.data ?: emptyMap())
                    rulesWithAssignments.add(Pair(rule, assignment))
                }
            }
            Result.success(rulesWithAssignments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAssignmentsForRule(ruleId: String): Result<List<String>> {
        return try {
            val assignments = assignmentsCollection
                .whereEqualTo("ruleId", ruleId)
                .get()
                .await()

            val protectedIds = assignments.documents.mapNotNull { doc ->
                doc.getString("protectedId")
            }
            Result.success(protectedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAssignment(assignment: RuleAssignment): Result<Unit> {
        return try {
            assignmentsCollection.document(assignment.id).set(assignment.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAssignmentById(assignmentId: String): Result<Pair<Rule, RuleAssignment>> {
        return try {
            val doc = assignmentsCollection.document(assignmentId).get().await()
            if (!doc.exists()) {
                return Result.failure(Exception("Assignment not found"))
            }
            val assignment = RuleAssignment.fromMap(doc.id, doc.data ?: emptyMap())
            val ruleDoc = rulesCollection.document(assignment.ruleId).get().await()
            if (!ruleDoc.exists()) {
                return Result.failure(Exception("Rule not found"))
            }
            val rule = Rule.fromMap(ruleDoc.id, ruleDoc.data ?: emptyMap())
            Result.success(Pair(rule, assignment))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAssignment(ruleId: String, protectedId: String): Result<Unit> {
        return try {
            val assignments = assignmentsCollection
                .whereEqualTo("ruleId", ruleId)
                .whereEqualTo("protectedId", protectedId)
                .get()
                .await()

            assignments.documents.forEach { doc ->
                assignmentsCollection.document(doc.id).delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
