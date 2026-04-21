package com.example.attentionspan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.attentionspan.R
import com.example.attentionspan.data.local.entity.Subject
import com.example.attentionspan.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectListScreen(
    viewModel: StudyViewModel,
    onSubjectClick: (Long) -> Unit,
    onInsightsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val subjects by viewModel.allSubjects.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.study_subjects)) },
                actions = {
                    IconButton(onClick = onInsightsClick) {
                        Icon(Icons.Default.Insights, contentDescription = stringResource(R.string.insights))
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_subject))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            items(subjects) { subject ->
                SubjectItem(subject = subject, onClick = { onSubjectClick(subject.id) })
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(stringResource(R.string.add_new_subject)) },
                text = {
                    TextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        placeholder = { Text(stringResource(R.string.subject_name_placeholder)) }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSubjectName.isNotBlank()) {
                                viewModel.insertSubject(newSubjectName)
                                newSubjectName = ""
                                showDialog = false
                            }
                        }
                    ) {
                        Text(stringResource(R.string.add))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun SubjectItem(subject: Subject, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
