import hashlib
import os
from functools import lru_cache
from pathlib import Path
import re


DEFAULT_PACKAGE_PATH = (
    Path(__file__).resolve().parents[2]
    / "data"
    / "ranah_siaga.db"
)


def configured_package_path() -> Path:
    configured = os.getenv("DATASET_PACKAGE_PATH")
    return Path(configured).expanduser().resolve() if configured else DEFAULT_PACKAGE_PATH


@lru_cache(maxsize=4)
def _sha256_for_file(path: str, size: int, modified_ns: int) -> str:
    del size, modified_ns
    digest = hashlib.sha256()
    with Path(path).open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def verify_package(path: Path, expected_size: int | None, expected_checksum: str) -> None:
    if not path.is_file():
        raise FileNotFoundError(path)

    stat = path.stat()
    if expected_size is not None and stat.st_size != expected_size:
        raise ValueError(
            f"Ukuran paket {stat.st_size} byte tidak sesuai metadata {expected_size} byte.",
        )

    actual_checksum = _sha256_for_file(str(path), stat.st_size, stat.st_mtime_ns)
    if actual_checksum.casefold() != expected_checksum.casefold():
        raise ValueError("Checksum paket dataset tidak sesuai metadata aktif.")


def remote_version_is_newer(remote: str, current: str) -> bool:
    if remote == current:
        return False
    numeric_pattern = re.compile(r"^\d+(?:\.\d+)*$")
    if numeric_pattern.fullmatch(remote) and numeric_pattern.fullmatch(current):
        remote_parts = tuple(int(part) for part in remote.split("."))
        current_parts = tuple(int(part) for part in current.split("."))
        width = max(len(remote_parts), len(current_parts))
        return remote_parts + (0,) * (width - len(remote_parts)) > (
            current_parts + (0,) * (width - len(current_parts))
        )
    return True
