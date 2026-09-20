from pydantic import BaseModel, Field
from datetime import datetime as DateTime
from typing import Literal, Optional

class BMKGRegionalEvent(BaseModel):
    tanggal: str = ""
    jam: str = ""
    datetime: str = ""
    coordinates: str = ""
    lintang: str = ""
    bujur: str = ""
    magnitude: str = ""
    kedalaman: str = ""
    wilayah: str = ""
    potensi: str = ""
    distance_km_from_padang: float = Field(default=0.0)

class BMKGStatusResponse(BaseModel):
    tanggal: str = ""
    jam: str = ""
    datetime: str = ""
    coordinates: str = ""
    lintang: str = ""
    bujur: str = ""
    magnitude: str = ""
    kedalaman: str = ""
    wilayah: str = ""
    potensi: str = ""
    dirasakan: Optional[str] = ""
    shakemap: Optional[str] = ""
    is_tsunami_potential: bool = False
    data_status: Literal["live", "stale"] = Field(
        default="live",
        description="Status kesegaran data utama: live atau stale",
    )
    regional_event: Optional[BMKGRegionalEvent] = Field(
        default=None,
        description="Data gempa regional khusus area Sumatera Barat",
    )
    regional_data_status: Literal["live", "stale", "failed"] = Field(
        default="live",
        description="Status kesegaran data regional",
    )
    fetched_at: DateTime = Field(
        description="Waktu pengambilan data dari BMKG dalam format ISO 8601 UTC",
    )
    source: str = Field(
        default="BMKG (Badan Meteorologi, Klimatologi, dan Geofisika)",
        description="Atribusi sumber data resmi sesuai ketentuan lisensi BMKG"
    )
