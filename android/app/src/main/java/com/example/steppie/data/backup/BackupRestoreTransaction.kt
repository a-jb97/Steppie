package com.example.steppie.data.backup

internal data class BackupRestoreStep(
    val name: String,
    val apply: suspend () -> Unit,
    val rollback: suspend () -> Unit,
)

internal suspend fun runBackupRestoreTransaction(steps: List<BackupRestoreStep>) {
    val startedSteps = mutableListOf<BackupRestoreStep>()
    try {
        steps.forEach { step ->
            startedSteps += step
            step.apply()
        }
    } catch (error: Throwable) {
        startedSteps.asReversed().forEach { step ->
            try {
                step.rollback()
            } catch (rollbackError: Throwable) {
                error.addSuppressed(BackupRollbackFailure(step.name, rollbackError))
            }
        }
        throw error
    }
}

private class BackupRollbackFailure(
    stepName: String,
    cause: Throwable,
) : Exception(stepName, cause)
