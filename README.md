# Iman

Iman is an Android app for prayer times, Quran reading, and Tasbih. It focuses on reliable daily-prayer reminders, a customizable widget, and a simple Quran-first experience.

## Features

- **Prayer times:** the five daily prayers with the current and next prayer and the Hijri date. About 25 calculation conventions, the Asr school (Standard or Hanafi), per-prayer adjustments of up to 15 minutes, GPS or manual location, and optional extra timings (Imsak, Sunrise, Sunset, Midnight, night thirds). Pull down on Home to refresh.
- **Reminders:** prayer-time and pre-prayer reminders that keep working after a reboot, an app update, or a time or timezone change. Choose a sound for each prayer: default, adhan, takbir, or silent.
- **Widget:** the five prayers with show/hide switches for every item, 24-hour times, an `HH:mm` countdown, and customizable colors, including the active-prayer indicator.
- **Qibla compass:** corrected for magnetic declination and smoothed, with a calibration hint.
- **Quran:** Uthmani and IndoPak scripts, search, sortable bookmarks, an optional reminder when you leave the reader without a bookmark, ayah-by-ayah audio with retry, and downloadable translations.
- **Tasbih:** dhikr collections and an immersive counter with two bead styles (slide or press), customizable background and bead colors, and volume-key counting.
- **Languages:** system language or a choice in Settings. English for now, and more can be added.

## Privacy

Iman has no accounts, no ads, and no analytics or tracking libraries. Your location stays on your phone, except when a calculation convention uses the AlAdhan online service: then your coordinates are sent to it to get the times. The app also contacts outside servers to stream ayah audio (EveryAyah), download translations, and check for updates (GitHub).

## Notes

- Set Iman's battery use to Unrestricted and allow Alarms & reminders, or reminders may be late.
- Some conventions (for example Indonesia, Turkey, and Malaysia) have no offline version and need internet.
- Iman is free, with no ads and nothing behind a payment. This is a condition of the QuranWBW permission below.

## Credits

The same credits are in the app under Settings > About > Credits. Full terms for every asset are in [`third_party/NOTICES.md`](third_party/NOTICES.md).

### Origin

