package com.example.presentation.common

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
fun OnResumeEffect(onResume: () -> Unit) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onResume()
    }
}
