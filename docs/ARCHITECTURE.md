# البنية المعمارية / Architecture

كل ما هنا مستخرج من قراءة الكود في هذه الشجرة (مع أرقام الأسطر للمراجعة).

## الطبقات الخمس

```
┌───────────────────────────────────────────────────────────────────────┐
│ 1. طبقة أندرويد (Java) — app/app/src/main/java/com/winlator/         │
│    MainActivity · ContainersFragment · XServerDisplayActivity ·       │
│    ControlsEditorActivity · SettingsFragment                          │
│    + الحزم: container/ xenvironment/ xserver/ xconnector/ sysvshm/    │
│             inputcontrols/ winhandler/ renderer/ box64/ alsaserver/   │
├───────────────────────────────────────────────────────────────────────┤
│ 2. الجسر الأصلي (JNI/C++) — app/app/src/main/cpp/                     │
│    winlator · vortekrenderer · gladiorenderer · virglrenderer ·       │
│    libadrenotools · midihandler                                       │
├───────────────────────────────────────────────────────────────────────┤
│ 3. الحاوية (guest): Wine + Box64 + glibc  ← rootfs.tzst               │
├───────────────────────────────────────────────────────────────────────┤
│ 4. تعريفات الرسوم داخل الحاوية (guest-side .so):                      │
│    vortek/ → libvulkan_vortek.so   ·   gladio/ → libGL.so             │
│    (بديلات Mesa: turnip / zink / virgl)                               │
├───────────────────────────────────────────────────────────────────────┤
│ 5. مغلّفات DirectX: DXVK · VKD3D · D7VK · D8VK · WineD3D · cnc-ddraw  │
└───────────────────────────────────────────────────────────────────────┘
```

## النقطة الأهم: Vortek وGladio وجهان (مضيف + ضيف)

هذا أكثر ما يُساء فهمه في المشروع:

| | الجهة المضيفة (تطبيق أندرويد) | الجهة الضيفة (داخل الحاوية) |
|---|---|---|
| **Vortek** | `cpp/vortekrenderer` → `System.loadLibrary("vortekrenderer")` في `VortekRendererComponent.java:37` | `vortek/` → `libvulkan_vortek.so`، يُستخرج من `graphics_driver/vortek-2.1.tzst` |
| **Gladio** | `cpp/gladiorenderer` → `System.loadLibrary("gladiorenderer")` في `GLXExtension.java:41` | `gladio/` → `libGL.so`، يُستخرج من `graphics_driver/gladio-1.1.tzst` |

الاتصال بين الجهتين عبر **Unix socket**:

```java
// XServerDisplayActivity.java:534
new VortekRendererComponent(xServer,
    UnixSocketConfig.create(rootPath, UnixSocketConfig.VORTEK_SERVER_PATH), options);

// UnixSocketConfig.java:13
public static final String VORTEK_SERVER_PATH = "/tmp/.vortek/V0";
```

النتيجة العملية: **تعديل `vortek/` أو `gladio/` لا يظهر في التطبيق إلا بعد إعادة بنائه
بـ NDK وإعادة تعبئته في `.tzst`** — راجع `docs/BUILD.md` §4.

## اختيار تعريف الرسوم

`GraphicsDrivers.java`:

- تعريفات Vulkan: `turnip` (لـ Adreno) و`vortek`.
- تعريفات OpenGL: `zink` و`virgl` و`gladio`.
- الافتراضي: `DEFAULT_VULKAN_DRIVER = vortek`، `DEFAULT_OPENGL_DRIVER = gladio`.
- `getDefaultDriver()` يختار `turnip,gladio` إذا `GPUHelper.getAdrenoModelId(context) > 0`
  وإلا `vortek,gladio` — أي أن هناك **كشفًا تلقائيًا بدائيًا موجودًا بالفعل**، وهو أساس
  جيد لبناء الميزة المطلوبة في upstream #1998.
- `DefaultVersion.DXVK(vulkanDriver)` يختار DXVK 2.4.1 مقابل 1.10.3 حسب
  `GPUHelper.vkGetApiVersion() >= 1.3` عند استخدام Vortek.

## الإصدارات المثبّتة (`com/winlator/core/DefaultVersion.java`)

`BOX64 0.4.4` · `TURNIP 26.1.0` · `VORTEK 2.1` · `ZINK 22.2.5` · `VIRGL 23.1.9` ·
`GLADIO 1.1` · `D7VK 1.11` · `D8VK 1.0` · `VKD3D 2.14.1` · `CNC_DDRAW 6.6` ·
`DXVK 2.4.1 / 1.10.3` · `SOUNDFONT SONiVOX-EAS-GM-Wavetable`

## حجم `app/src/main` (قبل استثناء الثنائيات)

| المسار | الحجم |
|---|---|
| `assets/` | 143 MB (منه `rootfs.tzst` ‏63 MB، `wincomponents/` ‏40 MB، `dxwrapper/` ‏15 MB) |
| `jniLibs/arm64-v8a/` | 11 MB (24 ملف `.so`) |
| `cpp/` | 7.4 MB (vortekrenderer 3.6 · virglrenderer 2.1 · gladiorenderer 728K · libadrenotools 688K · winlator 212K · midihandler 188K) |
| `java/` | 2.0 MB |
| `res/` | 1.2 MB |

## ملاحظات تقنية تستحق الانتباه

- `AndroidManifest.xml`: `android:extractNativeLibs="true"`، `android:isGame="true"`،
  `appCategory="game"`، `allowAudioPlaybackCapture="true"`، و`WRITE_EXTERNAL_STORAGE`
  مع `targetSdkVersion 28`.
- `app/app/src/main/cpp/libadrenotools/.gitmodules` بقايا من المشروع الأصلي —
  `lib/linkernsbypass/` موجود كملفات عادية (7 ملفات)، فلا حاجة لتهيئة submodule.
- لا اختبارات آلية إطلاقًا: **267 ملف Java** في `app/src/main/java`، ولا يوجد مجلد `test/`
  أو `androidTest/`، ولا أي تبعية اختبار (`junit`/`espresso`) في `app/app/build.gradle`.
  هذا أكبر خطر عند أي إعادة هيكلة — وأول ما يجب بناؤه.
