# Crayoner: working rules

A coloring book for ages 3 to 5. Sixteen pictures on a shelf, each one shown
in color; tap one and the same picture opens as bare outlines on a sheet, and
the child colors it with a finger. Native Android, paid once, fully offline:
no ads, no trackers, no accounts, no network. Open source under MIT.

The product is described in `README.md`; the code and its tests say the rest.

## How this file stays thin

Code is the source of truth. Constants, geometry, layout, copy and behavior
live in the code and its tests, never here, because prose is not executable
and a second story about the code drifts. The why of one file belongs in its
KDoc; the why of a choice that crosses files belongs in `docs/decisions.md`.

What a thin file can still say, and what this one is for:

- the laws that come from outside the code (constraints, shipping, process);
- cross-file invariants no single file can own (the renderer contract);
- the words this project uses for its own things (the glossary);
- where to look (the map).

If a rule here starts restating a value that is in the code, delete it here
in the same change. If you make a choice a future session could not recover
from the code, add a decision entry in the same commit.

Session start, in order: read this file, then `git fetch` and pull `main`.
The owner works from more than one machine, so never build on a stale head.

If a change you believe in contradicts a rule: do not silently drop the idea
and do not implement against the rule either. Name the conflict, make the
case, and let the owner decide. An overridden rule is updated here, never
left as a dead letter.

## Hard constraints (non-negotiable)

1. **No music.** Nothing in the app is pitched. There is exactly one sound,
   the rustle of paper or wax being handled; it is synthesized
   deterministically by `:tools` from code, and `checkSounds` refuses a
   second file. A pitched chime, if one is ever added, comes alone and never
   twice inside 1200 ms, because two pitched notes in sequence make an
   interval, and intervals are where melody starts.
2. **No animate beings.** No humans, animals, faces, mascots, characters or
   eyes-on-objects, in the app, the launcher icon or the store art. No two
   dots may sit opposite each other where a face could be read, spots and
   sprinkles are placed off center, and battlements are a row, a face is a
   pair (D-072). Inanimate subjects only: boats, houses, food, flowers,
   weather, vehicles, toys. No sun anywhere (D-026); weather is clouds, and
   not every page has any. `FingerTest` holds every visible piece of every
   picture to a fingertip.
3. **Zero manifest permissions.** No `INTERNET`, no exceptions; the Data
   safety declaration and the Families listing rest on it. androidx core-ktx
   merges in one app-private, signature-scoped receiver permission; that is
   the library's doing, documented here, not fought. No permission is added
   without the owner signing off in this file first.
4. **English only.** No localization infrastructure, no translated resources.
   Strings live in `res/values/strings.xml`, nowhere else. `supportsRtl`
   stays false: the layout never mirrors.
5. **Nothing may feel like AI slop.** No placeholder copy, no dead buttons,
   no lorem text, no stock iconography, no filler screens, no generic
   purple-gradient Material defaults. Every string is a real sentence, every
   color is chosen, every asset is drawn for this app. A missing piece is
   listed as a known gap, never faked.
6. **The app must not crash.** Defensive code, no `!!`, no unchecked casts,
   no swallowed exceptions, state that survives rotation, backgrounding and
   process death, and tests around every rule. A Play vitals crash is a
   stop-the-line event.
7. **No fail state, and the child does the work.** The app never says wrong,
   never scores, never times, never locks a picture, never measures or marks
   the work, and never decides that a picture is done. Nothing a child does
   is refused, and nothing the app does answers a mark: it has no opinion
   about where a color went. The app never fills an area for the child; what
   is on the sheet is what the hand put there, and the first touch on a fresh
   page always lands, in the color the area under it is asking for (D-011).
   The paper never moves under the hand: no zoom, pan, pinch or double tap
   anywhere (D-060). The only work the app ever removes is the
   keep-or-start-fresh question on Home (D-068), and it takes two deliberate
   presses.
8. **TalkBack works end to end.** Every area is a named target with a spoken
   action that colors it with the crayon in hand; every button has a spoken
   name, every state change a spoken label, and a crayon's color is spoken as
   its name. No label ever says done. This is a rule, not a feature that may
   regress.

The D numbers above and below are entries in `docs/decisions.md`.

## Style

- **No em-dashes (—), ever, unless absolutely necessary.** Not in chat,
  release notes, commit messages, code comments or this file. Use commas,
  colons, parentheses or a sentence break. The en-dash stays for number
  ranges ("1–2"); the ellipsis is not an em-dash and is fine.
- **American English, everywhere.** Spelling, words and idiom: color,
  license, center, gray, practice, traveled, honor, organize. Store text
  included. The word is crayon and the app is Crayoner.
