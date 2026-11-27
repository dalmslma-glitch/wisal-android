# وسيط تخزين وصال عبر Cloudflare Workers وBackblaze B2

هذا الخادم يحتفظ بمفاتيح Backblaze السرية خارج تطبيق Android. يتحقق من Firebase ID Token قبل إصدار رابط رفع مؤقت، ثم يعرض الملف عبر رابط وسيط عشوائي إلى Bucket خاص.

## إعداد Backblaze
- Bucket: `wisal-media-storage`
- Region: `us-east-005`
- Endpoint: `https://s3.us-east-005.backblazeb2.com`
- أنشئ Application Key محدوداً بهذا الـBucket، بصلاحيات قراءة وكتابة الملفات فقط.

## إعداد Cloudflare Worker
1. أنشئ حساب Cloudflare المجاني، ثم افتح Workers & Pages.
2. أنشئ Worker باسم `wisal-media-worker`.
3. من إعدادات Worker أضف Secrets التالية (لا تضعها في GitHub ولا ترسلها لأحد):
   - `B2_KEY_ID`: Key ID من Backblaze.
   - `B2_APPLICATION_KEY`: المفتاح السري.
   - `B2_BUCKET_NAME`: `wisal-media-storage`.
4. من جهازك، داخل هذا المجلد نفّذ:
   `npm install`
   ثم `npm run deploy`.
5. عنوان الخدمة سيكون مثل `https://wisal-media-worker.<your-subdomain>.workers.dev`.

## نقاط الأمان والحدود
- يقبل الخادم فقط Firebase ID Tokens التابعة لمشروع `wisal-f7bb5`.
- روابط الرفع مؤقتة لخمس دقائق.
- الملفات محدودة إلى 25 MB وأنواع MIME الشائعة.
- Bucket يبقى Private. رابط العرض يعمل كرمز حامل غير قابل للتخمين عملياً؛ من يملك الرابط يستطيع عرض ذلك الملف، لذا لا تنشر روابط المحادثات علناً.
- الخطة المجانية لـCloudflare Workers لها حدود استخدام، وBackblaze B2 لها حدود مجانية/رسوم عند تجاوز الحصة. راجع الأسعار الحالية قبل التوسع.
- هذا الخادم لا يوفّر تشفيراً طرفاً إلى طرف؛ لا ترفع محتوى شديد الحساسية قبل إضافة ذلك التصميم واختباره.
