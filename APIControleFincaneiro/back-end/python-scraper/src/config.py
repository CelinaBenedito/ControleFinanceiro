import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv()

# ── Diretórios ────────────────────────────────────────────────────────────────
# Raiz do python-scraper (um nível acima de /src)
BASE_DIR = Path(__file__).resolve().parent.parent

# Pasta onde os .ofx serão salvos — aponta para uploads/ofx do back-end Java
OFX_OUTPUT_DIR = Path(os.getenv("OFX_OUTPUT_DIR", str(BASE_DIR.parent / "uploads" / "ofx")))
OFX_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# ── API ───────────────────────────────────────────────────────────────────────
API_PORT = int(os.getenv("API_PORT", "8000"))
API_HOST = os.getenv("API_HOST", "127.0.0.1")  # Só localhost — nunca expor na rede

# ── Browser ───────────────────────────────────────────────────────────────────
BROWSER_TYPE     = os.getenv("BROWSER_TYPE", "chromium")   # chromium | firefox | webkit
HEADLESS         = os.getenv("HEADLESS", "false").lower() == "true"
BROWSER_TIMEOUT  = int(os.getenv("BROWSER_TIMEOUT", "300000"))  # 5 min (ms)
LOGIN_TIMEOUT    = int(os.getenv("LOGIN_TIMEOUT", "300"))        # 5 min (segundos)
DOWNLOAD_TIMEOUT = int(os.getenv("DOWNLOAD_TIMEOUT", "60"))      # 1 min (segundos)