- **Plain-text store text.** Release notes are pasted into Play Console,
  where quotes, markdown fences and dashes mangle or get auto-corrected:
  plain prose, no quote marks around phrases, no markdown, no em-dashes.
  Notes fit the 500 character field, counted before handing them over, never
  assumed.

## Shipping

- `applicationId` is permanently `io.github.muntasimulhaque.crayoner`. Play
  ties an app to its first package ID forever; the namespace and packages
  mirror it.
- The version walk is the owner's law: `versionCode` only ever increases and
  is never reused; `versionName` is `versionCode` divided by ten, one
  decimal. The current numbers live in `app/build.gradle.kts`; the law lives
  here.
- Toolchain versions live in the build files, not in this file. `targetSdk`
  moves only together with an AGP that supports it. `minSdk` is a product
  floor, not a build detail: only the owner moves it.
- **The signing keystore lives OUTSIDE the repo and never enters it.** Its
  home is the owner's vault, on Windows:

      D:\GDrive\BSCPLC\DM (Development)\Personal Docs\Pers\My Apps\
        Google Play Signing Key\Crayoner\
          crayoner-signing.keystore
          crayoner-signing-key-info.txt

  The base64 twin lives in the `KEYSTORE_BASE64` GitHub secret, and the
  passwords in `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`; those
  four secrets are the only key material anywhere near GitHub. The repo must
  never contain a keystore, a password, an alias or a base64 blob.
  `.gitignore` refuses `*.keystore`, `*.jks`, `signing-key-info.txt` and
  `play-store/aab/`. A session that finds key material staged for commit
  stops and reports it rather than pushing. If the keystore is lost the app
  can never be updated on Play again, so the vault is backed up in a second
  place. Local builds without it stay unsigned.
- Paid once on Play Console, and the owner handles the merchant profile and
  the upload. No billing SDK in the app, ever.

## Forbidden

1. `core/` imports nothing from `android.*`.
2. No composable takes a ViewModel. Screens take state and callbacks; the
   activity wires the host in, which is what lets the screenshot harness
   host every state with no-op callbacks.
3. No user-facing string outside `res/values/strings.xml`.
4. No color literal in UI code; colors come from `CrayonerColors` or from
   `core/Crayons` (picture colors are content, beside the artwork).
5. No `!!`, no unchecked casts, no swallowed exceptions.
6. No network code, no WebView, no third-party SDKs. AndroidX and Kotlin
   only; each new dependency is proposed here first.
7. No source file over 400 lines, no function over 40. Split early.
8. Never add a `Co-Authored-By` trailer or any AI attribution to a commit
   message. One person writes this app and the history should say so.

## The change loop

Implement first, then one question: anything else? The build waits for the
answer. Nothing is cut, no `versionCode` moves and no CI runs until the owner
says the session is done; only then does the build happen, once, with the
whole session in it. A run of small tweaks must not burn a versionCode each
on work the owner may still change. Then, in the same session: commit, push,
pull the AAB and pull the screenshots.

## Build

```
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"   # not on PATH

./gradlew :core:test :app:testReleaseUnitTest :app:lintRelease
./gradlew :tools:test :tools:checkIcons :tools:checkSounds
./gradlew :app:assembleRelease
./gradlew :tools:makeSheets      # every page, sample and line art, for review
./gradlew :tools:makeIcons :tools:makeSounds :tools:makeArt
./gradlew :tools:reviewMarks     # every control mark at coin size, for review
./gradlew :tools:reviewLean      # the app's crayon at four leans, for review
./gradlew :tools:cropProbe "-PcropArgs=in.png out.png x y w h zoom"
                                 # crop and enlarge any PNG, for close review
```

Never hand-edit a generated asset. `checkIcons` and `checkSounds` regenerate
and compare; they ignore a rasterizer's noise, not a person's change, and a
hand edit fails the build. CI is the loop: `build.yml` gates every push to
`main` on the tests, lint and the pins, then signs and publishes the AAB and
APK to the `latest-build` GitHub release. `screenshots.yml` recaptures the
store screenshots whenever UI files change: eight captures per form factor
(phone, 7", 10"), 24 in all, pinned to API 35.

Download the three `store-screenshots-*` artifacts with
`gh run download <run-id> -R muntasimulhaque/crayoner -D <dir>` and strip the
form-factor prefix into `play-store/screenshots/`. The scenes are static
state renders, so a rerun comes back effectively identical. Do not pre-check
UI on a local emulator unless CI cannot answer; a local AVD exists on the
owner's machine (API 37.1, `Pixel_4`, `Pixel_Tablet`) for when CI itself is
the question.

## Releasing

Small commits, plain messages, no AI trailers. Every release-candidate build
bumps `versionCode` +1 and `versionName` by 0.1; push to `main` and CI does
the rest, ending at the `latest-build` release. Release notes arrive in chat,
bare plain text, ready to paste, in US English, counted before handing them
over, with no contact line and no email.

