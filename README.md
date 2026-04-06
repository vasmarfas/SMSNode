# SMSNode — клиентское приложение

**English:** [`README.en.md`](README.en.md)

## Назначение

Клиентское приложение для взаимодействия с серверной частью SMSNode: отправка и приём SMS, управление диалогами, контактами и настройками, связанными с GSM-шлюзами.

## Платформы

Реализовано на Kotlin Multiplatform (Compose Multiplatform):

| Платформа | Модуль |
|-----------|--------|
| Android | `composeApp` |
| iOS | `composeApp` + проект Xcode в `iosApp` |
| Desktop | JVM |
| Web | WasmJS |

## Функциональность

**Интерфейс.** Адаптивная вёрстка: на узких экранах — нижняя навигация, на широких — боковая панель; светлая и тёмная темы (Material 3); ограничение максимальной ширины области контента (840 dp).

**Пользователь.** Регистрация и вход; список диалогов с фильтрами (все / непрочитанные / входящие / исходящие); чат с периодическим опросом сервера; шаблоны сообщений (в том числе глобальные, задаваемые администратором); назначенные SIM-карты с пользовательскими подписями; контакты, группы контактов, групповая отправка SMS.

**Администратор.** Шлюзы: создание, изменение и удаление записей, проверка доступности, удаление с учётом конфликтов (при необходимости — принудительное удаление); обнаружение каналов по данным keepalive. Пользователи: роли, деактивация, привязка SIM. Режим регистрации (открытый / закрытый / по заявкам). Обработка заявок на регистрацию. Просмотр сообщений по системе (аудит).

## Структура репозитория

- `composeApp/src/commonMain` — общий код (UI, ViewModel, HTTP-клиент Ktor, модели данных).
- `composeApp/src/androidMain`, `jvmMain`, `iosMain`, `jsMain`, `wasmJsMain` — платформенные реализации и точки входа.
- `iosApp` — проект Xcode для сборки под iOS.

## Сборка и запуск

**Android**

```shell
./gradlew :composeApp:assembleDebug
```

```shell
.\gradlew.bat :composeApp:assembleDebug
```

**Desktop (JVM)**

```shell
./gradlew :composeApp:run
```

```shell
.\gradlew.bat :composeApp:run
```

**Web (WasmJS, режим разработки)**

```shell
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

```shell
.\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
```

Документация по стеку: [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html), [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform).
