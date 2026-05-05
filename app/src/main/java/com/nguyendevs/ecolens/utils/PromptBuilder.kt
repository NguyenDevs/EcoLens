package com.nguyendevs.ecolens.utils

import com.nguyendevs.ecolens.api.GeminiContent
import com.nguyendevs.ecolens.api.GeminiPart
import com.nguyendevs.ecolens.managers.setting.LanguageManager

object PromptBuilder {

    fun buildCommonNamePrompt(scientificName: String, languageCode: String): String {
        val lang = when (languageCode) {
            LanguageManager.LANG_VI -> "Vietnamese"
            LanguageManager.LANG_CN -> "Simplified Chinese"
            LanguageManager.LANG_JP -> "Japanese"
            else -> "English"
        }
        return """
            You are a professional biologist and taxonomist with decades of experience in species nomenclature.
            Your task is to provide the most widely recognized and commonly used common name for the biological species "$scientificName" in $lang.
            
            Guidelines:
            - Return the single most popular and widely-used common name in $lang.
            - Prioritize names used in scientific literature, field guides, or official conservation databases in $lang-speaking regions.
            - If multiple common names exist, always choose the one most recognizable to the general public.
            - If no $lang common name exists, use a recognized phonetic transliteration or retain the scientific name as-is.
            - Do NOT include explanations, descriptions, markdown formatting, or any extra text whatsoever.
            - Output must be strictly valid JSON and nothing else.
            
            Output format: {"commonName": "name here"}
        """.trimIndent()
    }

    fun buildDetailsPrompt(scientificName: String, languageCode: String): String {
        return when (languageCode) {
            LanguageManager.LANG_VI -> buildVietnameseDetailsPrompt(scientificName)
            LanguageManager.LANG_CN -> buildChineseDetailsPrompt(scientificName)
            LanguageManager.LANG_JP -> buildJapaneseDetailsPrompt(scientificName)
            else -> buildEnglishDetailsPrompt(scientificName)
        }
    }

    fun buildConservationPrompt(scientificName: String, iucnCode: String, languageCode: String): String {
        val codeToUse = iucnCode.trim()
        val shouldSearch = codeToUse.isBlank() || codeToUse.equals("NE", ignoreCase = true)
        return when (languageCode) {
            LanguageManager.LANG_VI -> buildVietnameseConservationPrompt(scientificName, codeToUse, shouldSearch)
            LanguageManager.LANG_CN -> buildChineseConservationPrompt(scientificName, codeToUse, shouldSearch)
            LanguageManager.LANG_JP -> buildJapaneseConservationPrompt(scientificName, codeToUse, shouldSearch)
            else -> buildEnglishConservationPrompt(scientificName, codeToUse, shouldSearch)
        }
    }

    fun buildTaxonomyTranslationPrompt(
        kingdom: String, phylum: String, className: String,
        taxorder: String, family: String, genus: String, species: String
    ): String {
        return """
            You are a professional Vietnamese biologist and taxonomist with expertise in biological classification systems.
            Your task is to translate the following biological taxonomic classification into the most accurate and officially recognized Vietnamese terminology.
            
            Translation guidelines:
            - Use the official or most widely accepted Vietnamese biological terminology as found in Vietnamese scientific literature and textbooks.
            - For higher taxonomic ranks (Kingdom, Phylum, Class), use established Vietnamese vernacular names if available.
            - For Genus and Species, retain the Latin scientific name if no official Vietnamese equivalent exists.
            - If a term has multiple Vietnamese translations, choose the most standard and academically accepted one.
            - Do NOT add explanations, phonetic guides, markdown, or any extra text.
            - Output must be strictly valid JSON and nothing else.
            
            Input taxonomy:
            Kingdom: $kingdom
            Phylum: $phylum
            Class: $className
            Order: $taxorder
            Family: $family
            Genus: $genus
            Species: $species
            
            Output format (use exactly these keys):
            {"kingdom": "...", "phylum": "...", "className": "...", "taxorder": "...", "family": "...", "genus": "...", "species": "..."}
        """.trimIndent()
    }

