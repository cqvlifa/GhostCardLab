# GhostCard Lab — სრული გზამკვლევი

სასწავლო NFC პროექტი: ორი Android ტელეფონი ერთმანეთს ესაუბრება NFC-ით,
ერთი „ბარათია", მეორე „წამკითხველი", და მათ შორის გადის სიმულირებული
1 ₾-იანი ოპერაცია. რეალური ფული არ მოძრაობს, საბანკო ბარათი არ იმიტირება.

---

## 1. რა შევქმენით

Android აპლიკაცია, რომელსაც ერთ კოდში ორი როლი აქვს:

- **ბარათის როლი** — ტელეფონი იქცევა უკონტაქტო „ბარათად" Android-ის HCE
  (Host Card Emulation) ტექნოლოგიით.
- **წამკითხველის როლი** — იგივე ტელეფონი შეიძლება გახდეს წამკითხველიც და
  სხვა ტელეფონს „დააკითხოს".

აპი იყენებს **საკუთარ, კერძო AID-ს** `F047484F535401` (category = `other`).
ეს არ არის Visa/Mastercard და ვერ ესაუბრება ნამდვილ გადახდის ტერმინალს —
ის მხოლოდ თავის წყვილ აპს ცნობს.

---

## 2. რაზეა გათვლილი

- Android HCE-ს და APDU ბრძანებების სწავლა ცოცხალ მაგალითზე.
- challenge–response ავთენტიკაციის გაგება: წამკითხველი ითხოვს შემთხვევით
  „გამოწვევას", ბარათი ამოწმებს ხელმოწერას.
- HMAC-SHA256 ხელმოწერის როლის ჩვენება — რატომ ვერ გაიმეორებს თავდამსხმელი
  ჩაწერილ ოპერაციას (replay).
- ორი ფიზიკური მოწყობილობის NFC-კავშირის პრაქტიკა (Pixel 7 + Pixel 9 Pro XL).

ყველა თანხა სატესტო თეთრია. საწყისი ბალანსი 10 ₾, ყოველი ოპერაცია −1 ₾.

---

## 3. როგორ მუშაობს (არქიტექტურა)

ოთხი Java კლასი:

| ფაილი | როლი |
|---|---|
| `MainActivity.java` | ინტერფეისი + წამკითხველის ლოგიკა (NFC reader mode) |
| `LabService.java` | `HostApduService` — ბარათის მხარე, იღებს APDU-ებს |
| `Protocol.java` | APDU ძრავა: SELECT, გამოწვევა, ჩამოჭრა, HMAC, ქვითარი |
| `Store.java` | მონაცემები: გასაღები, ბალანსი, ჟურნალი (SharedPreferences) |

**ოპერაციის ნაკადი:**

1. წამკითხველი აგზავნის `SELECT`-ს კერძო AID-ით.
2. წამკითხველი ითხოვს `გამოწვევას` → ბარათი აბრუნებს 16 შემთხვევით ბაიტს.
3. წამკითხველი აგზავნის ჩამოჭრის ბრძანებას: nonce + transaction ID +
   თანხა + HMAC-SHA256 (საერთო გასაღებით ხელმოწერილი).
4. ბარათი ამოწმებს ხელმოწერას და nonce-ს, აკლებს 1 ₾-ს და აბრუნებს
   ხელმოწერილ ქვითარს ახალი ბალანსით.
5. წამკითხველი ქვითრის ხელმოწერას ამოწმებს და ჟურნალში წერს შედეგს.

**APDU პროტოკოლი:**

| ბრძანება | ბაიტები | პასუხი |
|---|---|---|
| SELECT | `00 A4 04 00 07 F047484F535401 00` | `01 9000` |
| გამოწვევა | `80 10 00 00 00` | 16 ბაიტი + `9000` |
| ჩამოჭრა | `80 20 00 00 44` + nonce(16) + ID(16) + თანხა(4) + HMAC(32) | ქვითარი + ნაშთი(4) + მრიცხველი(4) + HMAC(32) + `9000` |

შეცდომის კოდები: `6982` არასწორი MAC · `6984` განმეორებითი ID ·
`6985` არასწორი სესია · `6A80` დაუშვებელი თანხა · `6A84` არასაკმარისი
ბალანსი · `6581` ჟურნალი სავსე · `6A82` სხვა AID.

---

## 4. ფაილები, რომლებიც პროექტშია

```
GhostCardLab/
├─ settings.gradle              # პროექტის პარამეტრები, მოდული :app
├─ build.gradle                 # Android Gradle Plugin-ის ვერსია
├─ .gitignore                   # build/, local.properties და სხვ. გამორიცხვა
├─ README.md                    # მოკლე აღწერა
├─ GUIDE.md                     # ეს გზამკვლევი
├─ app/
│  ├─ build.gradle              # namespace, SDK, versionCode/Name
│  └─ src/main/
│     ├─ AndroidManifest.xml    # NFC უფლებები, Activity, HCE Service
│     ├─ java/ge/venice/ghostcardlab/
│     │  ├─ MainActivity.java   # UI + წამკითხველი
│     │  ├─ LabService.java     # ბარათის HCE სერვისი
│     │  ├─ Protocol.java       # APDU + HMAC ძრავა
│     │  └─ Store.java          # მონაცემთა შენახვა
│     └─ res/
│        ├─ xml/apduservice.xml # AID registration (category=other)
│        ├─ values/strings.xml  # აპის სახელი
│        ├─ values/colors.xml   # ფერების პალიტრა
│        ├─ values/themes.xml   # მუქი თემა
│        └─ drawable/*.xml      # ვექტორული იკონები
├─ tests/
│  ├─ ProtocolTest.java         # პროტოკოლის ტესტები
│  └─ RunTests.java             # ტესტების გამშვები
└─ .github/workflows/build.yml  # GitHub Actions — APK-ის ავტომატური აწყობა
```

