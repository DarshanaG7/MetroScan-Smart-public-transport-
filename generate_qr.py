"""Generate one QR code per bus stop.
Setup:  pip install "qrcode[pil]"
Usage:  python generate_qr.py https://metroscan.example.com
"""
import sys
from pathlib import Path
import qrcode

BASE_URL = sys.argv[1].rstrip("/") if len(sys.argv) > 1 else "http://localhost:8080"
STOP_IDS = ["MG01", "MJ02"]
out = Path("codes"); out.mkdir(exist_ok=True)

for stop in STOP_IDS:
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=10, border=4)
    qr.add_data(f"{BASE_URL}/?stop={stop}")
    qr.make(fit=True)
    qr.make_image(fill_color="black", back_color="white").save(out / f"{stop}.png")
    print("saved", out / f"{stop}.png")
