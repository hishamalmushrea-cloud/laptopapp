# البناء / Build

## 1) المتطلبات

مقروءة من ملفات البناء نفسها (`app/build.gradle`, `app/app/build.gradle`,
`app/gradle/wrapper/gradle-wrapper.properties`):

| المكوّن | الإصدار المطلوب | المصدر |
|---|---|---|
| JDK | **17** | يستلزمه AGP 8.4.x |
| Gradle | **8.14.5** | `gradle-wrapper.properties` |
| Android Gradle Plugin | **8.4.2** | `app/build.gradle` |
| `compileSdk` | **35** | `app/app/build.gradle` |
| `minSdk` / `targetSdk` | **26** / **28** | `app/app/build.gradle` |
| NDK | **24.0.8215888** | `ndkVersion` |
| CMake | **3.22.1** | `externalNativeBuild` + `cpp/CMakeLists.txt` |
| ABI | `arm64-v8a` فقط | `abiFilters` |

تثبيت SDK/NDK:

```bash
sdkmanager "platforms;android-35" "build-tools;34.0.0" \
           "ndk;24.0.8215888" "cmake;3.22.1"
```

## 2) جلب الأصول الثنائية (إلزامي قبل أي بناء)

المصدر في git لا يحتوي الثنائيات (~480 MB). بدونها يفشل `mergeDebugAssets`:

```bash
scripts/fetch-assets.sh              # تنزيل + تحقق SHA-256
scripts/fetch-assets.sh --verify-only # تحقق فقط مما هو موجود
```

الأصول تُنزَّل من الكوميت المثبّت في `scripts/upstream.env`، لذا البايتات مطابقة لما
شحنه upstream تمامًا.

## 3) بناء التطبيق

```bash
cd app
./gradlew assembleDebug
```

> **خطأ في upstream أصلحناه هنا:** `gradlew` مسجّل في git بالوضع `100644` (غير قابل
> للتنفيذ) في مستودع `winlator-app` نفسه — نفس blob sha ‏`1b6c787337ffb79f0e3cf8b1e9f00f680a959de1`.
> لذلك `./gradlew` يفشل بـ **exit code 126** ("cannot execute"). هذا أول ما أسقط بناء CI
> عندنا (run 34535792988). أصلحناه بـ `git update-index --chmod=+x app/gradlew`
> فصار `100755`. لو بنيت من نسخة upstream الأصلية ستواجه نفس الخطأ.

الناتج: `app/app/build/outputs/apk/debug/app-debug.apk`

> **ملاحظة على `app/app/build.gradle`:** `minifyEnabled true` مفعّل داخل `buildType` الخاص
> بـ **debug**. هذا غير معتاد — يبطئ دورات التطوير ويخفي مشاكل ProGuard عن بناء التطوير.
> اقتراح: تعطيله في debug وإبقاؤه في release فقط.
>
> لا يوجد `signingConfig` لـ release، لذا `assembleRelease` ينتج APK غير موقّع. أضف
> `signingConfigs` قبل أي توزيع.

## 4) بناء Vortek وGladio (منفصل عن Gradle)

**مهم وغير موثّق upstream:** مجلدا `vortek/` و`gladio/` **ليسَا** جزءًا من بناء Gradle.
`app/app/src/main/cpp/CMakeLists.txt` يبني فقط:
`winlator`, `vortekrenderer`, `virglrenderer`, `midihandler`, `libadrenotools`, `gladiorenderer`
— وهي الجهة **المضيفة** (داخل تطبيق أندرويد).

أما `vortek/` فينتج `libvulkan_vortek.so` و`gladio/` ينتج `libGL.so` — وهي مكتبات
**داخل الحاوية** (جهة الضيف) تُحمَّل عبر Wine/Box64، وتُشحن معبّأة في
`app/app/src/main/assets/graphics_driver/vortek-2.1.tzst` و`gladio-1.1.tzst`.

الدليل من الكود (`app/app/src/main/java/com/winlator/XServerDisplayActivity.java`):

```java
722:  FileUtils.delete(new File(libDir, "libvulkan_vortek.so"));
742:  TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, this,
          "graphics_driver/vortek-" + DefaultVersion.VORTEK + ".tzst", rootDir);
764:  ... "graphics_driver/gladio-" + DefaultVersion.GLADIO + ".tzst" ...
```

لذا بعد تعديل `vortek/` أو `gladio/` يجب:

```bash
# (لم يُتحقق منه في هذه البيئة — لا يوجد Android SDK هنا)
NDK=$ANDROID_HOME/ndk/24.0.8215888
TOOLCHAIN=$NDK/build/cmake/android.toolchain.cmake

cmake -S vortek -B build-vortek \
  -DCMAKE_TOOLCHAIN_FILE=$TOOLCHAIN -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-26
cmake --build build-vortek

# ثم إعادة التعبئة ورفع رقم الإصدار في DefaultVersion.VORTEK
```

## 5) تنظيف

```bash
cd app && ./gradlew clean
rm -rf .cache/upstream        # ذاكرة السكربتات (يمكن إبقاؤها)
```

## 6) لماذا لا يمكن البناء داخل بيئة Arena (موثّق بالقياس)

البيئة التي يعمل فيها الوكيل لا تصل إلا إلى قائمة سماح محدودة. نتائج `curl -w %{http_code}`:

| المضيف | المطلوب | النتيجة |
|---|---|---|
| `api.adoptium.net` | JDK 17 | فشل SSL (`SSL_ERROR_SYSCALL`) |
| `dl.google.com` | Android SDK / NDK | `000` محجوب |
| `services.gradle.org` | Gradle 8.14.5 | `000` محجوب |
| `maven.google.com` | AGP 8.4.2 | `000` محجوب |
| `repo1.maven.org` / `plugins.gradle.org` | تبعيات AndroidX | `000` محجوب |
| `github.com` / `api.github.com` / `pypi.org` | — | `200` |

ولا يوجد JDK (`java: command not found`) ولا `sdkmanager`، و`/etc/apt/sources.list` فارغ.
**النتيجة: `assembleDebug` لا يمكن تنفيذه هنا إطلاقًا** — البناء يتم إما عبر
GitHub Actions (‏`ci/build.yml`) أو على جهاز فيه Android Studio.

## 7) ما الذي **تم** التحقق منه فعليًا

- سلامة بنية Gradle: `app/settings.gradle` (`include ':app'`)، و`gradle-wrapper.jar` موجود (59,821 بايت)، و`gradlew` قابل للتنفيذ، وصلاحيات `100755` محفوظة في git للسكربتات.
- `bash -n` على كل السكربتات، وتنفيذ حقيقي لـ `fetch-assets.sh`: تنزيل `pulseaudio.tzst` (45,548 بايت) و`libpulseaudio.so` (80,312) و`wine-gecko-2.47.4-x86_64.msi` (53,898,752) ثم `sha256sum -c` = **OK** للثلاثة.
- **اختبار سلبي:** إفساد بايت واحد جعل التحقق يفشل ويعيد exit code 1.
- `ci/build.yml` صالح YAML (وظيفتان: `assets` ‏4 خطوات ← `apk` ‏10 خطوات).
- **لم يُنفَّذ على Actions** لأن التوكن الآلي لا يملك صلاحية `workflows` (رسالة الرفض موثّقة في `ci/README.md`).

أول بناء على Actions أو على جهاز فيه SDK هو الاختبار الحقيقي — سجّل أي فرق هنا.