---

## 5. GitHub-ზე აწყობა (GitHub Actions)

რადგან Android SDK-ის ლოკალურად დაყენება საჭირო არ არის, APK ყოველ push-ზე
ავტომატურად იწყობა GitHub-ის სერვერზე. ამას აკეთებს `.github/workflows/build.yml`:

```yaml
name: Build APK
on:
  push:
  workflow_dispatch:          # ხელით გაშვების ღილაკიც
permissions:
  contents: write             # Release-ის შესაქმნელად
jobs:
  build:
    runs-on: ubuntu-24.04     # SDK უკვე დაყენებულია ამ იმიჯზე
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with: { distribution: temurin, java-version: '17' }
      - name: Android SDK
        run: |
          yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses || true
          "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
            "platforms;android-35" "build-tools;35.0.0" "platform-tools"
          echo "sdk.dir=$ANDROID_HOME" > local.properties
      - uses: gradle/actions/setup-gradle@v4
        with: { gradle-version: '8.9' }
      - name: Protocol tests
        run: java tests/RunTests.java
      - name: Build debug APK
        run: gradle :app:assembleDebug --no-daemon --stacktrace
      - run: cp app/build/outputs/apk/debug/app-debug.apk GhostCardLab-debug.apk
      - uses: actions/upload-artifact@v4
        with: { name: GhostCardLab-debug-apk, path: GhostCardLab-debug.apk }
      - name: Publish release
        env: { GH_TOKEN: '${{ github.token }}' }
        run: |
          TAG="build-${{ github.run_number }}"
          gh release create "$TAG" GhostCardLab-debug.apk \
            --title "GhostCard Lab $TAG" --notes "Debug APK, auto-built." \
            --repo "${{ github.repository }}"
```

რას აკეთებს თითო ნაბიჯი:
1. **checkout** — კოდს ჩამოტვირთავს.
2. **setup-java** — JDK 17-ს აყენებს.
3. **Android SDK** — platform 35 და build-tools 35-ს ამატებს, `local.properties`-ს წერს.
4. **setup-gradle** — Gradle 8.9-ს ამზადებს.
5. **Protocol tests** — წმინდა Java ტესტები (NFC-ს არა).
6. **assembleDebug** — აწყობს `app-debug.apk`-ს.
7. **upload-artifact + Release** — APK-ს Actions-ში და Releases-ში დებს.

მზა APK: რეპოს გვერდზე **Releases** → ბოლო `build-N` → `GhostCardLab-debug.apk`.

---

## 6. როგორ გავიმეორო მსგავსი პროექტი ნულიდან

1. GitHub-ზე შექმენი ცარიელი რეპო (README-ის გარეშე).
2. ლოკალურად ააწყვე ზემოთ ნაჩვენები საქაღალდის სტრუქტურა.
3. `app/build.gradle`-ში მიუთითე:
   ```groovy
   plugins { id 'com.android.application' }
   android {
       namespace 'ge.venice.ghostcardlab'
       compileSdk 35
       defaultConfig {
           applicationId 'ge.venice.ghostcardlab'
           minSdk 26; targetSdk 35; versionCode 1; versionName '1.0'
       }
       compileOptions {
           sourceCompatibility JavaVersion.VERSION_17
           targetCompatibility JavaVersion.VERSION_17
       }
   }
   ```
4. ჩააგდე ოთხი Java ფაილი და `apduservice.xml` (საკუთარი AID-ით).
5. დაამატე `.github/workflows/build.yml` (ზემოთ).
6. push:
   ```sh
   git init -b main
   git add -A
   git commit -m "init"
   git remote add origin https://github.com/<user>/<repo>.git
   git push -u origin main
   ```
7. Actions ავტომატურად აეწყობა; APK Releases-ში გამოჩნდება.

**მნიშვნელოვანი:** შენი AID უნდა იყოს კერძო (`category="other"`), არა
გადახდის სისტემის. ასე აპი იზოლირებულია და მხოლოდ შენს წამკითხველს ესაუბრება.

---

## 7. ინსტალაცია და ტესტი ორ ტელეფონზე

1. გადმოწერე `GhostCardLab-debug.apk` და დააყენე ორივე ტელეფონზე
   (საჭიროა „უცნობი წყაროების" ნებართვა; debug ხელმოწერაა, ამიტომ
   Play Protect შეიძლება გაგაფრთხილოს — „მაინც დაყენება").
2. ორივეზე ჩართე NFC, გახსენი აპი.
3. ერთ ტელეფონზე აირჩიე **წამკითხველი**, დააკოპირე ეკრანზე ნაჩვენები
   64-სიმბოლოიანი HEX გასაღები, მეორეზე ჩასვი და **შეინახე** (ორივეზე
   ერთი და იგივე გასაღები).
4. მეორე ტელეფონზე აირჩიე **ბარათი** (საწყისი ბალანსი 10 ₾).
5. წამკითხველზე დააჭირე **„1 ₾ ოპერაციის მომზადება"**.
6. მიადე ტელეფონების უკანა მხარეები, გააჩერე შედეგამდე.
7. წარმატებისას ბარათის ბალანსი გახდება 9 ₾; ჟურნალში ჩაიწერება ოპერაცია.

---

*ყველა თანხა სიმულირებულია. პროექტი სასწავლოა და არ არის განკუთვნილი
რეალურ გადახდის სისტემებთან სამუშაოდ.*
