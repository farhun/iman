# Iman

Iman is an Android app for prayer times, Quran reading, and Tasbih. It focuses on reliable daily-prayer reminders, a customizable widget, and a simple Quran-first experience.

- **Repository:** https://github.com/farhun/iman
- **App ID:** `io.github.farhun.iman`

## Origin and notice

Iman is a modified version of the open-source [Khushu](https://github.com/greykaizen/khushu) project, whose source code is licensed under GPLv3. This version was modified in 2026.

Iman is an independent project. It is not affiliated with or endorsed by the original Khushu project, and the original name and logo are not used.

## Features

- **Prayer times:** Fajr, Dhuhr, Asr, Maghrib, and Isha, with the current and next prayer, the Hijri date, a choice of calculation methods, and automatic or manual location. Pull down on Home to refresh the location, prayer times, widget, and reminders.
- **Reminders:** prayer-time and pre-prayer reminders that keep working after a reboot, an app update, or a time or timezone change. Choose a sound for each prayer and for the pre-prayer reminder: default, adhan, takbir, or silent.
- **Widget:** a home-screen widget showing the five prayers, with show/hide switches for every item, a 24-hour display, an `HH:mm` countdown, and customizable colors, including the active-prayer indicator.
- **Qibla compass:** corrected for magnetic declination and smoothed so the needle stays steady, with a hint to calibrate the compass when the sensor reports low accuracy.
- **Quran:** a reader using the IndoPak script, with search, bookmarks that can be sorted and deleted, a reminder when you leave the reader without a bookmark (which can be turned off in Settings), ayah-by-ayah audio with a retry option when loading fails, and downloadable translations.
- **Tasbih:** dhikr collections, an immersive counter, and customizable beads.
- **Settings:** prayer calculation, reminders and sounds, widget, and appearance.

## Notes

- Reminder timing depends on phone settings. Set Iman's battery use to Unrestricted and allow Alarms & reminders.
- Ayah audio streams from the internet, so it needs a connection.

## Credits

All third-party assets and their terms are listed in [`third_party/NOTICES.md`](third_party/NOTICES.md).

### Original project
Khushu: https://github.com/greykaizen/khushu (GPLv3).

### Audio
The bundled adhan and takbir sounds come from **"Adhan Recordings from Doha, Qatar"** on the Internet Archive, which is marked with the [Public Domain Mark 1.0](https://creativecommons.org/publicdomain/mark/1.0/).

- `adhan.mp3`: the Maghrib adhan from that item, renamed.
- `takbir.mp3`: a shortened excerpt of the same recording.
- Source: https://archive.org/details/adhan.recordings.from.doha.qatar

Public domain material needs no attribution. It is credited here for transparency. Details and a record of the source are in [`third_party/audio/ADHAN-NOTICE.txt`](third_party/audio/ADHAN-NOTICE.txt).

### Quran script font
**AlQuran IndoPak by QuranWBW**, created by Ayman Siddiqui and R. Siddiqua for QuranWBW.com and Quran.com. Based on the Al Qalam Quran Majeed fonts, with ayah-number glyphs from KFGQPC. Provided for Sadaqa-e-Jaria (ongoing charity) purposes.

Source: https://github.com/marwan/indopak-quran-text

This font is **not** covered by this project's GPLv3 license. It keeps its own terms, which are reproduced in [`third_party/fonts/QuranIndoPak-NOTICE.txt`](third_party/fonts/QuranIndoPak-NOTICE.txt). It is used as downloaded, without modification, with attribution, in a free, non-commercial app.

[Permission status: written notice sent to QuranWBW.com on DATE. Add their reply here once received.]

### Data sources and libraries
- Prayer times: the `adhan2` library, with an optional AlAdhan API mode (aladhan.com).
- Ayah-by-ayah audio: EveryAyah (everyayah.com).
- Translations: Quran Foundation (quran.com) and QuranEnc (quranenc.com). [Add the fawazahmed0 API if the catalog output confirms it.] Bundled English and Urdu translations: [add translator names].
- [Add the source of the bundled Quran text.]

### Logo
[Confirm and edit: "The Iman logo and icons are original artwork created for this project." If the logo came from elsewhere, give the source and license instead.]

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release builds are split by ABI. Use `arm64-v8a` for most modern phones, or the universal APK if you're unsure.

## License

This project's source code is licensed under the GNU General Public License v3.0 (GPLv3). See [LICENSE](LICENSE) for details.

Third-party fonts, audio, and other assets have their own terms and are not covered by the GPLv3 license. See [Credits](#credits) and [`third_party/`](third_party/).
