package app.morphe.patches.imgur.fix.share

import app.morphe.patcher.Fingerprint

internal val shareDirectImageLinkFingerprint = Fingerprint(
    definingClass = "Lcom/imgur/mobile/common/ui/share/ShareUtils\$Companion;",
    name = "shareDirectImageLink",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Lcom/imgur/mobile/engine/analytics/ShareAnalytics\$ShareSourceType;",
        "Lcom/imgur/mobile/engine/analytics/ShareAnalytics\$ShareContentType;",
        "Ljava/util/List;"
    )
)

internal val shareActionsOnDownloadImageIntentFingerprint = Fingerprint(
    definingClass = "Lcom/imgur/mobile/common/ui/share/ShareActionsActivity;",
    name = "onDownloadImageIntent",
    returnType = "V",
    parameters = emptyList()
)

internal val getDirectImageLinkInitialIntentsFingerprint = Fingerprint(
    definingClass = "Lcom/imgur/mobile/common/ui/share/ShareUtils\$Companion;",
    name = "getDirectImageLinkInitialIntents",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf(
        "Landroid/content/Context;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Z",
        "Lcom/imgur/mobile/engine/analytics/ShareAnalytics\$ShareSourceType;",
        "Lcom/imgur/mobile/engine/analytics/ShareAnalytics\$ShareContentType;",
        "Ljava/util/List;"
    )
)
