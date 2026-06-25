package app.morphe.patches.reddit.customclients.boostforreddit.misc.settings

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.resource.utf8Writer
import app.morphe.patches.reddit.customclients.boostforreddit.BoostCompatible

@Suppress("unused")
val boostMorpheSettingsSkeletonPatch = resourcePatch(
    name = "Boost Morphe settings skeleton",
    description = "Dev-only Boost Morphe settings resource skeleton. Does not change Boost runtime behavior.",
    default = false
) {
    compatibleWith(*BoostCompatible)

    execute {
        get("res/xml/morphe_boost_settings_skeleton.xml").utf8Writer().use { writer ->
            writer.write(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">
                    <PreferenceCategory android:title="Morphe dev skeleton">
                        <Preference
                            android:key="morphe_boost_settings_skeleton_marker"
                            android:title="Morphe dev skeleton"
                            android:summary="Build-only resource skeleton. No runtime behavior is changed." />
                    </PreferenceCategory>
                </PreferenceScreen>
                """.trimIndent()
            )
        }
    }
}
