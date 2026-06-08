package com.example.snakeladder

internal enum class AppFeatureFlag {
    OFFLINE_FEATURES,
    LOCAL_SOCIAL,
    BACKEND_FEATURES
}

internal object FeatureFlags {
    private val enabled = setOf(
        AppFeatureFlag.OFFLINE_FEATURES,
        AppFeatureFlag.LOCAL_SOCIAL
    )

    fun isEnabled(flag: AppFeatureFlag): Boolean = flag in enabled

    fun isFeatureVisible(feature: ProFeature): Boolean {
        return when (feature.status) {
            ProFeatureStatus.BACKEND_REQUIRED -> isEnabled(AppFeatureFlag.BACKEND_FEATURES)
            ProFeatureStatus.IN_APP_FOUNDATION,
            ProFeatureStatus.OFFLINE_READY -> true
        }
    }
}
