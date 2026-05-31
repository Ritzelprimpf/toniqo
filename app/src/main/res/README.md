# Toniqo — Android brand assets

Drop these into your module's `src/main/res/`. Folder names already match
the resource-qualifier directories Android expects.

## Launcher icon (adaptive, API 26+)
```
res/mipmap-anydpi-v26/ic_launcher.xml          → adaptive icon manifest
res/mipmap-anydpi-v26/ic_launcher_round.xml    → same, round alias
res/drawable/ic_launcher_foreground.xml        → vector foreground (marks, in 72dp safe zone)
res/drawable/ic_launcher_background.xml         → vector background (solid #28302E)
```
The `<monochrome>` layer reuses the foreground so themed icons (Android 13+) work.

### Legacy raster fallback (API < 26)
```
res/mipmap-mdpi/ic_launcher.png      48×48
res/mipmap-hdpi/ic_launcher.png      72×72
res/mipmap-xhdpi/ic_launcher.png     96×96
res/mipmap-xxhdpi/ic_launcher.png   144×144
res/mipmap-xxxhdpi/ic_launcher.png  192×192
```
Keep these so devices below API 26 still get a crisp icon.

### Play Store listing
`play-store-icon-512.png` — 512×512, upload to the Play Console (do not bundle in APK).

## Monogram (in-app brand use — splash, About, headers)
```
res/drawable/logo_monogram.xml          ← preferred: vector, scales infinitely
res/drawable/.../toniqo_monogram_*.png  ← raster fallback (96 / 192 / 384 px, transparent)
```
Tint the strokes at runtime if you need the light-theme variant, or recolor in the XML
(swap #F3F6F5 → #1C2422 and #9CFF8B → #37A85F).

## Wordmark (splash, About)
The wordmark is **Space Grotesk SemiBold** with a mint period. Android can't rely on the
web font, so it ships as **font-baked transparent PNGs** — vector drawables can't hold text.
```
wordmark/toniqo_wordmark_light_{64,128,256}.png   ← for dark backgrounds (light glyphs)
wordmark/toniqo_wordmark_dark_{64,128,256}.png    ← for light backgrounds (dark glyphs)
```
Put the height you need closest above your target dp into the matching density bucket, or
load via Coil/Glide. Mint period is baked at #9CFF8B in both.

## Brand colors
`values/colors_brand.xml` — merge into your `colors.xml`:
- `brand_chassis` #28302E · `brand_fg` #F3F6F5 · `signal_mint` #9CFF8B

Full token set lives in `DESIGN_TOKENS.md` at the project root.

## Notes
- Vector drawables (`logo_monogram`, `ic_launcher_foreground`) are exact — they're the
  source of truth. PNGs are generated from the same geometry for fallback only.
- If you re-export the wordmark at a different size, bake the font (don't ship live text).
