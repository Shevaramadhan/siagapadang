import asyncio
import os
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker
from sqlalchemy import select, delete, update
from dotenv import load_dotenv

# Load env
load_dotenv(os.path.join(os.path.dirname(__file__), ".env"))

DATABASE_URL = os.getenv("DATABASE_URL")
if not DATABASE_URL:
    print("NO DATABASE_URL FOUND")
    exit(1)

from app.models.domain import (
    EmergencyEvent, EventStatus, DataVersion, 
    Checkin, ShelterOccupancyReport, EvacuationPoint, Base
)

async def run():
    engine = create_async_engine(DATABASE_URL, echo=True)
    async_session = sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)
    
    async with async_session() as session:
        # 1. Close event EVENT-PADANG-TEST-01
        print("Closing EVENT-PADANG-TEST-01...")
        result = await session.execute(
            select(EmergencyEvent).filter_by(external_event_id="EVENT-PADANG-TEST-01", status=EventStatus.ACTIVE)
        )
        event = result.scalar_one_or_none()
        if event:
            event.status = EventStatus.CLOSED
            session.add(event)
            print("Closed event!")
        else:
            print("Event not found or already closed.")

        # 2. Delete test data for e2e-uji-claude-1
        print("Deleting checkins for e2e-uji-claude-1...")
        await session.execute(
            delete(Checkin).where(Checkin.device_hash == "e2e-uji-claude-1")
        )
        await session.execute(
            delete(ShelterOccupancyReport).where(ShelterOccupancyReport.device_hash == "e2e-uji-claude-1")
        )

        # 3. Find TES_56 and delete its test data
        print("Deleting test data for TES_56...")
        tes56_result = await session.execute(
            select(EvacuationPoint).filter_by(external_id="TES_56")
        )
        tes56 = tes56_result.scalar_one_or_none()
        if tes56:
            await session.execute(
                delete(Checkin).where(Checkin.evacuation_point_id == tes56.id)
            )
            await session.execute(
                delete(ShelterOccupancyReport).where(ShelterOccupancyReport.evacuation_point_id == tes56.id)
            )
            print(f"Deleted checkins/occupancy for TES_56 (id={tes56.id})")
        else:
            print("TES_56 not found.")

        # 4. Fix dataset 'shelters' version 2026.08.01
        print("Updating DataVersion shelters...")
        dv_result = await session.execute(
            select(DataVersion).filter_by(dataset_name="shelters", version="2026.08.01")
        )
        dv = dv_result.scalar_one_or_none()
        if dv:
            dv.checksum = "ecd93df142533b2625d329df7128f9e7057c5ffd38e76de02fd9d4745fd74786"
            dv.size_bytes = 76414976
            # download_url is set by logic or db? Let's fix download_url just in case
            dv.download_url = "/api/v1/sync/network?version=2026.08.01" # Or whatever is correct. Wait, android needs it to not be null?
            # User said: "Itu SHA-256 dari berkas kosong, dan download_url-nya null. Jadi aplikasi diberi tahu ada pembaruan menuju sesuatu yang tidak ada isinya."
            # Actually, `download_url` might be returned by `sync.py` line 47:
            # if version.dataset_name == "network":
            #     item.download_url = "/api/v1/sync/network?version..."
            # Wait, what is the endpoint for shelters DB download?
            # It's `/api/v1/sync/ota/download` as per my documentation!
            dv.download_url = "/api/v1/sync/ota/download"
            session.add(dv)
            print("Updated shelters DataVersion.")
        else:
            print("DataVersion shelters 2026.08.01 not found.")

        await session.commit()
        print("Done!")

if __name__ == "__main__":
    import sys
    if sys.platform == 'win32':
        import selectors
        asyncio.set_event_loop_policy(
            asyncio.WindowsSelectorEventLoopPolicy()
        )
    asyncio.run(run())
