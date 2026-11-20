package com.example.uth_todo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.uth_todo.R
import com.example.uth_todo.data.tasks.TaskDates
import com.example.uth_todo.model.TodoTask

@Composable
fun TaskPreviewDialog(task: TodoTask, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(task.category.labelResource()))
                Text(task.date?.let { TaskDates.shortDate(it) } ?: stringResource(R.string.task_no_date))
                Text(task.time ?: stringResource(R.string.task_no_time))
                Text(stringResource(if (task.done) R.string.tasks_completed else R.string.tasks_pending))
                if (task.note.isNotBlank()) Text(task.note)
                if (task.subtasks.isNotEmpty()) {
                    Text(stringResource(R.string.task_subtasks), style = MaterialTheme.typography.titleSmall)
                    task.subtasks.forEach { subtask ->
                        Text(stringResource(if (subtask.done) R.string.task_subtask_done else R.string.task_subtask_pending, subtask.title))
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_action)) } })
}
