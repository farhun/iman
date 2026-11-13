package com.kaizen.khushu.data.model

data class ReciterMeta(
    val id: String,
    val name: String,
    val style: String,
    val audioUrlPattern: String,
) {
    companion object {
        fun everyayahDefaults(): List<ReciterMeta> = listOf(
            ReciterMeta("everyayah_mishari", "Mishary Al-Afasy", "Hafs An Asim", "https://everyayah.com/data/Alafasy_128kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_sudais", "Abdul Rahman Al-Sudais", "Hafs An Asim", "https://everyayah.com/data/Abdurrahmaan_As-Sudais_192kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_husary", "Mahmoud Khalil Al-Husary", "Hafs An Asim", "https://everyayah.com/data/Husary_128kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_minshawi", "Mohamed Siddiq Al-Minshawi", "Hafs An Asim", "https://everyayah.com/data/Minshawy_Murattal_128kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_ghamdi", "Saad Al-Ghamdi", "Hafs An Asim", "https://everyayah.com/data/Ghamadi_40kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_basfar", "Abdullah Basfar", "Hafs An Asim", "https://everyayah.com/data/Abdullah_Basfar_192kbps/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_ajamy", "Ahmad Al-Ajamy", "Hafs An Asim", "https://everyayah.com/data/Ahmed_ibn_Ali_al-Ajamy_128kbps_ketaballah.net/{surah_3}{ayah_3}.mp3"),
            ReciterMeta("everyayah_tablawi", "Mohammed Al-Tablawi", "Hafs An Asim", "https://everyayah.com/data/Mohammad_al_Tablaway_128kbps/{surah_3}{ayah_3}.mp3"),
        )
    }
}
