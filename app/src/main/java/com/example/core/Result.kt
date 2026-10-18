package com.example.core

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val throwable: Throwable, val message: String = throwable.message ?: "Unknown error") : Result<Nothing>
    data object Loading : Result<Nothing>
}
