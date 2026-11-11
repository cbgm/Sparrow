package com.cbgm.sparrow.feature.identity.di

/** Koin properties set by the app at start; both exist only in debug builds. */
const val DEBUG_BUILD_PROPERTY = "sparrow.debugBuild"

/** DEBUG_SMS_CODE from local.properties: Firebase test mode with this code. */
const val DEBUG_SMS_CODE_PROPERTY = "sparrow.debugSmsCode"

/** The app's Firebase project for the phone sign-in (FIREBASE_APP_ID, FIREBASE_API_KEY, FIREBASE_PROJECT_ID). */
const val FIREBASE_APP_ID_PROPERTY = "sparrow.firebaseAppId"
const val FIREBASE_API_KEY_PROPERTY = "sparrow.firebaseApiKey"
const val FIREBASE_PROJECT_ID_PROPERTY = "sparrow.firebaseProjectId"
