"""
automation.py — Lógica de automação com Playwright.

Fluxos suportados:
  1. Automático  → navigation_steps preenchidos  (ex: fill, click, wait…)
  2. Manual      → navigation_steps = None       (browser abre, usuário faz login)

Em ambos os casos o download do .ofx é detectado automaticamente via
evento de download do Playwright e salvo em OFX_OUTPUT_DIR.
"""

import asyncio
from datetime import datetime
from pathlib import Path
from typing import Optional

from playwright.async_api import async_playwright, Download, Page

from config import (
    BROWSER_TYPE, HEADLESS, BROWSER_TIMEOUT,
    LOGIN_TIMEOUT, DOWNLOAD_TIMEOUT, OFX_OUTPUT_DIR,
)
from logger import get_logger

log = get_logger("automation")


# ─────────────────────────────────────────────────────────────────────────────
# Modelos de steps
# ─────────────────────────────────────────────────────────────────────────────

class NavigationStep:
    """Representa um passo de navegação enviado pelo Java."""

    def __init__(self, action: str, selector: str | None = None,
                 text: str | None = None, timeout: int | None = None):
        self.action   = action
        self.selector = selector
        self.text     = text
        self.timeout  = timeout  # em ms para wait_for_selector, em s para wait


# ─────────────────────────────────────────────────────────────────────────────
# Runner principal
# ─────────────────────────────────────────────────────────────────────────────

async def capturar_ofx(
    bank_url: str,
    navigation_steps: Optional[list[NavigationStep]],
    description: str = "",
) -> Path:
    """
    Abre o banco no browser, executa os navigation_steps (ou aguarda login manual)
    e retorna o caminho do arquivo .ofx baixado.

    Levanta RuntimeError se nenhum download ocorrer dentro do timeout.
    """
    log.info(f"Iniciando captura OFX — {description or bank_url}")

    async with async_playwright() as pw:
        browser_launcher = getattr(pw, BROWSER_TYPE)
        browser = await browser_launcher.launch(
            headless=HEADLESS,
            args=["--disable-blink-features=AutomationControlled"],
        )

        context = await browser.new_context(
            accept_downloads=True,
            viewport={"width": 1280, "height": 800},
        )
        context.set_default_timeout(BROWSER_TIMEOUT)

        page = await context.new_page()

        # Captura downloads em background (pode ocorrer em qualquer momento)
        download_future: asyncio.Future[Download] = asyncio.get_event_loop().create_future()

        async def on_download(download: Download):
            if not download_future.done():
                download_future.set_result(download)

        page.on("download", on_download)

        log.info(f"Navegando para: {bank_url}")
        await page.goto(bank_url, wait_until="domcontentloaded")

        # ── Fluxo automático ──────────────────────────────────────────────────
        if navigation_steps:
            log.info(f"Executando {len(navigation_steps)} steps automáticos")
            await _executar_steps(page, navigation_steps)
        # ── Fluxo manual (login feito pelo usuário) ───────────────────────────
        else:
            log.info(
                f"Modo manual: aguardando login do usuário "
                f"(timeout: {LOGIN_TIMEOUT}s)"
            )
            await _aguardar_login_manual(page)

        # ── Aguarda o download ────────────────────────────────────────────────
        log.info(f"Aguardando download OFX (timeout: {DOWNLOAD_TIMEOUT}s)…")
        try:
            download: Download = await asyncio.wait_for(
                download_future, timeout=DOWNLOAD_TIMEOUT
            )
        except asyncio.TimeoutError:
            await browser.close()
            raise RuntimeError(
                f"Nenhum arquivo OFX foi baixado em {DOWNLOAD_TIMEOUT} segundos. "
                "Verifique se a página oferece um botão/link de download de OFX."
            )

        # ── Salva o arquivo ───────────────────────────────────────────────────
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        nome_original = Path(download.suggested_filename or "arquivo.ofx").stem
        nome_arquivo  = f"ofx_{timestamp}_{nome_original}.ofx"
        caminho       = OFX_OUTPUT_DIR / nome_arquivo

        await download.save_as(str(caminho))
        log.info(f"OFX salvo em: {caminho}")

        await browser.close()
        return caminho


# ─────────────────────────────────────────────────────────────────────────────
# Execução dos steps
# ─────────────────────────────────────────────────────────────────────────────

async def _executar_steps(page: Page, steps: list[NavigationStep]):
    for i, step in enumerate(steps):
        log.debug(f"Step {i + 1}/{len(steps)}: {step.action} → {step.selector}")

        match step.action:
            case "click":
                await page.locator(step.selector).click()

            case "fill":
                await page.locator(step.selector).fill(step.text or "")

            case "wait_for_selector":
                timeout_ms = step.timeout or 30_000
                await page.wait_for_selector(step.selector, timeout=timeout_ms)

            case "wait":
                seconds = step.timeout or 2
                await asyncio.sleep(seconds)

            case "navigate":
                await page.goto(step.selector, wait_until="domcontentloaded")

            case "download":
                # Espera pelo seletor e clica — o download é capturado pelo listener
                if step.selector:
                    await page.wait_for_selector(step.selector, timeout=step.timeout or 30_000)
                    await page.locator(step.selector).click()

            case _:
                log.warning(f"Step desconhecido: {step.action!r} — ignorado")


# ─────────────────────────────────────────────────────────────────────────────
# Login manual
# ─────────────────────────────────────────────────────────────────────────────

async def _aguardar_login_manual(page: Page):
    """
    Aguarda até LOGIN_TIMEOUT segundos detectando que a URL mudou
    (indicando que o usuário fez login e foi redirecionado).
    """
    url_inicial = page.url
    inicio = asyncio.get_event_loop().time()

    while (asyncio.get_event_loop().time() - inicio) < LOGIN_TIMEOUT:
        await asyncio.sleep(2)
        if page.url != url_inicial:
            log.info(f"Login detectado — nova URL: {page.url}")
            return

    log.warning("Timeout aguardando login manual — continuando mesmo assim")

