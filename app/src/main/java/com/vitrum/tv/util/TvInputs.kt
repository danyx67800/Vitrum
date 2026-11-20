package com.vitrum.tv.util

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.os.Build

data class HdmiInput(
    val id: String,
    val label: String,
    val intent: Intent,
)

object TvInputs {

    fun hdmiInputs(context: Context): List<HdmiInput> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return emptyList()
        val manager = runCatching {
            context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
        }.getOrNull() ?: return emptyList()
        val inputs = runCatching { manager.tvInputList }.getOrElse { emptyList() }
        return inputs.mapNotNull { info ->
            if (info.type != TvInputInfo.TYPE_HDMI) return@mapNotNull null
            val uri = runCatching {
                TvContract.buildChannelUriForPassthroughInput(info.id)
            }.getOrNull() ?: return@mapNotNull null
            val intent = Intent(Intent.ACTION_VIEW, uri)
            if (!SystemUtils.canResolve(context, intent)) return@mapNotNull null
            val label = runCatching { info.loadLabel(context).toString() }.getOrNull()
                ?: return@mapNotNull null
            HdmiInput(id = info.id, label = label, intent = intent)
        }
    }
}
