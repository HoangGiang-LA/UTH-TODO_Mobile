package com.example.uth_todo.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uth_todo.R
import com.example.uth_todo.ui.home.HomeGlyph
import com.example.uth_todo.ui.home.HomeIcon

enum class TaskTab { HOME, LIST }

@Composable
fun TaskBottomBar(
    selected: TaskTab,
    onHome: () -> Unit,
    onList: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onAdd: () -> Unit,
) {
    val addLabel = stringResource(R.string.task_add)
    Surface(shadowElevation = 5.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TaskNavItem(HomeIcon.HOME, stringResource(R.string.home_title), selected == TaskTab.HOME,
                onHome, Modifier.weight(1f).testTag("task_tab_home"))
            TaskNavItem(HomeIcon.LIST, stringResource(R.string.tasks_list_title), selected == TaskTab.LIST,
                onList, Modifier.weight(1f).testTag("task_tab_list"))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                FloatingActionButton(onClick = onAdd, shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(50.dp).testTag("task_add_action").semantics { contentDescription = addLabel }) {
                    HomeGlyph(HomeIcon.PLUS, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            TaskNavItem(HomeIcon.BELL, stringResource(R.string.notifications_title), false,
                onNotifications, Modifier.weight(1f).testTag("task_tab_notifications"))
            TaskNavItem(HomeIcon.SETTINGS, stringResource(R.string.settings_title), false,
                onProfile, Modifier.weight(1f).testTag("task_tab_profile"))
        }
    }
}

@Composable
private fun TaskNavItem(icon: HomeIcon, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    TextButton(onClick = onClick, modifier = modifier.semantics { selected = isSelected },
        contentPadding = PaddingValues(horizontal = 1.dp, vertical = 2.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HomeGlyph(icon, color = color)
            Text(label, fontSize = 10.sp, lineHeight = 12.sp, color = color, maxLines = 2)
        }
    }
}
