from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.database import get_db
from app.models.domain import DataVersion, EvacuationPoint, InundationZone, SafeZone
from app.schemas.sync import SyncCheckResponse, SyncDataResponse, DataVersionBase
from app.services.dataset_packages import (
    configured_package_path,
    remote_version_is_newer,
    verify_package,
)
import json
from urllib.parse import quote
from typing import List, Optional

router = APIRouter()

@router.get("/check", response_model=SyncCheckResponse)
def check_updates(
    dataset_name: Optional[str] = Query(default=None),
    current_version: Optional[str] = Query(default=None),
    current_checksum: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    """
    Endpoint untuk mendapatkan metadata versi terbaru dari semua dataset yang aktif.
    Android akan membandingkan daftar ini dengan versi lokal SQLite-nya.
    """
    # Ambil versi terbaru per dataset_name
    subquery = db.query(
        DataVersion.dataset_name, 
        func.max(DataVersion.id).label('max_id')
    ).filter(DataVersion.is_active == True).group_by(DataVersion.dataset_name).subquery()
    
    query = db.query(DataVersion).join(
        subquery, 
        (DataVersion.dataset_name == subquery.c.dataset_name) & (DataVersion.id == subquery.c.max_id)
    )
    if dataset_name:
        query = query.filter(DataVersion.dataset_name == dataset_name)
    latest_versions = query.all()

    versions = []
    for version in latest_versions:
        item = DataVersionBase.model_validate(version)
        if version.dataset_name == "network":
            item = item.model_copy(
                update={
                    "download_url": (
                        f"/api/v1/sync/network?version={quote(version.version, safe='')}"
                    ),
                },
            )
        versions.append(item)

    if current_checksum:
        # Resolve current_version dari database untuk perbandingan yang lebih kuat
        current_db_version = None
        if current_version:
            current_db_version = db.query(DataVersion).filter_by(version=current_version).first()
            
        has_update = False
        for version in latest_versions:
            if not version.version or not version.checksum:
                continue
            if version.checksum.casefold() != current_checksum.casefold():
                if current_db_version and version.id > current_db_version.id:
                    has_update = True
                elif not current_db_version and (current_version is None or remote_version_is_newer(version.version, current_version)):
                    has_update = True
    elif current_version:
        current_db_version = db.query(DataVersion).filter_by(version=current_version).first()
        has_update = False
        for version in latest_versions:
            if not version.version:
                continue
            if current_db_version and version.id > current_db_version.id:
                has_update = True
            elif not current_db_version and remote_version_is_newer(version.version, current_version):
                has_update = True
    else:
        has_update = bool(latest_versions)
    
    return SyncCheckResponse(
        has_update=has_update,
        latest_versions=versions,
        message="Daftar versi dataset terbaru berhasil diambil."
    )

@router.get("/shelters", response_model=SyncDataResponse)
def get_sync_shelters(db: Session = Depends(get_db)):
    """
    Mengunduh seluruh data shelter beserta metadata (elevasi, kapasitas, akses masuk) dalam format GeoJSON standar.
    """
    latest_shelter = db.query(DataVersion).filter_by(dataset_name="shelters", is_active=True).order_by(DataVersion.id.desc()).first()
    if not latest_shelter:
        raise HTTPException(status_code=404, detail="No shelter data version available.")
        
    shelters = db.query(EvacuationPoint).all()
    
    features = []
    for s in shelters:
        # Konversi PostGIS ke GeoJSON string secara aman jika koordinat ada
        geom = None
        if s.location is not None:
            geom_json_str = db.scalar(func.ST_AsGeoJSON(s.location))
            geom = json.loads(geom_json_str) if geom_json_str else None
            
        entrance_geom = None
        if s.entrance_coord is not None:
            entrance_json_str = db.scalar(func.ST_AsGeoJSON(s.entrance_coord))
            entrance_geom = json.loads(entrance_json_str) if entrance_json_str else None
            
        features.append({
            "type": "Feature",
            "geometry": geom,
            "properties": {
                "id": s.id,
                "external_id": s.external_id,
                "name": s.name,
                "capacity": s.capacity,
                "floors": s.floors,
                "elevation_m": s.elevation_m,
                "type": s.type.value if hasattr(s.type, 'value') else str(s.type or ""),
                "structural_condition": s.structural_condition.value if hasattr(s.structural_condition, 'value') else str(s.structural_condition or ""),
                "operational_status": s.operational_status.value if hasattr(s.operational_status, 'value') else str(s.operational_status or ""),
                "address": s.address,
                "source": s.source,
                "entrance_geometry": entrance_geom
            }
        })
        
    feature_collection = {
        "type": "FeatureCollection",
        "features": features
    }
    
    return SyncDataResponse(
        dataset_name="shelters",
        version=latest_shelter.version,
        checksum=latest_shelter.checksum,
        data=feature_collection
    )
    
@router.get("/network", response_class=FileResponse)
def get_sync_network(
    version: Optional[str] = Query(default=None),
    db: Session = Depends(get_db),
):
    """
    Mengunduh paket SQLite lengkap yang sudah diverifikasi terhadap metadata aktif.
    """
    active = db.query(DataVersion).filter_by(
        dataset_name="network",
        is_active=True,
    ).order_by(DataVersion.id.desc()).first()
    if active is None:
        raise HTTPException(status_code=404, detail="Versi dataset jaringan belum tersedia.")
    if version is not None and version != active.version:
        raise HTTPException(status_code=409, detail="Versi dataset yang diminta sudah tidak aktif.")

    package_path = configured_package_path()
    try:
        verify_package(package_path, active.size_bytes, active.checksum)
    except FileNotFoundError as error:
        raise HTTPException(status_code=503, detail="Paket dataset belum tersedia di server.") from error
    except ValueError as error:
        raise HTTPException(status_code=503, detail=str(error)) from error

    if not active.version or not active.checksum:
        raise HTTPException(status_code=500, detail="Data versi atau checksum tidak valid di server.")

    safe_version = "".join(
        character if character.isalnum() or character in ".-_" else "_"
        for character in active.version
    )
    return FileResponse(
        path=package_path,
        media_type="application/vnd.sqlite3",
        filename=f"siaga-padang-{safe_version}.db",
        headers={
            "ETag": f'"sha256-{active.checksum}"',
            "X-Dataset-Version": active.version,
            "X-Checksum-SHA256": active.checksum,
            "Cache-Control": "public, max-age=3600",
        },
    )


