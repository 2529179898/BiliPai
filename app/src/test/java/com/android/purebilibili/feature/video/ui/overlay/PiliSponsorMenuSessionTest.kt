package com.android.purebilibili.feature.video.ui.overlay

import com.android.purebilibili.data.model.response.SponsorCategory
import com.android.purebilibili.feature.plugin.SponsorBlockSegmentBehavior
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PiliSponsorMenuSessionTest {
    @Test
    fun storedCommunityServerAndIdentityAreNotReplacedWithDefaults() {
        val config = decodePiliSponsorMenuConfig("""{
            "serverBaseUrl":"https://community.example/api",
            "userId":"existing-persistent-community-user",
            "unrecognizedFutureOption":true
        }""").normalized()
        assertEquals("https://community.example/api", config.serverBaseUrl)
        assertEquals("existing-persistent-community-user", config.userId)
    }

    @Test
    fun dialogUsesActualPerCategoryBehaviorAndColors() {
        val config = decodePiliSponsorMenuConfig("""{
            "userId":"existing-user",
            "categoryBehaviorRaw":{"sponsor":"MARKER_ONLY","intro":"SKIP_ONCE"},
            "categoryColorHex":{"sponsor":"#123abc"}
        }""").normalized()
        assertEquals(SponsorBlockSegmentBehavior.MARKER_ONLY, config.behaviorFor(SponsorCategory.SPONSOR))
        assertEquals(SponsorBlockSegmentBehavior.SKIP_ONCE, config.behaviorFor(SponsorCategory.INTRO))
        assertEquals("#123ABC", config.categoryColorHex[SponsorCategory.SPONSOR])
    }

    @Test
    fun legacyPluginSettingsUseTheSameMigrationAsPlaybackPlugin() {
        val config = decodePiliSponsorMenuConfig("""{
            "userId":"existing-user", "autoSkip":false, "skipSponsor":false
        }""").normalized()
        assertEquals(SponsorBlockSegmentBehavior.DISABLED, config.behaviorFor(SponsorCategory.SPONSOR))
        assertEquals(SponsorBlockSegmentBehavior.MANUAL, config.behaviorFor(SponsorCategory.INTRO))
    }

    @Test
    fun newConfigurationGetsAnIdentityThroughExistingPluginNormalization() {
        assertTrue(decodePiliSponsorMenuConfig(null).normalized().userId.isNotBlank())
    }

    @Test
    fun malformedStoredConfigurationDoesNotSilentlyResetServerOrIdentity() {
        assertFailsWith<SerializationException> { decodePiliSponsorMenuConfig("{not-json}") }
    }
}