After every push that refreshes the release, download the newest AAB into
`play-store/aab/`, so the build to upload sits in one known place:

    gh release download latest-build -R muntasimulhaque/crayoner -p "*.aab" \
      -D play-store/aab

After the owner submits it for review, DELETE it: the folder holds only the
AAB awaiting submission, so a stale build can never be uploaded twice. The
listing kit and the console answers live in
`play-store/play-store-submission-guide.md`; the privacy policy is live at
`https://muntasimulhaque.github.io/crayoner/privacy.html`.

## Map

```
core/     pure Kotlin, zero Android: shapes, geometry, the crayon box, the
          sixteen pages, wax, wax grain, the crayon's own geometry, the
          control marks, strokes and the draft rules. Its tests are the
          executable rules.
:app      Android. host/ is the ViewModel, DataStore and SoundPool; ui/ is
          Compose. ui/Render.kt is the device renderer; ui/PageSemantics.kt
          is the TalkBack overlay; res/values/strings.xml is the only copy
          of a user-facing word.
:tools    offline generators: the Java2D renderer (RenderKit.kt), picture
          sheets, launcher icons, store art and the sound effect.
```

Where truth lives, by question:

- behavior and rules: `core/` and its tests, above all `PagesTest`,
  `FingerTest`, `AppIconTest`, `CrayonShapeTest`, `WaxTest`, `DraftTest` and
  `PagePointTest`.
- a picture: `core/pages/*`, composed in a square and fitted once onto the
  sheet in `core/Pages.kt`.
- a mark, or saving one: `core/Progress.kt`, `core/Draft.kt`, then
  `host/DraftSaver.kt` and `HostRulesTest`.
- a color: `core/Crayons.kt` for the box, `ui/Theme.kt` for the app's own
  palette.
- a word: `app/src/main/res/values/strings.xml`.
- a decision, or its history: `docs/decisions.md`.

## The renderer contract

`ui/Render.kt` (Compose) and `tools/RenderKit.kt` (Java2D) must produce the
same picture. They cannot share drawing code, so the shared constants are
duplicated on purpose (`ui/LineWeight.kt` beside `tools/RenderKit.kt`): they
change together or not at all. Every point below is a bug this project
already shipped once.

1. Interleaved fills and strokes, region by region: fill, stroke, next. All
   fills first prints the skeleton through the colors.
2. The ground (region zero) is never outlined.
3. One line weight, `STROKE_FRACTION`, in both files.
4. One grain over every colored area, from the one `core/WaxGrain` tile.
5. One wax, from `core/Wax`, for an area and for a mark. A mark is a single
   pass, never two, and nothing is drawn around it.
6. An eraser mark is not painted in a color: it lays the printed page back
   down, which is why the page image is opaque and carries the paper.
7. Every word is Chewy, painted with the wax ink tile. No text is flat type.
8. One sheet: `Page.ASPECT`, isotropic page units, one pixel scale for both
   axes.

## Glossary

The words this project uses for its own things, so two sessions mean the same
thing by them.

- **shelf / wall**: the home screen, every picture hung as a finished card.
  It records nothing about the child's work.
- **page / sheet / paper**: one picture, and the tall surface it is printed
  on. A page unit is isotropic: x runs 0 to 1 and y runs 0 to `Page.ASPECT`.
- **region / area**: one piece of print (`Region`). In prose, area means a
  region.
- **ground**: region zero, the sheet's own surface. Never outlined.
- **print / line**: the ink outlines the book prints. The print sits under
  the wax.
- **wax**: the colored material. `core/Wax` for an area, a `Stroke` for a
  mark.
- **mark / stroke**: one finished thing the hand drew. `Draft` holds the
  paper and the steps back and forward.
- **rubber**: the eraser. The UI copy says rubber, the code says erase.
- **capsule**: the floating row of tools under the sheet. A **seat** is one
  round control on it; the **bar** is the top row.
- **coin / plate**: the round plate every control is drawn on (`CoinSize`).
  The seat in hand wears the capsule's own cardboard as its plate, and that
  plate is the whole selection mark (D-051).
- **box / lid**: the box of thirty two crayons, opened over the page.
- **sample / peek**: the finished picture, held up large from the bar.
- **step back / step forward**: undo and redo, in the child's language.
- **order**: three different things, and never one. The box's order is the
  real thirty two count box's own; a region's order is paint order, first at
  the back; the capsule's order is the hand's: crayon, rubber, step back,
  step forward. When you say "the order", say which.

## Decision log

The why of the app, including the alternatives that were tried and rejected,
lives in `docs/decisions.md`. Read the entries that touch what you are about
to change before changing it, and add an entry when you make a choice a
future session could not recover from the code.
