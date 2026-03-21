package com.nguyendevs.ecolens.utils

import com.nguyendevs.ecolens.api.GeminiContent
import com.nguyendevs.ecolens.api.GeminiPart
import com.nguyendevs.ecolens.managers.setting.LanguageManager

/** Tiện ích khởi tạo các chuỗi lệnh truy vấn gửi tới hệ thống AI. */
object PromptBuilder {

    fun buildCommonNamePrompt(scientificName: String, languageCode: String): String {
        return when (languageCode) {
            LanguageManager.LANG_VI ->
                    """
                Provide the most common name for "$scientificName" in Vietnamese.
                Return JSON only: {"commonName": "Tên Tiếng Việt"}
                RETURN ONLY JSON, NO MARKDOWN.
            """.trimIndent()
            LanguageManager.LANG_CN ->
                    """
                Provide the most common name for "$scientificName" in Simplified Chinese.
                Return JSON only: {"commonName": "Name"}
                RETURN ONLY JSON, NO MARKDOWN.
            """.trimIndent()
            LanguageManager.LANG_JP ->
                    """
                Provide the most common name for "$scientificName" in Japanese.
                Return JSON only: {"commonName": "Name"}
                RETURN ONLY JSON, NO MARKDOWN.
            """.trimIndent()
            else ->
                    """
                Provide the most common name for "$scientificName" in English.
                Return JSON only: {"commonName": "Name"}
                RETURN ONLY JSON, NO MARKDOWN.
            """.trimIndent()
        }
    }

    fun buildDetailsPrompt(scientificName: String, languageCode: String): String {
        return when (languageCode) {
            LanguageManager.LANG_VI -> buildVietnameseDetailsPrompt(scientificName)
            LanguageManager.LANG_CN -> buildChineseDetailsPrompt(scientificName)
            LanguageManager.LANG_JP -> buildJapaneseDetailsPrompt(scientificName)
            else -> buildEnglishDetailsPrompt(scientificName)
        }
    }

    fun buildConservationPrompt(
            scientificName: String,
            iucnCode: String,
            languageCode: String
    ): String {
        val codeToUse = iucnCode.trim()
        val shouldSearch = codeToUse.isBlank() || codeToUse.equals("NE", ignoreCase = true)

        return when (languageCode) {
            LanguageManager.LANG_VI ->
                    buildVietnameseConservationPrompt(scientificName, codeToUse, shouldSearch)
            LanguageManager.LANG_CN ->
                    buildChineseConservationPrompt(scientificName, codeToUse, shouldSearch)
            LanguageManager.LANG_JP ->
                    buildJapaneseConservationPrompt(scientificName, codeToUse, shouldSearch)
            else -> buildEnglishConservationPrompt(scientificName, codeToUse, shouldSearch)
        }
    }

    fun buildTaxonomyTranslationPrompt(
            kingdom: String,
            phylum: String,
            className: String,
            taxorder: String,
            family: String,
            genus: String,
            species: String
    ): String {
        return """
            Translate the following biological taxonomic terms into the most accurate Vietnamese possible.
            If an exact Vietnamese name is unavailable, retain the scientific name or use a suitable transliteration.
            
            Input:
            Kingdom: $kingdom
            Phylum: $phylum
            Class: $className
            Order: $taxorder
            Family: $family
            Genus: $genus
            Species: $species
            
            Returns a unique JSON file with the corresponding keys. (kingdom, phylum, className, taxorder, family, genus, species).
            Example: {"kingdom": "Thực vật", "phylum": "Ngọc lan", ...}
            
            RETURN ONLY JSON. NO MARKDOWN.
        """.trimIndent()
    }

