import json
import logging
import time
import asyncio
import httpx
import math
from datetime import datetime, timezone
from typing import Optional, TypedDict

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.domain import EmergencyEvent, EventStatus
from app.schemas.bmkg import BMKGStatusResponse, BMKGRegionalEvent
from app.services.firebase import send_tsunami_warning_push

router = APIRouter()
logger = logging.getLogger(__name__)


class BMKGCache(TypedDict):
    data: Optional[BMKGStatusResponse]
    last_fetched_utama: float
    last_fetched_regional: float


# Cache lima menit mengurangi beban ke layanan publik BMKG.
bmkg_cache: BMKGCache = {
    "data": None,
    "last_fetched_utama": 0.0,
    "last_fetched_regional": 0.0,
}

BMKG_URL_UTAMA = "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
BMKG_URL_REGIONAL = "https://data.bmkg.go.id/DataMKG/TEWS/gempaterkini.json"
BMKG_ATTRIBUTION = "BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)"
PADANG_LAT = -0.9471
PADANG_LON = 100.4172
RADIUS_KM = 1500.0


def calculate_distance(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6371.0
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat / 2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon / 2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c


async def fetch_bmkg_data(client: httpx.AsyncClient, url: str) -> Optional[dict]:
    try:
        response = await client.get(url, timeout=2.0)
        response.raise_for_status()
        return response.json()
    except Exception as exc:
        logger.warning("Gagal mengambil %s: %s", url, exc)
        return None


@router.get("", response_model=BMKGStatusResponse)
async def get_bmkg_status(db: Session = Depends(get_db)):
    current_time = time.time()

    cached = bmkg_cache["data"]
    need_utama = cached is None or (current_time - bmkg_cache["last_fetched_utama"] >= 300)
    need_regional = cached is None or (current_time - bmkg_cache["last_fetched_regional"] >= 300)

    if not need_utama and not need_regional:
        return cached

    async with httpx.AsyncClient(headers={"User-Agent": "SiagaPadang/0.1 (+https://github.com/AkuSukaProject/siagapadang)"}) as client:
        tasks = []
        if need_utama:
            tasks.append(fetch_bmkg_data(client, BMKG_URL_UTAMA))
        else:
            tasks.append(asyncio.sleep(0, result=None))

        if need_regional:
            tasks.append(fetch_bmkg_data(client, BMKG_URL_REGIONAL))
        else:
            tasks.append(asyncio.sleep(0, result=None))

        results = await asyncio.gather(*tasks)
        utama_data = results[0] if need_utama else None
        regional_data = results[1] if need_regional else None

    if need_utama and utama_data is None:
        if cached:
            result = cached.model_copy()
            result.data_status = "stale"
            if need_regional:
                if regional_data:
                    pass # handled below
                else:
                    result.regional_data_status = "failed"
                    result.regional_event = None
            return result
        else:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Status BMKG sedang tidak tersedia. Coba lagi beberapa saat.",
            )

    result = None
    if utama_data and "Infogempa" in utama_data:
        gempa = utama_data.get("Infogempa", {}).get("gempa", {})
        potensi_text = str(gempa.get("Potensi") or "").lower()
        is_tsunami = "tsunami" in potensi_text and "tidak berpotensi" not in potensi_text
        result = BMKGStatusResponse(
            tanggal=str(gempa.get("Tanggal") or ""),
            jam=str(gempa.get("Jam") or ""),
            datetime=str(gempa.get("DateTime") or ""),
            coordinates=str(gempa.get("Coordinates") or ""),
            lintang=str(gempa.get("Lintang") or ""),
            bujur=str(gempa.get("Bujur") or ""),
            magnitude=str(gempa.get("Magnitude") or ""),
            kedalaman=str(gempa.get("Kedalaman") or ""),
            wilayah=str(gempa.get("Wilayah") or ""),
            potensi=str(gempa.get("Potensi") or ""),
            dirasakan=str(gempa.get("Dirasakan") or ""),
            shakemap=str(gempa.get("Shakemap") or ""),
            is_tsunami_potential=is_tsunami,
            data_status="live",
            fetched_at=datetime.now(timezone.utc),
            source=BMKG_ATTRIBUTION,
            regional_event=None,
            regional_data_status="live"
        )
        bmkg_cache["last_fetched_utama"] = current_time

        # Kirim notifikasi jika potensi tsunami
        if is_tsunami:
            event_id = str(gempa.get("DateTime") or "")
            # Cek apakah event ini sudah dikirim / sudah ada di database
            existing_event = db.query(EmergencyEvent).filter(EmergencyEvent.external_event_id == event_id).first()
            if not existing_event:
                # Daftarkan ke database untuk mencegah notifikasi ganda
                new_event = EmergencyEvent(
                    external_event_id=event_id,
                    source="BMKG",
                    status=EventStatus.ACTIVE,
                    is_simulation=False
                )
                db.add(new_event)
                db.commit()

                # Kirim push notification
                title = "Peringatan potensi tsunami dari BMKG"
                body = f"{gempa.get('Magnitude', '')} · {gempa.get('Wilayah', '')}. Ketuk untuk membuka arah ke TES/TEA terdekat."
                send_tsunami_warning_push(event_id, title, body)

    else:
        if cached:
            result = cached.model_copy()
            result.data_status = "stale"
        else:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Status BMKG sedang tidak tersedia (data kosong). Coba lagi beberapa saat.",
            )

    if need_regional:
        if regional_data:
            gempas = regional_data.get("Infogempa", {}).get("gempa", [])
            if isinstance(gempas, dict):
                gempas = [gempas]
            
            regional_event = None
            min_distance = RADIUS_KM + 1.0  # Mulai dengan batas atas
            for g in gempas:
                coords = str(g.get("Coordinates") or "").split(",")
                if len(coords) == 2:
                    try:
                        lat = float(coords[0])
                        lon = float(coords[1])
                        distance = calculate_distance(PADANG_LAT, PADANG_LON, lat, lon)
                        if distance <= RADIUS_KM and distance < min_distance:
                            min_distance = distance
                            regional_event = BMKGRegionalEvent(
                                tanggal=str(g.get("Tanggal") or ""),
                                jam=str(g.get("Jam") or ""),
                                datetime=str(g.get("DateTime") or ""),
                                coordinates=str(g.get("Coordinates") or ""),
                                lintang=str(g.get("Lintang") or ""),
                                bujur=str(g.get("Bujur") or ""),
                                magnitude=str(g.get("Magnitude") or ""),
                                kedalaman=str(g.get("Kedalaman") or ""),
                                wilayah=str(g.get("Wilayah") or ""),
                                potensi=str(g.get("Potensi") or ""),
                                distance_km_from_padang=round(distance, 2)
                            )
                    except ValueError:
                        continue
            
            result.regional_event = regional_event
            result.regional_data_status = "live"
            bmkg_cache["last_fetched_regional"] = current_time
        else:
            result.regional_data_status = "failed"
            result.regional_event = None
    else:
        if cached:
            result.regional_event = cached.regional_event
            result.regional_data_status = "stale"

    bmkg_cache["data"] = result
    return result
