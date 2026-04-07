/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.slide.api

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patches.reddit.customclients.AppCompatibility
import app.morphe.patches.reddit.customclients.ExtensionPatches
import app.morphe.patches.reddit.customclients.spoofClientPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.replaceStringMatchesWithFunc
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

internal const val EXTENSION_CLASS_NAME = "Lapp/morphe/extension/slide/APIUtils;"
internal const val GET_USER_AGENT_METHOD = "${EXTENSION_CLASS_NAME}->getUserAgent"
internal const val GET_REDIRECT_URI_METHOD = "$EXTENSION_CLASS_NAME->getRedirectUri"
internal const val MODIFY_DIALOG_METHOD = "$EXTENSION_CLASS_NAME->modifyDialog(Landroid/widget/LinearLayout;)V"
internal const val JRAW_UTILS_EXTENSION_CLASS_NAME = "Lapp/morphe/extension/reddit/http/JrawUtils;"

val spoofClientPatch = spoofClientPatch(
    name = "Spoof client",
    description = "Allows modifying Slide's client ID, redirect URI and user agent in settings. " +
            "Patch options will modify default values.",
) { clientIdOption, redirectUriOption, userAgentOption ->

    val clientId by clientIdOption
    val redirectUri by redirectUriOption
    val userAgent by userAgentOption

    dependsOn(
        ExtensionPatches.Slide
    )
    compatibleWith(*AppCompatibility.Slide)

    execute {
        userAgentFingerprints(packageMetadata.versionName).forEach { fingerprint ->
            fingerprint.matchAll().forEach { match ->
                match.replaceStringMatchesWithFunc(GET_USER_AGENT_METHOD)
            }
        }

        RedirectUriFingerprint.matchAll().forEach { match ->
            match.replaceStringMatchesWithFunc(GET_REDIRECT_URI_METHOD)
        }

        TutorialFingerprint.method.apply {
            val index = TutorialFingerprint.instructionMatches.last().index
            val register = (getInstruction(index) as FiveRegisterInstruction).registerC
            addInstruction(
                index + 1,
                """
                    invoke-static { v$register }, $MODIFY_DIALOG_METHOD
                """
            )
        }

        TutorialSaveFingerprint.method.apply {
            val index = TutorialSaveFingerprint.instructionMatches.first().index
            addInstruction(
                index,
                """
                    invoke-static {}, $EXTENSION_CLASS_NAME->persistSettings()V
                """
            )
        }

        ShowClientIdDialogSetupLayoutFingerprint.method.apply {
            val index = ShowClientIdDialogSetupLayoutFingerprint.instructionMatches.first().index
            val inst = ShowClientIdDialogSetupLayoutFingerprint.instructionMatches.first().instruction
            val register = (inst as FiveRegisterInstruction).registerC
            addInstruction(
                index + 1,
                """
                    invoke-static { v$register }, $MODIFY_DIALOG_METHOD
                """
            )
        }

        ShowClientIdDialogSaveFingerprint.method.apply {
            addInstruction(
                0,
                """
                    invoke-static {}, $EXTENSION_CLASS_NAME->persistSettings()V
                """
            )
        }

        JrawNewUrlFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                opcode == Opcode.NEW_INSTANCE && getReference<TypeReference>()?.type == "Ljava/net/URL;"
            }
            addInstructions(
                index,
                """
                    invoke-static { p0 }, $JRAW_UTILS_EXTENSION_CLASS_NAME->createUrl(Ljava/lang/String;)Ljava/net/URL;
                    move-result-object v0
                    return-object v0
                """
            )
        }

        JrawOnUserChallengeFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static { p1 }, $JRAW_UTILS_EXTENSION_CLASS_NAME->fixOauthFinalUrl(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object p1
                """
            )
        }

        LoadClientIdFingerprint.matchAll().forEach { match ->
            match.method.apply {
                val index = match.instructionMatches.last().index
                replaceInstruction(
                    index,
                    """
                        invoke-static {}, $EXTENSION_CLASS_NAME->getClientId()Ljava/lang/String;
                    """
                )
            }
        }

        GetDefaultClientIdFingerprint.method.returnEarly(clientId!!)
        GetDefaultRedirectUriFingerprint.method.returnEarly(redirectUri!!)
        GetDefaultUserAgentFingerprint.method.returnEarly(userAgent!!)
    }
}
