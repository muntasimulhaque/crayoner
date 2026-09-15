# Crayoner: decision log

Why the app is the way it is: the choices the code cannot explain, with the
alternatives that were tried and rejected, and the bugs that taught them.

Read the entries that touch what you are about to change before you change
it. When you make a choice a future session could not recover from the code,
add an entry in the same commit, next number, at the bottom, and keep it to
what the code cannot say.

Numbers are permanent. A superseded entry is never deleted; the entry that
supersedes it says so. No dates are recorded: the order in this file is the
order they were made.

- D-001 Name: Crayoner. A real English word (one who crayons, in Collins and
  WordReference), unused by any app, package or repository. Settled after
  checking Colorlet (taken by a kids coloring app on the App Store),
  Crayonlet, Tintlet, Crayon Cove, Colorling, Hueberry and Scriblet.
- D-002 Package: `io.github.muntasimulhaque.crayoner`, settled before the
  app exists in Play Console, where it becomes permanent.
- D-003 License: MIT. D-004 English only. D-005 Price: paid once.
- D-006 Toolchain pinned to the house: Gradle 9.5.0, AGP 9.3.0, Kotlin
  2.2.10, Compose BOM 2026.08.00, Java 17, minSdk 24, compile/target 37.
- D-007 Minimal dependencies on purpose; each new one is proposed here
  first. Current set: BOM, core-ktx, activity-compose, lifecycle (runtime +
  viewmodel) compose 2.8.7, DataStore preferences 1.1.1, JUnit, test runner
  and ext-junit for the capture harness.
- D-008 Pictures are pure data in `core`, drawn by two renderers that must
  agree; the agreement is written out in Architecture above because every
  point of it has already been a bug.
- D-009 The sample is the picture the child copies, and it is always one
  gesture away: one tap holds it up large. It once sat directly above the
  sheet, the way a coloring book prints the finished picture facing the
  page; D-027 moved it into the bar because on a phone it cost the page too
  much of its own height. The principle survives, the placement does not.
- D-010 The tray shows only the picture's own colors, in the picture's own
  order. Fewer choices, every choice useful, and the tray teaches the page.
  Superseded in 0.1 by D-015 and still superseded: one box of sixteen for
  the whole book.
- D-011 The first touch on a fresh page starts a mark in the color that
  area wants. Feedback beats instruction, and a first act that does nothing
  teaches nothing. Amended by D-024: the app no longer fills the area, it
  picks up the color and the child's own mark goes down in it.
- D-012 Haptics on paint, on a match and on a finish, never on a miss. A
  finished mark answers whether or not it landed on new paper: the child's
  hand did something, and that is what a haptic is for.
- D-013 A color the picture did not ask for still lands. Gentle, immediate,
  reversible, and never spoken as wrong.
- D-014 No `.twa` suffix, no scar to inherit: this app was native from the
  first line.
- D-015 **One crayon box for the whole book.** Sixteen crayons, the same
  sixteen in the same places on every page, because a real box of crayons
  does not change its contents because of the page you open. A child reaches
  for red because red is always in the same place. The rule that makes this
  possible: every area's picture color is a crayon in the box, held by
  `PagesTest`. Supersedes the earlier per-picture tray.
- D-016 **The colors are crayon colors, not paint colors.** Wax is softer,
  warmer and never fully saturated: no pure black, no pixel-bright chroma.
  The palette was retuned by hand toward earth. Where a printed crayon really
  does reach a bright yellow or a clear sky, it still does; where a paint
  would, a crayon here does not. Superseded by D-033, which took the
  Crayola hexes at their printed values and let the wax do the softening.
- D-017 **Every colored area carries wax grain** (`core/WaxGrain`), a fine
  four-pixel tooth shared by both renderers. A flat fill is paint; the grain
  is what makes it a crayon. It is generated in core, wraps seamlessly, and
  is a whisper (average alpha under 6 of 255): a loud grain reads as dirt.
- D-018 **No width slider, no tip sizes, no brush controls.** Amended by
  D-024: the reasoning changed, the conclusion did not. Coloring is stroke
  based now, so a width control would no longer be dead, and the second half
  of the original argument is the one that carries: in real life width is
  not a control either, it is a property of the crayon, and nobody hands a
  small child a slider. The crayon has one fixed width
  (`CRAYON_TIP_FRACTION`), and D-035 lowered it from 0.052 to 0.030 of the
  page, because on these pictures a tip any wider swallowed a window frame
  or a sprinkle whole. There is no fail state still: a wide crayon over a
  line is not an error, it is how wax behaves.