    private fun buildVietnameseDetailsPrompt(scientificName: String): String =
            """
        Provide detailed information about "$scientificName" in Vietnamese.
        
        FORMAT RULES:
        • Use **text** to bold important keywords
        • Use ##text## to green highlight places, names, measurements
        • Use • for bullet points
        
        JSON FORMAT:
        {
          "description": "Comprehensive 4-5 sentence overview. Use **bold** and ##green## for key features, places and measurements.",
          "characteristics": "Bullet list, each line starts with •:\n• Body morphology\n• Body structure\n• Size dimensions (use ##measurements##)\n• Colors\n• Identifying features\n• Special biological traits",
          "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()

    private fun buildVietnameseConservationPrompt(
            scientificName: String,
            codeToUse: String,
            shouldSearch: Boolean
    ): String {
        return if (!shouldSearch) {
            """
            Analyze IUCN conservation status "$codeToUse" for species "$scientificName" in Vietnamese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** $codeToUse (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        } else {
            """
            Determine the IUCN conservation status for species "$scientificName" in Vietnamese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** [Found IUCN Code] (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }

    // ==================== ENGLISH PROMPTS ====================

    private fun buildEnglishDetailsPrompt(scientificName: String): String =
            """
        Provide detailed information about "$scientificName" in English.
        
        FORMAT RULES:
        • Use **text** to bold important keywords
        • Use ##text## to green highlight places, names, measurements
        • Use • for bullet points
        
        JSON FORMAT:
        {
          "description": "Comprehensive 4-5 sentence overview. Use **bold** and ##green## for key features, places and measurements.",
          "characteristics": "Bullet list, each line starts with •:\n• Body morphology\n• Body structure\n• Size dimensions (use ##measurements##)\n• Colors\n• Identifying features\n• Special biological traits",
          "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()

    private fun buildEnglishConservationPrompt(
            scientificName: String,
            codeToUse: String,
            shouldSearch: Boolean
    ): String {
        return if (!shouldSearch) {
            """
            Analyze IUCN conservation status "$codeToUse" for species "$scientificName" in English.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** $codeToUse (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        } else {
            """
            Determine the IUCN conservation status for species "$scientificName" in English.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** [Found IUCN Code] (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }

    // ==================== CHINESE PROMPTS ====================

    private fun buildChineseDetailsPrompt(scientificName: String): String =
            """
        Provide detailed information about "$scientificName" in Simplified Chinese.
        
        FORMAT RULES:
        • Use **text** to bold important keywords
        • Use ##text## to green highlight places, names, measurements
        • Use • for bullet points
        
        JSON FORMAT:
        {
          "description": "Comprehensive 4-5 sentence overview. Use **bold** and ##green## for key features, places and measurements.",
          "characteristics": "Bullet list, each line starts with •:\n• Body morphology\n• Body structure\n• Size dimensions (use ##measurements##)\n• Colors\n• Identifying features\n• Special biological traits",
          "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()

    private fun buildChineseConservationPrompt(
            scientificName: String,
            codeToUse: String,
            shouldSearch: Boolean
    ): String {
        return if (!shouldSearch) {
            """
            Analyze IUCN conservation status "$codeToUse" for species "$scientificName" in Simplified Chinese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** $codeToUse (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        } else {
            """
            Determine the IUCN conservation status for species "$scientificName" in Simplified Chinese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** [Found IUCN Code] (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }

    // ==================== JAPANESE PROMPTS ====================

    private fun buildJapaneseDetailsPrompt(scientificName: String): String =
            """
        Provide detailed information about "$scientificName" in Japanese.
        
        FORMAT RULES:
        • Use **text** to bold important keywords
        • Use ##text## to green highlight places, names, measurements
        • Use • for bullet points
        
        JSON FORMAT:
        {
          "description": "Comprehensive 4-5 sentence overview. Use **bold** and ##green## for key features, places and measurements.",
          "characteristics": "Bullet list, each line starts with •:\n• Body morphology\n• Body structure\n• Size dimensions (use ##measurements##)\n• Colors\n• Identifying features\n• Special biological traits",
          "distribution": "Vietnam first if applicable, then worldwide. Use ##green highlight## for locations.",
          "habitat": "Detailed environment: elevation, climate, vegetation, food sources."
        }
        
        RETURN ONLY JSON.
    """.trimIndent()

    private fun buildJapaneseConservationPrompt(
            scientificName: String,
            codeToUse: String,
            shouldSearch: Boolean
    ): String {
        return if (!shouldSearch) {
            """
            Analyze IUCN conservation status "$codeToUse" for species "$scientificName" in Japanese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** $codeToUse (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        } else {
            """
            Determine the IUCN conservation status for species "$scientificName" in Japanese.
            
            Format the result in JSON (use \n for new lines):
            • **Conservation Status:** [Found IUCN Code] (Brief meaning)
            • **Status Explanation:** (Brief description of the status for this species)
            • **Main Threats:** (List main threats)
            
            Return JSON:
            { "conservationStatus": "Formatted content..." }
            
            RETURN ONLY JSON.
            """.trimIndent()
        }
    }
    fun buildChatSystemInstruction(): GeminiContent {
        return GeminiContent(
                role = "user",
                parts =
                        listOf(
                            GeminiPart(
                                text =
                                    """
                    You are EcoLens AI, a helpful assistant specializing in biology, nature, and environmental science.
                    
                    STRICT RULES:
                    1. ONLY answer questions related to:
                       - Animals, Plants, Insects, Fungi, Protozoa, Chromista
                       - Nature, Environment, Ecology, Conservation
                       - Biological processes, Habitats, Taxonomy
                    
                    2. If a user asks about anything else (e.g., programming, math, history, politics, general advice), you must POLITELY REFUSE.
                       - Sample refusal: "I'm sorry, I can only assist with questions about nature, animals, and plants." (Translate this to the user's language).
                    
                    3. Keep answers concise, accurate, and easy to understand.
                """.trimIndent()
                            )
                        )
        )
    }
}
