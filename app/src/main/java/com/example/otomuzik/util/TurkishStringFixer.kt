package com.example.otomuzik.util

object TurkishStringFixer {
    fun fix(input: String?): String {
        if (input.isNullOrBlank() || input == "<unknown>") return input ?: ""
        var fixed = input
        
        fixed = fixed
            .replace('ý', 'ı')
            .replace('Ý', 'İ')
            .replace('þ', 'ş')
            .replace('Þ', 'Ş')
            .replace('ð', 'ğ')
            .replace('Ð', 'Ğ')

        fixed = fixed
            .replace("Ã§", "ç").replace("Ã‡", "Ç")
            .replace("Ã¶", "ö").replace("Ã–", "Ö")
            .replace("Ã¼", "ü").replace("Ãœ", "Ü")
            .replace("Ã½", "ı").replace("Ä±", "ı")
            .replace("Ã\u009E", "Ş").replace("ÅŸ", "ş")
            .replace("Åž", "Ş")
            .replace("Ã\u0090", "Ğ").replace("ÄŸ", "ğ")
            .replace("Äž", "Ğ")
            .replace("Ä°", "İ")
            .replace("Ã¾", "ş")
            .replace("Ã°", "ğ")
            .replace("Ã\u009D", "İ")

        fixed = fixed
            .replace("â€™", "'").replace("â€˜", "'")
            .replace("â€œ", "\"").replace("â€\u009D", "\"").replace("â€", "\"")
            .replace("â€“", "-").replace("â€”", "-")

        fixed = fixed
            .replace("莽", "ç")
            .replace("谋", "ı")
            .replace("陌", "İ")
            .replace("脟", "Ç")
            .replace("脰", "Ö")
            .replace("脺", "Ü")
            .replace("脻", "ü")
            .replace("枚", "ö")
            .replace("艧", "ş")
            .replace("艩", "Ş")
            .replace("臒", "ğ")
            .replace("臑", "Ğ")
            .replace("ÅŸ", "ş")

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
