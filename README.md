# Crayoner

A coloring book for small hands. A picture is shown in color, the same
picture is shown as bare outlines, and the child colors the outlines from a
tray of crayons until it matches. Built by a father for his children; on
Google Play so other families can use it too.

**Made for ages 3 to 5.** Paid once, and yours. Open source under MIT, and
the whole thing is in this repository.

- **Play Store package:** `io.github.muntasimulhaque.crayoner`
- **License:** MIT (free to use, read, fork and learn from)
- **Price:** paid once, no ads, no purchases, no subscriptions
- **Privacy policy:** [online](https://muntasimulhaque.github.io/crayoner/privacy.html) · [in this repo](docs/privacy.html)

<p align="center">
  <img src="play-store/screenshots/phone/01_home.png" width="170" alt="The picture shelf">
  <img src="play-store/screenshots/phone/03_blank.png" width="170" alt="A new page, taped to the desk">
  <img src="play-store/screenshots/phone/05_coloring.png" width="170" alt="Half colored, one crayon in hand">
  <img src="play-store/screenshots/phone/08_done.png" width="170" alt="The finished picture">
</p>

## What it is

Sixteen pictures, all of them there from the first launch. Tap one and the
page opens as bare outlines, with a crayon and a rubber on the tray and the
finished picture in the top bar, a tap away.

| | |
|---|---|
| **The picture wall** | Every picture hung like a child's drawing, taped to the wall with washi tape, each leaning at a small angle of its own. One they have stamped wears a wax seal. |
| **A page** | A real sheet of paper, taped to the desk at its own corners. The finished picture sits in the bar as one more round button: one tap holds it up large, one tap puts it back. |
| **The crayons** | One crayon and one rubber, the two things a hand actually holds. Press the crayon and all thirty two colors of a real box open over the page, with the color in hand standing up out of it. Every area of every picture asks for a color that box holds. |
| **Coloring** | Your finger draws the crayon. Nothing is filled in for you: a mark follows your hand, lays down wax the way wax behaves, and covers the printed lines it is dragged over. Coloring makes the quiet rubbing sound a crayon really makes. |
| **The rubber** | Press it and your crayon becomes an eraser: it takes the wax off and leaves the printed line, exactly as a rubber does on paper. Nothing is ever wiped. |
| **Finishing** | No progress bar, no score, and nothing that measures the work. When the child decides a picture is done, they press the seal themselves and their picture is stamped and hung on the wall. Only the child ever says finished. |

Every mark is saved a moment after it is finished, so a phone call, a
rotation or a killed app costs nothing. There is no score, no timer, no fail
state, and no picture is ever locked.

## Why parents pick it

- **Ages 3 to 5.** Sixteen pictures with five to eight big areas each, huge
  touch targets, and nothing that needs reading. Every screen fits any size
  of phone or tablet, held either way.
- **From an Islamic perspective.** No people, no animals, no faces, no
  mascots and no characters anywhere, in the pictures, the icon or the store
  art. Shape and color carry the warmth instead. No music: every sound is a
  short, deliberately inharmonic effect or one struck bell.
- **Private by construction.** No ads, no trackers, no analytics, no
  accounts, no third-party SDKs and no internet access at all. The app
  declares zero permissions, so there is nothing for it to collect or send.
- **Built for TalkBack.** Every area of every picture is a named target with
  a spoken action, so the app works end to end without sight.
- **Paid once.** No purchase, no subscription, no unlock, no upsell. The
  source is on GitHub under the MIT license, so anyone can read exactly what
  the app does.

## Building

```
./gradlew :core:test :app:testReleaseUnitTest   # the rules
./gradlew :app:assembleRelease                  # R8 release
./gradlew :tools:makeSheets                     # every page, for review
./gradlew :tools:makeSounds                     # regenerate the three effects
./gradlew :tools:makeIcons :tools:makeArt       # launcher icons and store art
```

An Android SDK and the Android Studio JBR are needed (`JAVA_HOME` must point
at the JBR; it is not on PATH).

Every push to `main` triggers GitHub Actions, which runs the tests, lint and
the asset pins, then builds a **signed release AAB and APK**. Signing uses
four repository secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
`KEY_PASSWORD`. The keystore is never committed; if it is lost, the app can
never be updated again.

A second workflow renders the Play Store screenshots on phone, 7" and 10"
emulators: eight scenes per form factor, the Play Console maximum.

## Tech

Kotlin and Jetpack Compose. The pictures are **vector shapes drawn in code**,
not images, so one definition is crisp at any size and the sample, the page
and the wall card all draw the same data. Two renderers share that data:
Compose on the device, Java2D in the offline generators that produce the
review sheets, the icons and the store art. Both build a colored area out of
the same generated **wax** (core/Wax.kt): a surface of the crayon's own color
carrying the paper's tooth and the drag of the hand, with the passes of the
back and forth over it. That is what makes a fill read as crayon rather than
as paint, and the same tile generator paints the app's words.

```
core/     pure Kotlin, zero Android imports: shapes, geometry, the crayon
          box, the sixteen pages, the crayon's own shape, wax grain, and
          every rule about coloring
:app      host/ (ViewModel, DataStore, SoundPool) and ui/ (Compose)
:tools    offline generators: picture sheets, launcher icons, store art, sounds
```

The organizing principle: **the rules are pure data and functions; Android
is a player of those rules, not a participant.** A page is a list of areas,
an area is a list of shapes, and progress is the list of marks the child's
hand has made: each one a line with a color on it, or a line made with the
rubber. That is why the whole coloring engine is playable in
plain JVM tests, and why the store screenshots render straight from state.

See [AGENTS.md](AGENTS.md) for the working rules, the design decisions and
the lessons behind them.

## Privacy

Crayoner collects no data: no accounts, no analytics, no ads, no network
access, and no third-party SDKs of any kind. Because the app declares zero
permissions, there is nothing for it to collect. The only thing it keeps is
your own progress on your own device. The full policy is
[in this repo](docs/privacy.html) and
[hosted online](https://muntasimulhaque.github.io/crayoner/privacy.html).

## Store listing

The Play Store name, descriptions and the questionnaire answers live in
[play-store/play-store-submission-guide.md](play-store/play-store-submission-guide.md).
