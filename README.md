# Iman

Iman is an Android app for prayer times, Quran reading, and Tasbih. It focuses on reliable daily-prayer reminders, a customizable widget, and a simple Quran-first experience.

- **Repository:** https://github.com/farhun/iman
- **App ID:** `io.github.farhun.iman`

> Iman is a modified fork of the open-source Khushu project by *[https://github.com/greykaizen/khushu/]*. Original code license: *[LICENSE](LICENSE)*. The original project's name and logo are not used here.

## What's in the app

- **Home:** current and next prayer, today's five prayers, Hijri date, and a Qibla compass.
- **Tasbih:** dhikr counter and collections.
- **Quran:** surah list, reader, audio, search, and bookmarks.
- **Widget:** home-screen prayer times widget.
- **Settings:** prayer calculation, reminders and sounds, widget customization, appearance.

## What I changed from the original

### Widget
- Show/hide switch for every widget item.
- Shows only the five obligatory prayers: Fajr, Dhuhr, Asr, Maghrib, Isha.
- Countdown shown as `HH:mm`, and prayer times in 24-hour format.
- Active-prayer indicator is a pill behind the current prayer's time, with a customizable color (default `#7ED957`).
- The widget refreshes at each prayer time so the indicator moves on time.

### Home screen
- Removed: starry/astronomy visual, Events feature, Hijri month event browser, Mosque directory, the sunrise/sunset arc card, and the radio buttons and dots in Today's Prayers.
- Today's Prayers is display-only, and the Qibla button is always visible.
- Added a "NOW" label above the current prayer.
- The location name sits under "Today's Prayers" with a pin icon, and long names are truncated.
- Pull-to-refresh updates the GPS location (when GPS mode is on), the widget, and the reminders, and always stops the spinner.

### Qibla compass
- Fixed the bearing formula and added magnetic declination correction.
- Added smoothing and tilt handling so the needle stays stable.
- "N" and the north tick rotate with the compass; the needle is hidden when no sensor exists.
- Added a calibration hint (figure-8 motion) when the sensor reports low accuracy.

### Navigation and content
- Study contains only the Holy Quran. Learn, Hadith, and the Pray (Salah) section were removed.
- Bottom navigation: Home, Tasbih, Quran.

### Quran
- Search and a bookmark list with sorting (Latest first by default) and delete with confirmation.
- Bookmarked ayahs show a filled bookmark icon.
- Exit reminder: leaving the reader on an unbookmarked ayah shows a "Not bookmarked" dialog with a "Don't show this again" option, which can be turned back on in Settings > Appearance.
- Fixed a bookmark save/exit-check race.
- Ayah playback has a 15-second loading timeout, an error state, and a Retry button.
- Arabic script font changed to **QuranIndoPak** (see Credits).

### Prayer reminders and sounds
- Prayer times are saved per date and used as the single source for all reminders.
- Prayer-time reminders use `setAlarmClock()`; pre-prayer reminders use exact alarms, one per prayer.
- Declared `SCHEDULE_EXACT_ALARM`, with a prompt that opens the system setting when it's off.
- Each alarm schedules the next one before posting; everything reschedules after boot, app update, time or timezone change, and at midnight.
- Per-prayer sound choice for Fajr, Dhuhr, Asr, Maghrib, Isha, and the pre-prayer reminder: Default, Adhan, Takbir, or Silent. `adhan.mp3` and `takbir.mp3` are bundled in `res/raw`.

### Settings, About, and branding
- Removed the "History" item, the "Support" (crypto address) section, and the first-install welcome modal.
- Renamed the app to Iman, with a new app ID and repository, and set the version to 0.1.
- App logo and icons are being replaced.

## Known limitations / planned
- Arabic prayer names in Today's Prayers and Tasbih still use the system fallback font.
- Reminder timing depends on phone settings: set Iman's battery to Unrestricted and allow Alarms & reminders.
- Planned: a first-launch permission screen (location, notifications, exact alarms) and faster Home screen startup.
- Single-ayah playback still streams from the internet, even if the reciter was downloaded.

## Credits

### Quran script font
**AlQuran IndoPak by QuranWBW** (`QuranIndoPak`), made by Ayman Siddiqui for [QuranWBW.com](https://quranwbw.com). It is based on the Al Qalam Quran Majeed fonts (© Al Qalam, © Ghandhara), with ayah-number glyphs from KFGQPC. Credits embedded in the font: Abdul Majeed Khan, Arif Karim, Shakir-ul-Qadree, Jawad.

**Font license note:** the font's own notice says it was made for Sadaqa-e-Jaria (ongoing charity) and requires written notice to QuranWBW.com for modification, distribution, or development. Iman is free of charge and must not be sold with this font. Contact QuranWBW.com before distributing.

### Other
- The Uthmanic Hafs font is downloaded at runtime from the quran-android project when that script is selected.
- Prayer times use the `adhan2` library; optional API mode uses AlAdhan.

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release builds are split by ABI. Use `arm64-v8a` for most modern phones, or the universal APK if you're unsure.

## 📄 License & Brand Policy

**License:** This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
