from pydantic import BaseModel, Field
from typing import Optional

class ObstructionReportCreate(BaseModel):
    latitude: float = Field(..., description="Garis lintang halangan (Y)", ge=-90, le=90)
    longitude: float = Field(..., description="Garis bujur halangan (X)", ge=-180, le=180)
    dataset_version_id: int = Field(..., description="ID versi dataset graf rute yang digunakan Android saat ini")
    edge_external_id: str = Field(..., description="External ID ruas jalan")
    description: Optional[str] = Field(None, description="Opsional deskripsi halangan")

class ObstructionReportResponse(BaseModel):
    status: str
    message: str
    obstruction_id: int
    is_confirmed_blocked: bool
