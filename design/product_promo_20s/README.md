# Snake & Ladder Product Promo

20-second vertical product promo for the Android game.

## Files
- `index.html` is the HyperFrames composition source.
- `media/fresh_screenshots/` contains fresh screenshots captured from the connected Android device for this promo.
- `media/overlays/` contains generated transparent title/feature overlays.
- `renders/snake_ladder_product_promo_20s.mp4` is the local MP4 render.

## Storyboard
- `0.0-4.0s`: Live board and dice gameplay.
- `4.0-8.0s`: Match controls and settings.
- `8.0-12.0s`: Campaign quest map and progression.
- `12.0-16.0s`: Launch hub with modes, saves, store, progress, and guide.
- `16.0-20.0s`: Closing title card.

## Render
The HyperFrames source uses the fresh screenshots as timed media clips:

```bash
npx hyperframes lint
npx hyperframes inspect --samples 12
npx hyperframes render --output renders/snake_ladder_product_promo_20s.mp4
```

In this environment, `npx hyperframes lint` could not be completed because npm registry access was unavailable, and a network-enabled `npx` run was rejected as external package execution. The local fallback render uses the same fresh screenshot assets:

```bash
bash render_promo.sh
```
