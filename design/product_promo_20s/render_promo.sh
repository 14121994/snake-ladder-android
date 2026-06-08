#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

mkdir -p renders .tmp media/overlays
PYTHON_BIN="${PYTHON_BIN:-/Users/adkamat/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3}"
if [[ ! -x "$PYTHON_BIN" ]]; then
  PYTHON_BIN="python3"
fi
"$PYTHON_BIN" render_overlays.py

make_segment() {
  local input="$1"
  local overlay="$2"
  local output="$3"
  local crop_y="$4"
  local pan_y="$5"

  ffmpeg -y -loop 1 -i "$input" -loop 1 -i "$overlay" \
    -filter_complex "[0:v]zoompan=z='1+0.035*on/119':x='(iw-ow/zoom)/2':y='${crop_y}+${pan_y}*on/119':d=120:s=1080x1920:fps=30,format=rgba[base];[1:v]format=rgba[ov];[base][ov]overlay=0:0:format=auto,format=yuv420p[v]" \
    -map "[v]" -an -r 30 -t 4 -c:v libx264 -pix_fmt yuv420p "$output"
}

make_segment media/fresh_screenshots/board.png media/overlays/01_hero.png .tmp/seg01.mp4 160 38
make_segment media/fresh_screenshots/settings.png media/overlays/02_settings.png .tmp/seg02.mp4 88 18
make_segment media/fresh_screenshots/campaign.png media/overlays/03_campaign.png .tmp/seg03.mp4 80 22
make_segment media/fresh_screenshots/menu.png media/overlays/04_menu.png .tmp/seg04.mp4 118 26
make_segment media/fresh_screenshots/launch.png media/overlays/05_close.png .tmp/seg05.mp4 120 18

printf "file 'seg01.mp4'\nfile 'seg02.mp4'\nfile 'seg03.mp4'\nfile 'seg04.mp4'\nfile 'seg05.mp4'\n" > .tmp/concat.txt

ffmpeg -y -f concat -safe 0 -i .tmp/concat.txt -c copy .tmp/video.mp4

ffmpeg -y \
  -f lavfi -t 20 -i anullsrc=channel_layout=stereo:sample_rate=44100 \
  -i media/dice_roll_sfx.ogg \
  -i media/snake_hiss_sfx.ogg \
  -filter_complex "[1:a]adelay=2100|2100,volume=0.85[r1];[2:a]adelay=6100|6100,volume=0.55[h1];[1:a]adelay=10200|10200,volume=0.70[r2];[1:a]adelay=17600|17600,volume=0.80[r3];[0:a][r1][h1][r2][r3]amix=inputs=5:duration=first,alimiter=limit=0.90[a]" \
  -map "[a]" -c:a aac -b:a 160k .tmp/audio.m4a

ffmpeg -y -i .tmp/video.mp4 -i .tmp/audio.m4a \
  -map 0:v:0 -map 1:a:0 -c:v copy -c:a aac -shortest -movflags +faststart \
  renders/snake_ladder_product_promo_20s.mp4

ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 renders/snake_ladder_product_promo_20s.mp4
