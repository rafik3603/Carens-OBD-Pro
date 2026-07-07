package com.example.data.obd

enum class ObdConnectionState {
    DISCONNECTED,
    CONNECTING,
    INITIALIZING,
    CONNECTED,
    ERROR
}

data class DtcInfo(
    val code: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val category: String // "Engine", "Transmission", "Body", "Chassis"
)

data class ObdSensorData(
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolantTemp: Int = 0,
    val engineLoad: Int = 0,
    val batteryVoltage: Double = 12.4,
    val batteryStateOfCharge: Int = 78,
    val fuelLevel: Int = 100,
    val fuelPressure: Int = 0, // Common rail diesel injection pressure in Bar (e.g. 250 - 1600)
    val throttlePosition: Int = 0,
    val isEngineRunning: Boolean = false,
    
    // Transmission Parameters
    val transmissionGear: String = "P", // "P", "R", "N", "D1", "D2", "D3", "D4", "D5", "D6"
    val transmissionTemp: Int = 75, // Transmission fluid temp in °C
    val transmissionPressure: Double = 4.5, // Transmission fluid line pressure in Bar
    val transmissionSlipPercent: Double = 0.0, // Torque converter clutch slip %
    
    // Oil Life and Maintenance Parameters
    val oilLifePercent: Int = 85,
    val oilRemainingKm: Int = 8500,
    val oilTemperature: Int = 25,
    val oilPressure: Double = 3.2, // Engine oil pressure in Bar (e.g. 1.0 - 5.5 Bar)
    
    // Car Doors and Trunk Status
    val doorDriverOpen: Boolean = false,
    val doorPassengerOpen: Boolean = false,
    val doorRearLeftOpen: Boolean = false,
    val doorRearRightOpen: Boolean = false,
    val hoodOpen: Boolean = false,
    val trunkOpen: Boolean = false,
    
    // Windows status (0 = fully closed, 100 = fully open)
    val windowFrontLeftOpenPercent: Int = 0,
    val windowFrontRightOpenPercent: Int = 0,
    val windowRearLeftOpenPercent: Int = 0,
    val windowRearRightOpenPercent: Int = 0,
    
    // Door Lock system
    val isCentralLocked: Boolean = true,
    
    // Total Mileage / Odometer
    val odometerKm: Int = 184250,
    
    // Turbo Diesel CRDi Specific Parameters for Kia Carens 2008
    val dpfSootLevel: Int = 12,
    val turboBoostPressure: Double = 0.0, // Bar
    
    val activeDtcs: List<DtcInfo> = emptyList()
)

object KiaCarensFaults {
    val AVAILABLE_FAULTS = listOf(
        DtcInfo(
            code = "P0202",
            descriptionAr = "خلل في دائرة بخاخ الديزل - الأسطوانة رقم 2 (CRDi Injector Circuit)",
            descriptionEn = "CRDi Injector Circuit Malfunction - Cylinder 2",
            category = "Engine (المحرك)"
        ),
        DtcInfo(
            code = "P0401",
            descriptionAr = "تدفق غير كافٍ لصمام إعادة تدوير الغاز EGR (انسداد كربوني شائع)",
            descriptionEn = "EGR Flow Insufficient Detected (Carbon buildup)",
            category = "Emission (نظام الانبعاثات)"
        ),
        DtcInfo(
            code = "P0101",
            descriptionAr = "أداء غير صحيح لحساس تدفق الهواء MAF (حساس الهواء)",
            descriptionEn = "MAF Sensor Circuit Range/Performance",
            category = "Engine (المحرك)"
        ),
        DtcInfo(
            code = "P0299",
            descriptionAr = "ضغط توربو منخفض للغاية (قصور في شاحن التوربين CRDi)",
            descriptionEn = "Turbocharger Underboost Condition",
            category = "Engine (المحرك)"
        ),
        DtcInfo(
            code = "P0380",
            descriptionAr = "خلل في دائرة شمعات التسخين / التدفئة (Glow Plugs Circuit)",
            descriptionEn = "Glow Plugs/Heater Circuit A Malfunction",
            category = "Engine (المحرك)"
        ),
        DtcInfo(
            code = "P2002",
            descriptionAr = "كفاءة فلتر جزيئات الديزل DPF تحت الحد المسموح (انسداد شكمان الديزل)",
            descriptionEn = "Diesel Particulate Filter Efficiency Below Threshold",
            category = "Emission (نظام الانبعاثات)"
        )
    )
}

