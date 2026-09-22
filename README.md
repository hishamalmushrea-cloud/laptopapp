# 🌟 laptopapp

![C](https://img.shields.io/badge/C-A8B9CC?style=for-the-badge&logo=c&logoColor=white) ![Repo Size](https://img.shields.io/github/repo-size/hishamalmushrea-cloud/laptopapp?style=for-the-badge) ![Issues](https://img.shields.io/github/issues/hishamalmushrea-cloud/laptopapp?style=for-the-badge) ![Last Commit](https://img.shields.io/github/last-commit/hishamalmushrea-cloud/laptopapp?style=for-the-badge) [![License](https://img.shields.io/github/license/hishamalmushrea-cloud/laptopapp?style=for-the-badge)](https://github.com/hishamalmushrea-cloud/laptopapp/blob/main/LICENSE)

## 📖 About this Project
Welcome to the laptopapp repository!

## 🚀 Tech Stack
- **Primary Language:** C

## 🔗 Connect & Support
[![Trendshift](https://trendshift.io/api/badge/repositories/4119)](https://trendshift.io/)
[![Discord](https://img.shields.io/badge/Discord-Join%20Us-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.com/)
[![X (formerly Twitter) Follow](https://img.shields.io/twitter/follow/hishamalmushrea-cloud?style=social)](https://x.com/hishamalmushrea-cloud)

---

> نسخة تطويرية من [Winlator](https://github.com/brunodev85/winlator) مُدمَجة في مستودع واحد،
> جاهزة للتعديل والبناء.
>
> A single-repo, build-ready development fork of Winlator — the Android app that runs
> Windows (x86_64) applications through Wine + Box86/Box64.

---

## لماذا هذا المستودع؟ / Why this repo

المشروع الأصلي موزّع على **أربعة مستودعات** عبر git submodules، بلا CI، وبلا توثيق بناء.
هنا تم:

1. **دمج الشجرة**: `app/` (من `winlator-app`) + `vortek/` + `gladio/` أصبحت مجلدات عادية — لا submodules.
2. **فصل الكود عن الثنائيات**: المصدر فقط في git (**16 MB، 1049 ملفًا**). الملفات الضخمة
   (**480 MB، 73 ملفًا**) تُجلب عند الطلب وتُتحقّق ببصمة SHA-256.
3. **بناء قابل للتكرار**: كل ملف ثنائي مثبّت على كوميت upstream محدّد + بصمة.

| | الحجم | في git؟ |
|---|---|---|
| الكود المصدري (Java/C/C++/GLSL/XML/CMake) | 16 MB | ✅ نعم |
| `rootfs.tzst` + مكونات Wine + DXVK/VKD3D/Turnip + `jniLibs` | 480 MB | ❌ يُجلب بسكربت |

---

## البدء السريع / Quick start

```bash
# 1) جلب الأصول الثنائية (~480 MB) والتحقق من بصماتها
scripts/fetch-assets.sh

# 2) بناء APK (يتطلب Android SDK + NDK 24.0.8215888 + CMake 3.22.1)
cd app && ./gradlew assembleDebug
# الناتج: app/app/build/outputs/apk/debug/app-debug.apk
```

التفاصيل الكاملة: **[docs/BUILD.md](docs/BUILD.md)** · البنية المعمارية: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**

> 📱 **تعمل من الهاتف؟** اقرأ **[docs/PHONE-WORKFLOW.md](docs/PHONE-WORKFLOW.md)** أولًا —
> فيه كيف تعدّل وتجرّب وتبني بدون كمبيوتر.

---

## محتوى المستودع / Layout

```
app/                      شجرة Gradle (من winlator-app)
├── app/src/main/java/    طبقة أندرويد — Java (حاويات، إدخال، XServer)
├── app/src/main/cpp/     كود أصلي: vortekrenderer، gladiorenderer، virglrenderer،
│                         libadrenotools، midihandler، winlator
├── app/src/main/assets/  بيانات التطبيق (JSON محفوظة في git، الثنائيات تُجلب)
└── app/src/main/jniLibs/ مكتبات .so مسبقة البناء (تُجلب)
vortek/                   مغلّف Vulkan فوق تعريف الجهاز المضيف (D3D → Vulkan)
gladio/                   OpenGL عبر GLES
wine_addons/              Wine Mono / Gecko .msi (تُجلب)
installable_components/   Box64، DXVK، VKD3D، WineD3D، Turnip (تُجلب)
android_alsa/             خادم ALSA
glibc_patches/            رقع glibc (من Termux Pacman)
input_controls/           ملفات تعريف الإدخال
scripts/                  fetch-assets.sh · sync-upstream.sh · refresh-checksums.sh
docs/                     BUILD.md · ARCHITECTURE.md · ROADMAP.md
```

## السكربتات / Scripts

| السكربت | الوظيفة |
|---|---|
| `scripts/fetch-assets.sh` | تنزيل الأصول الثنائية من الكوميت المثبّت + تحقق SHA-256. يدعم `--dry-run` و`--only` و`--force` و`--verify-only` |
| `scripts/sync-upstream.sh` | مقارنة مع upstream وسرد الملفات المتغيّرة، و`--apply` لسحبها |
| `scripts/refresh-checksums.sh` | إعادة توليد `scripts/assets.sha256` بعد تحديث الأصول |
| `scripts/install-ci.sh` | تفعيل GitHub Actions (ينسخ `ci/build.yml` إلى `.github/workflows/`) |

## البناء الآلي / CI

`ci/build.yml` جاهز (وظيفتان: تحقق البصمات ثم بناء APK ونشر `SHA256SUMS.txt`)، لكنه
موضوع في `ci/` لا في `.github/workflows/` لأن التوكن الآلي في هذه البيئة لا يملك صلاحية
`workflows` وGitHub رفض الـ push صراحةً. فعّله بأمر واحد:

```bash
scripts/install-ci.sh && git add .github/workflows/build.yml && git commit -m "Enable CI" && git push
```

التفاصيل: **[ci/README.md](ci/README.md)**

---

## قرار التقنية / Stack decision

**إبقاء Java + XML + CMake/NDK كما في upstream** — وليس Kotlin/Compose.
السبب: الهدف هو التعديل على Winlator نفسه وسحب تحديثاته باستمرار؛ إعادة الكتابة بلغة أخرى
تجعل كل دمج من upstream صراعًا يدويًا. Kotlin مسموح به **للإضافات الجديدة فقط** (يتعايش مع
Java داخل نفس وحدة Gradle)، وطبقة الأداء تبقى C/C++ عبر NDK.

---

## أولويات التطوير / Roadmap

مرتّبة حسب الأثر ÷ الجهد — التفاصيل في **[docs/ROADMAP.md](docs/ROADMAP.md)**:

1. **CI + توقيع + بصمات** لكل إصدار (أعلى أثر، أقل جهد).
2. **توثيق البناء** — تمّ في `docs/BUILD.md`.
3. **قاعدة ملفات تعريف لكل لعبة** (upstream #2000).
4. **تصدير/استيراد إعدادات الحاوية** (upstream #2002 — 11 تعليقًا).
5. **كشف تلقائي للكرت الرسومي** واختيار التعريف الأمثل (upstream #1998).
6. **حوار تشخيص الأعطال** مع التقاط مخرجات العملية (upstream #2001).
7. **رفع `targetSdkVersion`** عن 28 (حاليًا 28 مع `compileSdk 35`).

---

## provenance / المصدر الأصلي

مثبّت على:

| المكوّن | الكوميت | التاريخ |
|---|---|---|
| `winlator` | `5949297d` (v11.2.0) | 2026-08-19 |
| `winlator-app` → `app/` | `c03f6ab5` | 2026-08-19 |
| `vortek` | `b1730c5d` | 2026-08-19 |
| `gladio` | `116c0d14` | 2026-08-04 |

التفاصيل وطريقة إعادة المزامنة: **[UPSTREAM.md](UPSTREAM.md)**

## الترخيص / License

**LGPL-2.1** كما في upstream — راجع [LICENSE](LICENSE) و[NOTICE](NOTICE).
أي تعديل على الكود يبقى تحت نفس الرخصة مع الحفاظ على نسب الفضل للمؤلف `brunodev85`
والمشاريع الطرف الثالث (Wine، Box86/Box64، Mesa، DXVK، VKD3D، cnc-ddraw).