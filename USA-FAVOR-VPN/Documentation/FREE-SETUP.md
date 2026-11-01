# التشغيل المجاني — خطوة بخطوة

هاد النسخة 1.1.0-spark ما كتحتاجش Blaze ولا Cloud Functions باش تصاوب وتفعّل الأكواد. خاص غير Firebase Spark وFirestore وAuthentication، داخل الحصة المجانية. الخادم ديال VPN بوحدو؛ هادشي ما كيعطيكش استضافة VPN مجانية.

1. ثبّت USA-Admin.apk الجديد فوق النسخة القديمة، وسجّل بنفس حساب الأدمن. ما تمسحش بيانات تطبيق المستخدم اللي فيه كود مفعّل: الربط كيتبع تثبيت التطبيق.
2. افتح مشروع **usa-favor** فـFirebase Console. من **Firestore Database** اختار **Create database / Créer une base de données**. اختار **Standard** و**Native** ومعرّف **(default)**؛ ما تختارش Enterprise. اختار منطقة مناسبة، ثم Production mode. ما خاصكش تزيد بطاقة بنكية لهاد المسار.
3. من الأدمن افتح **App → Free setup → COPY FIRESTORE RULES**. رجع لـFirebase → Firestore → **Rules / Règles**، عوّض النص كامل بالقواعد المنسوخة واضغط **Publish / Publier**. الملف نفسه كاين فـFirebase-Backend/firestore.rules. ما تستعملش allow read, write: if true.
4. من **Authentication → Sign-in method / Mode de connexion** فعّل **Email/Password** للأدمن و**Anonymous / Anonyme** للمستخدم. الحساب ديالك خاص يبقى عندو admin:true؛ التسجيل العادي فـAuthentication بوحدو ما كيعطيش صلاحية الأدمن.
5. رجع للأدمن، دير Sign out ثم Sign in وRefresh. افتح **Licenses**، اختار المدة وStart on activation أوStart now، واضغط **GENERATE LICENSE**. غادي يبان كود حقيقي تقدر تنسخو وتبيعو يدوياً.
6. ثبّت USA-Favor.apk على هاتف Android 64-bit. دخّل الكود وفعّلو بالإنترنت. نفس الكود ما كيتفعّلش فتثبيت ثاني. فالأدمن، افتح أجهزة/جلسات الكود باش تشوف التثبيت المربوط والمحاولات المرفوضة. تقدر تدير Revoke أوReset binding. Reset ما كيرجعش مدة الاشتراك من الأول.
7. جرّب كود قصير، الإلغاء وإعادة الربط. من بعد زِد خادم تملك صلاحية إدارته حسب GATEWAY.md. بلا خادم منشور ومتحقق منو، التطبيق غادي يبيّن أنه ما كاين حتى خادم متاح؛ تفعيل الكود ماشي دليل أن VPN خدام.

إلا بقى PERMISSION_DENIED، تأكد من المشروع (usa-favor)، قاعدة (default)، نشر القواعد الجديدة وصلاحية admin:true. إلا كان App Check مفروض من قبل، سجّل تثبيت الاختبار بالطريقة الرسمية قبل استعماله؛ نسخة Debug ماشي إصدار Play Integrity منشور.

Spark عندو حدود يومية للتخزين والقراءة والكتابة وعدد المستخدمين. إلا سالات الحصة كتوقف الطلبات حتى التجديد؛ ماشي خدمة بلا حدود. ما كاينش حظر أوتوماتيكي لمجرد محاولة هاتف ثاني، باش ما يقدرش شي واحد يوقف كود ديال زبون بمحاولة متعمدة. الحظر من قرار الأدمن.
