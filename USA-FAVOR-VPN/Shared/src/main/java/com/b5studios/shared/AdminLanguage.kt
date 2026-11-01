package com.b5studios.shared

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

val LocalAdminLanguage=staticCompositionLocalOf{"en"}
@Composable fun adminLabel(text:String)=AdminLanguage.translate(text,LocalAdminLanguage.current)
@Composable fun AdminText(text:String,modifier:Modifier=Modifier,color:Color=Color.Unspecified,fontSize:TextUnit=TextUnit.Unspecified,fontWeight:FontWeight?=null,letterSpacing:TextUnit=TextUnit.Unspecified,style:TextStyle=LocalTextStyle.current){
 Text(adminLabel(text),modifier=modifier,color=color,fontSize=fontSize,fontWeight=fontWeight,letterSpacing=letterSpacing,style=style)
}
object AdminLanguage {
 private val arabic=mapOf(
 "welcome" to "رسالة الترحيب","announcement" to "الإعلان","maintenance message" to "رسالة التوقف","default server" to "الموقع الافتراضي","server order" to "ترتيب المواقع","support" to "الدعم","privacy url" to "رابط الخصوصية","terms url" to "رابط الشروط","help" to "المساعدة","activation popup" to "رسالة التفعيل","minimum version" to "أقل إصدار","update notice" to "رسالة التحديث","maintenance" to "توقف الخدمة","show social" to "إظهار التواصل","show tips" to "إظهار النصائح","name" to "الاسم","icon" to "الأيقونة","url" to "الرابط","info" to "معلومات","shield" to "حماية","globe" to "العالم","message" to "رسالة","overview" to "نظرة عامة","license health" to "حالة الرخص","security alerts" to "تنبيهات الأمان","infrastructure" to "خدمة VPN","quick actions" to "إجراءات سريعة",
 "One code is bound to one installation. Another installation is refused; you choose whether to ban it." to "يرتبط كل كود بتثبيت واحد. يُرفض التثبيت الآخر، ويمكنك اختيار إيقاف الرخصة.",
 "Second activation was rejected. This is an attempt, not proof of two live VPN sessions." to "رُفض التفعيل الثاني. هذه محاولة تفعيل وليست دليلاً على اتصال جهازين.",
 "Navigation and license authorization remain available even when a dashboard panel is hidden." to "تبقى القائمة وإدارة الرخص متاحة حتى عند إخفاء إحدى بطاقات اللوحة.",
 "HTTPS or support email • order controls client display" to "روابط HTTPS أو بريد الدعم • الترتيب يتحكم في ظهورها عند المستخدم",
 "Package library" to "مكتبة الحزم","Import a package" to "استيراد حزمة","CHOOSE JSON PACKAGE" to "اختيار حزمة JSON","Package action" to "نوع الحزمة","PUBLISH PACKAGE" to "نشر الحزمة",
 "Import section tools, supported provider details or update settings. Review the package before publishing." to "استورد أدوات الأقسام أو بيانات مزوّد مدعوم أو إعدادات التحديث. راجع الحزمة قبل نشرها.",
 "The package is validated. Publishing replaces the matching configuration." to "تم التحقق من الحزمة. النشر يعوّض الإعدادات المقابلة لها.",
 "Home" to "الرئيسية","Licenses" to "الرخص","VPN" to "VPN","App" to "التطبيق","Social" to "التواصل",
 "Refresh" to "تحديث","Sign out" to "تسجيل الخروج","Saved successfully" to "تم الحفظ بنجاح","Request failed" to "تعذّر تنفيذ الطلب",
 "Administrator sign in" to "دخول المالك","Email" to "البريد الإلكتروني","Password" to "كلمة السر","SIGN IN" to "تسجيل الدخول","Reset password" to "استرجاع كلمة السر",
 "Unlock on this phone" to "فتح الإدارة على هذا الهاتف","UNLOCK WITH PHONE" to "فتح بالبصمة أو قفل الهاتف","Use another account" to "استخدام حساب آخر",
 "Sign in once with your authorized owner account. Later, use your phone unlock without typing a password." to "اربط حساب المالك المعتمد مرة واحدة، ثم افتح الإدارة بقفل الهاتف دون كتابة كلمة السر.",
 "Private management • Secure licenses • B5 STUDIOS" to "إدارة خاصة • رخص آمنة • B5 STUDIOS",
 "Management studio" to "استوديو الإدارة","App information" to "معلومات التطبيق","Client sections" to "أقسام المستخدم","Admin layout" to "تنظيم لوحة الإدارة","Service help" to "مساعدة الخدمة",
 "Control center" to "لوحة الإدارة","B5 STUDIOS • PRIVATE MANAGEMENT" to "B5 STUDIOS • إدارة خاصة","POWERED BY B5 STUDIOS" to "بإشراف B5 STUDIOS",
 "Secure online license management" to "إدارة الرخص عبر الإنترنت","Customize" to "تخصيص","Total licenses" to "مجموع الرخص","Active" to "نشطة","Expired" to "منتهية","Banned" to "موقوفة",
 "Subscription overview" to "نظرة على الاشتراكات","Unused codes" to "أكواد غير مستخدمة","Activated in last 7 days" to "التفعيلات خلال 7 أيام","OPEN LICENSE STUDIO" to "إدارة الرخص",
 "Device protection" to "حماية الأجهزة","Recorded conflicts (latest 100)" to "محاولات التفعيل المرفوضة","REVIEW DEVICE ATTEMPTS" to "مراجعة محاولات الأجهزة",
 "Management workspace" to "مساحة الإدارة","CLIENT INFORMATION & SECTIONS" to "معلومات وأقسام المستخدم","SOCIAL LINKS & SUPPORT" to "الروابط والدعم","VPN SETTINGS" to "إعدادات VPN",
 "License studio" to "إدارة الرخص","Single-use codes • one installation per license" to "كود واحد • تثبيت واحد لكل رخصة","Create a subscription" to "إنشاء اشتراك",
 "Duration days · custom supported" to "المدة بالأيام","Start on activation" to "تبدأ عند التفعيل","Start now" to "تبدأ الآن","GENERATE LICENSE" to "توليد كود","COPY CODE" to "نسخ الكود","SHARE VIA WHATSAPP" to "مشاركة عبر واتساب",
 "Search loaded licenses" to "البحث في الرخص","SEARCH ENTIRE INVENTORY BY FULL CODE" to "البحث بالكود الكامل","LOAD MORE LICENSES" to "تحميل المزيد",
 "all" to "الكل","active" to "نشطة","expired" to "منتهية","revoked" to "موقوفة","unused" to "غير مستخدمة",
 "Status" to "الحالة","Activated" to "التفعيل","Expires" to "تنتهي في","Device bound" to "الجهاز مربوط","Yes" to "نعم","No" to "لا","Not activated" to "لم تفعّل بعد",
 "COPY" to "نسخ","Revoke license" to "إيقاف الرخصة","Reactivate license" to "إعادة تفعيل الرخصة","Extend by days" to "زيادة المدة بالأيام","EXTEND" to "تمديد","VIEW DEVICE / ATTEMPTS" to "الأجهزة والمحاولات","Reset device binding" to "إعادة ربط الجهاز","Delete license" to "حذف الرخصة",
 "Authorized sessions" to "الأجهزة المعتمدة","No sessions" to "لا توجد جلسات","Confirm" to "تأكيد","Cancel" to "إلغاء","Close" to "إغلاق","Save" to "حفظ",
 "Attempts from another installation" to "محاولات من تثبيت آخر","Reported phone" to "الهاتف المسجّل","Attempted" to "وقت المحاولة","BAN THIS LICENSE" to "إيقاف هذه الرخصة",
 "VPN Connection Settings" to "إعدادات اتصال VPN","Service status" to "حالة الخدمة","Provider" to "المزوّد","Connection details" to "بيانات الربط","Saved" to "محفوظة","Not added" to "غير مضافة","Provider setup" to "ربط المزوّد","Configured" to "مُعدّ","Pending integration" to "في انتظار الربط","Expiry protection" to "حماية مدة الرخصة","Verified" to "تم التحقق","Pending" to "قيد الإعداد",
 "REFRESH SERVICE STATUS" to "تحديث حالة الخدمة","Add provider connection" to "إضافة بيانات المزوّد","Public partner project ID" to "معرّف مشروع الشراكة العام","Provider HTTPS backend address" to "عنوان خدمة المزوّد HTTPS","SAVE CONNECTION DETAILS" to "حفظ بيانات الربط",
 "Customer connections" to "اتصالات الزبناء","Allow customer connections" to "السماح باتصالات الزبناء","Paused" to "موقوفة مؤقتاً","Allowed" to "مسموح بها","Waiting for provider" to "في انتظار المزوّد","USA server locations" to "مواقع الولايات المتحدة","Per-app routing" to "اختيار التطبيقات","Supported" to "مدعوم","Unavailable" to "غير متاح","SAVE LOCATIONS" to "حفظ المواقع",
 "App management" to "إدارة التطبيق","Remote client settings" to "إعدادات المستخدم","PUBLISH SETTINGS" to "نشر الإعدادات","Social links" to "روابط التواصل","ADD LINK" to "إضافة رابط","PUBLISH SOCIAL LINKS" to "نشر روابط التواصل","Move up" to "للأعلى","Remove" to "حذف","New link" to "رابط جديد",
 "ADD CLIENT SECTION" to "إضافة قسم","PUBLISH SECTIONS" to "نشر الأقسام","Title" to "العنوان","Body text" to "النص","Optional HTTPS / email URL" to "رابط HTTPS أو بريد اختياري","Visible" to "ظاهر","Hidden" to "مخفي","home" to "الرئيسية","settings" to "الإعدادات",
 "Section type" to "نوع القسم","Information card" to "بطاقة معلومات","Code tool" to "أداة بالكود","Button action" to "وظيفة الزر","Button label" to "نص الزر","LOAD EXAMPLE TOOL" to "إدراج أداة تجريبية","HTML code" to "كود HTML","CSS design" to "تصميم CSS","JavaScript behavior" to "وظائف JavaScript",
 "none" to "بدون زر","link" to "فتح رابط","license" to "عرض الرخصة","activity" to "نشاط الاتصال","refresh" to "تحديث البيانات","select_apps" to "اختيار التطبيقات","connect" to "اتصال VPN","disconnect" to "قطع الاتصال","update" to "تحديث التطبيق",
 "App updates" to "تحديثات التطبيق","Latest version code" to "رقم الإصدار الجديد","Minimum version code" to "أقل إصدار مسموح","APK HTTPS download link" to "رابط تنزيل APK عبر HTTPS","APK SHA-256 checksum" to "بصمة APK من نوع SHA-256","Update message" to "رسالة التحديث","Enable update notice" to "إظهار التحديث","Require update" to "التحديث إجباري","PUBLISH UPDATE" to "نشر التحديث",
 "Organize the Admin dashboard" to "تنظيم لوحة الإدارة","ADD PRIVATE ADMIN CARD" to "إضافة بطاقة خاصة","SAVE DASHBOARD LAYOUT" to "حفظ تنظيم اللوحة","Language" to "اللغة","Lock" to "قفل الإدارة","Code editor" to "محرر الأكواد",
 "Provider integration details" to "تفاصيل الربط المتقدمة",
 "Enter the public details supplied with your authorized partner account." to "أدخل البيانات العامة التي يزوّدك بها حساب الشراكة المرخّص.",
 "Public connection details only. Keep provider passwords and master API keys with your service operator." to "هذه الحقول للبيانات العامة فقط. تبقى كلمات السر ومفاتيح المزوّد لدى مشغّل الخدمة.",
 "Allowing connections takes effect when your provider service is ready." to "يُسمح بالاتصال عندما تصبح خدمة المزوّد جاهزة.",
 "Supported USA locations appear here after provider integration." to "تظهر المواقع الأمريكية المدعومة هنا بعد ربط المزوّد.",
 "Provider not connected. Saved details become usable after your authorized provider and client app integration are prepared. Saving this form does not create a VPN connection." to "المزوّد غير مربوط. تصبح البيانات قابلة للاستخدام بعد تهيئة المزوّد وتطبيق المستخدم. حفظ البيانات وحده لا يشغّل VPN.",
 "New connection details are saved for integration. The current verified service remains in use until the new details are approved." to "تم حفظ بيانات جديدة للربط. تبقى الخدمة المعتمدة الحالية حتى اعتماد البيانات الجديدة.",
 "Create cards, native actions and isolated HTML/CSS/JavaScript tools. Changes arrive when the client refreshes." to "أنشئ بطاقات وأزرار خدمات وأدوات HTML/CSS/JavaScript معزولة. تصل التغييرات عند تحديث بيانات المستخدم.",
 "Code tools run without network, device permissions, Firebase credentials or a native bridge." to "الأدوات البرمجية تعمل دون وصول للشبكة أو صلاحيات الهاتف أو بيانات Firebase أو ربط مباشر بوظائف النظام.",
 "Use a newer APK signed with the same key. Android asks for installation confirmation and then offers Open." to "استعمل APK أحدث بنفس مفتاح التوقيع. يطلب Android تأكيد التثبيت ثم يعرض زر فتح.",
 "Manage licenses, provider connection details, USA locations, announcements and social links from this Admin app." to "دبّر الرخص وبيانات المزوّد والمواقع الأمريكية والإعلانات وروابط التواصل من تطبيق الإدارة.",
 "Your licensing service and authorized VPN provider must be prepared once before customers can activate and connect. The customer app must include the provider's supported VPN integration." to "تحتاج خدمة الرخص والمزوّد المرخّص لتهيئة أولية، ويجب دمج مكتبة المزوّد المدعومة في تطبيق المستخدم.",
 "If sign-in, saving or activation is unavailable, the service is not fully prepared. Contact your service operator; no server or database console is part of everyday Admin use." to "إذا تعذّر الدخول أو الحفظ أو التفعيل، فالخدمة لم تجهز بالكامل. لا تحتاج الإدارة اليومية إلى لوحة خادم أو قاعدة بيانات."
 )
 fun translate(text:String,language:String):String {
  if(language!="ar")return text
  arabic[text]?.let{return it}
  if(text.matches(Regex("[0-9]+d")))return text.dropLast(1)+" يوم"
  if(text.startsWith("New section "))return "قسم جديد "+text.removePrefix("New section ")
  return text
 }
}