Iman is a modified version of [Khushu](https://github.com/greykaizen/khushu), whose source code is licensed under GPLv3. This version was modified in 2026. Iman is an independent project. It is not affiliated with or endorsed by the Khushu project, and the Khushu name and logo are not used.

### IndoPak Quran text and font: QuranWBW

The IndoPak (Hanafi) Quran text and the AlQuranNeo font come from **QuranWBW.com** (https://www.QuranWBW.com), prepared by Ayman Siddiqui and R. Siddiqua exclusively for the service of the Noble Qur'an as a Sadaqah Jariyah initiative.

**Prior written permission was obtained from QuranWBW.com to use the original, unmodified Quran text and font in this app. The permission is restricted to displaying the Quran text only.** It is conditional on the app staying free, with no ads and no paywall. The text and font are not modified, and the font is used only for IndoPak ayah text.

- **Copyright holders and sources:** © Ayman Siddiqui, © Al-Qalam and Ghandhara Software, © King Fahd Glorious Quran Printing Complex (KFGQPC).
- **Quran text sources:** KFGQPC, TypeMyBook.com (Ata Rafi), InPage Quran Publishing Resources.
- **Font development:** Ayman Siddiqui and R. Siddiqua, the Al-Qalam Font Project, Ghandhara Software, KFGQPC.
- **Verification and quality assurance:** Hafiz Muhammad Zain Viqar, Content Lead at Deen App, and his team of Huffuz: Hafiz Muavia bin Muhammad Haneef, Hafiz Ali Ahmed bin Aftab Ahmed, and Hafiz Abdul Rehman bin Ramzan.
- **Special thanks:** Abdul Majeed Khan, Arif Karim, Shakir-ul-Qadree, and Jawad (Al Qalam Quran Majeed font), and all contributors who directly or indirectly assisted in the digitization, preservation, verification, testing, and enhancement of Quranic text and typography.

These are not covered by this project's GPLv3 license. Terms: [`third_party/fonts/QuranWBW-NOTICE.txt`](third_party/fonts/QuranWBW-NOTICE.txt).

### Uthmani Quran text and font: KFGQPC

The Uthmani script uses the **QPC Hafs** text (Ayah by Ayah) and the **KFGQPC Uthmanic Hafs** font (`uthmanic_hafs_v2_2.ttf`), published by the King Fahd Glorious Quran Printing Complex (KFGQPC) and obtained from the [Quranic Universal Library (QUL)](https://qul.tarteel.ai/): [text](https://qul.tarteel.ai/resources/quran-script/86), [font](https://qul.tarteel.ai/resources/font/245). They are used unmodified and only for Quran text.

They remain the property of KFGQPC and are not covered by this project's GPLv3 license. Terms: [`third_party/fonts/QPC-HAFS-NOTICE.txt`](third_party/fonts/QPC-HAFS-NOTICE.txt).

### Other fonts

- **Noto Sans Arabic** (Arabic text outside the Quran script): © The Noto Project Authors, SIL Open Font License 1.1.
- **Plus Jakarta Sans** (interface): © The Plus Jakarta Sans Project Authors (Tokotype), SIL Open Font License 1.1.

License texts are in [`third_party/fonts/`](third_party/fonts/).

### Audio

The adhan and takbir sounds come from **"Adhan Recordings from Doha, Qatar"** on the Internet Archive, marked [Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/). The takbir is a shortened excerpt of the same recording. Source: https://archive.org/details/adhan.recordings.from.doha.qatar (accessed 2026-11-07). Details: [`third_party/audio/ADHAN-NOTICE.txt`](third_party/audio/ADHAN-NOTICE.txt).

Ayah-by-ayah recitation is streamed from EveryAyah (everyayah.com).

### Data and libraries

- **Prayer times:** the `adhan2` library, with an optional AlAdhan API mode (aladhan.com).
- **Translations:** the [Free Quran API](https://github.com/fawazahmed0/quran-api) by Fawaz Ahmed (Unlicense), through the jsDelivr CDN. Each translation keeps its own translator's or publisher's copyright.
- **Libraries:** listed in `app/build.gradle.kts`, each under its own license.

### Logo

The Iman logo and icons are original artwork created for this project.

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Before building, place the two Quran fonts in `app/src/main/res/font/`: `quran_neo_arabic.ttf` (the AlQuranNeo font from the IndoPak package at QuranWBW.com) and `uthmanic_hafs_v2_2.ttf` (KFGQPC, from QUL). Release builds are split by ABI, with a universal APK as well. Use `arm64-v8a` for most modern phones.

## License

This project's source code is licensed under the GNU General Public License v3.0 (GPLv3). See [LICENSE](LICENSE). Third-party fonts, text, audio, and other assets have their own terms and are not covered by it. See [Credits](#credits).

### Trademark & Brand Policy

**The Iman name, logo, and original brand identity are not included in the software license.**

The source code is released under the GNU General Public License v3.0 (GPLv3). This license grants permission to use, modify, and redistribute the code under its terms. However, it does not grant permission to use the Iman name, logo, or other brand assets.

- **Name and Logo:** The Iman name, logo, and original branding assets may not be used in a way that suggests endorsement, affiliation, or that a modified version is an official Iman release without prior written permission.
- **Forks and Derivative Projects:** If you distribute a modified version, please use a different project name, logo, and visual identity to distinguish your version from the official Iman project.
- **Attribution:** You may make factual references to Iman when identifying the original project or acknowledging its source, provided that such references do not imply official affiliation or endorsement.
- **Third-Party Assets:** Assets belonging to third parties remain subject to their respective copyright and license terms.

All rights in the Iman branding and original assets remain with their respective rights holders. No trademark or branding rights are granted by the software license.

This policy does not modify or restrict the rights granted under the GPLv3 for the software source code.