- D-019 **The shelf is a picture wall.** Sheets taped to a wall with washi
  tape, each leaning at a small stable angle of its own, named underneath.
  The tape is one material everywhere (tinting it per picture went muddy,
  because a tint of a picture's own colors is a tint of the picture). State
  marks sit beside the name, never over the art, except the seal, which goes
  on the work because that is where a stamp goes (D-032). Superseded in part
  by D-041: the wall carries no state marks and no seal.
- D-020 **Every screen adapts to its real measured size**, in both
  orientations. The play screen has exactly two shapes: stacked below
  640 dp of width, side by side at or above it *and* in landscape. A portrait
  tablet is wide but stacking is still right there, because standing the
  tools beside the sheet steals half the page's size. Decided from
  `BoxWithConstraints`, never from a device guess. See D-071 for which side
  the tools stand on and which way the capsule runs.
- D-021 The box sizes itself in three shapes (four, six, or four columns),
  with the crayon's width capped so a very wide tablet draws crayon-sized
  crayons rather than ribbons. Superseded by D-034: the box is a lid that
  opens over the page, and its columns are counted from the room the lid
  gets.
- D-022 Haptics: paint, match, finish. Never a miss. Superseded by D-038:
  paint and the seal, and there is no match to feel, because nothing is
  compared any more. And by D-041: the seal is gone, so a finished mark is
  the one thing left that answers the hand.
- D-023 **Store form: a Game, category Educational.** Every serious kids
  coloring app on Play is a game and says so (Bimi Boo, Crayola), and a
  parent looking for one is browsing Games, where the Similar apps row
  lives. Educational rather than Casual because a paid, quiet, ad-free kids
  app cannot out-shout the ad-driven giants in Casual, and because it is
  true: the app has rewards, celebration and play. Target audience 5 and
  under plus a Game is a normal combination and lands in the Families
  program. Supersedes the hedged "Educational or Puzzle" note in the
  submission guide.
- D-024 **The child colors with their own hand.** A finger draws a crayon
  mark that follows it; a tap no longer fills an area with a color. This
  supersedes the tap-to-fill model that shipped in 0.1, which was wrong for
  the one reason that matters: a coloring book does not color itself. What
  the child sees on their sheet is now exactly what their hand did.
  - A mark is a `Stroke` (`core/Progress.kt`): a color and the points the
    finger traveled. `Strokes.extend` drops points closer than `MIN_STEP`
    (0.4% of the page), so a thousand finger events make a short mark
    rather than a save full of coordinates.
  - Saving is the stroke list, coordinates and colors, capped at 900 points
    a stroke and 600 strokes a page. A page whose areas are reordered in
    code still reads an old save, because a stroke belongs to the paper and
    not to an area.
  - The first touch rule (D-011) survives intact: with no crayon in hand,
    the app picks up the color the area under the finger asks for.
  - Areas still exist: they draw the picture and they name themselves to a
    screen reader. They just are not filled by the app any more, except for
    a screen reader's action, which scribbles the area in the crayon in hand
    (`Strokes.scribble`), because a child who cannot aim a finger cannot
    draw a mark either. Nothing counts them any more: see D-037.
- D-025 **The picture is finished when the crayon has been everywhere on
  it**, whatever colors were chosen. The app counts the areas a mark has
  touched and celebrates when no area is left untouched. It never judges
  the colors: a child who colors the sky orange gets the same sparkle, the
  same confetti and the same well done as a child who matches the sample
  exactly. Hard constraint 7 says there is no fail state, and this is what
  that means in code. A parent who wants a matching picture has the sample
  in the bar and the child has the reason to reach for it; the app's job is
  to celebrate the work, and a scold about the wrong blue would be the
  first thing this app has ever refused. Superseded by D-032: counting the
  areas was a progress meter in disguise, the count fired while a child was
  still working, and no count can know what done means.
- D-026 **No sun, and the sky is not wallpaper.** Every outdoor page had a
  sun in a corner, which is the oldest cliché in children's books and made
  the pages read as one picture repeated. Weather is clouds now, placed
  differently on every page, and the sun helper is gone from `core/Art.kt`
  so it cannot drift back.
- D-027 **The sample is a button in the bar**, one more round coin the same
  size, shape and shadow as Home, the eraser and the sound switch, with the
  finished picture inside it. It replaced a card that sat above the sheet
  and stole the page's own height, which was the wrong trade on a phone and
  looked like a panel that had been dropped into the layout. Supersedes the
  layout half of D-009; the principle of D-009 survives, the sample is still
  the thing the child copies, and one tap still holds it up large.
- D-028 **The picked crayon casts a shadow and nothing else.** The white
  plate that used to sit behind the crayon in hand is gone: a crayon lying
  in a box casts a shadow, it does not sit on a white tile, and the plate
  read as chrome. Selection is now the spring lift, a slightly larger
  crayon, and the shadow it throws on the cardboard, plus the paper collar
  a held crayon wears. Extended by D-034: in the box of colors the chosen
  crayon stands up out of a tray of lying ones, and that is its whole
  selection mark. Superseded in the box by D-041: no shadow, and the chosen
  crayon lies with the rest.
- D-029 **The icon's crayon points down, and it is drawn from core.** Point
  down is how a crayon is held and how a child recognizes one, and a crayon
  standing on its point cannot be mistaken for anything else. The icon's
  wax, wrapper and rules now come from the same geometry the tray draws
  (`core/CrayonShape`), including the wax grain, so the launcher icon, the
  store icon, the shelf header and the tray are literally one object.
  `CrayonShapeTest` pins the proportions: three-ish to one, a blunt cone,
  a squared base.
- D-030 **The palette is real crayon colors.** The sixteen were rebuilt from
  the Crayola standard list (Red #EE204D, Orange #FF7538, Yellow #FCE883,
  Green #1CAC78, Blue Green #0D98BA, Sky Blue #76D7EA, Blue #1F75FE, Violet
  #926EAE, Carnation Pink #FFAACC, Brown #B4674D, Gray #95918C) and softened
  six percent toward paper, because wax sits on the tooth instead of soaking
  in. The old palette was tuned toward earth and landed on mud: a yellow
  crayon really is a bright yellow. Sand and forest are the two mixed ones
  and each says so in `core/Crayons.kt`. Superseded by D-033: the softening
  is gone, because a paler hex is not a softer crayon, and the box grew to
  the real thirty two.
- D-031 **Wax grain carries two scales.** The tile was one fine tooth at an
  average alpha under 6, which is invisible at arm's length and reads as
  faint dirt when it is not. It now carries a four pixel tooth and a sixteen
  pixel mottle of coverage, at a mean alpha under 12, and the mark renderer
  draws the grain through the mark rather than over it. That is what makes a
  colored area read as wax pressed into paper instead of flat fill. Kept,
  and now one half of D-036: the grain is laid over a wax surface rather
  than over a flat fill, and the same tile is what makes the app's words
  read as written in wax.
- D-032 **The child decides when a picture is done, and the mark of it is a
  wax seal.** There is no progress meter anywhere in the app, and no rule
  that watches the coloring: the app cannot know when a coloring is
  finished, and guessing would mean either telling a child they are done
  before they are or telling them they are not. So the child says it: one
  press of the seal, and their picture is stamped in the corner with a disc
  of coral wax carrying the app's own crayon, the shelf keeps the seal, one
  bell rings and one haptic lands. It can be taken back the same way. This
  supersedes D-025 and the whole celebration (the plate, the confetti, the
  well done) that came with it, and it replaces the honey star on the shelf:
  a star is what you press to come back to something, a stamp is what a
  finished piece of work wears. Superseded by D-041: the seal, the stamp,
  the bell and every state mark on the shelf are gone, and with them the
  last place the app could have claimed to know a picture was finished.
- D-033 **The box is the real thirty two count box, at printed hexes.** The
  sixteen were softened six percent toward paper, which read as faded wax
  rather than as a softer crayon, and sixteen colors could not cover the
  book: sixteen pictures of sky, sea, grass, skin-warm walls and stone
  needed more colors than that to ask for honestly. The box is now the
  thirty two (add red orange, bluetiful, indigo, wisteria, melon, tan,
  chestnut, timberwolf and the rest), in the box's own order, at Crayola's
  printed values, with nothing mixed down. A crayon drawn over the tooth of
  paper does not come out paler than the stick: it comes out broken, and the
  broken edge is the medium. Supersedes D-016 and D-030.
- D-034 **The tray is two coins: a crayon and a rubber.** Thirty two crayons
  under a page is a tray nobody can hold in a hand, and a child of three
  does not choose between thirty two crayons anyway: they pick one up and
  draw. So the tray holds the crayon in hand and the rubber, and pressing
  the crayon opens the box over the page. In the box the chosen color stands
  up among lying ones, which is its whole selection mark: no ring, no plate,
  no tick. Supersedes D-015's always-visible tray and D-021's sizing.
  Amended by D-041: the chosen color lies with the others, a little longer
  and drawn with a heavier line.
- D-035 **The crayon is a crayon's width.** `CRAYON_TIP_FRACTION` went from
  0.052 of the page to 0.030, and the rubber is 0.044: at 0.052 a single
  mark covered a window frame, a sprinkle or a flower's center whole, and
  those are the small things a child is being taught to aim at. Narrower is
  still wider than a real crayon tip relative to a page, so a scribble still
  covers paper.
- D-036 **Wax is generated, not simulated.** A colored area is a surface: a
  tile of the crayon's own color carrying the paper's tooth and a noise
  stretched along the rubbing direction, at an average coverage of about
  0.88, with the passes of the hand drawn over it. The old model (a flat
  fill, then a translucent gray speckle over it) read as paint with dirt on
  it, because a veil over a flat color is still a flat color. The same
  generator serves the device and the offline sheets, so the store art and
  the screen are the same wax, and the same tile generator paints the app's
  words: a word covers the way a colored area covers.
- D-037 **Nothing in the app counts the child's work.** No area counting, no
  progress line, no sparkle, no outline pulse, no ink ring, no completion
  rule. Every one of them was the app telling the child something about
  their own picture, and all of them are gone: a color the picture asks for
  and a color it does not are the same wax on the same paper. What remains
  of the old feedback is the screen reader's action, which colors an area
  because a child who cannot aim a finger cannot draw a mark either.
- D-038 **The sound of coloring is the rub of the crayon, not a click per
  mark.** Tapping the paper used to make a snap, which is a machine
  answering. What a crayon actually does is rub: `sfx_rub` is a seamless
  loop built from whole numbers of cycles of its own length, started when
  the finger goes down and stopped when it lifts, so the hand is the
  envelope. The rubber rubs at a lower rate. Three effects remain in the
  app (rub, rustle, chime), and `checkSounds` refuses a fourth file.
  Superseded by D-042: the rub is gone, and the bell went with the seal; one
  effect remains, the rustle of a crayon picked up or put down.
- D-039 **Every word is written in one hand and painted with wax.** Chewy
  (Apache 2.0, bundled offline) replaces Baloo 2 as the app's only face, at
  every size, for every word, and text is drawn with the wax ink brush from
  `core/WaxGrain` rather than filled flat: letters thin where the paper's
  tooth is high, exactly as a colored area does. A crayon app whose words
  were set in type was the last place the illusion broke.
- D-040 **The launcher icon is the app's crayon mirrored.** Same crayon,
  same lean, same proportions and the same drawing code: the mark that faces
  the other way. One line in `IconDesign.paintCrayon` does it, so both icons
  stay one object, and `checkIcons` still pins them to the generator.
- D-041 **Nothing in the app says a picture is done, and the shelf says
  nothing about the child's work at all.** The seal is gone whole: the round
  button in the bar, the stamp on the sheet, the seal on the shelf card, the
  set of sealed ids in DataStore, the bell, the heavier haptic, and the
  small coral crayon that named the page being worked on. A picture was
  never the app's to finish, and a mark on the wall was the app keeping
  score in the one place a child could see it. What is left is the plain
  thing the book always was: every picture equal, every picture open, the
  work saved exactly as the hand left it. In the box of colors the color in
  hand lies with the others, a little longer and drawn with a heavier line,
  and anything that would sit behind it (ring, plate, tick, shadow) is
  still forbidden. Amends D-019, D-022, D-028, D-034 in part; supersedes
  D-032 and the shadow half of D-028.
- D-042 **The app makes one sound, and never while the child is coloring.**
  The rub loop is cut: over a phone's own speaker a loop of shaped noise is
  not the sound of wax, it is a hiss the child cannot turn off, and the hand
  is not its envelope in any way the ear believes. The bell went with the
  seal, and nothing pitched remains. What is left is a rustle when a crayon
  or the rubber is picked up or put down, and the sound switch still silences
  it. Supersedes D-038 and amends the hard constraint on sound.
- D-043 **The box of colors opens over the page and puts itself away.** A tap
  outside the box closes it, and so does a pull down on it: a lid held over
  the page is exactly the thing a hand pushes away, and a three year old
  pushing at it should not be told to aim at the small dark edge of the
  screen.
- D-044 **The eraser is painted with the page itself, paper and print
  together.** A rubber that leaves the wax where it was is a rubber that
  does nothing: an eraser mark lays the printed page back over the child's
  marks, so the wax goes and the line that was printed there stays, exactly
  as a rubber behaves on paper, where the print is under the wax. That is
  why the page image carries the sheet's own paper and is opaque: an image
  with a transparent ground puts nothing back.
- D-045 **Undo takes back the last mark, and the shelf has no switches.** Two
  changes that belong together, because both are about what a press means.
  The page's capsule now carries a third seat: the crayon, the rubber, and
  the step back, which takes the mark the hand finished last off the paper.
  One mark and one press, never a stack, because a three year old's hand
  wanders and the mark just made is the one they are thinking about; an empty
  sheet has no step to undo and the press simply lands. It is `Draft.undo` in
  :core, tested there, and it is deliberately not an edit mode: a mark taken
  back is a mark taken back, and the child draws straight on. The rubber
  stays what it was, the way to change your mind about a whole picture, and
  nothing anywhere confirms anything. The other half: the sound switch is off
  the shelf and lives only on the page, where a child hears a crayon being
  picked up and can turn it off then and there without leaving the picture
  they are coloring. A wall of pictures does not need a switch on it. The
  seat layout and the sound switch stand; the one-mark rule is superseded by
  D-056, which lets undo walk back one mark a press.
- D-046 **The paper is brought closer with a chip, not with fingers, and the
  sheet is no longer taped down.** A page opens whole, and the child can
  bring it twice as close: a chip on the desk under the capsule, with the
  whole sheet drawn at one end of it and a closer look at the other, and one
  press anywhere on the chip puts the paper back down whole. There is no
  pinch and no double tap anywhere on the sheet. A pinch is two fingers on a
  page a small hand is coloring, which is exactly the mistake that ruins the
  mark under it, and a double tap is what two dots in the same place looks
  like when a three year old makes them; neither may move the paper. What
  moves it is a deliberate act on a thing that is not the picture. The tape
  on the sheet's four corners is gone with it: a sheet a child can bring
  closer to their face is a sheet nobody taped to a desk, and tape that
  stayed one size while the paper grew would be the one thing on screen that
  gave the illusion away. The tape stays on the shelf, where pictures really
  are taped to a wall. `core/PageView` is the window, `PageViewTest` holds it
  to the paper, and both renderers draw the page through it at the frame's
  own resolution, so a closer look is a sharper look and never a magnified
  bitmap. Zooming changes nothing about the work: every mark is a line of
  page coordinates, and a mark is exactly as wide on the paper at any
  closeness. Superseded whole by D-060: the sheet is now always whole and
  there is no zoom at all.
- D-047 **The crayon is wax, and nothing is drawn around it.** A real crayon
  has no line around it: the wrapper's edge is where the paper wound over the
  wax ends, and the wax beside it is the stick's own color laid on thick. The
  ink outline was there to say crayon and did the opposite, because at the
  size of a tray seat a line is most of what the eye reads and a drawn crayon
  became a diagram of one. What is left is wax: the body in the crayon's own
  color, the cone a hair deeper where the light leaves the tip, the wrapper
  the same wax taken deeper, its two printed rules deeper again, and the
  grain over the whole stick. All four colors come from one recipe in
  `core/CrayonInk`, derived from the wax itself, so the tray, the box, the
  shelf's own crayon and the launcher icon are the same object at every size.
  The same rule applies to the child's marks, where a wide faint pass had
  been standing in for the feather of wax: a pale edge is a border, and a
  border is a sticker. A mark is its own wax and nothing else.
- D-048 **The wax tile has no seam, and the drag is a smear.** The noise that
  makes a streak was built by sampling a lattice in the drag's own rotated
  frame and wrapping the sample modulo the cell counts. That cannot wrap: a
  rotation does not commute with a wrap except at a quarter turn, so every
  angle but a right angle left a hard step down the tile's own edge, and the
  step was big (measured: an alpha jump of 187 of 255 where the material's
  own neighbors differ by 20). A tiled shader shows that as a grid of faint
  rectangles over every colored area, and it dominated the picture. The drag
  is now two smears of a wrapping base field, sampled with wraparound
  indexing along the drag's direction: that is periodic by construction, so
  the tile has no seam at any angle. Two scales are needed, and this is the
  other half of the lesson: one smear short enough to stay local reads as a
  stain, one long enough to read as a hand reads as a gradient, so a long
  wisp and a shorter one ride together. `WaxTest` now measures the tile for
  a seam at eleven angles, both tile kinds, and measures that the material
  really is stretched along the hand that made it: both of these are bugs
  that no close reading of the code caught and one store capture did.
- D-049 **A closer look is movable, and a thing that is not the paper moves
  it.** Bringing the paper closer was half a feature: a window at twice the
  scale shows a quarter of the sheet, `PageView` pins it to the paper's edge,
  and a page opened at the middle could therefore never show its own
  corners. The child could zoom in and then find most of the picture out of
  reach, which is worse than not zooming at all. The chip on the desk now
  carries a little map of the sheet with the part on screen marked on it, and
  a press or a drag anywhere on that map puts that part of the page in the
  middle of the screen. `PageView.slid` is the one new rule in :core, held to
  the paper by the same clamp that was already there, so a child throwing the
  paper about can never see the desk. Nothing about the work moves: every
  mark is a line of page coordinates and a window is only what a renderer is
  looking at. The map is a second control on the chip, which is a thing on
  the desk and not the picture, so the one rule that matters survives: no
  gesture on the paper itself may ever move it, because a hand rests on the
  page while it colors. See D-046. Superseded by D-060, along with D-046 and
  the whole zoom feature.
- D-050 **The capsule runs the way the hand works: step back, crayon,
  rubber.** The order was crayon, rubber, undo, which put the one control
  that undoes work at the far end of the row, where a wandering thumb lands
  it by accident, and the crayon, which is picked up a hundred times a
  session, off center. The crayon now sits in the middle, where a thumb
  naturally falls; the step back sits at the left, out of the way, because a
  child should have to mean it; and the rubber sits at the right, as the
  other end of the same axis. The capsule's seats also came down from 62 to
  84 dp to a floor of 62 and a ceiling of 66, with tighter padding: three
  comfortable targets already cost most of a phone's width, and the capsule
  is a tool lying on a desk under the paper, not a second thing to look at.
  The glyphs inside the seats grew to use the room instead, so nothing got
  harder to hit. Amends D-045. Amended by D-064: the capsule holds four
  seats now, on the bar's own coins, and the pair of steps sits together.
- D-051 **Every selected seat wears the same plate, in the capsule's own
  cardboard.** The rubber's seat filled with a sepia and the step back's with
  nothing at all, so the same state was drawn two ways and the row read as
  two kinds of control rather than one row of three. Every seat now wears the
  same round plate, in the same `Cardboard`, lifted the same way; which tool
  is in hand reads at a glance and no seat means anything different from any
  other. Amends D-028 and D-034's selection language. Amended again by
  D-059: the plate is the *whole* selection mark, and the rubber's sleeve no
  longer repaints itself when the tool is picked up.
- D-052 **One line weight for every outline mark in the app.** The house and
  the speaker were drawn at a hairline and the tray's rubber and step back at
  a marker, so the top bar and the floating capsule looked like two different
  apps. All of them now come out of one `iconStroke`, one fraction of each
  mark's own box, and the fraction is chosen for the smallest size a mark is
  ever drawn at: a mark that reads at 24 dp reads at 26 dp, and the other
  way round is not true. Amended by D-064: one `IconSize` for every mark in
  the app, bar and capsule both, and the capsule's seats are `CoinSize`, the
  bar's own.
- D-053 **The launcher icon says what the app is without a word.** The icon
  was one large crayon, mirrored, which a child of three cannot read: it
  says writing tool at least as readily as coloring book, and at the size of
  a launcher tile its proportions read as a bullet. The mark is now two
  strokes, a crayon standing point down with the band of wax it has just
  drawn beside it, because that is the whole app in one glance and it needs
  no reading. The band is a filled shape of growing width with ragged edges,
  not a wide line, which is what a crayon drag really leaves; the whole mark
  leans and is mirrored exactly as before, so it is still the app's own
  crayon facing the other way. The drawing lives in `Mark` and `MarkBox` in
  `:tools`, measured from its own geometry and fitted by its furthest point
  from its own center, so no launcher mask can clip a tip at any density.
  Supersedes D-040's "the icon is the crayon alone" while keeping its mirror.
  Superseded in part by D-057: the sentence and the mirror stand, the low
  leaned crayon in a swath is replaced by an upright one, because the low
  mark merged into a single red hook at a launcher's smallest size. Amended
  by D-065: the mirror is gone, the crayon faces the app's own way, the band
  is a line rather than a slab, and the stance is `CrayonShape.MARK_TURN` in
  :core, which the tray and the nameplate draw too.
- D-054 **The crayon rules were drawn off the end of the crayon, and every
  screen showed the two hairs.** `drawWrapperRules` was handed a pixel
  measure and used it as an inset into a band measured in the crayon's own
  thicknesses, so each rule landed roughly a hundred thicknesses along the
  stick and past its end: two thin coral hairs floating beside the mark, on
  the shelf header, in the capsule, in the box and in the icon. `RULE_INSET`
  is now a share of the band's own length, so the two unit systems can no
  longer be mixed, and `CrayonShapeTest` holds both rules inside the band
  and inside the crayon. The bug had been in every capture since the wrapper
  gained its rules, and it was found by making the rules green on a device
  and watching the hairs turn green with them: a mark drawn in a color
  nothing else in the app uses is worth more than any amount of reading.
- D-055 **The wax is the stick's color, and a mark is one pass of it.** Two
  things were wrong with the child's own marks, and they had the same
  cause. The wax surface laid down an average coverage of 0.78, so a mark
  came out a pale wash of the crayon rather than the crayon; and the mark
  renderer then drew two translucent passes of it, one narrow over one wide,
  and two translucent passes of a color multiply rather than average: the
  core of every mark came out nearly pure, the grain was pushed out of the
  one place it was most needed, and the pale edges the pair left behind are
  exactly what makes ink from a pen and not wax from a stick. Coverage is
  now 0.90 for an area and a mark alike, with the variation carried by the
  drag and the tooth instead of by a low average, and a mark is a single
  pass. Measured on a device, a red mark over paper now carries an implied
  alpha of 0.95 of the crayon's own hex, against 0.75 before. Both renderers
  changed together, because this is exactly the agreement the renderer
  section exists to hold. Amends D-036 and D-047.

- D-056 **The wax is crayon, not ink, and undo is a walk back.** Two things
  the child feels at once. A mark's wax ran at a mean alpha of 233 with a
  standard deviation of 11, four percent variation: high coverage with a
  narrow swing is not wax on paper, it is flat ink with a faint texture,
  which is exactly what a sign pen leaves and not what a crayon does. The
  coverage stays high but the swing and the tooth grow, on a mark and on a
  filled area alike, so the stick's own color still lands and the paper's
  grain is what the eye reads as crayon. `WaxTest` now holds the variation
  wide as well as the mean high, and still holds the drag stronger than the
  tooth, so a mark stays stretched along the hand that made it. Undo, the
  other half: one press took back exactly one mark and the button then went
  dead, which is no use to a hand that drew three marks it did not mean. The
  page now carries the paper as it was before each finished mark, up to
  sixteen of them, and each press walks back one. The stack is bounded, and
  drawing after a step back starts a fresh stack from the paper now on the
  desk, so a press always returns to a paper the hand really made. Amends
  D-036, D-047, D-055 and supersedes the one-step rule of D-045.
- D-057 **The launcher icon is legible at twenty four pixels.** The mark was
  a low, heavily leaned crayon lying in a wide swath, and at the size a
  launcher really draws it the two elements merged into one red hook: the
  crayon became unreadable exactly where it is seen most, on the home
  screen. The crayon now stands almost upright, is the biggest thing in the
  tile, and leans only twenty degrees; its blunt tip reaches down into a
  swath that runs out to the right from just under it, so the two read as a
  crayon and the wax it has just laid down even in a 24 pixel tile. The mark
  is still the app's own crayon from `core/CrayonShape`, still mirrored so
  the home screen faces the other way, and still fitted by its furthest
  point from its own center so no launcher mask clips a tip. Supersedes
  D-053's poster (the low, leaned crayon in a swath) while keeping its
  sentence and its mirror.
- D-058 **The bar and the capsule sit on one rail.** The top bar (home, the
  sample, the sound switch) and the capsule (the step back, the crayon, the
  rubber) are both rows of the same round controls, but they were each
  centered at their own natural width, so the two rows did not line up and
  the phone read as two objects that happened to be stacked. Both are now
  laid on one shared rail, a single width the two rows fill, and the rail
  stops growing on a very wide screen so a tablet never gets a fence of
  controls. The capsule's own padding came in with it; its seats keep their
  comfortable floor. See D-050.
- D-059 **The rubber's own color never changes.** Selecting the rubber
  repainted its sleeve in solid ink as well as lifting the seat, so the same
  selection was drawn twice and the rubber appeared to change color when it
  was picked up. A real rubber is one object whatever its owner is doing
  with it, and the seat's own cardboard plate already says which tool is in
  hand: the sleeve is the printed paper wound around the block, always, and
  the plate is the whole selection mark. Amends D-051.
- D-060 **The page is whole, always, and it is taller than it is wide.**
  Bringing the paper closer was tried and it was wrong, and this is the
  decision to take it out rather than patch it again. A window at twice the
  scale showed a quarter of the sheet, so most of every picture went out of
  reach, and the only way back was a small map of the sheet on a chip under
  the capsule: a three year old will not find that map and cannot aim at it,
  which makes the zoom a trap rather than a tool. The sheet is now always
  whole; there is no zoom, no pan, no pinch and no double tap anywhere in
  the app, no `core/PageView`, and no chip. The screen the work sat in was
  also wrong: the sheet was forced square and centered, leaving two bands of
  empty desk above and below it on a phone, and making the small things a
  child is learning to aim at (a window frame, a sprinkle, a flower's
  center) too small to hit. A page is now a sheet taller than it is wide by
  `Page.ASPECT` (1.2), and every picture, composed in a square, is fitted
  onto it once in `core/Pages.kt`. Page units are isotropic, so nothing is
  stretched: a circle stays a circle and a mark is as wide whichever way the
  hand dragged it. The taller sheet alone makes every element a fifth bigger
  on a phone, and it is what lets the picture take the whole screen.
  Supersedes D-046 and D-049, and with them the whole zoom feature.

- D-061 **A full-bleed band may run off the sheet, a thing may not.** The
  uniform fit onto the taller sheet grows every picture by a fifth, and a
  band that was authored edge to edge (a sky, a sea, a lawn) is meant to
  reach past the paper's own sides and be clipped. A cloud is not: fitting
  the square by 1.2 sliced the edge clouds of the sail, tree, balloon, kite
  and rainbow pages flat, which reads as a mistake and not as weather. Every
  region that is not already a full-width band is now slid back by the least
  it takes to sit wholly on the sheet, a few hundredths of the page. This
  was caught by a new test, `noPartOfAPictureIsCutOffByTheSheet`, written
  after the 0.7 build had already shipped: the review sheet was the only
  place the cut showed, and the test is what makes it impossible to miss
  next time. See D-060.
- D-062 **The top bar is full width on a tablet, and on a rail only on a
  phone.** Laying the top bar and the capsule on one shared rail is right on
  a phone, where the two rows are stacked and should line up. On a tablet
  the sheet and the tools stand side by side and the bar is the full width
  of the screen, exactly as it always was: a rail there dragged the home
  button into the middle of the desk. Amends D-058.
- D-063 **The finger lands where the finger is.** A mark is placed by
  `core/pagePointOf`, the one conversion from a point on the glass to a point
  of the paper. Page units are isotropic, so a frame point is divided by the
  sheet's *width* on both axes, and the frame's height is never a number to
  divide by. Dividing y by the height is the bug this replaces, and it was a
  bad one: the sheet is 1.2 times taller than it is wide, so on a phone every
  mark landed a fifth of the sheet above the fingertip, which is exactly the
  gap between a crayon and the wax it just laid down. The eraser was placed
  the same wrong way and is fixed by the same line. `PagePointTest` walks the
  frame at five scales and holds the paper's corners, its middle and its
  edges, and holds that a point of the paper always resolves to an area: a
  first touch may never land on nothing.
- D-064 **The tray is four seats on the bar's own coins, and the paper walks
  both ways.** Two changes to the same row. The capsule and the bar now draw
  one coin (`CoinSize`) and one icon (`IconSize`), and the capsule's rail is
  *measured* from the coins it holds rather than chosen, so the two rows line
  up at both ends by construction and neither can drift away from the other.
  Adding the fourth seat made the rail one coin wider instead of making every
  seat smaller, which is what kept the seats at the size of the coins above
  them. The step forward sits beside the step back with the crayon and the
  rubber on their right, because the two steps are one pair and a child who
  walked a mistake out has to be able to walk it back without hunting for the
  other end of the row. `Draft.redo` is the exact mirror of `Draft.undo`,
  bounded the same way and read off the same list of real papers, and drawing
  clears the steps forward so no press can put back a mark the hand has
  already drawn over. Amends D-045, which had one direction of a two-way
  pair.
- D-065 **The app's crayon leans, and it leans the same way everywhere.** The
  mark is one crayon at one angle: `CrayonShape.MARK_TURN` is the held
  stance leaned 20 degrees, with the tip down and to the left and the base up
  and to the right, the way a right hand holds a crayon to draw a line that
  runs away to the right. The launcher icon draws that turn, the wall's
  nameplate draws that turn, and the tray's crayon draws that turn, all
  through one `drawCrayonShape`; the icon generator no longer mirrors its
  drawing, because a mark that faces one way in the launcher and the other
  way in the app is two crayons. The shelf's mark used to lie on its side,
  which made it a third one. The wax band the icon's crayon has just laid
  down is a line, not a slab, at 0.095 to 0.115 of the mark's own box: a
  band a fifth of the crayon long read as a painted swoosh beside the stick
  rather than as the drag the stick made. Supersedes the mirror half of
  D-040, amends D-029, D-052 in part.

- D-066 **The control marks are one drawing, and the capsule runs from the
  crayon.** Three things about the row of round buttons, fixed together
  because they were one complaint: the floating capsule did not look like
  the bar. First, the marks themselves. The house and the speaker were line
  drawings at one weight; the steps were a swooping cubic with a fat wedge
  jammed on it; the rubber was a filled parallelogram with a sleeve painted
  in a second surface, and at the size of a seat it read as a bow tie. They
  are now one set, in `core/AppIcon.kt`: data, in a unit box, sampled into
  polylines so every renderer walks the same points, struck at one weight
  (`AppIcon.LINE`, 0.098 of the mark's own box), and drawn at `IconSize`
  wherever they appear. The steps are an open ring with a wedge whose base
  sits across the stroke it ends, and the step forward is the step back
  reflected point for point, so the pair is one object in two directions.
  The rubber is a rounded block lying at a small angle, with the sleeve's
  two printed rules across its near end: the rounding is what says rubber,
  because a squared block with lines across it is a book or a battery. The
  rules are lighter than the outline (a share of the mark's own weight),
  because they are printed on the object rather than drawn around it, and
  they stop inside the block's edges so a round cap cannot paint two nubs
  of ink out in the paper beside it. `AppIconTest` holds all of it: every
  mark inside its own box, every mark filling that box, the two steps
  exactly mirrored, the rubber's rules inside the block, and nothing drawn
  as a solid blob. Second, the seats. Every round control in the app is now
  the same `CoinPlate`: the same coin, the same shadow, the same dip under a
  press, the same ink ripple, whether it is floating in the bar or lying in
  the capsule, so a row of buttons is one object even when it is two rows.
  Third, the order, which the owner asked for directly: the capsule now runs
  **crayon, rubber, step back, step forward**. The crayon is what a child
  picks up a hundred times and it now sits at the near end of the row; the
  rubber, the other thing that is held and put down, sits beside it; and the
  two steps take the far end as the pair they are, so a child who walked a
  mistake out has to pass the crayon to reach them. Supersedes the seat
  order of D-050 and D-064. See D-065 (one lean), D-047 (nothing around the
  wax), D-051 and D-059 (one selection plate).
- D-067 **Nothing in the app is taped down.** The shelf's pictures were
  held to the wall with a strip of washi tape, and the tape was cream with
  its own edge drawn, because tape you cannot see is not holding anything
  up. The owner's word: no tape. So the tape is gone whole, from the four
  corners of the wall's pictures, from the palette (`CrayonerColors.Tape`,
  `TapeEdge`, `TapeFiber`), and from the code (`ui/Tape.kt` is deleted,
  because a material nothing paints with is a material the next change
  reaches for by accident). What holds a picture up on this wall is that it
  is on the wall: the cards are still sheets of paper with a soft shadow and
  a small hand-placed tilt, and the picture is the thing. Supersedes the
  tape half of D-019 and the sentence in D-046 that kept the tape on the
  shelf.
- D-068 **The app asks one question: keep this picture, or start that one
  fresh.** Every mark is saved as it is made, so nothing here was ever lost
  by leaving a page, and for two versions the app simply kept the work and
  said nothing. The owner asked for a choice when leaving a page mid
  coloring, and the version that fits this app is one short question, asked
  only when the page has marks on it: two big coins, a tick on the capsule's
  cardboard for keeping it and a cross on plain paper for starting over.
  Both answers are safe. Keeping it costs nothing, because the marks are
  already saved; starting fresh only means the next visit opens on a clean
  sheet, which is a thing a child may well want. The system's own Back and a
  press on the scrim both put the question away and leave every mark where
  it was, so the only way out of a question is never into one of its
  answers, and a bare sheet never raises it at all. This is the one place
  the app ever removes work, and it takes two deliberate presses to do it:
  Home, and then the cross. Amends the "nothing anywhere confirms anything"
  of D-045 and the "no start over" of D-044, which are now true of
  everything except this one question.
- D-069 **A page is allowed to have no weather, and nothing is too small to
  color.** Two rules about the pictures, both from looking at the book
  rather than reading it. First, clouds: every outdoor page had one in the
  same corner at the same size, which is the sun's old wallpaper habit
  wearing a different hat, and the owner said so. Now some pages carry
  weather and some do not; the ones that do put their clouds in different
  corners, at different heights and different sizes, and a page with an
  empty sky is a page whose sky is a big field to color. Second, and
  enforced: every area of every picture must contain a circle of at least
  `FingerTest.FLOOR` (0.070 of the page's own width) across, because a
  fingertip that lands in a space as wide as the wax it lays down has
  colored that part of the picture. The measure is exact, by a squared
  distance transform over the area's outline, and it is held by
  `FingerTest` plus a second test that holds the ruler itself to shapes
  whose answer can be worked out by hand. The sprinkles went: a sprinkle
  wide enough to hit is not a sprinkle. The stars, apples, cherries, window
  circles, stripes, raindrops, track and pole all grew until the smallest
  thing in the book is a fingertip's width. `noPartOfAPictureIsCutOffByThe
  Sheet` was also strengthened from region bounds to every piece of every
  region, because a region is often several scattered things painted
  together and the raindrops it was hiding were being sliced by the edge of
  the paper.
- D-070 **The launcher mark puts the crayon at the end of its own line.**
  The icon's crayon used to stand at the near end of the wax band with the
  band running away to the right, and the owner read it, correctly, as a
  crayon held above a scratch rather than one that has just drawn it. The
  crayon now stands at the far end with the line trailing back to the left
  and growing a little as it goes, the way wax really builds up, and the
  crayon's own tip is in the line it made. The lean came up from 20 to 26
  degrees at the same time (`CrayonShape.MARK_LEAN`), because at 20 the
  stick stood to attention beside the app's name and read as a stick rather
  than as a crayon in a hand; the angle is now the held stance of a hand
  that is drawing, everywhere the mark appears. The wrapper's two rules also
  stopped short of the wax's own flanks (`CrayonShape.RULE_END_INSET`),
  because a round cap reaches half a stroke past the point it is given, and
  rules drawn flank to flank painted two nubs of ink out in the paper beside
  the crayon at the smallest launcher sizes. The launcher icon, the store
  icon, the wall's nameplate and the capsule's crayon are still one object
  at one angle. Amends D-065 (the lean and the band's direction) and D-054
  (the rules' insets).
- D-071 **The tools stand on the left, and the bar runs edge to edge.** Two
  layout corrections the owner asked for together, both about where a hand
  already expects to find things. First: on a sideways screen the capsule
  stood on the right, in a three hundred dp column, and the paper sat to its
  left; that is backwards. A page reads left to right and a child's tools
  belong on the side the line starts from, which is also the side the hand
  coloring a page comes from. The capsule now stands at the desk's left
  edge, and it turns on its own side to do it: the four seats run top to
  bottom in the same order they run left to right on a phone, so the tool a
  hand wants is always in the first seat and the row always grows away from
  the corner the hand comes from. The column is 74 dp wide, which is the
  capsule's own thickness and its shadow, so the paper gets back most of the
  300 dp the old column took. Second: the top bar. When the capsule's rail
  was measured from its own coins (D-064), the bar was still laid on that
  same rail, so on a phone it became a narrow row floating in the middle of
  the desk with the Home button more than a hand's width in from the corner.
  The bar is now full width in both shapes, with a ten dp margin, so Home is
  in the top left corner and the sound switch is in the top right, which is
  where they were before the rail and where a thumb looks without asking.
  The capsule keeps its own measured width and stays centered under the
  sheet on a phone. Supersedes the shared rail of D-058 and the "rail only
  on a phone" half of D-062.
- D-072 **The castle is not a face.** The castle page was two towers of one
  height, each with a round window and a pointed roof, and a door on the
  center line. Two round windows set level above a door are a face read from
  across the room, which hard constraint 2 forbids, and with the windows
  gone the two roofs became the pair that still made one. The page was
  rebuilt so that nothing on it is symmetrical: a tall crenellated keep on
  the right, a low crenellated wall running out to the left, one door off
  the center line, and no windows and no roofs anywhere. Every merlon is a
  square on a wall's own top edge, at two heights and several widths, and no
  two of them sit opposite each other. The rule this settles is that no two
  alike shapes may be set level over a third: battlements are a row, a face
  is a pair.
- D-073 **The one sound answers paper being handled, and nothing else.** The
  rustle now plays wherever a hand moves paper or wax: a crayon picked from
  the box, the rubber picked up or put down, the box lid opening and
  closing, the sample sheet held up and laid back down, and a picture taken
  from the wall. It is silent for the two steps (they change the paper, but
  no hand has picked anything up, and the haptic already answers them), for
  Home, and for the keep question, which are navigation rather than
  handling. The sound switch plays the rustle only when it is turned back
  on: a parent who has just allowed sound should hear it land, and a switch
  being turned off is silent because that is what it was just asked for.
  Nothing plays while a finger is on the paper. The effect itself is
  unchanged, and there is still exactly one of it.
- D-074 **Every separate piece a child can see is a fingertip wide.** The
  old floor (D-069) measured each region once and asked whether the region
  held a circle 0.070 of the page across. That let a big sibling hide a
  small one: the four apples passed on the width of the largest apple, and
  a child who wanted the smallest one still could not put a finger on it.
  `FingerTest` now rasterizes each page, keeps the topmost region at every
  cell (the same rule a finger uses), splits each region into its visible,
  connected pieces, and holds every piece to a floor: a compact piece must
  hold a circle 0.155 across (a fingertip on the smallest phone the app is
  used on), and a long band must hold one 0.075 across (wider than the wax
  tip, so a stroke can be drawn along it). The ruler's own test stays. What
  the audit found was fixed in the pictures: the ice cream's and cupcake's
  cherries, the tree's apples and trunk, the mushroom's spots, the kite's
  tail bows, the flower petals and centers and leaves, the umbrella's
  raindrops and handle, the rocket's stars and fins, the car's windows and
  the balloon's basket, the lighthouse's stripes, lamp and roof, and the two
  slivers of lawn the car and train pages printed between the road and the
  paper's own edge (the grass went; the road and the ballast run to the
  bottom of the sheet). The balloon's second cloud went with them: it could
  not fit between the envelope and the paper's edge without being sliced,
  and a sliced cloud is not weather. Amends D-069, which set the rule but
  measured the wrong thing.
- D-075 **The shelf remembers a kept picture the moment it is kept.** Every
  mark was saved a quarter of a second after it was made, but the in-memory
  shelf the app reads when a page reopens was the map it was born with:
  `rememberDraft` wrote to the disk and never to the shelf. A child who
  pressed Home, answered Keep it just as it is, and opened the same picture
  again saw blank paper, which read as if the answer had done nothing at
  all. The draft now goes into the shelf in the same breath as it starts
  for the disk, and starting fresh lets it go in the same breath; the write
  still settles a quarter of a second later, so a process death costs at
  most the mark in flight. `HostRulesTest` holds the rule without a device.
  Amends D-068.
- D-076 **Neither the wall nor the wax makes the hand wait.** Four cost
  fixes with one cause: work was being done at the moment the finger needed
  the frame. The wall's prewarm rendered sixteen pictures at the grid
  *cell's* width while every card asks for its own inner width, so no card
  ever found a prewarmed picture, every card rendered its own again, and
  the cache carried two sets for one wall; the prewarm now measures the
  card's own width, to the pixel, and each card is laid out at exactly the
  size of the picture on it. The page's layer of finished marks no longer
  re-renders when a mark lands: it grows by the one mark drawn on top of
  it, and only a step back or the rubber rebuilds it whole. A live mark was
  building a new shader every frame (one for its wax, one for the printed
  page under the rubber); both are built once and kept. And a sheet reads
  its own width on every pointer event and adds the point the finger lifted
  at to the line, so a resize can never pull the wax off the fingertip and
  a quick flick ends where the hand really stopped. The screenshot harness
  had the same disease in a different organ: it waited on the main thread
  for a copy that answers on the main thread, so every capture timed out
  and the first one came back transparent; the wait now happens off the
  main thread, with a software draw of the view tree as the fallback, and
  the first capture is the wall as it really is. See D-063 for the mapping
  that is now proved in x and y: `TouchProbeTest` taps three points on the
  sheet and finds each dot's own center, not only its row.
- D-077 **The picture store is a store, and the wax is made once.** Two
  costs that were one bug each, both of them in the way a page becomes
  pixels. The store of rendered pages was keyed by a class that compares by
  identity, so every look-up built a fresh key, every look-up missed, and
  every screen drew its own copy of a picture that had just been drawn: the
  wall's prewarm rendered sixteen sheets that no card could find, the coin
  in the bar drew a page again on every visit, and the sheet re-rendered its
  own print whenever it was composed. The key is a data class now and the
  store does what it says. Its budget is split in two, because the wall's
  sixteen cards and the page being colored are not the same kind of thing:
  the wall's pictures are kept for as long as the wall can be looked at,
  since a card with no picture is a card that has to be drawn line by line
  on the frame the finger is asking for, which was the wall's worst stutter,
  and every other picture (the sheet, its marks, the sample held up, the
  coin in the bar) lives beside them and is let go first. A picture that is
  still being made is drawn as the page's own print until it lands: never
  the wax, which is the expensive half, and never a hole. Two more costs
  went with it. A wax tile was cached under the size it was drawn at, and a
  tile does not depend on that size at all: the tooth, the mottle and the
  drag are the same ninety six pixels whether they cover a card or a whole
  sheet, so four sizes meant the book's ninety six areas were made four
  times over. And the tile's own walk over its lattice paid for a `pow`, a
  `Math.round` and two remainders for every pixel; it is four times faster
  now and the same tile to the last pixel, pinned by a hash in `WaxTest`.
  The marks' layer grows by the one mark that landed instead of being copied
  whole every time, the page's paths are built once at each size and shared
  by the printed picture and the live drawing alike, and every background
  render goes through one gate, so one picture is being made at a time and
  the one a card is waiting for is drawn before the wall's own march reaches
  the pages after it. The capture harness changed with it: a scene is looked
  at until two copies in a row match instead of being copied at a fixed
  moment, because a wall whose pictures are still being made goes on
  changing for a second after its cards are up.
- D-078 **The finger and the wax are the same frame.** The mark under the
  finger used to be part of the screen's own state and travelled to the
  screen through a flow: a touch wrote a value, a coroutine collector woke
  up on the next dispatch, the whole desk recomposed, and the wax landed a
  frame or two after the fingertip that made it. On a slow frame that is a
  mark a tenth of a second behind the hand, which is the one thing this app
  cannot be wrong about. Three changes, all of them between the glass and
  the paper. The screen's state is Compose's own state rather than a flow,
  so a write made inside the touch event is read by the composition of that
  same frame. The mark in flight is not part of the screen's state at all:
  it has its own state, and the one canvas that paints it asks for it inside
  the draw, so a move event costs a redraw of one canvas and never a
  recomposition of the desk. And every point the system batched into one
  event is walked, oldest first, so a quick flick is the line the hand
  really drew rather than the last two dots of it. The mark also never
  freezes: a stroke that has spent its nine hundred points is thinned in its
  older half rather than stopped, so the crayon cannot leave the paper while
  the child is still pressing it down, and its two ends stay exactly where
  the hand put them. Amends the stroke budget of D-024. Measured on the CI
  emulator, whose graphics are software and whose absolute frames are the
  emulator's and not a phone's: a scrolled wall frame spends about a
  millisecond in layout and one to three in draw, a drawing hand about a
  millisecond, the page's own composition on opening went from seventy
  milliseconds to fifteen, and the worst frame of a launch went from eleven
  seconds to a quarter of one. A gesture that is cut off in the middle of
  itself, by the screen turning over or a phone call arriving, finishes the
  mark the hand had already put down instead of leaving half of one hanging
  on the paper. Still open, and named rather than hidden: a page that has
  spent its six hundred strokes drops the marks made after that, and the
  honest answer is to thin the oldest marks rather than refuse the newest
  one.
