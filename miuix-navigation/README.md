# Bundled Miuix navigation (Android)

Source: compose-miuix-ui/miuix, miuix-nav-android 0.9.4-5c91d5e5-SNAPSHOT.
The exact published sources are bundled under the upstream Apache-2.0 license.
Android expect/actual declarations are resolved to ordinary Android functions.

Local changes: optional per-host predictive-back release policy, frozen for each
session; peak and applied (anchored) progress tracking. BiliPai enables this only for
video card returns. Floating cards can accept completion or cancellation signals on
release; explicit expansion back toward detail cancels. The opt-in adapter drains
queued progress before cancellation decisions, and stale session work cannot drive
or pop a newer gesture. Other routes retain the upstream cancellation/completion
behavior, and discrete back actions still complete immediately.

The app uses this module instead of the published miuix-nav artifact so the patch
is reproducible in CI and local builds. Keep the other Miuix modules at the matching
version when updating these sources.

Upgrade review (2026-10-02): the pinned `5c91d5e5` source is nine commits after
`v0.9.4`, including the navigation focus fix. The three subsequent upstream main
commits do not modify `miuix-nav`; changing to `0.9.4` would roll back newer fixes.
The app uses Miuix's entry lifecycle and ViewModel stores, so it does not need
`lifecycle-viewmodel-navigation3`. The app's `navigation3` package name is a
historical name, not an AndroidX runtime dependency.

See [the navigation upgrade review](UPGRADE_REVIEW.md)
for verified release claims, deep-link integration, and validation limits.

## Video-card depth integration

The app follows the covered-entry ownership in the upstream
[CrossActivityTransition example](https://github.com/compose-miuix-ui/miuix/blob/main/example/shared/src/commonMain/kotlin/navigation/CrossActivityTransition.kt)
and the fixed backdrop placement in
[AppContent](https://github.com/compose-miuix-ui/miuix/blob/main/example/shared/src/commonMain/kotlin/AppContent.kt).
It retains its video-card bounds morph rather than copying the full cross-activity animation.

The video transition transforms the entire covered navigation entry once, centered at 97% at
full depth, with a 24dp rounded clip. Inner snapshot/chrome effects retain zero scale reduction.
Reduced motion keeps the entry at full size. The detached source snapshot uses the same scale,
shape and visual progress as the covered entry; it stops drawing once the source renderer is ready.

During a video-card session the fixed opaque backing belongs to the app host, below the retained
snapshot. NavDisplay's internal backdrop is unspecified for this session so it cannot cover the
snapshot. Other navigation transitions retain the upstream backdrop. Global wallpaper remains
root-owned without an additional depth scale. Related-video sources retain their real player entry
because a Compose GraphicsLayer recording cannot include its Surface video content.

Video-to-video scale gaps extend the black player/window canvas instead of exposing a white
theme-surface frame. Native card transitions no longer apply the legacy secondary-body 88%
yield scale or translation. The player's fixed system-bar inset is cropped out while floating;
it is restored over the last 10% of expansion, using deferred layout reads shared with the
cover and viewport height. Covered parent details do not follow the child's body-yield animation.

Now-playing capsules use host-space bounds only as outer navigation metadata. Their inner media
frame starts at zero and compensates dimensions once; frozen bitmap/layer pixels are fitted to
that measured frame without another inverse transform. Cover, real player and capsule snapshot
share this target even when the bar is icon-only or its bitmap capture is unavailable.
