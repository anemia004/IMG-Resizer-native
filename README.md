# 📷 IMG Resizer – Native Android App

A fully native Android image resizer with a retro Windows 95 aesthetic.  
Resize images by dimensions or compress them to an exact file size – all processing happens directly on the device using Android’s `Bitmap` APIs.  
No WebView, no JavaScript, no HTML.

> **This repository supersedes the [old WebView wrapper](https://github.com/anemia004/IMG-Resizer).**  
> The previous version loaded `img-resizer.htm` inside a `WebView`; this version is a **proper native Android application** built with Java and XML layouts, offering significantly better performance, lower memory usage, and a true offline experience.

---

## ✨ Features

- **Native image processing** – resize and compress using `Bitmap.createScaledBitmap()` and `Bitmap.compress()`.
- **Exact file‑size targeting** – a binary quality search + smart dimension reduction guarantees the output meets the requested KB limit.
- **Format conversion** – output to PNG, JPEG, or WebP.
- **Retro Windows 95 UI** – custom drawables for 3D borders, gradient title bar, and raised/sunken elements.
- **Offline‑first** – no internet permission, no network requests; works entirely on‑device.
- **Persistent digital signature** – signed with a private keystore (via GitHub Actions secrets) so updates can be installed over the old version without uninstalling.

---

## 🔧 Tech Stack

| Component            | Implementation                                                                 |
|----------------------|--------------------------------------------------------------------------------|
| Language             | Java (no Kotlin)                                                               |
| UI                   | Native XML layouts + custom drawables (no AppCompat, no Material components)   |
| Image Handling       | `BitmapFactory`, `Bitmap.createScaledBitmap`, `Bitmap.compress`                |
| File Access          | `Intent.ACTION_OPEN_DOCUMENT` (no runtime permissions needed for reading)      |
| Save to Downloads    | `MediaStore.Downloads` (API 29+) or direct `FileOutputStream` (older)          |
| CI/CD                | GitHub Actions with `gradle/gradle-build-action@v3`                            |
| Signing              | Private keystore decoded from a GitHub secret (`base64`)                       |

---

## 📦 Installation

1. Go to the [Releases](https://github.com/anemia004/IMG-Resizer-native/releases) page.
2. Download the latest `.apk` file.
3. **If a previous (WebView) version is installed, uninstall it first** (the old app used a different signature).
4. Install the APK – future updates will install directly over this version without losing data.

---

## 🛠️ Building from Source

1. Clone the repository.
2. Open the project in Android Studio (or use the command line).
3. Set the following environment variables (or add them to `local.properties`):
   - `KEYSTORE_FILE` – path to your `.jks` keystore.
   - `KEYSTORE_PASSWORD` – keystore password.
   - `KEY_ALIAS` – key alias.
   - `KEY_PASSWORD` – key password.
4. Run `./gradlew assembleRelease`.

The signed APK will be in `app/build/outputs/apk/release/`.

---

## 🆚 Comparison with the old WebView wrapper

| Aspect              | Old (WebView)                               | New (Native)                                         |
|---------------------|---------------------------------------------|------------------------------------------------------|
| Architecture        | `WebView` loads `img-resizer.htm`           | Pure Java with XML layouts                           |
| Image processing    | JavaScript `canvas.toBlob()`                | Native `Bitmap` APIs – **up to 10× faster**          |
| Memory footprint    | High (WebView + HTML engine)                | Low                                                  |
| UI responsiveness   | Depends on WebView rendering                | Immediate native touch feedback                      |
| Offline capability  | Works, but relies on WebView's `file://`    | Full offline; uses `ContentResolver` and `File` APIs |
| Updates             | Each build had a new debug signature        | Permanent private key – seamless updates             |

---

## 📄 License

MIT – do whatever you want with it.  
The Windows 95 aesthetic is a tribute, not a copyright claim.
