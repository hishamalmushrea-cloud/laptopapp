# Upstream provenance & re-sync

هذا المستودع نسخة **مدموجة (vendored)** — ليست fork من GitHub ولا submodules.
هذا الملف يسجّل بالضبط من أين جاء كل جزء وكيف تحدّثه.

## Pinned revisions

مصدر الحقيقة الآلي: **[`scripts/upstream.env`](scripts/upstream.env)** (يقرأه كل من
`fetch-assets.sh` و`sync-upstream.sh`). الجدول أدناه نسخة مقروءة منه.

| المجلد هنا | المستودع الأصلي | الكوميت المثبّت | تاريخ الكوميت | ملاحظات |
|---|---|---|---|---|
| *(جذر المستودع)* | [`brunodev85/winlator`](https://github.com/brunodev85/winlator) | `5949297d9dc83ad24ce3f5119fe382da7c899a78` | 2026-08-19 | HEAD بعد وسم `v11.2.0` (`fb66541b`) |
| `app/` | [`brunodev85/winlator-app`](https://github.com/brunodev85/winlator-app) | `c03f6ab558c6f94cbac6ec0c791b12f3428fbdf6` | 2026-08-19 | "Bump version code to 32" |
| `vortek/` | [`brunodev85/vortek`](https://github.com/brunodev85/vortek) | `b1730c5def9b575672e671aee11d79ae7adc63d1` | 2026-08-19 | "Update utility functions" |
| `gladio/` | [`brunodev85/gladio`](https://github.com/brunodev85/gladio) | `116c0d14dedbea3bd057f98f1db138bb1efe225e` | 2026-08-04 | "Check the vertex attrib index limit" |

في upstream هذه الثلاثة الأخيرة submodules داخل `winlator`. هنا فُكّت وأصبحت مجلدات عادية
حتى يمكن التعديل عليها مباشرة دون التعامل مع مؤشرات submodules.

## ما الذي استُثني من الدمج

| المسار | السبب | كيف يعود |
|---|---|---|
| `*.tzst`, `*.msi`, `*.sf2` (73 ملفًا، ~480 MB) | ثنائيات، لا مصدر | `scripts/fetch-assets.sh` |
| `app/app/src/main/jniLibs/arm64-v8a/*.so` (24 ملفًا، 11 MB) | مكتبات مسبقة البناء | `scripts/fetch-assets.sh` |
| `.git`, `.gitmodules`, `.idea` | غير لازمة للتطوير | — |

قائمة البصمات الكاملة: [`scripts/assets.sha256`](scripts/assets.sha256).

## إعادة المزامنة مع upstream

```bash
# 1) ما الجديد؟
scripts/sync-upstream.sh

# 2) سحب الملفات المصدرية المتغيّرة (لا يلمس الثنائيات)
scripts/sync-upstream.sh --apply
git diff                       # راجع كل تغيير

# 3) تثبيت المؤشرات الجديدة
$EDITOR scripts/upstream.env

# 4) تحديث الأصول الثنائية وبصماتها
scripts/fetch-assets.sh --force
scripts/refresh-checksums.sh
git add scripts/assets.sha256 scripts/upstream.env

# 5) بناء للتأكد
cd app && ./gradlew assembleDebug
```

## الفروقات المقصودة عن upstream

هذه إضافاتنا، ولا يجب أن يمسحها `sync-upstream.sh` (مستثناة فيه صراحةً):

- `README.md` (أُعيدت كتابته)، `NOTICE`، `UPSTREAM.md`
- `docs/` — `BUILD.md`, `ARCHITECTURE.md`, `ROADMAP.md`
- `scripts/` — `fetch-assets.sh`, `sync-upstream.sh`, `refresh-checksums.sh`, `install-ci.sh`, `upstream.env`, `assets.sha256`
- `ci/` — `build.yml` (بناء آلي، غير موجود upstream إطلاقًا) + `README.md`
- `.gitignore` الخاص بنا (يحلّ محل ملف upstream في الجذر)

## ملاحظات موثّقة أثناء الدمج

- `app/app/src/main/cpp/libadrenotools/.gitmodules` ملف **بقايا** من مشروع libadrenotools الأصلي.
  مستودع `winlator-app` لا يملك `.gitmodules` في جذره، والمجلد
  `libadrenotools/lib/linkernsbypass/` موجود كملفات عادية (7 ملفات) — أي أن البناء **لا** يحتاج
  تهيئة submodule إضافية. تحقّقنا من ذلك: `libadrenotools/CMakeLists.txt` يستدعي
  `add_subdirectory(lib/linkernsbypass)` والملفات حاضرة.
- `app/app/build.gradle`: `compileSdk 35`، `minSdkVersion 26`، **`targetSdkVersion 28`**،
  `versionCode 32`، `versionName "11.2"`، `ndkVersion 24.0.8215888`، CMake `3.22.1`.
- **سبب `targetSdkVersion 28` مؤكَّد من upstream نفسه** (لم يعد استنتاجًا): في كوميت
  `0757a30` بمستودع `winlator-app` كتب المطوّر: *"the foreground service time limit does
  not apply to this project as long as `targetSdkVersion` is below 35"*. أي أن البقاء تحت
  28 قرار مقصود له تبعات على سلوك الخدمة الأمامية، وليس إهمالًا.

## فرق معلّق من upstream (وقت الدمج)

`scripts/sync-upstream.sh` أظهر أن مستودع `winlator-app` تحرّك بعد التثبيت
(`c03f6ab5` → `4f55d11`)، بينما `winlator` ما زال على `5949297d`.
التغيير هو **PR #39 المدموج**: *"fix container closes on background"* — خدمة أمامية
(Foreground Service) تحمي الجلسة من الإغلاق عند خروج التطبيق للخلفية.

**11 ملفًا** تختلف:

```
app/build.gradle                                   (+ androidx.lifecycle:lifecycle-process:2.5.1)
app/src/main/AndroidManifest.xml
app/src/main/java/com/winlator/SettingsFragment.java
app/src/main/java/com/winlator/XServerDisplayActivity.java
app/src/main/java/com/winlator/services/ForegroundService.java
app/src/main/java/com/winlator/services/NotificationUtils.java
app/src/main/res/drawable/icon_notification.png
app/src/main/res/layout/settings_fragment.xml
app/src/main/res/values/strings.xml  (+ values-pt, values-ru)
```

من كوميتات upstream: `a4c4d1b` تنفيذ الخدمة الأمامية · `45414e0` WeakReference لـ
NotificationUtils · `0757a30` إزالة مؤقّت 6 ساعات · `19dc687` إصلاح ANR وسباق في
بثّ حالة الشاشة · `4f55d11` دمج PR #39.

**قرارنا: لم نسحبها.** التثبيت بقي على الحالة المُصدَرة `v11.2.0` لأنها حالة متماسكة
ومجرَّبة، بينما هذه كوميتات بعد الإصدار ولا يمكن بناؤها/اختبارها في هذه البيئة.
لسحبها عندما تجهّز بيئة Android SDK:

```bash
scripts/sync-upstream.sh --apply && git diff
```

