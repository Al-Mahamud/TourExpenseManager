# Tour Expense Manager 🌍✈️

A modern, offline-first Android application built with **Kotlin**, **Jetpack Compose**, and **Room (SQLite)** for managing tour and group travel expenses. All data is securely stored locally on the device with zero cloud dependencies, logins, or internet connection required.

---

## 📱 Features

### 1. 🧳 Tours & Groups
- Create and manage multiple tour/trip groups (e.g. *"Cox's Bazar Tour"*, *"Weekend Roadtrip"*).
- Instant tour overview with creation dates and member counts.
- Delete tours with cascade deletion confirmation.

### 2. 👥 Group Members
- Add travelers/participants to each tour group.
- Edit member names or remove members.
- Avatar badges and fast member selection.

### 3. 💵 Expense Tracking & Smart Splits
- Add expenses with title, amount, date, and payer.
- **Equal Split**: Automatically divides the total evenly among chosen participants, down to the exact penny without rounding discrepancies.
- **Custom Split**: Manually specify each participant's individual share.
- **Strict Real-Time Validation**: For custom splits, prevents saving unless the sum of shares matches the total expense down to the exact cent, with live feedback on remaining/over-allocated amounts.
- Edit or delete recorded expenses anytime.

### 4. 📊 Balances & Settlement Engine
- **Summary Table**: Real-time breakdown of **Paid**, **Owes**, and **Balance** for each traveler:
  - `totalPaid = sum of expenses paid by member`
  - `totalOwed = sum of member's shares across all expenses`
  - `balance = totalPaid - totalOwed`
  - Visual badges: Green `+$X.XX` for members who should receive money; Red `-$X.XX` for members who need to pay.
- **Greedy Two-Pointer Settlement Algorithm**: Automatically calculates the minimum number of transactions to settle all debts (*"Who Pays Whom"*):
  - Example: `Rahim pays Al $1,500.00`
- **One-Tap Share**: Share the complete settlement summary via WhatsApp, SMS, Messenger, or email.

---

## 🏗️ Architecture & Tech Stack

```
Compose UI  ──►  ViewModel  ──►  DAO  ──►  Room  ──►  SQLite
```

- **Language**: Kotlin
- **UI Toolkit**: Jetpack Compose
- **Design System**: Material 3 (with Dynamic Theming and Dark Mode support)
- **State Management**: Android ViewModel + Kotlin StateFlow
- **Local Persistence**: Android Room Database (SQLite) with foreign key cascading
- **Navigation**: Jetpack Navigation Compose
- **Backend / Auth**: None (100% offline-first)

---

## 📂 Project Structure

```
TourExpenseManager/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/tourexpensemanager/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── GroupEntity.kt
│   │   │   │   │   ├── MemberEntity.kt
│   │   │   │   │   ├── ExpenseEntity.kt
│   │   │   │   │   ├── ExpenseShareEntity.kt
│   │   │   │   │   └── dao/
│   │   │   │   │       ├── GroupDao.kt
│   │   │   │   │       ├── MemberDao.kt
│   │   │   │   │       └── ExpenseDao.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── TourViewModel.kt
│   │   │   │   │   └── CalculationEngine.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── GroupScreen.kt
│   │   │   │   │   ├── AddExpenseScreen.kt
│   │   │   │   │   ├── AddMemberScreen.kt
│   │   │   │   │   ├── ResultScreen.kt
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt
│   │   │   │   │       ├── Theme.kt
│   │   │   │   │       └── Type.kt
│   │   │   │   └── navigation/
│   │   │   │       ├── AppNavigation.kt
│   │   │   │       └── Screen.kt
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/java/com/tourexpensemanager/
│   │       └── CalculationEngineTest.kt
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew.bat
```

---

## 🚀 How to Run

1. Open **Android Studio** (Koala, Ladybug, Meerkat, or newer).
2. Select **Open** and navigate to `D:\Projects\TourExpenseManager`.
3. Allow Gradle to sync dependencies.
4. Select an Android Emulator or connected device and click **Run (Shift + F10)**.
"# TourExpenseManager" 