    private fun buildVietnameseDetailsPrompt(scientificName: String): String =
        """
            Bạn là một nhà sinh vật học chuyên nghiệp với chuyên môn sâu về phân loại học, sinh thái học và bảo tồn tự nhiên.
            Hãy cung cấp thông tin toàn diện, chi tiết và chính xác về loài "$scientificName" bằng tiếng Việt chuẩn mực.
            Viết như thể bạn đang biên soạn một mục trong bách khoa toàn thư sinh học chuyên nghiệp — nội dung phong phú, chính xác và giàu mô tả.
            
            QUY TẮC ĐỊNH DẠNG BẮT BUỘC:
            - Dùng **từ khóa** để in đậm tên loài, thuật ngữ sinh học quan trọng, tập tính đặc trưng và đặc điểm nổi bật.
            - Dùng ##văn bản## để tô màu xanh lá cho TẤT CẢ địa danh, tên vùng địa lý, số đo và kích thước.
            - Dùng • ở đầu mỗi dòng cho danh sách (KHÔNG dùng -, *, hay số thứ tự).
            - Viết câu hoàn chỉnh, rõ ràng, có chiều sâu khoa học, tránh liệt kê cộc lốc.
            
            YÊU CẦU NỘI DUNG CHI TIẾT:
            
            "description" — Viết 5-6 câu mô tả tổng quan toàn diện:
            - Câu 1: Giới thiệu loài, tên khoa học, vị trí phân loại và điểm đặc biệt khiến loài này nổi bật trong giới tự nhiên.
            - Câu 2: Mô tả đặc điểm hình thái hoặc sinh học nổi bật nhất giúp nhận dạng loài.
            - Câu 3: Vai trò sinh thái của loài trong hệ sinh thái (con mồi, kẻ săn mồi, thụ phấn, phân hủy, loài chủ chốt, v.v.).
            - Câu 4: Tập tính hoặc đặc điểm tiến hóa đặc sắc, độc đáo của loài này.
            - Câu 5-6: Mối quan hệ với con người hoặc tầm quan trọng về bảo tồn và khoa học.
            Áp dụng **in đậm** cho từ khóa và ##xanh## cho địa danh, số liệu trong toàn bộ đoạn.
            
            "characteristics" — Danh sách bullet toàn diện, MỖI mục bắt đầu bằng • và viết ít nhất 1-2 câu đầy đủ:
            • **Hình thái tổng thể:** Mô tả cấu trúc cơ thể tổng thể, tỷ lệ các bộ phận, dáng vẻ chung và đặc trưng thể hình.
            • **Kích thước:** Chiều dài/cao/sải cánh cụ thể dùng ##số đo##, cân nặng trung bình và tối đa, sự khác biệt kích thước giữa con đực và cái nếu có.
            • **Màu sắc và hoa văn:** Mô tả chi tiết màu sắc từng bộ phận cơ thể, hoa văn đặc trưng, sự khác biệt giữa con đực/cái/non, thay đổi màu theo mùa nếu có.
            • **Cấu trúc đặc biệt:** Lông, vảy, vỏ, cánh, nanh, vuốt, gai, tuyến độc, cơ quan phát quang sinh học hoặc bất kỳ cấu trúc tiến hóa độc đáo nào của loài.
            • **Đặc điểm nhận dạng:** Những dấu hiệu quan trọng giúp phân biệt loài này với các loài tương tự hoặc cùng chi.
            • **Tập tính sinh học:** Hoạt động ngày/đêm/chạng vạng, cách săn mồi hoặc kiếm ăn, chiến lược phòng thủ, cấu trúc xã hội (đơn độc, bầy đàn, quần thể).
            • **Sinh sản:** Mùa sinh sản, hình thức giao phối, số lượng con/trứng mỗi lứa, thời gian mang thai/ấp trứng, hành vi chăm sóc con non, tuổi trưởng thành sinh dục.
            • **Tuổi thọ:** Tuổi thọ trung bình và tối đa ngoài tự nhiên và trong điều kiện nuôi nhốt nếu có số liệu.
            • **Giác quan và khả năng đặc biệt:** Thị giác, thính giác, khứu giác, điện cảm, định hướng từ trường, khả năng ngụy trang, bắt chước, hay bất kỳ năng lực sinh học phi thường nào.
            
            "distribution" — Viết 3-5 câu mô tả vùng phân bố chi tiết:
            - Ưu tiên đề cập ##Việt Nam## và các tỉnh/vùng/hệ sinh thái cụ thể tại Việt Nam nếu loài có mặt.
            - Mô tả phân bố toàn cầu: lục địa, quốc gia, đai độ cao cụ thể dùng ##số liệu##.
            - Nêu các quần thể lớn nhất hoặc vùng phân bố cốt lõi.
            - Đề cập xu hướng thay đổi vùng phân bố nếu có (mở rộng, thu hẹp do biến đổi khí hậu hay mất môi trường sống).
            Dùng ##tên địa danh## cho TẤT CẢ địa điểm được đề cập.
            
            "habitat" — Viết 4-5 câu mô tả môi trường sống chi tiết:
            - Loại sinh cảnh cụ thể (rừng nhiệt đới, đồng cỏ, đất ngập nước, rạn san hô, v.v.).
            - Độ cao, nhiệt độ và điều kiện khí hậu phù hợp dùng ##số liệu##.
            - Thảm thực vật hoặc cấu trúc môi trường mà loài phụ thuộc vào để sinh tồn.
            - Nguồn thức ăn chính, con mồi ưa thích hoặc thực vật ký chủ với bối cảnh sinh thái cụ thể.
            - Mối quan hệ cộng sinh, ký sinh hoặc cạnh tranh quan trọng trong hệ sinh thái mà loài tham gia.
            
            Output — strictly valid JSON, no markdown, absolutely no extra text outside the JSON:
            {
              "description": "...",
              "characteristics": "...",
              "distribution": "...",
              "habitat": "..."
            }
        """.trimIndent()

