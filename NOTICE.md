# Third-party notices

Hawkeye itself is licensed under BSD-3-Clause; see [LICENSE](LICENSE).
This file lists the third-party components Hawkeye builds against or ships, and reproduces their license terms.

| Component                              | License                | How it is used                          |
| -------------------------------------- | ---------------------- | --------------------------------------- |
| raylib 5.5                             | zlib/libpng            | Fetched at build time (CMake FetchContent) |
| MAVLink generated C library (`c_library_v2`) | MIT (generator output exception) | Git submodule at `lib/c_library_v2` |
| Vehicle 3D models in `models/`         | BSD-3-Clause           | Derived from PX4/jMAVSim assets          |
| JetBrains Mono, Inter                  | SIL OFL 1.1            | Bundled fonts in `fonts/`                |

## raylib

<https://github.com/raysan5/raylib> — zlib/libpng license.

```
Copyright (c) 2013-2024 Ramon Santamaria (@raysan5)

This software is provided "as-is", without any express or implied warranty. In no event
will the authors be held liable for any damages arising from the use of this software.

Permission is granted to anyone to use this software for any purpose, including commercial
applications, and to alter it and redistribute it freely, subject to the following restrictions:

  1. The origin of this software must not be misrepresented; you must not claim that you
  wrote the original software. If you use this software in a product, an acknowledgment
  in the product documentation would be appreciated but is not required.

  2. Altered source versions must be plainly marked as such, and must not be misrepresented
  as being the original software.

  3. This notice may not be removed or altered from any source distribution.
```

## MAVLink generated C library

<https://github.com/mavlink/c_library_v2> — generated output of the MAVLink generator.

The MAVLink generator is (L)GPLv3, but its [COPYING](https://github.com/mavlink/mavlink/blob/master/COPYING) file grants an explicit exception placing the generated code, and any binary that embeds it, under the MIT license reproduced below. Hawkeye embeds only generated code, so this is the license that applies.

```
Permission is hereby granted, free of charge, to any person obtaining a copy
of the generated software (the "Generated Software"), to deal
in the Generated Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Generated Software, and to permit persons to whom the Generated
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Generated Software.

THE GENERATED SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE GENERATED SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE GENERATED SOFTWARE.
```

## Vehicle 3D models

The `.obj` and `.mtl` assets in `models/` are derived from [PX4/jMAVSim](https://github.com/PX4/jMAVSim), which is licensed BSD-3-Clause. The same terms as Hawkeye's own [LICENSE](LICENSE) apply to them.

## Fonts

JetBrains Mono and Inter are licensed under the SIL Open Font License 1.1.
The full license text, with both copyright notices, ships in [fonts/OFL.txt](fonts/OFL.txt) and is installed alongside the fonts in every Hawkeye package.
