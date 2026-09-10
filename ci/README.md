# CI

`build.yml` جاهز لكنه **ليس** في `.github/workflows/` لسبب تقني: التوكن الآلي المستخدم
في هذه البيئة لا يملك صلاحية `workflows`، وGitHub يرفض أي push ينشئ أو يعدّل ملف workflow
بدونها:

```
remote: refusing to allow a GitHub App to create or update workflow
        `.github/workflows/build.yml` without `workflows` permission
```

## التفعيل (أمر واحد)

```bash
scripts/install-ci.sh          # ينسخ ci/build.yml إلى .github/workflows/build.yml
git add .github/workflows/build.yml
git commit -m "Enable CI"
git push                       # requires a token/user with the `workflows` scope
```

أو يدويًا:

```bash
mkdir -p .github/workflows && cp ci/build.yml .github/workflows/build.yml
```

## ماذا يفعل

| الوظيفة | الخطوات |
|---|---|
| `assets` | تنزيل 73 ملفًا ثنائيًا من الكوميتات المثبّتة، ثم **إفشال البناء** إذا لم تطابق أي بصمة `scripts/assets.sha256` |
| `apk` | JDK 17 + Android 35 + NDK 24.0.8215888 + CMake 3.22.1 → `assembleDebug` → رفع الـ APK مع `SHA256SUMS.txt` |
