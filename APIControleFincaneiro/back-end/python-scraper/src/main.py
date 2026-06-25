"""
main.py — API FastAPI que recebe triggers do Java e aciona o Playwright.

Endpoints:
  GET  /         — info geral
  GET  /health   — health check (usado pelo Java para saber se o Python está pronto)
  POST /capture  — dispara o scraping e baixa o OFX
  GET  /files    — lista os OFX já baixados

A API escuta apenas em 127.0.0.1 (localhost) — nunca exposta na rede.
"""

import asyncio
import os
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

# Garante que imports relativos funcionem tanto em dev quanto no .exe do PyInstaller
sys.path.insert(0, str(Path(__file__).resolve().parent))

from automation import NavigationStep, capturar_ofx
from config import API_HOST, API_PORT, OFX_OUTPUT_DIR
from logger import get_logger

log = get_logger("main")

# ─────────────────────────────────────────────────────────────────────────────
# App
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title="MyFinance OFX Scraper",
    description="API interna que captura arquivos OFX de plataformas bancárias via Playwright.",
    version="1.0.0",
)

# Só aceita origens do próprio localhost
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)


# ─────────────────────────────────────────────────────────────────────────────
# Schemas Pydantic
# ─────────────────────────────────────────────────────────────────────────────

class NavigationStepDTO(BaseModel):
    action:   str
    selector: Optional[str]  = None
    text:     Optional[str]  = None
    timeout:  Optional[int]  = None


class CaptureRequestDTO(BaseModel):
    bank_url:         str                           = Field(..., description="URL de login do banco")
    navigation_steps: Optional[list[NavigationStepDTO]] = Field(
        None,
        description="Passos de navegação. Se null, o usuário faz login manualmente."
    )
    description:      Optional[str] = Field(None, description="Descrição para logging")


class CaptureResponseDTO(BaseModel):
    success:   bool
    message:   str
    file_path: Optional[str] = None
    file_name: Optional[str] = None


class FileInfoDTO(BaseModel):
    filename:    str
    path:        str
    size_bytes:  int
    created_at:  float


class FilesResponseDTO(BaseModel):
    total:            int
    output_directory: str
    files:            list[FileInfoDTO]


# ─────────────────────────────────────────────────────────────────────────────
# Endpoints
# ─────────────────────────────────────────────────────────────────────────────

@app.get("/", tags=["Info"])
def root():
    return {
        "app":     "MyFinance OFX Scraper",
        "version": "1.0.0",
        "status":  "running",
        "docs":    f"http://{API_HOST}:{API_PORT}/docs",
    }


@app.get("/health", tags=["Info"])
def health():
    """Health check — Java chama este endpoint para saber se a API está pronta."""
    return {"status": "ok", "timestamp": datetime.now().isoformat()}


@app.post("/capture", response_model=CaptureResponseDTO, tags=["OFX"])
async def capture(req: CaptureRequestDTO):
    """
    Inicia o scraping bancário.

    - Recebe a URL do banco e os passos de navegação (ou null para login manual).
    - Credenciais podem ser embutidas nos navigation_steps como texto nos steps do
      tipo 'fill' — elas nunca são salvas em disco.
    - Retorna o caminho do arquivo .ofx gerado.
    """
    log.info(f"[POST /capture] banco={req.bank_url} | desc={req.description}")

    steps: Optional[list[NavigationStep]] = None
    if req.navigation_steps:
        steps = [
            NavigationStep(
                action=s.action,
                selector=s.selector,
                text=s.text,
                timeout=s.timeout,
            )
            for s in req.navigation_steps
        ]

    try:
        caminho: Path = await capturar_ofx(
            bank_url=req.bank_url,
            navigation_steps=steps,
            description=req.description or "",
        )
        return CaptureResponseDTO(
            success=True,
            message="OFX capturado com sucesso",
            file_path=str(caminho),
            file_name=caminho.name,
        )
    except Exception as ex:
        log.error(f"Erro durante captura OFX: {ex}", exc_info=True)
        raise HTTPException(status_code=500, detail=str(ex))


@app.get("/files", response_model=FilesResponseDTO, tags=["OFX"])
def list_files():
    """Lista os arquivos OFX presentes em uploads/ofx."""
    arquivos = [
        FileInfoDTO(
            filename=f.name,
            path=str(f),
            size_bytes=f.stat().st_size,
            created_at=f.stat().st_ctime,
        )
        for f in sorted(OFX_OUTPUT_DIR.glob("*.ofx"), key=lambda p: p.stat().st_ctime, reverse=True)
    ]
    return FilesResponseDTO(
        total=len(arquivos),
        output_directory=str(OFX_OUTPUT_DIR),
        files=arquivos,
    )


# ─────────────────────────────────────────────────────────────────────────────
# Entry-point
# ─────────────────────────────────────────────────────────────────────────────

if __name__ == "__main__":
    log.info(f"Iniciando MyFinance OFX Scraper em http://{API_HOST}:{API_PORT}")
    log.info(f"OFX output dir: {OFX_OUTPUT_DIR}")
    uvicorn.run(
        "main:app",
        host=API_HOST,
        port=API_PORT,
        log_level="warning",  # Uvicorn silencioso — logs próprios via logger.py
        reload=False,
    )

