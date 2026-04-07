/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.boostforreddit.http.imgur

import app.morphe.patcher.Fingerprint


internal object InstallImgurPaidOkHttpInterceptorFingerprint : Fingerprint(
    definingClass = "Lbc/a;",
    name = "d",
)

internal object InstallImgurFreeOkHttpInterceptorFingerprint : Fingerprint(
    definingClass = "Lbc/a;",
    name = "e",
)
