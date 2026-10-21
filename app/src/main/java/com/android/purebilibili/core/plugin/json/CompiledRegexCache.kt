package com.android.purebilibili.core.plugin.json

/** Patterns are content keys: editing/deleting a rule cannot reuse another rule's matcher.
 * Invalid patterns are cached too; malformed rules must not throw once per video/danmaku.
 */
internal class CompiledRegexCache(
    private val capacity: Int = 128,
    private val compile: (String) -> Regex = { Regex(it) },
) {
    private val entries = LinkedHashMap<String, Regex?>(16, 0.75f, true)

    @Synchronized
    fun get(pattern: String): Regex? {
        if (entries.containsKey(pattern)) return entries[pattern]
        val compiled = try { compile(pattern) } catch (_: IllegalArgumentException) { null }
        entries[pattern] = compiled
        while (entries.size > capacity.coerceAtLeast(1)) {
            entries.remove(entries.keys.first())
        }
        return compiled
    }
}