    private fun buildVietnameseConservationPrompt(scientificName: String, codeToUse: String, shouldSearch: Boolean): String {
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

    private fun buildEnglishDetailsPrompt(scientificName: String): String =
        """
            You are a professional biologist, naturalist, and field ecologist with deep expertise in taxonomy, behavioral biology, and conservation science.
            Provide comprehensive, detailed, and scientifically accurate information about the species "$scientificName" in English.
            Write as if you are composing an entry for a professional biological encyclopedia — thorough, precise, and richly descriptive.
            
            MANDATORY FORMATTING RULES:
            - Use **keyword** to bold species names, important biological terminology, key behaviors, and standout traits.
            - Use ##text## to green-highlight ALL place names, geographic regions, measurements, and numerical data.
            - Use • at the start of every bullet point (do NOT use -, *, or numbered lists).
            - Write complete, well-constructed sentences with scientific depth. Avoid terse, list-like fragments.
            
            DETAILED CONTENT REQUIREMENTS:
            
            "description" — Write 5-6 comprehensive sentences:
            - Sentence 1: Introduce the species, its scientific name, taxonomic position, and what makes it biologically remarkable.
            - Sentence 2: Describe the most distinctive morphological or biological feature.
            - Sentence 3: Explain the species' ecological role (prey, predator, pollinator, decomposer, keystone species, etc.).
            - Sentence 4: Highlight a fascinating behavioral or evolutionary adaptation unique to this species.
            - Sentence 5-6: Describe the species' relationship with humans and its significance in conservation or science.
            Apply **bold** to key terms and ##green## to place names and figures throughout.
            
            "characteristics" — Comprehensive bullet list, EACH item starts with • and contains at least 1-2 full sentences:
            • **Overall morphology:** Describe the general body structure, proportions, body plan, and overall appearance.
            • **Size and weight:** Specific length/height/wingspan using ##measurements##, average and maximum body weight, size differences between sexes if applicable.
            • **Coloration and patterns:** Detailed description of colors on each body region, sexual dimorphism, age-related variation, and seasonal color changes if applicable.
            • **Distinctive structures:** Scales, feathers, shell, wings, fangs, claws, spines, venom apparatus, bioluminescence, or any unique evolutionary structures specific to this species.
            • **Identification features:** Key distinguishing marks that separate this species from similar or closely related species.
            • **Behavior and activity patterns:** Diurnal/nocturnal/crepuscular activity, hunting or foraging strategy, defensive behaviors, social structure (solitary, colonial, pack, herd).
            • **Reproduction:** Breeding season, mating system, clutch/litter size, gestation or incubation period, parental care behaviors, age at sexual maturity.
            • **Lifespan:** Average and maximum lifespan in the wild and in captivity if data is available.
            • **Sensory abilities and special adaptations:** Vision, hearing, olfaction, electroreception, magnetic navigation, camouflage, mimicry, or any other extraordinary biological capability.
            
            "distribution" — Write 3-5 detailed sentences:
            - Mention ##Vietnam## and specific provinces/regions first if the species occurs there.
            - Describe global distribution: continents, countries, specific elevation zones using ##figures##.
            - Name the largest populations or core distribution areas.
            - Mention range shifts due to climate change or habitat loss if documented.
            Use ##place names## for ALL geographic locations mentioned.
            
            "habitat" — Write 4-5 detailed sentences:
            - Specific habitat type (tropical rainforest, grassland, wetland, coral reef, deep ocean, etc.).
            - Elevation range, temperature range, and climatic conditions using ##figures##.
            - Vegetation structure or physical environment the species depends on for survival.
            - Primary food sources, preferred prey, or host plants with ecological context.
            - Key symbiotic, parasitic, or competitive ecological relationships the species is part of.
            
            Output — strictly valid JSON, no markdown, absolutely no extra text outside the JSON:
            {
              "description": "...",
              "characteristics": "...",
              "distribution": "...",
              "habitat": "..."
            }
        """.trimIndent()

    private fun buildEnglishConservationPrompt(scientificName: String, codeToUse: String, shouldSearch: Boolean): String {
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

    private fun buildChineseDetailsPrompt(scientificName: String): String =
        """
            您是一位专业生物学家、自然学家和野外生态学家，在分类学、行为生物学和保护科学方面具有深厚专业知识。
            请用标准简体中文提供关于物种"$scientificName"的全面、详细且科学准确的信息。
            请以撰写专业生物学百科全书条目的标准来写作——内容翔实、精确且描述丰富。
            
            强制格式规则:
            - 使用 **关键词** 加粗物种名称、重要生物术语、关键行为和突出特征。
            - 使用 ##文本## 绿色高亮所有地名、地理区域、测量数据和数值。
            - 每个要点以 • 开头（不使用 -、* 或数字编号）。
            - 写完整、结构良好的句子，具有科学深度，避免简短的片段式列举。
            
            详细内容要求:
            
            "description" — 写5-6句全面综述:
            - 第1句：介绍该物种、学名、分类地位及其在自然界中的突出之处。
            - 第2句：描述最独特的形态或生物学特征，有助于识别该物种。
            - 第3句：解释该物种的生态角色（猎物、捕食者、传粉者、分解者、关键种等）。
            - 第4句：突出该物种独特的行为或进化适应性。
            - 第5-6句：描述该物种与人类的关系及其在保护或科学上的重要性。
            全文对关键术语使用**加粗**，对地名和数据使用##绿色##。
            
            "characteristics" — 全面的要点列表，每项以•开头，至少包含1-2个完整句子:
            • **总体形态：** 描述总体体型结构、身体比例、体型和整体外观及体型特征。
            • **尺寸与体重：** 使用##测量值##描述具体长度/高度/翼展，平均和最大体重，如适用则说明两性体型差异。
            • **体色与花纹：** 详细描述各身体部位的颜色、特征花纹、两性异形、年龄差异及季节性颜色变化。
            • **独特结构：** 鳞片、羽毛、壳、翅膀、獠牙、爪、刺、毒液器官、生物发光或该物种特有的任何独特进化结构。
            • **识别特征：** 区分该物种与近似种或近缘种的关键识别标志。
            • **行为与活动模式：** 昼行/夜行/晨昏活动，狩猎或觅食策略，防御行为，社会结构（独居、群居、种群）。
            • **繁殖：** 繁殖季节、交配制度、产卵/产仔数量、妊娠或孵化期、亲代照护行为、性成熟年龄。
            • **寿命：** 野外平均和最大寿命及圈养寿命（如有数据）。
            • **感觉能力与特殊适应性：** 视觉、听觉、嗅觉、电感、磁场导航、伪装、拟态或其他非凡的生物能力。
            
            "distribution" — 写3-5句详细描述:
            - 如该物种在越南有分布，优先提及##越南##及具体省份/地区/生态系统。
            - 描述全球分布：洲、国、具体海拔带，使用##数据##。
            - 指出最大种群或核心分布区。
            - 如有记录，提及气候变化或栖息地丧失导致的分布范围变化。
            所有提及的地理位置使用##地名##。
            
            "habitat" — 写4-5句详细描述:
            - 具体栖息地类型（热带雨林、草地、湿地、珊瑚礁、深海等）。
            - 海拔范围、温度范围和气候条件，使用##数据##。
            - 该物种赖以生存的植被结构或物理环境。
            - 主要食物来源、偏好猎物或寄主植物及生态背景。
            - 该物种参与的关键共生、寄生或竞争生态关系。
            
            输出格式——严格有效的JSON，无markdown，JSON之外绝对没有多余文本:
            {
              "description": "...",
              "characteristics": "...",
              "distribution": "...",
              "habitat": "..."
            }
        """.trimIndent()

    private fun buildChineseConservationPrompt(scientificName: String, codeToUse: String, shouldSearch: Boolean): String {
        return if (!shouldSearch) {
            """
                您是一位国际保护生物学家，在IUCN红色名录标准、种群生态学和全球保护项目方面具有深厚专业知识。
                请用标准简体中文专业、严谨地分析物种"$scientificName"的IUCN保护状态"$codeToUse"。
                请以为政策制定者和野生动物管理者撰写正式保护状态报告的标准来写作。
                
                使用\n换行。每个部分以•开头，每条至少包含2-3个完整的详细句子。
                
                必须包含的内容:
                
                • **保护状态: $codeToUse**
                提供IUCN"$codeToUse"类别的完整官方定义，解释使物种获得此状态的具体标准，并描述此分类对物种长期生存前景的意义。
                
                • **种群分析:**
                提供野外剩余个体数量估计（如有数据）、当前种群趋势（增加/减少/稳定）、近几十年的下降速率、剩余种群的碎片化程度，以及与历史种群水平的比较。
                
                • **主要威胁与根本原因:**
                按严重程度排序，详细分析各主要威胁:
                - 栖息地破坏和退化（农业扩张、城镇化、毁林、湿地排干）
                - 非法狩猎、偷猎、野生动物贸易和过度开发
                - 气候变化对该物种的具体影响（温度敏感性、海平面上升、降水模式改变）
                - 污染（农药、重金属、塑料、光、噪音）
                - 入侵物种、新兴疾病和生态竞争
                - 该生物特有的物种特异性威胁
                
                • **保护项目与保护措施:**
                描述积极的保护举措——保护区、圈养繁殖项目、再引入项目、科学研究、生态廊道、国家和国际法律保护（CITES列名、国家立法）以及基于社区的保护参与。
                
                • **展望与建议:**
                评估种群恢复的现实前景，确定最优先需要采取的行动，突出保护工作中的成功案例，并预测如果不实施充分干预的可能结果。
                
                输出——严格有效的JSON，无markdown:
                {"conservationStatus": "完整格式化内容..."}
            """.trimIndent()
        } else {
            """
                您是一位国际保护生物学家，在IUCN红色名录标准、种群生态学和全球保护项目方面具有深厚专业知识。
                请确定"$scientificName"的当前IUCN保护状态，并用标准简体中文进行全面分析。
                如果无法确认准确的IUCN代码，请根据现有生态和种群数据提供您最专业的评估。
                
                使用\n换行。每个部分以•开头，每条至少包含2-3个完整的详细句子。
                
                必须包含的内容:
                
                • **保护状态: [确定的IUCN代码]**
                说明找到的IUCN代码，提供完整的官方定义、使该物种获得此状态的标准，以及这对物种长期生存意味着什么。
                
                • **种群分析:**
                野外剩余个体数量估计（如有）、当前种群趋势、下降速率、种群碎片化程度、与历史基线的比较。
                
                • **主要威胁与根本原因:**
                按严重程度排序，详细分析:
                - 栖息地破坏（农业、城镇化、毁林、湿地排干）
                - 非法狩猎、偷猎、野生动物贸易
                - 气候变化对该物种的具体影响
                - 污染（农药、重金属、塑料、光、噪音）
                - 入侵物种、新兴疾病、生态竞争
                - 物种特异性威胁
                
                • **保护项目与保护措施:**
                保护区、圈养繁殖、再引入项目、研究计划、生态廊道、CITES列名、国家立法、社区保护工作。
                
                • **展望与建议:**
                现实的恢复前景、最优先行动、现有工作的成功案例、以及没有充分保护干预的预期结果。
                
                输出——严格有效的JSON，无markdown:
                {"conservationStatus": "完整格式化内容..."}
            """.trimIndent()
        }
    }

    private fun buildJapaneseDetailsPrompt(scientificName: String): String =
        """
            あなたは分類学、行動生物学、保護科学に深い専門知識を持つプロの生物学者、自然学者、野外生態学者です。
            "$scientificName"という種について、標準的な日本語で包括的、詳細かつ科学的に正確な情報を提供してください。
            プロの生物学百科事典の項目を執筆するような水準で書いてください——徹底的で、正確で、豊かな記述を心がけてください。
            
            必須フォーマットルール:
            - **キーワード** を使用して種名、重要な生物学用語、主要な行動、際立った特徴を太字にする。
            - ##テキスト## を使用してすべての地名、地理的地域、測定値、数値データを緑色でハイライトする。
            - 各箇条書きの先頭に • を使用する（-、*、番号付きリストは使用しない）。
            - 科学的な深みを持つ完全な文章を書く。断片的なリスト形式は避ける。
            
            詳細な内容要件:
            
            "description" — 5〜6文の包括的な概要を書く:
            - 第1文：種、学名、分類上の位置、自然界での生物学的な注目すべき点を紹介する。
            - 第2文：種の識別に役立つ最も特徴的な形態的または生物学的特徴を説明する。
            - 第3文：その種の生態的役割（被食者、捕食者、送粉者、分解者、キーストーン種など）を説明する。
            - 第4文：この種に固有の興味深い行動的または進化的適応を強調する。
            - 第5〜6文：人間との関係および保護・科学上の重要性を説明する。
            全文を通じて重要な用語に**太字**、地名と数値に##緑色##を適用する。
            
            "characteristics" — 包括的な箇条書きリスト、各項目は•で始まり、少なくとも1〜2文の完全な文を含む:
            • **全体的な形態：** 全体的な体の構造、体の比率、体型、全体的な外観と体型の特徴を説明する。
            • **サイズと体重：** ##測定値##を使用した具体的な長さ/高さ/翼長、平均および最大体重、該当する場合は性別間のサイズ差。
            • **体色と模様：** 各身体部位の色、特徴的な模様、性的二型、年齢による変異、季節的な色変化（該当する場合）の詳細な説明。
            • **独特な構造：** 鱗、羽毛、殻、翼、牙、爪、棘、毒器官、生物発光、またはこの種に固有のユニークな進化的構造。
            • **識別特徴：** この種を類似種や近縁種から区別する主要な識別マーク。
            • **行動と活動パターン：** 昼行性/夜行性/薄暮性の活動、狩猟または採食戦略、防衛行動、社会構造（単独、群体、群れ）。
            • **繁殖：** 繁殖期、交配システム、卵/仔の数、妊娠または孵化期間、親の世話の行動、性成熟年齢。
            • **寿命：** データが利用可能な場合の野生での平均および最大寿命と飼育下での寿命。
            • **感覚能力と特殊な適応：** 視覚、聴覚、嗅覚、電気受容、磁気ナビゲーション、カモフラージュ、擬態、またはその他の並外れた生物学的能力。
            
            "distribution" — 3〜5文の詳細な説明:
            - 種がベトナムに生息する場合、##ベトナム##と特定の省/地域/生態系を優先的に記載する。
            - 世界的な分布：大陸、国、##数値##を使用した特定の標高帯を記述する。
            - 最大の個体群または中核的な分布域を記載する。
            - 記録がある場合は、気候変動や生息地の喪失による分布域の変化を記載する。
            記載されるすべての地理的位置に##地名##を使用する。
            
            "habitat" — 4〜5文の詳細な説明:
            - 具体的な生息地タイプ（熱帯雨林、草原、湿地、サンゴ礁、深海など）。
            - ##数値##を使用した標高範囲、気温範囲、気候条件。
            - 種が生存のために依存する植生構造または物理的環境。
            - 生態学的文脈を含む主要な食料源、好む獲物、または宿主植物。
            - 種が関与する主要な共生、寄生、または競争的な生態的関係。
            
            出力形式——厳密に有効なJSON、markdownなし、JSON以外の余分なテキストは一切含めない:
            {
              "description": "...",
              "characteristics": "...",
              "distribution": "...",
              "habitat": "..."
            }
        """.trimIndent()

    private fun buildJapaneseConservationPrompt(scientificName: String, codeToUse: String, shouldSearch: Boolean): String {
        return if (!shouldSearch) {
            """
                あなたはIUCNレッドリスト基準、個体群生態学、世界的な保護プログラムに深い専門知識を持つ国際的な保護生物学者です。
                "$scientificName"のIUCN保全状況"$codeToUse"を標準的な日本語でプロフェッショナルな深みと厳密さで分析してください。
                政策立案者や野生生物管理者向けの正式な保全状況報告書を作成するような水準で書いてください。
                
                \nを改行に使用する。各セクションは•で始まり、少なくとも2〜3の完全で詳細な文を含む。
                
                必須内容:
                
                • **保全状況: $codeToUse**
                IUCN「$codeToUse」カテゴリの完全な公式定義を提供し、種がこの状況に該当するための具体的な基準を説明し、この分類が種の長期的な生存見通しにとって何を意味するかを記述する。
                
                • **個体群分析:**
                野生の残存個体数の推定値（データが存在する場合）、現在の個体群動向（増加/減少/安定）、近年の数十年間の減少率、残存個体群の断片化の程度、歴史的な個体群水準との比較を提供する。
                
                • **主な脅威と根本原因:**
                深刻度順に各主要脅威を詳細に分析する:
                - 生息地の破壊と劣化（農業拡大、都市化、森林伐採、湿地の干拓）
                - 違法狩猟、密猟、野生生物取引、乱獲
                - この種に固有の気候変動の影響（温度感受性、海面上昇、降水パターンの変化）
                - 汚染（農薬、重金属、プラスチック、光、騒音）
                - 外来種、新興疾病、生態学的競争
                - この生物の生物学または生態学に固有の種特異的脅威
                
                • **保護プログラムと保護措置:**
                積極的な保護活動を説明する——保護区、飼育繁殖プログラム、再導入プロジェクト、科学研究、生態的回廊、国内および国際的な法的保護（CITESリスト、国内法）、地域社会に基づく保護への関与。
                
                • **展望と提言:**
                個体群回復の現実的な見通しを評価し、最優先で必要な行動を特定し、既存の保護活動の成功事例を強調し、適切な介入が実施されない場合の予測される結果を示す。
                
                出力——厳密に有効なJSON、markdownなし:
                {"conservationStatus": "完全にフォーマットされたコンテンツ..."}
            """.trimIndent()
        } else {
            """
                あなたはIUCNレッドリスト基準、個体群生態学、世界的な保護プログラムに深い専門知識を持つ国際的な保護生物学者です。
                "$scientificName"の現在のIUCN保全状況を特定し、標準的な日本語で包括的な分析を提供してください。
                正確なIUCNコードが確認できない場合は、利用可能な生態学的および個体群データに基づいて最善のプロフェッショナルな評価を提供してください。
                
                \nを改行に使用する。各セクションは•で始まり、少なくとも2〜3の完全で詳細な文を含む。
                
                必須内容:
                
                • **保全状況: [特定されたIUCNコード]**
                見つかったIUCNコードを示し、完全な公式定義、この種がこの状況に該当する基準、および種の長期的な生存にとって何を意味するかを提供する。
                
                • **個体群分析:**
                野生の残存個体数の推定（利用可能な場合）、現在の個体群動向、減少率、個体群の断片化、歴史的ベースラインとの比較。
                
                • **主な脅威と根本原因:**
                深刻度順に詳細に分析する:
                - 生息地の破壊（農業、都市化、森林伐採、湿地排水）
                - 違法狩猟、密猟、野生生物取引
                - この種に固有の気候変動の影響
                - 汚染（農薬、重金属、プラスチック、光、騒音）
                - 外来種、新興疾病、生態学的競争
                - 種特異的脅威
                
                • **保護プログラムと保護措置:**
                保護区、飼育繁殖、再導入プロジェクト、研究プログラム、生態的回廊、CITESリスト、国内法、地域社会による保護活動。
                
                • **展望と提言:**
                現実的な回復見通し、最優先行動、既存活動の成功事例、適切な保護介入なしに予測される結果。
                
                出力——厳密に有効なJSON、markdownなし:
                {"conservationStatus": "完全にフォーマットされたコンテンツ..."}
            """.trimIndent()
        }
    }

    fun buildTextTranslationPrompt(text: String, languageCode: String): String {
        val targetLang = when (languageCode) {
            LanguageManager.LANG_EN -> "English"
            LanguageManager.LANG_CN -> "Simplified Chinese"
            LanguageManager.LANG_JP -> "Japanese"
            else -> "English"
        }
        return """
            You are a professional scientific translator specializing in biology, ecology, and natural history content.
            Translate the following text accurately and naturally into $targetLang.
            
            Critical translation rules:
            - Preserve ALL formatting markers exactly as they appear: **text** stays **text**, ##text## stays ##text##, and bullet points starting with • must remain as •.
            - Maintain precise scientific and biological terminology — use the standard $targetLang equivalent for each term.
            - Ensure the translation reads naturally and fluently in $targetLang, not like a literal word-for-word translation.
            - Do NOT add explanations, translator notes, or any additional content.
            - Do NOT alter the structure, order, or formatting of the original text.
            - Output must be strictly valid JSON only, absolutely no markdown or extra text.
            
            Input text:
            $text
            
            Output format: {"translatedText": "translated text here"}
        """.trimIndent()
    }

    fun buildChatSystemInstruction(): GeminiContent {
        return GeminiContent(
            role = "user",
            parts = listOf(
                GeminiPart(
                    text = """
                        You are EcoLens AI, an expert virtual assistant dedicated exclusively to biology, nature, and environmental science.
                        You have the combined knowledge of a professional biologist, ecologist, taxonomist, and naturalist.
                        
                        YOUR SCOPE — answer ONLY questions about:
                        - All living organisms: Animals, Plants, Insects, Fungi, Bacteria, Protozoa, Chromista, and Archaea
                        - Nature, Environment, Ecology, Biodiversity, and Conservation biology
                        - Biological processes: Anatomy, Physiology, Genetics, Evolution, Cell biology, Biochemistry
                        - Habitats, Ecosystems, Food webs, Nutrient cycles, and Biomes
                        - Taxonomy, Systematics, and Phylogenetics
                        - Environmental issues directly related to living organisms and ecosystems
                        - Wildlife behavior, Animal cognition, and Ethology
                        - Botany, Zoology, Mycology, Marine biology, and all biological subdisciplines
                        
                        STRICT BEHAVIORAL RULES:
                        1. SCOPE ENFORCEMENT: If a question falls outside the above scope (programming, mathematics, history, cooking, politics, human medicine, general knowledge, entertainment, etc.), REFUSE politely in the same language the user used.
                           Standard refusal (adapt to user's language): "I'm sorry, I can only assist with questions about biology, nature, animals, plants, and the environment. Please ask me about the natural world!"
                        
                        2. ANSWER QUALITY: Provide accurate, well-structured, scientifically grounded answers. Include specific details, examples, and context. Avoid vague or generic responses.
                        
                        3. UNCERTAINTY: When scientific consensus is unclear or data is limited, explicitly acknowledge the uncertainty rather than speculating or fabricating information.
                        
                        4. LANGUAGE MATCHING: Always respond in the same language the user used.
                        
                        5. TONE: Be engaging, informative, and enthusiastic about nature and biology. Make science accessible without sacrificing accuracy.
                    """.trimIndent()
                )
            )
        )
    }
}