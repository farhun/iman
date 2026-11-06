# Khushu

Khushu is an Android app for prayer times, Quran reading, and Tasbih. This is a customized fork focused on a simpler, more reliable daily-prayer experience.

- **Repository:** https://github.com/farhun/khushu
- **App ID:** `io.github.farhun.khushu` (code namespace remains `com.kaizen.khushu`)
- **Version:** 0.1

> Based on the original Khushu app by *[add original author / repo link here]*. *[Add license here.]*

## What's in the app

- **Home:** current and next prayer, today's five prayers, Hijri date, and a Qibla compass.
- **Tasbih:** dhikr counter and collections.
- **Quran:** surah list, reader, audio, search, and bookmarks.
- **Widget:** home-screen prayer times widget.
- **Settings:** prayer calculation, reminders and sounds, widget customization, appearance.

## What I changed from the original

### Widget
- Show/hide switch for every widget item.
- Shows only the five obligatory prayers: Fajr, Dhuhr, Asr, Maghrib, Isha (no Sunrise or extra timings).
- Countdown shown as `HH:mm` (no seconds).
- Prayer times shown in 24-hour format.
- Active-prayer indicator is a pill behind the current prayer's time, with a customizable color (default `#7ED957`).
- The widget refreshes at each prayer time, so the indicator moves on time.

### Home screen
- Removed: starry/astronomy visual, Events feature (including its settings and catalog), Hijri month event browser, Mosque directory, the sunrise/sunset arc card, and the radio buttons and colored dots in Today's Prayers.
- Today's Prayers is now display-only.
- The Explore section is gone; the Qibla button is always visible.
- Added a "NOW" label above the current prayer so it lines up with "NEXT PRAYER".
- The location name moved under the "Today's Prayers" heading with a pin icon, and long names are truncated.
- Pull-to-refresh now updates the GPS location when GPS mode is on, refreshes the widget, reschedules reminders, and always stops the spinner.

### Qibla compass
- Fixed the bearing formula.
- Added magnetic declination correction (true north).
- Added circular smoothing and a continuous tilt-handling method so the needle stays stable when the phone moves.
- "N" and the north tick rotate with the compass.
- The needle is hidden when no compass sensor is available.
- Added a calibration hint (figure-8 motion) when the sensor reports low accuracy.

### Navigation and content
- Study now contains only the Holy Quran. Learn and Hadith were removed.
- The Pray (Salah) section was removed completely.
- Bottom navigation: Home, Tasbih, Quran.

### Quran
- Search and a bookmark list on the surah list screen.
- Bookmark list: delete button with confirmation dialog, and sorting by Latest first (default) or Oldest first.
- Bookmarked ayahs show a filled bookmark icon, and unbookmarked ones show an outline.
- Exit reminder: leaving the reader on an unbookmarked ayah shows a "Not bookmarked" dialog with a "Don't show this again" checkbox. It can be turned back on in Settings > Appearance.
- Fixed a bookmark save/exit-check race that made the dialog appear after bookmarking.
- Ayah playback has a 15-second loading timeout, an error state, and a Retry button.
- Arabic script font changed to **QuranIndoPak** (see Credits).

### Prayer reminders and sounds
- Prayer times are saved per date and used as the single source for all reminders, so reminders match what the app shows.
- Prayer-time reminders use `setAlarmClock()`. Pre-prayer reminders use exact alarms with separate alarms per prayer.
- Declared `SCHEDULE_EXACT_ALARM`, with a notification prompt that opens the system setting when exact alarms are off.
- Each alarm schedules the next one before posting its notification, and everything reschedules after boot, app update, time change, timezone change, and at midnight.
- Per-prayer sound choice for Fajr, Dhuhr, Asr, Maghrib, Isha, and the pre-prayer reminder: Default, Adhan, Takbir, or Silent. `adhan.mp3` and `takbir.mp3` are bundled in `res/raw`.

### Settings and About
- Removed the "History" item and the "Support Khushu" (crypto address) section.
- All repository links point to https://github.com/farhun/khushu, including Report issue and the update check.
- The Settings sheet now shows only the info button and version number, at the bottom.
- Removed the first-install welcome modal.
- Changed the installed app ID to `io.github.farhun.khushu` and the version to 0.1.

## Known limitations / planned
- Arabic prayer names in Today's Prayers and Tasbih still use the system fallback font, not QuranIndoPak.
- Reminder timing still depends on phone settings: set Khushu's battery to Unrestricted and allow Alarms & reminders.
- Planned: a first-launch permission screen (location, notifications, exact alarms) and faster Home screen startup.
- Single-ayah playback still streams from the internet, even if the reciter was downloaded.

## Credits

### Quran script font
**AlQuran IndoPak by QuranWBW** (`QuranIndoPak`), made by Ayman Siddiqui for [QuranWBW.com](https://quranwbw.com). It is based on the Al Qalam Quran Majeed fonts (© Al Qalam, © Ghandhara), with ayah-number glyphs from KFGQPC. Credits embedded in the font: Abdul Majeed Khan, Arif Karim, Shakir-ul-Qadree, Jawad.

**Font license note:** the font's own notice states it was made for Sadaqa-e-Jaria (ongoing charity) and requires written notice to QuranWBW.com for modification, distribution, or development. This app is not sold and is free of charge. Anyone redistributing it should contact QuranWBW.com first and must not charge for an app that includes this font.

### Other
- Quran script fallback font: Scheherazade New is no longer used by the Quran reader.
- Uthmanic Hafs font is downloaded at runtime from the quran-android project (`hafs_newandroid.ttf`) when that script is selected.
- Prayer time calculations use the `adhan2` library; optional API mode uses AlAdhan.
- *[Add text source credit for the IndoPak Quran text (QuranWBW) if you use it, plus the original app's credits.]*

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

Release builds are split by ABI. Use `arm64-v8a` for most modern phones, or the universal APK if you're unsure.

## 📄 License & Brand Policy

**License:** This project is licensed under the **GNU General Public License v3.0 (GPLv3)**. See the [LICENSE](LICENSE) file for details.
