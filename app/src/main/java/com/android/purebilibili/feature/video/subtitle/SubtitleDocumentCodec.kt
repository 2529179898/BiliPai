package com.android.purebilibili.feature.video.subtitle

import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.math.roundToLong

enum class SubtitleDocumentFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"), WEBVTT("vtt", "text/vtt"), SRT("srt", "application/x-subrip")
}

data class SubtitleDocumentIdentity(val bvid: String, val cid: Long)

/** Positive offsets delay subtitles; this is unrelated to dragging their on-screen position. */
fun subtitlePositionForOffset(positionMs: Long, offsetMs: Long): Long = positionMs - offsetMs

fun subtitleDocumentResultBelongsTo(request: SubtitleDocumentIdentity, current: SubtitleDocumentIdentity): Boolean =
    request == current && request.bvid.isNotBlank() && request.cid > 0L

fun decodeSubtitleDocument(bytes: ByteArray): String {
    val charset = when {
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }
    return bytes.toString(charset).removePrefix("\uFEFF")
}

private val cueTiming = Regex("^\\s*([0-9:.,]+)\\s*-->\\s*([0-9:.,]+)(?:\\s+.*)?$")
// Escape both literal braces for Android's ICU regex engine; this file also initializes during playback.
private val assOverrides = Regex("\\{[^}]*\\}")
private val textTags = Regex("<[^>]*>")

fun parseSubtitleDocument(fileName: String, source: String): List<SubtitleCue> {
    val raw = source.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n')
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    val cues = when {
        extension in setOf("json", "bcc") || raw.trimStart().startsWith("{") -> parseBiliSubtitleBody(raw)
        extension in setOf("ass", "ssa") || raw.contains("[Events]", ignoreCase = true) -> parseAss(raw)
        else -> parseTimedText(raw)
    }.filter { it.endMs > it.startMs && it.content.isNotBlank() }.sortedBy { it.startMs }
    require(cues.isNotEmpty()) { "未找到有效字幕；支持 JSON/BCC、WEBVTT、SRT、ASS" }
    return cues
}

private fun timestampMs(raw: String): Long {
    val parts = raw.trim().replace(',', '.').split(':')
    require(parts.size in 2..3) { "无效字幕时间" }
    val seconds = parts.last().toDouble()
    val minutes = parts[parts.lastIndex - 1].toLong()
    val hours = if (parts.size == 3) parts.first().toLong() else 0L
    require(seconds.isFinite() && seconds >= 0 && seconds < 60 && minutes >= 0 && (parts.size == 2 || minutes < 60) && hours >= 0)
    return hours * 3_600_000 + minutes * 60_000 + (seconds * 1000).roundToLong()
}

private fun plainText(text: String): String = text.replace(textTags, "")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&amp;", "&")

private fun parseTimedText(raw: String): List<SubtitleCue> {
    val lines = raw.lines()
    val result = mutableListOf<SubtitleCue>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index].trim()
        if (line.startsWith("NOTE") || line == "STYLE" || line == "REGION") {
            while (index < lines.size && lines[index].isNotBlank()) index++
            continue
        }
        val timing = cueTiming.matchEntire(line)
        if (timing == null) { index++; continue }
        index++
        val body = mutableListOf<String>()
        while (index < lines.size && lines[index].isNotBlank()) body += lines[index++]
        result += SubtitleCue(timestampMs(timing.groupValues[1]), timestampMs(timing.groupValues[2]), plainText(body.joinToString("\n")).trim())
    }
    return result
}

private fun parseAss(raw: String): List<SubtitleCue> {
    var events = false
    var fields = listOf("layer", "start", "end", "style", "name", "marginl", "marginr", "marginv", "effect", "text")
    return buildList {
        raw.lineSequence().forEach { sourceLine ->
            val line = sourceLine.trim()
            if (line.startsWith("[")) { events = line.equals("[Events]", true); return@forEach }
            if (!events) return@forEach
            if (line.startsWith("Format:", true)) {
                fields = line.substringAfter(':').split(',').map { it.trim().lowercase(Locale.ROOT) }
            } else if (line.startsWith("Dialogue:", true)) {
                val values = line.substringAfter(':').trimStart().split(',', limit = fields.size)
                if (values.size != fields.size) return@forEach
                fun field(name: String): String? = fields.indexOf(name).takeIf { it >= 0 }?.let { values[it] }
                val text = field("text") ?: return@forEach
                add(SubtitleCue(timestampMs(field("start") ?: return@forEach), timestampMs(field("end") ?: return@forEach),
                    text.replace(assOverrides, "").replace("\\N", "\n").replace("\\n", "\n").replace("\\h", " ").trim()))
            }
        }
    }
}

fun encodeSubtitleDocument(cues: List<SubtitleCue>, format: SubtitleDocumentFormat): String = when (format) {
    SubtitleDocumentFormat.JSON -> buildJsonObject {
        put("body", buildJsonArray {
            cues.forEach { cue -> add(buildJsonObject {
                put("from", cue.startMs / 1000.0); put("to", cue.endMs / 1000.0); put("content", cue.content)
            }) }
        })
    }.toString()
    SubtitleDocumentFormat.WEBVTT, SubtitleDocumentFormat.SRT -> buildString {
        if (format == SubtitleDocumentFormat.WEBVTT) append("WEBVTT\n\n")
        cues.forEachIndexed { index, cue ->
            if (format == SubtitleDocumentFormat.SRT) append(index + 1).append('\n')
            append(formatSubtitleTimestamp(cue.startMs, format)).append(" --> ")
                .append(formatSubtitleTimestamp(cue.endMs, format)).append('\n')
                .append(if (format == SubtitleDocumentFormat.WEBVTT) cue.content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") else cue.content)
                .append("\n\n")
        }
    }
}

private fun formatSubtitleTimestamp(ms: Long, format: SubtitleDocumentFormat): String {
    val time = ms.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d%s%03d", time / 3_600_000, time / 60_000 % 60,
        time / 1000 % 60, if (format == SubtitleDocumentFormat.SRT) "," else ".", time % 1000)
}
