package com.akusukaproject.siagapadang.ui.facilities

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akusukaproject.siagapadang.SiagaPadangApplication
import com.akusukaproject.siagapadang.data.model.Facility
import com.akusukaproject.siagapadang.data.model.FacilityKind
import com.akusukaproject.siagapadang.data.model.GeoCoordinate
import com.akusukaproject.siagapadang.data.model.TsunamiZoneOverlay
import com.akusukaproject.siagapadang.domain.NearestNodeFinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class FacilityItem(
    val facility: Facility,
    val distanceMeters: Int?,
)

enum class FacilitiesMode { LIST, MAP }

data class FacilitiesUiState(
    val items: List<FacilityItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val query: String = "",
    val kindFilter: FacilityKind? = null,
    val mode: FacilitiesMode = FacilitiesMode.LIST,
    val selectedId: String? = null,
    val userLocation: GeoCoordinate? = null,
    val tsunamiZoneOverlay: TsunamiZoneOverlay? = null,
    val zoneOverlayErrorMessage: String? = null,
    val meetingPointMessage: String? = null,
) {
    val visibleItems: List<FacilityItem>
        get() = items.filter { item ->
            (kindFilter == null || item.facility.kind == kindFilter) &&
                (query.isBlank() || item.facility.name.contains(query.trim(), ignoreCase = true))
        }

    val selected: FacilityItem?
        get() = items.firstOrNull { it.facility.id == selectedId }

    fun countOf(kind: FacilityKind?): Int = items.count { kind == null || it.facility.kind == kind }
}

class FacilitiesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SiagaPadangApplication
    private val mutableUiState = MutableStateFlow(FacilitiesUiState())
    val uiState: StateFlow<FacilitiesUiState> = mutableUiState.asStateFlow()

    init {
        loadTsunamiZoneOverlay()
        viewModelScope.launch {
            val facilities = runCatching { app.evacuationRepository.loadFacilities() }.getOrElse {
                mutableUiState.update {
                    it.copy(isLoading = false, errorMessage = "Daftar TES dan TEA tidak dapat dibaca dari data lokal.")
                }
                return@launch
            }
            val location = if (hasLocationPermission()) {
                runCatching { app.locationProvider.lastKnownLocation() }.getOrNull()?.coordinate
            } else {
                null
            }
            val items = withContext(Dispatchers.Default) {
                val withDistance = facilities.map { facility ->
                    FacilityItem(
                        facility = facility,
                        distanceMeters = location?.let {
                            NearestNodeFinder.distanceMeters(it, facility.coordinate).roundToInt()
                        },
                    )
                }
                if (location == null) withDistance.sortedBy { it.facility.name } else withDistance.sortedBy { it.distanceMeters }
            }
            mutableUiState.update { it.copy(items = items, isLoading = false, userLocation = location) }
        }
    }

    private fun loadTsunamiZoneOverlay() {
        viewModelScope.launch {
            runCatching { app.zoneRepository.loadMapOverlay() }
                .onSuccess { overlay ->
                    mutableUiState.update {
                        it.copy(tsunamiZoneOverlay = overlay, zoneOverlayErrorMessage = null)
                    }
                }
                .onFailure {
                    mutableUiState.update {
                        it.copy(zoneOverlayErrorMessage = "Batas zona belum dapat dibaca dari data lokal.")
                    }
                }
        }
    }

    fun setQuery(query: String) = mutableUiState.update { it.copy(query = query) }

    fun setKindFilter(kind: FacilityKind?) = mutableUiState.update { it.copy(kindFilter = kind) }

    fun setMode(mode: FacilitiesMode) = mutableUiState.update { it.copy(mode = mode) }

    fun select(facilityId: String?) = mutableUiState.update { it.copy(selectedId = facilityId, meetingPointMessage = null) }

    /** Menjadikan tempat terpilih sebagai titik temu keluarga (F-07), tersimpan di HP. */
    fun setAsFamilyMeetingPoint(item: FacilityItem) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { app.familyPlanRepository.setMeetingPoint(item.facility.name) }
            }
            mutableUiState.update {
                it.copy(
                    meetingPointMessage = if (result.isSuccess) {
                        "${item.facility.name} menjadi titik temu keluarga."
                    } else {
                        "Titik temu gagal disimpan."
                    },
                )
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        app.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
