# MetroScan
Run from this folder:
    javac backend/MetroScanServer.java && java -cp backend MetroScanServer
    open http://localhost:8080/?stop=MG01
QR codes:
    pip install "qrcode[pil]" && python qr/generate_qr.py http://<your-host>:8080
