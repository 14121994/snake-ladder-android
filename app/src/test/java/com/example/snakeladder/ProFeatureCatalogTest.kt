package com.example.snakeladder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFeatureCatalogTest {

    @Test
    fun catalog_containsExactlyFiftyUniqueFeatures() {
        val features = ProFeatureCatalog.features

        assertEquals(50, features.size)
        assertEquals(features.size, features.map { it.id }.distinct().size)
        assertTrue(features.all { it.title.isNotBlank() && it.playerValue.isNotBlank() })
    }

    @Test
    fun catalog_marksBackendDependentSocialAndCloudFeatures() {
        val backendIds = ProFeatureCatalog.features
            .filter { it.status == ProFeatureStatus.BACKEND_REQUIRED }
            .map { it.id }

        assertTrue("online multiplayer should require backend support", "online_multiplayer" in backendIds)
        assertTrue("ranked ladder should require backend support", "ranked_ladder" in backendIds)
        assertTrue("cloud saves should require backend support", "cloud_saves" in backendIds)
        assertTrue("cross-device sync should require backend support", "cross_device_sync" in backendIds)
    }

    @Test
    fun catalog_coversEveryProductCategory() {
        val grouped = ProFeatureCatalog.byCategory()

        assertEquals(ProFeatureCategory.entries.toSet(), grouped.keys)
        assertEquals(50, grouped.values.sumOf { it.size })
        assertTrue(grouped.values.all { it.isNotEmpty() })
    }

    @Test
    fun catalog_statusCountsCoverAllFeatures() {
        val counts = ProFeatureCatalog.statusCounts()

        assertEquals(ProFeatureStatus.entries.toSet(), counts.keys)
        assertEquals(50, counts.values.sum())
        assertTrue((counts[ProFeatureStatus.OFFLINE_READY] ?: 0) > (counts[ProFeatureStatus.BACKEND_REQUIRED] ?: 0))
        assertEquals(1, counts[ProFeatureStatus.IN_APP_FOUNDATION])
    }

    @Test
    fun featureFlags_hideBackendRequiredFeaturesByDefault() {
        val online = ProFeatureCatalog.features.first { it.id == "online_multiplayer" }
        val daily = ProFeatureCatalog.features.first { it.id == "daily_challenges" }

        assertTrue(FeatureFlags.isFeatureVisible(daily))
        assertTrue(!FeatureFlags.isFeatureVisible(online))
    }
}