data class KiaDtcDefinition(
    val code: String,
    val descriptionAr: String,
    val descriptionEn: String,
    val category: String, // "Engine (المحرك)", "Transmission (ناقل الحركة)", "Emission (العادم والانبعاثات)", "Body/Chassis (الهيكل والتعليق)"
    val isKiaSpecific: Boolean,
    val symptoms: List<String>,
    val causes: List<String>,
    val solutions: List<String>
)

object KiaDtcDatabase {
    val DTC_LIST = listOf(
        KiaDtcDefinition(
            code = "P0101",
            descriptionAr = "أداء غير صحيح لحساس تدفق الهواء MAF (حساس الهواء)",
            descriptionEn = "MAF Sensor Circuit Range/Performance",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "ضعف عزم وتسارع السيارة بشكل ملحوظ",
                "خروج دخان أسود عند الضغط على دواسة الوقود",
                "تفتفة واهتزاز المحرك عند الوقوف التام (Rough Idle)"
            ),
            causes = listOf(
                "تراكم الغبار والزيت على مقاومة الحساس الداخلية",
                "تشقق أو تهريب هواء في خراطيم الهواء الموصلة للمحرك",
                "فلتر الهواء متسخ ويسد مجرى تدفق الهواء"
            ),
            solutions = listOf(
                "تنظيف الحساس باستخدام بخاخ منظف الكترونيات جاف (لا تستخدم بخاخ زيتي)",
                "فحص سلامة خراطيم الهواء وربط القفايز بإحكام لمنع تسريب الهواء",
                "استبدال فلتر هواء المحرك إذا كان تالفاً أو متسخاً"
            )
        ),
        KiaDtcDefinition(
            code = "P0113",
            descriptionAr = "جهد مرتفع في دائرة حساس درجة حرارة هواء السحب IAT",
            descriptionEn = "Intake Air Temperature Sensor 1 Circuit High Input",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "صعوبة تشغيل المحرك في الأجواء الباردة",
                "زيادة معدل استهلاك وقود الديزل",
                "إضاءة لمبة التشيك إنجن (Check Engine) في الطبلون"
            ),
            causes = listOf(
                "انقطاع أو تلف أسلاك فيشة حساس الحرارة IAT",
                "رطوبة أو صدأ في موصلات الفيشة الكهربائية",
                "تلف المقاومة الحرارية الداخلية للحساس نفسه"
            ),
            solutions = listOf(
                "فحص ضفيرة وأسلاك الحساس والتأكد من عدم وجود قطع فيها",
                "تنظيف فيشة الحساس ببخاخ منظف الكترونيات جاف لإزالة الأكسدة",
                "استبدال حساس IAT (غالباً ما يكون مدمجاً مع حساس MAF)"
            )
        ),
        KiaDtcDefinition(
            code = "P0115",
            descriptionAr = "خلل في دائرة حساس درجة حرارة مياه تبريد المحرك ECT",
            descriptionEn = "Engine Coolant Temperature Circuit Malfunction",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "ارتفاع قراءة مؤشر الحرارة بالطبلون أو هبوطها المفاجئ",
                "عمل مراوح التبريد بأقصى سرعة بشكل مستمر",
                "استهلاك ديزل غير طبيعي مع تراجع العزم"
            ),
            causes = listOf(
                "تلف حساس مياه التبريد ECT وعدم دقة قراءاته",
                "نقص شديد في سائل التبريد أو وجود هواء بنظام التبريد",
                "صدأ وتراكم ترسبات كلسية على رأس الحساس الملامس للمياه"
            ),
            solutions = listOf(
                "استبدال حساس حرارة سائل التبريد ECT",
                "تنسيم نظام التبريد والتأكد من عدم وجود هواء بداخل الخراطيم",
                "تنظيف دائرة مياه الرديتر واستخدام مياه تبريد أصلية لمنع الترسبات"
            )
        ),
        KiaDtcDefinition(
            code = "P0201",
            descriptionAr = "خلل في دائرة بخاخ الديزل - الأسطوانة رقم 1 CRDi",
            descriptionEn = "Cylinder 1 Injector Circuit Malfunction",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "تفتفة قوية واهتزاز شديد للمحرك عند التشغيل وفي الخمول",
                "تقطيع وفقدان مفاجئ للعزم أثناء القيادة",
                "صوت طقطقة واضح من المحرك (Diesel Knocking)"
            ),
            causes = listOf(
                "تلف الملف الكهربائي (Solenoid) للبخاخ رقم 1",
                "تراكم الكربون على رأس فوهة البخاخ وانسداد فتحات الرش",
                "خلل أو تراكم رطوبة في فيشة البخاخ رقم 1"
            ),
            solutions = listOf(
                "فحص مقاومة البخاخ رقم 1 واستبداله إذا كانت تالفة",
                "تنظيف البخاخات بجهاز الموجات فوق الصوتية (Ultrasound Cleaning)",
                "استخدام مادة منظف بخاخات ديزل عالية الجودة في خزان الوقود"
            )
        ),
        KiaDtcDefinition(
            code = "P0202",
            descriptionAr = "خلل في دائرة بخاخ الديزل - الأسطوانة رقم 2 (CRDi Injector)",
            descriptionEn = "CRDi Injector Circuit Malfunction - Cylinder 2",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "اهتزاز شديد للمحرك في وضع الوقوف",
                "ضعف عزم دوران محرك الديزل وسماع تفتفة",
                "صعوبة واضحة في تشغيل المحرك بارداً"
            ),
            causes = listOf(
                "عطل كهربائي داخلي بالملف الكهرومغناطيسي للبخاخ 2",
                "ارتخاء فيش الكهرباء المغذية للبخاخ بسبب الاهتزازات",
                "انسداد فتحات البخاخ برواسب ديزل سيء الجودة"
            ),
            solutions = listOf(
                "إعادة تثبيت وتنظيف فيشة بخاخ الأسطوانة 2",
                "استبدال رأس أو قلب البخاخ المعطل (Nozzle/Solenoid)",
                "برمجة كود البخاخ الجديد (IMA Code) في كمبيوتر ECU لضبط تدفق الوقود"
            )
        ),
        KiaDtcDefinition(
            code = "P0299",
            descriptionAr = "ضغط توربو منخفض للغاية (قصور في شاحن التوربين CRDi)",
            descriptionEn = "Turbocharger Underboost Condition",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "عجز تام في تسارع السيارة فوق سرعة 80 كم/س",
                "دخول محرك الديزل في وضع الأمان المقيد (Limp Mode)",
                "سماع صوت صفير عالي أو تسريب هواء من المحرك"
            ),
            causes = listOf(
                "تهريب وضياع هواء التوربين من خراطيم الإنتركولر",
                "تلف بلف التحكم في ضغط التوربو (VGT Solenoid Actuator)",
                "تراكم الكربون وتصلب ريش التوربين ذو الهندسة المتغيرة (VGT)"
            ),
            solutions = listOf(
                "فحص خراطيم الإنتركولر والتأكد من خلوها من التشققات والتهريب",
                "استبدال بلف التوربو VGT الكهربائي المغذي لشفط الهواء",
                "فك التوربين وتنظيف ريش التوجيه الداخلية من الكربون المتراكم"
            )
        ),
        KiaDtcDefinition(
            code = "P0380",
            descriptionAr = "خلل في دائرة شمعات التسخين / التدفئة (Glow Plugs)",
            descriptionEn = "Glow Plugs/Heater Circuit A Malfunction",
            category = "Engine (المحرك)",
            isKiaSpecific = false,
            symptoms = listOf(
                "صعوبة بالغة في تشغيل محرك الديزل في الصباح أو الأجواء الباردة",
                "خروج دخان أبيض كثيف برائحة ديزل غير محروق فور التشغيل",
                "عدم استقرار المحرك في الدقائق الأولى بعد التشغيل"
            ),
            causes = listOf(
                "احتراق شمعة تسخين واحدة أو أكثر (Glow Plugs)",
                "تلف كتاوت تشغيل شمعات التسخين (Glow Plug Relay)",
                "انقطاع في الكيبل أو الفيوز المغذي للشمعات بالطاقة"
            ),
            solutions = listOf(
                "فحص شمعات التدفئة بواسطة مالتيميتر واستبدال التالفة فوراً",
                "فحص علبة الفيوزات وكتاوت نظام التسخين والتأكد من سلامته",
                "تنظيف شريط التوصيل النحاسي المغذي لرؤوس شمعات التسخين"
            )
        ),
        KiaDtcDefinition(
            code = "P0401",
            descriptionAr = "تدفق غير كافٍ لصمام إعادة تدوير الغاز EGR (انسداد كربوني)",
            descriptionEn = "EGR Flow Insufficient Detected (Carbon buildup)",
            category = "Emission (العادم والانبعاثات)",
            isKiaSpecific = false,
            symptoms = listOf(
                "تراجع كفاءة واستهلاك الوقود بشكل واضح",
                "تفتفة خفيفة عند القيادة بسرعات ثابتة هادئة",
                "ارتفاع درجة حرارة المحرك بشكل طفيف"
            ),
            causes = listOf(
                "تراكم رواسب الكربون الجافة داخل فتحات صمام الـ EGR وسد المجرى",
                "عطل بلف التحكم المغناطيسي المشغل للصمام بالشفط",
                "تراكم السخام في الأنبوب المعدني الموصل من الشكمان لـ EGR"
            ),
            solutions = listOf(
                "فك صمام EGR وتنظيفه بالكامل ببخاخ مزيل الكربون وفرشاة سلكية",
                "تنظيف الأنبوب الموصل وصمام التهوية بالكامل وتجربة عمل الصمام كهربائياً",
                "تنشيط بلف الـ EGR برمجياً لمعايرة وضع الفتح والإغلاق التام"
            )
        ),
        KiaDtcDefinition(
            code = "P0500",
            descriptionAr = "خلل في إشارة حساس سرعة السيارة VSS",
            descriptionEn = "Vehicle Speed Sensor Malfunction",
            category = "Body/Chassis (الهيكل والتعليق)",
            isKiaSpecific = false,
            symptoms = listOf(
                "عدم حركة مؤشر عداد السرعة (KM/H) في الطبلون",
                "تغيرات قاسية وغير مريحة في تبديلات القير الأوتوماتيكي",
                "توقف نظام تثبيت السرعة (Cruise Control) عن العمل"
            ),
            causes = listOf(
                "عطل حساس السرعة VSS المثبت على القير أو عطل حساسات الـ ABS",
                "ارتخاء الفيشة الكهربائية للحساس أو انقطاع أسلاك الضفيرة",
                "تلف الترس البلاستيكي الداخلي المشغل للحساس بداخل ناقل الحركة"
            ),
            solutions = listOf(
                "فحص موصلات الحساس وتنظيفها ببخاخ الكترونيات لإزالة الأوساخ والترطيب",
                "قراءة سرعة العجلات من كمبيوتر ABS لمعرفة العجلة المسببة للخلل",
                "استبدال حساس سرعة السيارة VSS أو حساس ABS المتضرر"
            )
        ),
        KiaDtcDefinition(
            code = "P0700",
            descriptionAr = "خلل في نظام التحكم بناقل الحركة (طلب إضاءة لمبة الأعطال MIL)",
            descriptionEn = "Transmission Control System Malfunction (MIL Request)",
            category = "Transmission (ناقل الحركة)",
            isKiaSpecific = false,
            symptoms = listOf(
                "تأخر أو خشونة واضحة عند تبديل السرعات (ضربة في القير)",
                "احتجاز القير في السرعة الثالثة كوضع حماية (Safe Mode)",
                "ارتفاع صوت محرك السيارة بشكل مفرط مقارنة بالسرعة الفوقية"
            ),
            causes = listOf(
                "وجود كود عطل نشط مخزن بداخل كمبيوتر القير TCU",
                "نقص أو انتهاء صلاحية زيت القير الأوتوماتيكي",
                "خلل كهربائي بأحد بلوف التحكم بالضغط (Solenoid Valve)"
            ),
            solutions = listOf(
                "الدخول على كمبيوتر القير (TCU) وقراءة الأكواد الدقيقة المخزنة به",
                "قياس مستوى زيت القير وفحص لزوجته ولونه واستبداله إذا لزم الأمر",
                "فحص ضفيرة القير الكهربائية والفيش الجانبية لمنع تسريب الزيت إليها"
            )
        ),
        KiaDtcDefinition(
            code = "P1186",
            descriptionAr = "ضغط الوقود منخفض للغاية - السكك المشتركة CRDi (كيا حصرياً)",
            descriptionEn = "CRDi Fuel Pressure - Minimum Limit at Engine Speed Too Low",
            category = "Engine (المحرك)",
            isKiaSpecific = true,
            symptoms = listOf(
                "انطفاء محرك السيارة فجأة أثناء السير والضغط على دواسة الوقود",
                "صعوبة تشغيل السيارة بعد انطفائها وتطلب محاولات متكررة",
                "انخفاض العزم بشكل فجائي مع إضاءة لمبة التشيك إنجن"
            ),
            causes = listOf(
                "انسداد فلتر ديزل الوقود بالأوساخ والترسبات الطينية",
                "ضعف طرمبة الديزل الكهربائية الموجودة داخل التانكي (الخزان)",
                "تهريب داخلي ببلف منظم ضغط السكك المشتركة (Rail Pressure Regulator)"
            ),
            solutions = listOf(
                "استبدال فلتر الديزل فوراً بنوعية أصلية لضمان مرور الوقود الكافي",
                "فحص ضغط طرمبة الديزل للتأكد من وصوله لـ 3.5 بار على الأقل للضغط المنخفض",
                "فحص تهريب الديزل الراجع من البخاخات للتأكد من عدم وجود بخاخ يسرب الديزل للخلف"
            )
        ),
        KiaDtcDefinition(
            code = "P1188",
            descriptionAr = "عطل بلف تنظيم ضغط وقود الديزل (كيا حصرياً)",
            descriptionEn = "Fuel Pressure Regulator Malfunction (Kia Specific)",
            category = "Engine (المحرك)",
            isKiaSpecific = true,
            symptoms = listOf(
                "عدم ثبات صوت المحرك وتذبذب عداد الـ RPM بشكل مستمر",
                "تقطيع وتفتفة مرافقة لخروج دخان أبيض خفيف عند تشغيل السيارة",
                "انطفاء المحرك في وضع الوقوف التام"
            ),
            causes = listOf(
                "تلف أو اتساخ ذرات الأوساخ لفلتر الصمام الصغير ببلف تنظيم الضغط",
                "خلل في الإشارة الكهربائية القادمة من كمبيوتر ECU لبلف التنظيم",
                "تلف الحلقات المطاطية (O-rings) لمانع تهريب صمام الضغط"
            ),
            solutions = listOf(
                "فك بلف منظم ضغط الديزل وتنظيفه بحذر شديد ببخاخ ضغط قوي",
                "استبدال الحلقات المطاطية (O-Rings) المانعة لتهريب الضغط",
                "استبدال بلف منظم ضغط الديزل المرتفع (IMV / Inlet Metering Valve)"
            )
        ),
        KiaDtcDefinition(
            code = "P1102",
            descriptionAr = "قراءة حساس الهواء MAF ضمن المدى ولكن أقل من المتوقع (كيا حصرياً)",
            descriptionEn = "MAF Sensor Signal Low - In Range But Lower Than Expected",
            category = "Engine (المحرك)",
            isKiaSpecific = true,
            symptoms = listOf(
                "بلادة وبطء شديد في استجابة دواسة البنزين والوقود",
                "تأخر تفعيل شاحن التوربو بالشكل المطلوب",
                "معدل انبعاثات وتلوث مرتفع بفتحة الشكمان"
            ),
            causes = listOf(
                "اتساخ شديد بحساس MAF بسبب رداءة فلتر الهواء المستعمل",
                "تهريب هواء خفيف جداً في ليات التنفيس أو بلف تبخير الزيت",
                "تجمع أبخرة الزيت من التربو على شبكة الحساس"
            ),
            solutions = listOf(
                "استخدام بخاخ تنظيف الكترونيات جاف لتنظيف الحساس مع تركيز الرش برفق",
                "التحقق من عدم وجود شروخ مجهرية في مجاري سحب الهواء بعد الفلتر",
                "فحص وصيانة بلف تبخير زيت المحرك (PCV Valve) لمنع خروج الزيت مجدداً"
            )
        ),
        KiaDtcDefinition(
            code = "P1693",
            descriptionAr = "خلل في جهاز الاستقبال ونظام الأمان المشفر للإيموبلايزر (كيا حصرياً)",
            descriptionEn = "Immobilizer Transponder Error (Kia Specific)",
            category = "Body/Chassis (الهيكل والتعليق)",
            isKiaSpecific = true,
            symptoms = listOf(
                "دوران المحرك (دق سلف) دون استجابة للاشتعال والتشغيل التام",
                "ظهور رمز المفتاح باللون الأصفر أو اختفائه تماماً من لوحة العدادات",
                "تعطيل عمل بخاخات الديزل وطرمبة الوقود كإجراء حماية"
            ),
            causes = listOf(
                "تلف أو ضعف الشريحة المغناطيسية بداخل مفتاح السيارة",
                "خلل في حلقة هوائي الإيموبلايزر (Antenna Coil) المحيطة بفتحة السويتش",
                "فقدان الاتصال والتشفير بين كمبيوتر المحرك ECU ووحدة الإيموبلايزر Smartra"
            ),
            solutions = listOf(
                "تجربة تشغيل السيارة بالمفتاح الاحتياطي للتأكد من سلامة الشريحة",
                "فحص وتنظيف فيشة ملف الهوائي المحيط بالسويتش أو استبداله إذا تطلب الأمر",
                "عمل إعادة تهيئة وبرمجة لمفتاح السيارة باستخدام جهاز الفحص OBD2"
            )
        ),
        KiaDtcDefinition(
            code = "P1725",
            descriptionAr = "خلل في إشارة حساس سرعة ناقل الحركة التلقائي (كيا حصرياً)",
            descriptionEn = "Input/Turbine Speed Sensor Circuit Malfunction",
            category = "Transmission (ناقل الحركة)",
            isKiaSpecific = true,
            symptoms = listOf(
                "دخول القير في وضع الطوارئ وتوقفه عند تعشيق محدد",
                "وميض لمبة O/D (أوفر درايف) أو لمبة الأعطال بالطبلون",
                "نتعة قوية جداً عند التبديل من وضع الوقوف P إلى وضع الحركة D"
            ),
            causes = listOf(
                "عطل حساس سرعة عمود الدوران الداخلي للقير (Input Speed Sensor)",
                "ترسب جزيئات الرايش المعدني من الكلتشات وتغطيتها لرأس الحساس المغناطيسي",
                "تلف الكيبل الكهربائي المار بجانب القير"
            ),
            solutions = listOf(
                "فك الحساس وتنظيف رأسه المغناطيسي من برادة الحديد المتراكمة والرايش",
                "قياس مقاومة الحساس والتأكد من مطابقتها للقيمة المصنعية لكيا (حوالي 250 - 400 أوم)",
                "استبدال حساس سرعة عمود الدوران لقير كيا"
            )
        ),
        KiaDtcDefinition(
            code = "P2002",
            descriptionAr = "انسداد أو انخفاض كفاءة فلتر جزيئات الديزل DPF (شكمان البيئة)",
            descriptionEn = "Diesel Particulate Filter Efficiency Below Threshold",
            category = "Emission (العادم والانبعاثات)",
            isKiaSpecific = false,
            symptoms = listOf(
                "تراجع حاد في عزم المحرك وعدم تعدي الـ RPM لـ 2800 دورة بالدقيقة",
                "ارتفاع ملحوظ في معدل استهلاك وقود الديزل",
                "خروج رائحة عادم غير مكتمل الاحتراق مقلقة بالصالون"
            ),
            causes = listOf(
                "امتلاء قنوات فلتر DPF بالرماد غير القابل للاحتراق (Soot/Ash)",
                "توقف دورة التجديد الذاتي للفلتر بسبب القيادة القصيرة داخل المدينة",
                "تلف حساس فرق الضغط المطلق المغذي لكمبيوتر السيارة بقيم الفلتر"
            ),
            solutions = listOf(
                "القيام بمشوار قيادة طويل سريع بـ RPM مستقر فوق 2500 لتنشيط التجديد التلقائي (DPF Regeneration)",
                "تفعيل خيار التجديد الإجباري (Forced Regeneration) عبر جهاز الفحص الذكي",
                "فك الشكمان وتنظيف فلتر DPF يدوياً بمواد تذيب الكربون وتغسله بالكامل"
            )
        ),
        KiaDtcDefinition(
            code = "P2113",
            descriptionAr = "خلل معايرة وضع الصفر لبوابة الفراشة الخانقة للديزل (كيا حصرياً)",
            descriptionEn = "Throttle Actuator Minimal Stop Performance (Kia Specific)",
            category = "Engine (المحرك)",
            isKiaSpecific = true,
            symptoms = listOf(
                "خشونة واهتزاز عنيف جداً عند إطفاء محرك السيارة (Engine Shudder)",
                "دخول محرك الديزل في وضع الأمان وتقييد العزم",
                "صعوبة اشتعال واستجابة لدواسة الديزل"
            ),
            causes = listOf(
                "تراكم الكربون والزيوت اللزجة داخل بلف الفراشة الخانقة وسد حركة الترس",
                "تكسر التروس البلاستيكية الداخلية المشغلة للبوابة الإلكترونية",
                "حاجة البوابة لتعلم قيم الصفر برمجياً بعد فك البطارية"
            ),
            solutions = listOf(
                "فك بوابة الهواء وتنظيفها بالكامل باستخدام منظف كربريتر جاف وقطعة قماش",
                "التأكد من سلامة تروس البوابة الداخلية وعدم وجود تكسر أو تآكل بها",
                "إجراء برمجة وإعادة تعلّم وضع الخمول للبوابة (Throttle Body Alignment) عبر تطبيق OBD"
            )
        )
    )
}

data class SensorHistoryPoint(
    val timestamp: Long,
    val rpm: Int,
    val coolantTemp: Int,
    val fuelPressure: Int
)

