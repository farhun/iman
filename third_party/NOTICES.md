# Third-party notices

This folder records the source and terms of everything in Iman that was not written for this project. The project's own source code is under GPLv3 (see the `LICENSE` file in the repository root). The items below are **not** covered by that license unless stated.

| Asset | Source | Terms | Details |
|---|---|---|---|
| Khushu source code (Iman is a modified version) | https://github.com/greykaizen/khushu | GPLv3 | Original name and logo are not used |
| IndoPak Quran text (`indopak.json`) and the AlQuranNeo font (`quran_neo_arabic.ttf`) | QuranWBW.com, IndoPak package | Written permission with conditions. Not GPLv3. No sale, no modification, no redistribution without attribution | `fonts/QuranWBW-NOTICE.txt` |
| Amiri Quran (font) | https://github.com/aliftype/amiri, also on Google Fonts | SIL Open Font License 1.1 | `fonts/AmiriQuran-OFL.txt` |
| Noto Sans Arabic (font) | Google Fonts / Noto Project | SIL Open Font License 1.1 | `fonts/NotoSansArabic-OFL.txt` |
| Plus Jakarta Sans (font) | Google Fonts / Tokotype | SIL Open Font License 1.1 | `fonts/PlusJakartaSans-OFL.txt` |
| `adhan.mp3`, `takbir.mp3` | Internet Archive, "Adhan Recordings from Doha, Qatar" | Public Domain Mark 1.0 | `audio/ADHAN-NOTICE.txt` |
| Ayah-by-ayah audio | EveryAyah, everyayah.com | Streamed, not bundled. See the site's terms | |
| Translations (downloadable) | Free Quran API by Fawaz Ahmed, https://github.com/fawazahmed0/quran-api, through cdn.jsdelivr.net | The repository is under the Unlicense. Each translation keeps its translator's or publisher's own copyright, and the terms of each edition are not checked here | |
| Prayer-time API (for some conventions) | AlAdhan, aladhan.com | See the service's terms. The app sends your coordinates to their server in this mode | |
| Libraries (`adhan2`, Jetpack Compose, Room, Media3, OkHttp, and others) | See `app/build.gradle.kts` and `gradle/libs.versions.toml` | Each library's own license | Not copied here |

The OFL license files come with each font's download. Keep each one next to the font notice, because the license asks that its text and the copyright statement travel with the font.

When you add a new font, sound, image, or data file that someone else made, add a row here and a notice file next to it.
