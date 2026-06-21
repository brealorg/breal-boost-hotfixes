package app.morphe.patches.reddit.customclients.boostforreddit.ads

import app.morphe.patcher.Fingerprint

internal val maxMediationFingerprint = Fingerprint(
    strings = listOf("MaxMediation: Attempting to initialize SDK")
)

internal val admobMediationFingerprint = Fingerprint(
    strings = listOf("AdmobMediation: Attempting to initialize SDK")
)

private fun boostAdFragmentFingerprint(
    classType: String,
    methodName: String
) = Fingerprint(
    custom = { method, classDef ->
        classDef.type == classType &&
            method.name == methodName
    }
)

internal val boostAdFragmentFingerprints = arrayOf(
    boostAdFragmentFingerprint(
        "Lcom/rubenmayayo/reddit/ui/fragments/j;",
        "W2"
    ),
    boostAdFragmentFingerprint(
        "Lcom/rubenmayayo/reddit/ui/fragments/e;",
        "c3"
    ),
    boostAdFragmentFingerprint(
        "Lcom/rubenmayayo/reddit/ui/fragments/h;",
        "W2"
    ),
    boostAdFragmentFingerprint(
        "Lcom/rubenmayayo/reddit/ui/fragments/g;",
        "W2"
    ),
    boostAdFragmentFingerprint(
        "Lcom/rubenmayayo/reddit/ui/fragments/c;",
        "c3"
    )
)

internal val commentsMaxAdViewCreateFingerprint = Fingerprint(
    strings = listOf("MaxMediation: Create MaxAdView"),
    custom = { _, classDef ->
        classDef.type == "Lcom/rubenmayayo/reddit/ui/comments/CommentsActivity;"
    }
)
