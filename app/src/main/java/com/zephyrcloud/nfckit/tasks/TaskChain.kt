package com.zephyrcloud.nfckit.tasks

import kotlinx.serialization.Serializable

@Serializable
data class TaskStep(
    val action: TaskActionSpec,
    val condition: TaskCondition = TaskCondition.Always,
    val delayBeforeMs: Long = 0,
)

@Serializable
data class TaskChain(val steps: List<TaskStep>)

sealed class StepResult {
    data class Ran(val action: TaskActionSpec, val outcome: ActionResult) : StepResult()
    data class Skipped(val action: TaskActionSpec, val reason: String) : StepResult()
}

sealed class ActionResult {
    data object Success : ActionResult()
    data class Failed(val reason: String) : ActionResult()
}
