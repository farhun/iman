# Iman

Iman is an Android app for prayer times, Quran reading, and Tasbih. It focuses on reliable daily-prayer reminders, a customizable widget, and a simple Quran-first experience.

- **Repository:** https://github.com/farhun/iman
- **App ID:** `io.github.farhun.iman`

## Features

- **Prayer times:** Fajr, Dhuhr, Asr, Maghrib, and Isha, with the current and next prayer and the Hijri date. Choose from about 25 calculation conventions, the Asr school (Standard or Hanafi), per-prayer adjustments of up to 15 minutes, and automatic (GPS) or manual location. Optional extra timings: Imsak, Sunrise, Sunset, Midnight, and the night thirds. Pull down on Home to refresh the location, prayer times, widget, and reminders.
- **Reminders:** prayer-time and pre-prayer reminders that keep working after a reboot, an app update, or a time or timezone change. Choose a sound for each prayer and for the pre-prayer reminder: default, adhan, takbir, or silent.
- **Widget:** a home-screen widget showing the five prayers, with show/hide switches for every item, a 24-hour display, an `HH:mm` countdown, and customizable colors, including the active-prayer indicator.
- **Qibla compass:** corrected for magnetic declination and smoothed so the needle stays steady, with a hint to calibrate the compass when the sensor reports low accuracy.
- **Quran:** a reader with two scripts (Uthmani and IndoPak), search, bookmarks that can be sorted and deleted, a reminder when you leave the reader without a bookmark (which can be turned off in Settings), ayah-by-ayah audio with a retry option when loading fails, and downloadable translations.
- **Tasbih:** dhikr collections and an immersive counter with two bead styles (slide or press). You can change the background and bead colors. Tap anywhere or use the volume keys to count.
- **Languages:** follows the system language, or choose one in Settings. English is available now, and more languages can be added.
- **Settings:** prayer calculation, reminders and sounds, widget, Tasbih screen, appearance, and language.

## Privacy

Iman has no accounts, no ads, and no analytics or tracking libraries. Your location stays on your phone, with one exception: if a calculation convention uses the AlAdhan online service, your coordinates are sent to that service to get the times. The app also contacts outside servers to stream ayah audio (EveryAyah), download translations, and check for updates (GitHub).

## Notes

- Reminder timing depends on phone settings. Set Iman's battery use to Unrestricted and allow Alarms & reminders.
- Some calculation conventions (for example Indonesia, Turkey, and Malaysia) have no offline version and need an internet connection.
- Ayah audio streams from the internet, so it needs a connection.
- Iman is free, has no ads, and has nothing locked behind a payment. This is also a condition of the Quran font permission below.

## Credits

All third-party assets and their terms are listed in [`third_party/NOTICES.md`](third_party/NOTICES.md).

### Original project
Khushu: https://github.com/greykaizen/khushu (GPLv3).

Iman is a modified version of the open-source [Khushu](https://github.com/greykaizen/khushu) project, whose source code is licensed under GPLv3. This version was modified in 2026.

Iman is an independent project. It is not affiliated with or endorsed by the original Khushu project, and the original name and logo are not used.

### IndoPak Quran text and font (QuranWBW)
The IndoPak (Hanafi) Quran text and the AlQuranNeo font come from the IndoPak package of **QuranWBW.com** (https://www.QuranWBW.com), prepared by Ayman Siddiqui and R. Siddiqua exclusively for the service of the Noble Qur'an and distributed as a Sadaqah Jariyah initiative.

**Prior written permission was obtained from QuranWBW.com to use the original, unmodified Quran text and font in this app. The permission is restricted to displaying the Quran text only,** on the conditions that the app stays free for all users (Sadaqa-e-Jaria), with no ads and no paywall, and that the credits below are shown. The font is used only for the IndoPak ayah text in the Quran reader, and the text and font are not modified.

Copyright holders and sources, as listed by QuranWBW:
- © Ayman Siddiqui
- © Al-Qalam and Ghandhara Software
- © King Fahd Glorious Quran Printing Complex (KFGQPC)

Quran text sources: King Fahd Glorious Quran Printing Complex (KFGQPC), TypeMyBook.com (Ata Rafi), and InPage Quran Publishing Resources.

Font development: Ayman Siddiqui and R. Siddiqua, the Al-Qalam Font Project, Ghandhara Software, and the King Fahd Glorious Quran Printing Complex (KFGQPC).

Verification and quality assurance: Hafiz Muhammad Zain Viqar, Content Lead at Deen App, and his team of Huffuz: Hafiz Muavia bin Muhammad Haneef, Hafiz Ali Ahmed bin Aftab Ahmed, and Hafiz Abdul Rehman bin Ramzan.

Special thanks: Abdul Majeed Khan, Arif Karim, Shakir-ul-Qadree, and Jawad (Al Qalam Quran Majeed font), and all contributors who directly or indirectly assisted in the digitization, preservation, verification, testing, and enhancement of Quranic text and typography.

The IndoPak text and font are not covered by this project's GPLv3 license, and they must not be sold, modified, or redistributed without attribution. The full terms are in [`third_party/fonts/QuranWBW-NOTICE.txt`](third_party/fonts/QuranWBW-NOTICE.txt). The font file is not included in this repository. When building from source, download the IndoPak package from QuranWBW.com and place its AlQuranNeo `.ttf` at `app/src/main/res/font/quran_neo_arabic.ttf`.

### Other fonts
- **Amiri Quran**, used for the Uthmani script: by Khaled Hosny and Sebastian Kosch, © The Amiri Project Authors (https://github.com/aliftype/amiri), SIL Open Font License 1.1.
- **Noto Sans Arabic**, used for Arabic text outside the Quran script: © The Noto Project Authors, SIL Open Font License 1.1.
- **Plus Jakarta Sans**, used for the interface: © The Plus Jakarta Sans Project Authors (Tokotype), SIL Open Font License 1.1.

The license text for each font is in [`third_party/fonts/`](third_party/fonts/).

### Audio files
The bundled adhan and takbir sounds come from **"Adhan Recordings from Doha, Qatar"** on the Internet Archive, which is marked with the [Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/).

- `adhan.mp3`: the Maghrib adhan from that item, renamed.
- `takbir.mp3`: a shortened excerpt of the same recording.
- Source: https://archive.org/details/adhan.recordings.from.doha.qatar (accessed 2026-11-07)

Public domain material needs no attribution. It is credited here for transparency. Details are in [`third_party/audio/ADHAN-NOTICE.txt`](third_party/audio/ADHAN-NOTICE.txt).

### Data sources and libraries
- Prayer times: the `adhan2` library, with an optional AlAdhan API mode (aladhan.com).
- Ayah-by-ayah audio: EveryAyah (everyayah.com).
- Translations: editions from the [Free Quran API](https://github.com/fawazahmed0/quran-api) by Fawaz Ahmed, whose repository is under the Unlicense, downloaded through the jsDelivr CDN. Each translation belongs to its own translator or publisher and keeps its own copyright.
- Uthmani Quran text: [add the source].
- Other libraries are listed in `app/build.gradle.kts`, each under its own license.

### Logo
The Iman logo and icons are original artwork created for this project.

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Before building, add the QuranWBW font as described in the credits. Release builds are split by ABI, with a universal APK as well. Use the `arm64-v8a` APK for most modern phones, or the universal APK if you're unsure.

## License

This project's source code is licensed under the GNU General Public License v3.0 (GPLv3). See [LICENSE](LICENSE) for details.

Third-party fonts, text, audio, and other assets have their own terms and are not covered by the GPLv3 license. See [Credits](#credits) and [`third_party/`](third_party/).
