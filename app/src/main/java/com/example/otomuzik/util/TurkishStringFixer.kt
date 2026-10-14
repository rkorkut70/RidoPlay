package com.example.otomuzik.util

object TurkishStringFixer {
    fun fix(input: String?): String {
        if (input.isNullOrBlank() || input == "<unknown>") return input ?: ""
        var fixed = input

        // 1. İki karakterli Windows-1250 (Orta Avrupa) UTF-8 mojibake eşleşmeleri
        fixed = fixed
            .replace("Äą", "ı")
            .replace("Ä°", "İ")
            .replace("Äź", "ğ")
            .replace("Äž", "Ğ")
            .replace("Ĺź", "ş")
            .replace("Ĺž", "Ş")
            .replace("Ă§", "ç")
            .replace("Ă‡", "Ç")
            .replace("Ă¶", "ö")
            .replace("Ă–", "Ö")
            .replace("Ăź", "ü")
            .replace("Ăž", "ü")
            .replace("ĂĽ", "ü")
            .replace("Ă¼", "ü")
            .replace("Ăœ", "Ü")
            .replace("ĂŚ", "Ü")
            .replace("Ă\u009C", "Ü")

        // 2. İki karakterli Windows-1252 / ISO-8859-1 (Latin-1) UTF-8 mojibake eşleşmeleri
        fixed = fixed
            .replace("Ã§", "ç")
            .replace("Ã‡", "Ç")
            .replace("Ã¶", "ö")
            .replace("Ã–", "Ö")
            .replace("Ã¼", "ü")
            .replace("Ãœ", "Ü")
            .replace("Ã\u009C", "Ü")
            .replace("Ä±", "ı")
            .replace("ÄŸ", "ğ")
            .replace("ÅŸ", "ş")
            .replace("Åž", "Ş")
            .replace("Ã½", "ı")
            .replace("Ã\u009D", "İ")
            .replace("Ã¾", "ş")
            .replace("Ã\u009E", "Ş")
            .replace("Ã°", "ğ")
            .replace("Ã\u0090", "Ğ")

        // 3. Tek karakterli eski kod sayfası uyuşmazlıkları (ISO-8859-9 / Windows-1254 -> ISO-8859-1)
        fixed = fixed
            .replace('ý', 'ı')
            .replace('Ý', 'İ')
            .replace('þ', 'ş')
            .replace('Þ', 'Ş')
            .replace('ð', 'ğ')
            .replace('Ð', 'Ğ')

        // 4. Tipografik tırnak ve tire bozulmaları
        fixed = fixed
            .replace("â€™", "'").replace("â€˜", "'")
            .replace("â€œ", "\"").replace("â€\u009D", "\"").replace("â€", "\"")
            .replace("â€“", "-").replace("â€”", "-")

        // 5. Çince / GBK yanlış çözümleme haritaları
        fixed = fixed
            .replace("莽", "ç")
            .replace("谋", "ı")
            .replace("陌", "İ")
            .replace("脟", "Ç")
            .replace("脰", "Ö")
            .replace("脺", "Ü")
            .replace("脻", "ü")
            .replace("眉", "ü")
            .replace("鼁", "üz")
            .replace("黿", "üz")
            .replace("枚", "ö")
            .replace("艧", "ş")
            .replace("艩", "Ş")
            .replace("臒", "ğ")
            .replace("臑", "Ğ")

        return fixed
    }

    /**
     * Araç içi klavyelerde ve aramalarda Türkçe karakter uyumsuzluğunu (ç/c, ğ/g, ı/i, ö/o, ş/s, ü/u)
     * ve büyük/küçük harf farklarını ortadan kaldırarak arama terimlerini eşitler.
     */
    fun normalizeForSearch(input: String?): String {
        if (input.isNullOrBlank() || input == "<unknown>") return ""
        val fixed = fix(input)
        return fixed
            .replace('ı', 'i')
            .replace('İ', 'i')
            .replace('I', 'i')
            .replace('ş', 's')
            .replace('Ş', 's')
            .replace('ç', 'c')
            .replace('Ç', 'c')
            .replace('ğ', 'g')
            .replace('Ğ', 'g')
            .replace('ö', 'o')
            .replace('Ö', 'o')
            .replace('ü', 'u')
            .replace('Ü', 'u')
            .lowercase(java.util.Locale.ROOT)
            .trim()
    }
}
