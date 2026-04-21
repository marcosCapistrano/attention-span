package com.example.attentionspan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.attentionspan.R
import com.example.attentionspan.ui.viewmodel.StudyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronometerScreen(
    subjectId: Long,
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    val timerRunning by viewModel.timerRunning.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    val subjects by viewModel.allSubjects.collectAsState(initial = emptyList())
    val subject = subjects.find { it.id == subjectId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(subject?.name ?: stringResource(R.string.study_session)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTime(elapsedTime),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (timerRunning) {
                        viewModel.stopTimer(subjectId)
                    } else {
                        viewModel.startTimer(subjectId, subject?.name ?: "Study Session")
                    }
                },
                modifier = Modifier
                    .size(200.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (timerRunning) Color.Red else Color.Green
                )
            ) {
                Text(
                    text = if (timerRunning) stringResource(R.string.stop) else stringResource(R.string.start),
                    fontSize = 24.sp,
                    color = Color.White
                )
            }
        }
    }
}

fun formatTime(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}
