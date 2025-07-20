package com.example.myapplication4.data.api

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception, val message: String? = exception.localizedMessage) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}