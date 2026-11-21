# ![Iman logo](app/src/main/res/raw/readme_icon.png) Iman

Iman is an Android app for prayer times, Quran reading, and Tasbih. It focuses on reliable daily-prayer reminders, a customizable widget, and a simple Quran-first experience.

## Features

🕌 **Prayer times, right on your home screen**
See today's five prayers, what's happening now, and what's coming next. Your times work even without internet. Fine-tune any prayer by a few minutes, use your location or set your own, and see extra times like sunrise and midnight.

🔔 **Reminders you can count on**
Get a heads-up before each prayer and when it's time. Pick the sound you like for each one: a gentle default, the adhan, the takbir, or silence.

📱 **A widget for your home screen**
Keep the five prayers and a countdown in front of you. Choose what you want to see, and pick the colors that feel right.

🧭 **Qibla finder**
Point your phone toward Makkah. The compass steadies itself, and it'll let you know when it needs a quick calibration.

📖 **Read the Quran**
Choose the Uthmani or IndoPak script. English and Indonesian translations come built in, with more to download. Search, bookmark your place, and listen to each ayah.

📿 **Tasbih counter**
Count your dhikr in a simple, focused way. Tap anywhere or use the volume buttons, and make it yours with your own colors.

🌍 **Your language**
Iman follows your phone's language, or you can choose one in Settings. It's available in English and Indonesian.

## Privacy

Iman has no accounts, no ads, and no analytics or tracking libraries. Your location stays on your phone, except when a calculation convention uses the AlAdhan online service: then your coordinates are sent to it to get the times. The app also contacts outside servers to stream ayah audio (EveryAyah), download translations, and check for updates (GitHub).

## Credits

Full credits and license terms are in [`third_party/NOTICES.md`](third_party/NOTICES.md) and in the app under Settings > About > Credits.

- **Original project:** [Khushu](https://github.com/greykaizen/khushu), GPLv3. Iman is a modified version (2026), independent, and not affiliated with Khushu.
- **IndoPak Quran text and font:** [QuranWBW.com](https://www.QuranWBW.com), used with prior written permission, for Quran text only. Terms: [`QuranWBW-NOTICE.txt`](third_party/fonts/QuranWBW-NOTICE.txt).
- **Uthmani Quran text and font:** King Fahd Glorious Quran Printing Complex (KFGQPC), via the [Quranic Universal Library](https://qul.tarteel.ai/). Terms: [`QPC-HAFS-NOTICE.txt`](third_party/fonts/QPC-HAFS-NOTICE.txt).
- **Translations:** [QuranEnc.com](https://quranenc.com) (bundled English and Indonesian editions, re-published under its terms). Downloadable translations from the [Free Quran API](https://github.com/fawazahmed0/quran-api) by Fawaz Ahmed. Each translation keeps its own copyright.
- **Fonts:** Noto Sans Arabic and Plus Jakarta Sans, under the SIL Open Font License 1.1.
- **Audio:** adhan and takbir from the [Internet Archive](https://archive.org/details/adhan.recordings.from.doha.qatar), marked [Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/). Ayah audio streamed from [EveryAyah](https://everyayah.com).
- **Prayer times:** [adhan2](https://github.com/batoulapps/adhan-kotlin) library, and optional [AlAdhan](https://aladhan.com) API.
- **Logo:** original artwork created for this project.

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
