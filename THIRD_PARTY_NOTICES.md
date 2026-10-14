# Third-party notices

## Telegram / NagramX ThanosEffect

The particle-dissolve shaders are imported unchanged from Telegram / NagramX.
The GLES3 transform-feedback renderer and TextureView/EGL lifecycle are ported
to BiliPai's Compose video-card integration.

Sources: [NagramX revision 2db685af](https://github.com/risin42/NagramX/tree/2db685af00a4352c877ecf96474cbf0494284715)
and [Telegram revision f2908b14](https://github.com/DrKLO/Telegram/tree/f2908b14133bbffbf7ab04f641ecb5bfaf533242).
Credit: Telegram and NagramX contributors.
License: GNU General Public License; NagramX distributes its source under GPL-3.0.
The port is distributed under this repository's [GPL-3.0 license](LICENSE).
Source paths, preserved behavior, and modifications are recorded in
[the port documentation](docs/telegram-thanos-port.md).

## HyperIsland

The liquid navigation indicator motion tuning and deformation formulas are adapted from
[HyperIsland](https://github.com/1812z/HyperIsland).

Copyright (c) 2026 1812z

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Nagram profile header

The centered avatar/name geometry, scroll-driven avatar scaling (24–96dp), name scaling
(1–1.12), 60% expanded-height snap threshold, 1000px/s upward-fling threshold, and
EASE_OUT_QUINT curve (0.23, 1, 0.32, 1) are adapted from
[NextAlone/Nagram ProfileActivity.java](https://github.com/NextAlone/Nagram/blob/fb56ea9801e5de68c60f0152b444bbe2a0fda520/TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java)
and its CubicBezierInterpolator.java at the same revision.

The active avatar/camera fusion rendering, GPU alpha-threshold composition and CPU
fallback are adapted from
[ProfileGooeyView.java](https://github.com/NextAlone/Nagram/blob/fb56ea9801e5de68c60f0152b444bbe2a0fda520/TMessagesProj/src/main/java/org/telegram/ui/Components/ProfileGooeyView.java),
with camera cutout parsing from
[NotchInfoUtils.java](https://github.com/NextAlone/Nagram/blob/fb56ea9801e5de68c60f0152b444bbe2a0fda520/TMessagesProj/src/main/java/org/telegram/messenger/NotchInfoUtils.java).
The Telegram native bitmap blur dependency is replaced by a separable triangular
kernel for the small software buffer. View lifecycle and Compose hosting are adapted
to this application.
The app-owned Compose implementations are `ProfileIdentityHeader.kt`,
`ProfileHeaderMotion.kt`, `ProfileAvatarEffect.kt`, `ProfileAvatarFusionView.java`
and `ProfileCameraCutout.java`.

Upstream Telegram source copyright Nikolai Kudashov, 2013–2018, licensed under
GPL-2.0-or-later; Nagram modifications by Nagram contributors, distributed under GPL-3.0.
This Compose adaptation is distributed under this repository's [GPL-3.0 license](LICENSE).
It uses BiliPai account data, colors, badges, editing and navigation actions.
See [port details](docs/nagram-profile-header-port.md).
