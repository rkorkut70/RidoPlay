package com.example.otomuzik.util

import com.example.otomuzik.model.Song
import java.text.Collator
import java.util.Locale

/**
 * Türkçe karakter uyumlu alfabetik şarkı sıralama ve indeksleme yardımcısı.
 * Parça numaralarını (örn. "01 - ", "02. ", "03_") başlık kıyaslamasında temizler,
 * Türkçe harf sırasına (A, B, C, Ç, D, E, F, G, Ğ, H, I, İ, ... Z) tam uygunluk sağlar.
 */
object SongSortHelper {
    val TURKISH_LOCALE: Locale = Locale.forLanguageTag("tr-TR")

    val TURKISH_COLLATOR: Collator = Collator.getInstance(TURKISH_LOCALE).apply {
        strength = Collator.SECONDARY
    }

    // Türkçe + Latin alfabesi kanonik sıralaması
    val ALPHABET_ORDER: List<Char> = listOf(
        '#',
        'A', 'B', 'C', 'Ç', 'D', 'E', 'F', 'G', 'Ğ', 'H', 'I', 'İ',
        'J', 'K', 'L', 'M', 'N', 'O', 'Ö', 'P', 'Q', 'R', 'S', 'Ş',
        'T', 'U', 'Ü', 'V', 'W', 'X', 'Y', 'Z'
    )

    // Başlıktaki parça numarası ön ekleri ("01 - ", "01. ", "01_", "(01) ", "[01] ")
    private val TRACK_NUMBER_REGEX = Regex("""^["'(\[{]*(?:\d{1,3}[\s._\-–)]+)[\s._\-–]*""")
    private val LEADING_SYMBOLS_REGEX = Regex("""^["'(\[{]+""")

    /**
     * Başlığın başındaki tırnak, parantez ve parça numarası ön eklerini temizler.
     * Bu sayede "01 - Tarkan" veya "02. Sezen Aksu" gibi parçalar '#' yerine
     * gerçek şarkı harfiyle ('T', 'S') sıralanır ve indekslenir.
     */
    fun cleanTitle(title: String?): String {
        if (title.isNullOrBlank()) return ""
        val trimmed = title.trim()
        val stripped = trimmed.replaceFirst(TRACK_NUMBER_REGEX, "").trim()
        val withoutQuotes = (if (stripped.isNotEmpty()) stripped else trimmed)
            .replaceFirst(LEADING_SYMBOLS_REGEX, "")
            .trim()
        return if (withoutQuotes.isNotEmpty()) withoutQuotes else trimmed
    }

    /**
     * Şarkı başlığının alfabetik çubuk için ilk harfini döner.
     * Türkçe 'I' (noktasız) ve 'İ' (noktalı) ayrımını eksiksiz yapar.
     */
    fun getFirstLetter(title: String?): Char {
        val clean = cleanTitle(title)
        val firstChar = clean.firstOrNull() ?: return '#'
        val upperStr = firstChar.toString().uppercase(TURKISH_LOCALE)
        val upperChar = upperStr.firstOrNull() ?: '#'
        return when (upperChar) {
            in 'A'..'Z', 'Ç', 'Ğ', 'İ', 'I', 'Ö', 'Ş', 'Ü' -> upperChar
            else -> '#'
        }
    }

    /**
     * Şarkıları Türkçe alfabe düzeninde kıyaslar.
     */
    fun compareSongs(a: Song, b: Song): Int {
        val cleanA = cleanTitle(a.title)
        val cleanB = cleanTitle(b.title)
        val cmp = TURKISH_COLLATOR.compare(cleanA, cleanB)
        return if (cmp != 0) cmp else TURKISH_COLLATOR.compare(a.title, b.title)
    }

    /**
     * Şarkı listesini Türkçe alfabeye ve temizlenmiş başlıklara göre sıralar.
     */
    fun sortSongs(songs: List<Song>): List<Song> {
        return songs.sortedWith { a, b -> compareSongs(a, b) }
    }
}
