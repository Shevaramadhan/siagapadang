import os
import json
import logging
from firebase_admin import credentials, initialize_app, messaging

logger = logging.getLogger(__name__)

# Initialize Firebase App
firebase_initialized = False
try:
    firebase_service_account_json = os.getenv("FIREBASE_SERVICE_ACCOUNT")
    if firebase_service_account_json:
        # Parse JSON string from environment variable
        cert_dict = json.loads(firebase_service_account_json)
        cred = credentials.Certificate(cert_dict)
        initialize_app(cred)
        firebase_initialized = True
        logger.info("Firebase Admin diinisialisasi menggunakan FIREBASE_SERVICE_ACCOUNT env var.")
    else:
        logger.warning("FIREBASE_SERVICE_ACCOUNT tidak ditemukan, notifikasi push dinonaktifkan.")
except Exception as e:
    logger.error(f"Gagal menginisialisasi Firebase Admin: {e}")


def send_tsunami_warning_push(event_id: str, title: str, body: str):
    """
    Mengirimkan push notification melalui Firebase Cloud Messaging.
    """
    if not firebase_initialized:
        logger.warning(f"Push notifikasi gagal dikirim untuk event {event_id}: Firebase belum diinisialisasi.")
        return

    try:
        msg = messaging.Message(
            topic="padang",
            data={
                "type": "tsunami_warning",
                "event_id": event_id,
                "title": title,
                "body": body,
            },
            android=messaging.AndroidConfig(priority="high", ttl=1800),
        )
        response = messaging.send(msg)
        logger.info(f"Push notification berhasil dikirim untuk event {event_id}: {response}")
    except Exception as e:
        logger.error(f"Gagal mengirim push notification: {e}")
