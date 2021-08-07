package com.udacity.udacitylocationreminder.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.udacity.udacitylocationreminder.utils.FirebaseUserLiveData

class AuthenticationViewModel : ViewModel() {

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }
}