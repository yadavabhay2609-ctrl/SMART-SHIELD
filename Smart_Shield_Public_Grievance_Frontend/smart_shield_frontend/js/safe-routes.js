/**
 * Smart Shield - Safe Routes & Safety Scoring Controller (Module 4)
 * Handles route calculation, HTML5 corridor map visualization,
 * safety score breakdown, and citizen community route reviews.
 */

document.addEventListener('DOMContentLoaded', () => {
  // 1. Session & Auth Check
  const token = getToken();
  const user = getUser();

  if (user) {
    const userNameEl = document.getElementById('userName');
    const userRoleEl = document.getElementById('userRole');
    const userAvatarEl = document.getElementById('userAvatar');
    if (userNameEl) userNameEl.textContent = user.fullName || 'Citizen';
    if (userRoleEl) userRoleEl.textContent = user.role || 'Citizen';
    if (userAvatarEl && user.fullName) {
      const parts = user.fullName.trim().split(/\s+/);
      const initials = parts.length > 1
        ? (parts[0][0] + parts[1][0]).toUpperCase()
        : parts[0].substring(0, 2).toUpperCase();
      userAvatarEl.textContent = initials;
    }
  }

  // Auth/Logout handler
  const authBtn = document.getElementById('authBtn');
  if (authBtn) {
    if (token) {
      authBtn.innerHTML = '↪ &nbsp; Logout';
      authBtn.addEventListener('click', () => {
        clearAuth();
        window.location.replace('../../index.html');
      });
    } else {
      authBtn.innerHTML = '➔ &nbsp; Sign In';
      authBtn.addEventListener('click', () => {
        window.location.href = 'grievance.html#login';
      });
    }
  }

  // Mobile sidebar toggle
  const menuBtn = document.getElementById('menuBtn');
  const sidebar = document.getElementById('sidebar');
  if (menuBtn && sidebar) {
    menuBtn.addEventListener('click', () => {
      sidebar.classList.toggle('open');
    });
  }

  // State
  let currentTravelMode = 'WALK';
  let currentRouteId = null;
  let currentRouteData = null;
  let detectedCity = null;
  let detectedState = null;
  let destSearchDebounceTimer = null;
  let leafletMapInstance = null;
  let leafletRoutePolyline = null;
  let leafletRouteCasing = null;
  let leafletOriginMarker = null;
  let leafletDestMarker = null;
  let leafletFacilityMarkers = [];
  let isMapTilesLoaded = false;

  // DOM Elements
  const configNotice = document.getElementById('configNotice');
  const configNoticeText = document.getElementById('configNoticeText');
  const routeForm = document.getElementById('routeForm');
  const originInput = document.getElementById('originInput');
  const destInput = document.getElementById('destInput');
  const originLatInput = document.getElementById('originLat');
  const originLngInput = document.getElementById('originLng');
  const destLatInput = document.getElementById('destLat');
  const destLngInput = document.getElementById('destLng');
  const useGpsBtn = document.getElementById('useGpsBtn');
  const originLocationBanner = document.getElementById('originLocationBanner');
  const originLocationText = document.getElementById('originLocationText');
  const destSuggestionsDropdown = document.getElementById('destSuggestionsDropdown');
  const quickDestSection = document.getElementById('quickDestSection');
  const quickDestLabel = document.getElementById('quickDestLabel');
  const quickCityTag = document.getElementById('quickCityTag');
  const quickDestChips = document.getElementById('quickDestChips');

  const modeWalkBtn = document.getElementById('modeWalk');
  const modeDriveBtn = document.getElementById('modeDrive');
  const calculateBtn = document.getElementById('calculateBtn');
  const calcBtnText = document.getElementById('calcBtnText');
  const calcBtnSpinner = document.getElementById('calcBtnSpinner');

  const routeResultsSection = document.getElementById('routeResultsSection');
  const routeIdBadge = document.getElementById('routeIdBadge');
  const safetyScoreNum = document.getElementById('safetyScoreNum');
  const scoreCircle = document.getElementById('scoreCircle');
  const safetyLevelTag = document.getElementById('safetyLevelTag');
  const safetyLevelDesc = document.getElementById('safetyLevelDesc');
  const dataConfidenceBadge = document.getElementById('dataConfidenceBadge');
  const metricDistance = document.getElementById('metricDistance');
  const metricDuration = document.getElementById('metricDuration');
  const metricRating = document.getElementById('metricRating');
  const metricReviewsCount = document.getElementById('metricReviewsCount');
  const warningsCard = document.getElementById('warningsCard');
  const warningsList = document.getElementById('warningsList');
  const factorsContainer = document.getElementById('factorsContainer');
  const facilitiesSection = document.getElementById('facilitiesSection');
  const facilitiesList = document.getElementById('facilitiesList');

  // Inspection Card Elements
  const inspectStartName = document.getElementById('inspectStartName');
  const inspectStartSub = document.getElementById('inspectStartSub');
  const inspectEndName = document.getElementById('inspectEndName');
  const inspectEndSub = document.getElementById('inspectEndSub');
  const inspectModeIcon = document.getElementById('inspectModeIcon');
  const inspectDistanceText = document.getElementById('inspectDistanceText');
  const inspectDurationText = document.getElementById('inspectDurationText');
  const routeEngineBadge = document.getElementById('routeEngineBadge');
  const mapStatusPill = document.getElementById('mapStatusPill');
  const mapFallbackBanner = document.getElementById('mapFallbackBanner');
  const fallbackTitle = document.getElementById('fallbackTitle');
  const fallbackMsg = document.getElementById('fallbackMsg');
  const mapRetryBtn = document.getElementById('mapRetryBtn');

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[m]));
  }

  // Rating Form Elements
  const ratingForm = document.getElementById('ratingForm');
  const starRating = document.getElementById('starRating');
  const selectedStarRatingInput = document.getElementById('selectedStarRating');
  const starText = document.getElementById('starText');
  const selectedSafetyTagInput = document.getElementById('selectedSafetyTag');
  const routeComment = document.getElementById('routeComment');
  const charCount = document.getElementById('charCount');
  const reviewsList = document.getElementById('reviewsList');

  // Helper: Category Icon Map
  function getCategoryIcon(cat) {
    const map = {
      HOSPITAL: '🏥',
      POLICE: '🚓',
      METRO: '🚇',
      TRANSIT: '🚆',
      GOVT: '🏛',
      EDUCATION: '🎓',
      COMMERCIAL: '🏢'
    };
    return map[cat] || '📍';
  }

  // 2. Load Configuration Status
  loadConfigStatus();
  // Pre-load verified destination suggestions for default / initial city
  loadDestinationSuggestions(null);

  // Polyline decoding utility for Google encoded polyline algorithm
  function decodePolyline(encoded) {
    if (!encoded) return [];
    const points = [];
    let index = 0, len = encoded.length;
    let lat = 0, lng = 0;
    while (index < len) {
      let b, shift = 0, result = 0;
      do {
        b = encoded.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      const dlat = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lat += dlat;
      shift = 0;
      result = 0;
      do {
        b = encoded.charCodeAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      const dlng = ((result & 1) ? ~(result >> 1) : (result >> 1));
      lng += dlng;
      points.push({ lat: lat / 1e5, lng: lng / 1e5 });
    }
    return points;
  }

  function showMapFallbackBanner(title, msg) {
    if (mapFallbackBanner) {
      if (fallbackTitle) fallbackTitle.textContent = title || 'Routing Service Notice';
      if (fallbackMsg) fallbackMsg.textContent = msg || 'Free routing service temporarily unavailable. Please retry.';
      mapFallbackBanner.style.display = 'block';
    }
  }

  function hideMapFallbackBanner() {
    if (mapFallbackBanner) {
      mapFallbackBanner.style.display = 'none';
    }
  }

  if (mapRetryBtn) {
    mapRetryBtn.addEventListener('click', () => {
      hideMapFallbackBanner();
      if (routeForm) {
        routeForm.dispatchEvent(new Event('submit'));
      }
    });
  }

  function loadConfigStatus() {
    SmartShieldAPI.getSafeRouteConfig()
      .then(cfg => {
        if (configNotice && configNoticeText) {
          configNotice.style.display = 'flex';
          configNotice.style.background = '#f0fdf4';
          configNotice.style.borderColor = '#bbf7d0';
          configNotice.style.color = '#166534';
          configNoticeText.textContent = 'Free OpenStreetMap tiles & OSRM real-road routing active. Google Maps dependency safely removed.';
        }
        if (mapStatusPill) {
          mapStatusPill.textContent = 'OpenStreetMap — Live Streets';
          mapStatusPill.className = 'map-status-pill live';
        }
      })
      .catch(err => {
        console.warn('Config check note:', err);
        if (configNotice && configNoticeText) {
          configNotice.style.display = 'flex';
          configNotice.style.background = '#f0fdf4';
          configNotice.style.borderColor = '#bbf7d0';
          configNotice.style.color = '#166534';
          configNoticeText.textContent = 'OpenStreetMap & OSRM real-road routing active.';
        }
        if (mapStatusPill) {
          mapStatusPill.textContent = 'OpenStreetMap — Live Streets';
          mapStatusPill.className = 'map-status-pill live';
        }
      });
  }

  // 2. Leaflet Map Initialization with OpenStreetMap Tiles
  function initLeafletMap() {
    const mapEl = document.getElementById('leafletMap');
    if (!mapEl || typeof L === 'undefined') return;

    if (!leafletMapInstance) {
      const initialLat = parseFloat(originLatInput?.value) || 28.6692;
      const initialLng = parseFloat(originLngInput?.value) || 77.4538;

      leafletMapInstance = L.map('leafletMap', {
        center: [initialLat, initialLng],
        zoom: 13,
        zoomControl: true,
        scrollWheelZoom: true
      });

      // Standard OpenStreetMap street-map tiles with required attribution
      const osmTileLayer = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener noreferrer">OpenStreetMap</a> contributors'
      });

      osmTileLayer.on('tileload', () => {
        isMapTilesLoaded = true;
        hideMapFallbackBanner();
        if (mapStatusPill) {
          mapStatusPill.textContent = 'OpenStreetMap — Live Streets';
          mapStatusPill.className = 'map-status-pill live';
        }
      });

      osmTileLayer.on('tileerror', (e) => {
        console.warn('OpenStreetMap tile load notice:', e);
        if (!isMapTilesLoaded) {
          showMapFallbackBanner(
            'Map Tile Network Notice',
            'OpenStreetMap street tiles could not be fetched from the network. Check your connection or retry.'
          );
        }
      });

      osmTileLayer.addTo(leafletMapInstance);
    }

    setTimeout(() => {
      if (leafletMapInstance) {
        leafletMapInstance.invalidateSize();
      }
    }, 150);
  }

  // Feature 1: Map Fullscreen / Expansion Controls
  const routeMapContainer = document.getElementById('routeMapContainer');
  const mapExpandBtn = document.getElementById('mapExpandBtn');
  const mapCollapseBtn = document.getElementById('mapCollapseBtn');

  function toggleMapExpansion(forceExpand) {
    if (!routeMapContainer) return;
    const isCurrentlyExpanded = routeMapContainer.classList.contains('is-expanded');
    const targetState = forceExpand !== undefined ? forceExpand : !isCurrentlyExpanded;

    if (targetState) {
      routeMapContainer.classList.add('is-expanded');
      document.body.classList.add('map-locked');
      if (mapExpandBtn) mapExpandBtn.style.display = 'none';
      if (mapCollapseBtn) mapCollapseBtn.style.display = 'inline-flex';
    } else {
      routeMapContainer.classList.remove('is-expanded');
      document.body.classList.remove('map-locked');
      if (mapExpandBtn) mapExpandBtn.style.display = 'inline-flex';
      if (mapCollapseBtn) mapCollapseBtn.style.display = 'none';
    }

    // Call Leaflet's invalidateSize() after opening, closing, or resizing
    setTimeout(() => {
      if (leafletMapInstance) {
        leafletMapInstance.invalidateSize();
      }
    }, 100);
    setTimeout(() => {
      if (leafletMapInstance) {
        leafletMapInstance.invalidateSize();
      }
    }, 300);
  }

  if (mapExpandBtn) {
    mapExpandBtn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      toggleMapExpansion(true);
    });
  }

  if (mapCollapseBtn) {
    mapCollapseBtn.addEventListener('click', (e) => {
      e.preventDefault();
      e.stopPropagation();
      toggleMapExpansion(false);
    });
  }

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && routeMapContainer && routeMapContainer.classList.contains('is-expanded')) {
      toggleMapExpansion(false);
    }
  });

  window.addEventListener('resize', () => {
    if (leafletMapInstance) {
      leafletMapInstance.invalidateSize();
    }
  });

  // Real Road Routing via Open Source Routing Machine (OSRM Demo Service)
  async function fetchRoadRoute(oLat, oLng, dLat, dLng, travelMode) {
    const profile = travelMode === 'DRIVE' ? 'driving' : 'foot';
    // OSRM format: /route/v1/{profile}/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson
    const osrmUrl = `https://router.project-osrm.org/route/v1/${profile}/${oLng},${oLat};${dLng},${dLat}?overview=full&geometries=geojson`;

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000);

    try {
      const response = await fetch(osrmUrl, {
        signal: controller.signal,
        headers: {
          'Accept': 'application/json'
        }
      });
      clearTimeout(timeoutId);

      if (!response.ok) {
        throw new Error(`Routing provider returned HTTP status ${response.status}`);
      }

      const json = await response.json();
      if (json.code !== 'Ok' || !json.routes || json.routes.length === 0) {
        throw new Error(json.message || 'No road route could be found between these coordinates.');
      }

      const primary = json.routes[0];
      // IMPORTANT: GeoJSON coordinates are [longitude, latitude]
      // Leaflet positions use [latitude, longitude]
      const roadLatLngs = primary.geometry.coordinates.map(pt => [pt[1], pt[0]]);

      return {
        distanceMeters: Math.round(primary.distance),
        durationSeconds: Math.round(primary.duration),
        latLngs: roadLatLngs,
        geometry: primary.geometry,
        roadFollowed: true
      };
    } catch (err) {
      clearTimeout(timeoutId);
      console.warn('OSRM routing fetch failed:', err.message);
      throw err;
    }
  }

  // 3. Travel Mode Selector
  if (modeWalkBtn && modeDriveBtn) {
    modeWalkBtn.addEventListener('click', () => {
      currentTravelMode = 'WALK';
      modeWalkBtn.classList.add('active');
      modeDriveBtn.classList.remove('active');
    });

    modeDriveBtn.addEventListener('click', () => {
      currentTravelMode = 'DRIVE';
      modeDriveBtn.classList.add('active');
      modeWalkBtn.classList.remove('active');
    });
  }

  // Feature 2: Capture Complete Available Current Address with Reverse Geocoding
  // Client-side cache to minimize geocoding requests & comply with Nominatim usage policy
  const reverseGeocodeCache = new Map();
  let lastReverseGeocodeTime = 0;

  async function reverseGeocodeDetailedAddress(lat, lng) {
    const cacheKey = `${lat.toFixed(5)},${lng.toFixed(5)}`;
    if (reverseGeocodeCache.has(cacheKey)) {
      return reverseGeocodeCache.get(cacheKey);
    }

    // Comply with Nominatim 1 request/sec rate limit
    const now = Date.now();
    const timeSinceLast = now - lastReverseGeocodeTime;
    if (timeSinceLast < 1000) {
      await new Promise(r => setTimeout(r, 1000 - timeSinceLast));
    }
    lastReverseGeocodeTime = Date.now();

    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 6000);
      const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`;
      const res = await fetch(url, {
        signal: controller.signal,
        headers: { 'Accept': 'application/json' }
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const data = await res.json();
        if (data && data.address) {
          const a = data.address;
          // Extract address components without inventing anything
          const house = a.house_number || a.building || a.house_name || '';
          const road = a.road || a.street || a.residential || a.pedestrian || a.footway || a.path || '';
          const locality = a.neighbourhood || a.suburb || a.locality || a.block || a.subdivision || '';
          const district = a.city_district || a.subdistrict || a.district || a.county || '';
          const city = a.city || a.town || a.village || a.municipality || a.hamlet || '';
          const state = a.state || '';
          const postcode = a.postcode || '';
          const country = a.country || '';

          // Assemble ordered, non-empty, deduplicated address parts
          const rawParts = [house, road, locality, district, city, state, postcode, country];
          const seen = new Set();
          const cleanParts = [];
          for (const p of rawParts) {
            const trimmed = (p || '').trim();
            if (trimmed && !seen.has(trimmed.toLowerCase())) {
              seen.add(trimmed.toLowerCase());
              cleanParts.push(trimmed);
            }
          }

          const detailedAddress = cleanParts.length > 0 ? cleanParts.join(', ') : (data.display_name || '').trim();
          const detectedCityName = city || district || locality || null;
          const result = {
            success: true,
            detailedAddress: detailedAddress,
            city: detectedCityName,
            state: state,
            country: country
          };
          reverseGeocodeCache.set(cacheKey, result);
          return result;
        }
      }
    } catch (e) {
      console.warn('Nominatim reverse geocoding notice:', e);
    }

    // Compatible fallback: inspect existing local verified boundaries
    try {
      const localRes = await SmartShieldAPI.resolveLocation(lat, lng);
      if (localRes && localRes.resolved && localRes.city) {
        const fallbackAddr = `${localRes.city}, ${localRes.state}${localRes.country ? ', ' + localRes.country : ''}`;
        const result = {
          success: true,
          detailedAddress: fallbackAddr,
          city: localRes.city,
          state: localRes.state,
          country: localRes.country
        };
        reverseGeocodeCache.set(cacheKey, result);
        return result;
      }
    } catch (e2) {
      console.warn('Local boundary resolution notice:', e2);
    }

    // Exact address unavailable fallback (Requirement 7 & 8)
    const fallbackResult = {
      success: false,
      detailedAddress: 'Exact address unavailable — using GPS coordinates',
      city: null,
      state: null,
      country: null
    };
    reverseGeocodeCache.set(cacheKey, fallbackResult);
    return fallbackResult;
  }

  // 4. Current Location — Browser GPS with Detailed Address (Requirement 1-10)
  if (useGpsBtn) {
    useGpsBtn.addEventListener('click', () => {
      if (!navigator.geolocation) {
        showToast('Geolocation is not supported by your browser', 'error');
        return;
      }

      useGpsBtn.disabled = true;
      useGpsBtn.textContent = '⏳ Detecting location...';

      navigator.geolocation.getCurrentPosition(
        async pos => {
          const lat = pos.coords.latitude;
          const lng = pos.coords.longitude;
          const accuracy = pos.coords.accuracy;

          if (lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180) {
            // Requirement 2 & 6: Preserve exact GPS coordinates as routing origin
            if (originLatInput) originLatInput.value = lat.toFixed(6);
            if (originLngInput) originLngInput.value = lng.toFixed(6);

            const hint = document.getElementById('originCoordHint');
            if (hint) {
              hint.textContent = `GPS origin captured: ${lat.toFixed(6)}, ${lng.toFixed(6)} (accuracy: ±${Math.round(accuracy)}m)`;
            }

            useGpsBtn.disabled = false;
            useGpsBtn.textContent = '✓ GPS Active';

            // Resolve detailed address
            const geoResult = await reverseGeocodeDetailedAddress(lat, lng);
            if (geoResult.success && geoResult.detailedAddress) {
              detectedCity = geoResult.city;
              detectedState = geoResult.state;
              if (originInput) originInput.value = geoResult.detailedAddress;
              if (originLocationBanner) {
                originLocationBanner.style.display = 'flex';
                if (originLocationText) originLocationText.textContent = geoResult.detailedAddress;
              }
              if (geoResult.city) {
                loadDestinationSuggestions(geoResult.city);
              } else {
                loadDestinationSuggestions(null);
              }
              showToast('Location captured: ' + geoResult.detailedAddress, 'success');
            } else {
              // Requirement 7: Keep valid GPS coordinates and display clear fallback
              detectedCity = null;
              detectedState = null;
              const fallbackText = 'Exact address unavailable — using GPS coordinates';
              if (originInput) originInput.value = fallbackText;
              if (originLocationBanner) {
                originLocationBanner.style.display = 'flex';
                if (originLocationText) originLocationText.textContent = fallbackText;
              }
              loadDestinationSuggestions(null);
              showToast('GPS coordinates captured (street address unavailable).', 'info');
            }
          } else {
            useGpsBtn.disabled = false;
            useGpsBtn.textContent = '📍 Try Again';
            showToast('Invalid coordinates received from GPS sensor.', 'error');
          }
        },
        err => {
          console.warn('GPS error:', err.message);
          useGpsBtn.disabled = false;
          if (err.code === 1 /* PERMISSION_DENIED */) {
            useGpsBtn.textContent = '✕ Permission Denied';
            showToast('Location permission is required to use your current location.', 'error');
          } else {
            useGpsBtn.textContent = '✕ Try Again';
            showToast('Current location could not be determined. Please enable location access or enter a destination manually.', 'error');
          }
        },
        { timeout: 12000, enableHighAccuracy: true, maximumAge: 30000 }
      );
    });
  }

  // Requirement 9: Allow the user to edit or confirm the displayed address
  // without silently replacing the GPS coordinates with unrelated coordinates
  if (originInput) {
    originInput.addEventListener('input', () => {
      const val = originInput.value.trim();
      if (originLocationText) {
        originLocationText.textContent = val || 'Custom location';
      }
      const hint = document.getElementById('originCoordHint');
      if (hint && originLatInput && originLngInput && originLatInput.value && originLngInput.value) {
        hint.textContent = `Retaining GPS routing coordinates: ${originLatInput.value}, ${originLngInput.value}`;
      }
    });
  }

  // Real OpenStreetMap Place Search Service (Photon / Nominatim + Municipal Fallback)
  const placeSearchCache = new Map();
  let lastPlaceSearchTime = 0;

  async function searchOpenStreetMapPlaces(query, lat, lng, city) {
    const trimmedQuery = (query || '').trim();
    if (!trimmedQuery) return [];

    // Coarse coordinates to ~1km cache bucket to protect privacy and optimize caching
    const coarseLat = (lat != null && !isNaN(lat)) ? parseFloat(lat).toFixed(2) : '';
    const coarseLng = (lng != null && !isNaN(lng)) ? parseFloat(lng).toFixed(2) : '';
    const cacheKey = `${trimmedQuery.toLowerCase()}|${coarseLat}|${coarseLng}|${(city || '').toLowerCase()}`;

    if (placeSearchCache.has(cacheKey)) {
      return placeSearchCache.get(cacheKey);
    }

    // Rate-limiting throttle (min 300ms between calls)
    const now = Date.now();
    const elapsed = now - lastPlaceSearchTime;
    if (elapsed < 300) {
      await new Promise(r => setTimeout(r, 300 - elapsed));
    }
    lastPlaceSearchTime = Date.now();

    // 1. Primary: Photon (Free, OpenStreetMap-based, ElasticSearch location-biased)
    try {
      const controller = new AbortController();
      const tid = setTimeout(() => controller.abort(), 6000);
      let photonUrl = `https://photon.komoot.io/api/?q=${encodeURIComponent(trimmedQuery)}&limit=7`;
      if (lat != null && lng != null && !isNaN(lat) && !isNaN(lng)) {
        photonUrl += `&lat=${lat}&lon=${lng}`;
      }
      const resp = await fetch(photonUrl, { signal: controller.signal });
      clearTimeout(tid);

      if (resp.ok) {
        const data = await resp.json();
        if (data && data.features && data.features.length > 0) {
          const results = [];
          for (const f of data.features) {
            const p = f.properties || {};
            const coords = f.geometry && f.geometry.coordinates;
            if (!coords || coords.length < 2) continue;

            const placeLon = coords[0];
            const placeLat = coords[1];
            const placeName = p.name || p.street || p.housenumber || '';
            if (!placeName) continue;

            // Address formatting from real properties without fabrication
            const addrParts = [
              p.housenumber,
              p.street,
              p.locality || p.district,
              p.city || p.county,
              p.state,
              p.country
            ].filter(Boolean);
            const address = addrParts.join(', ');

            // Map OpenStreetMap tags to categories
            let category = 'PLACE';
            const osmKey = (p.osm_key || '').toLowerCase();
            const osmVal = (p.osm_value || '').toLowerCase();
            const lowerName = placeName.toLowerCase();

            if (osmVal === 'hospital' || osmVal === 'clinic' || osmVal === 'doctors' || lowerName.includes('hospital')) {
              category = 'HOSPITAL';
            } else if (osmVal === 'police' || lowerName.includes('police')) {
              category = 'POLICE';
            } else if (osmVal === 'subway' || osmVal === 'station' || lowerName.includes('metro')) {
              category = 'METRO';
            } else if (osmVal === 'train_station' || osmVal === 'bus_station' || lowerName.includes('railway') || lowerName.includes('station')) {
              category = 'TRANSIT';
            } else if (osmVal === 'townhall' || osmVal === 'courthouse' || osmKey === 'office' || lowerName.includes('court') || lowerName.includes('collectorate')) {
              category = 'GOVT';
            } else if (osmVal === 'school' || osmVal === 'college' || osmVal === 'university') {
              category = 'EDUCATION';
            } else if (osmKey === 'shop' || osmVal === 'mall' || osmVal === 'supermarket') {
              category = 'COMMERCIAL';
            }

            results.push({
              name: placeName,
              category: category,
              address: address || p.city || p.state || 'OpenStreetMap Location',
              city: p.city || p.county || city || '',
              state: p.state || '',
              latitude: placeLat,
              longitude: placeLon,
              source: 'OpenStreetMap'
            });
          }

          if (results.length > 0) {
            placeSearchCache.set(cacheKey, results);
            return results;
          }
        }
      }
    } catch (e) {
      console.warn('Photon place search notice:', e);
    }

    // 2. Secondary: Nominatim OpenStreetMap Search
    try {
      const controller = new AbortController();
      const tid = setTimeout(() => controller.abort(), 6000);
      const searchTerms = city ? `${trimmedQuery} in ${city}` : trimmedQuery;
      const nomUrl = `https://nominatim.openstreetmap.org/search?format=jsonv2&q=${encodeURIComponent(searchTerms)}&limit=6&addressdetails=1`;
      const resp = await fetch(nomUrl, {
        signal: controller.signal,
        headers: { 'Accept': 'application/json' }
      });
      clearTimeout(tid);

      if (resp.ok) {
        const items = await resp.json();
        if (Array.isArray(items) && items.length > 0) {
          const results = [];
          for (const item of items) {
            const latVal = parseFloat(item.lat);
            const lngVal = parseFloat(item.lon);
            if (isNaN(latVal) || isNaN(lngVal)) continue;

            const name = item.name || (item.address && (item.address.amenity || item.address.building || item.address.road)) || (item.display_name ? item.display_name.split(',')[0] : 'OpenStreetMap Place');
            const catRaw = (item.category || item.type || '').toLowerCase();
            let category = 'PLACE';
            if (catRaw.includes('hospital') || catRaw.includes('clinic')) category = 'HOSPITAL';
            else if (catRaw.includes('police')) category = 'POLICE';
            else if (catRaw.includes('subway') || catRaw.includes('metro')) category = 'METRO';
            else if (catRaw.includes('station') || catRaw.includes('railway') || catRaw.includes('transit')) category = 'TRANSIT';
            else if (catRaw.includes('government') || catRaw.includes('court') || catRaw.includes('townhall')) category = 'GOVT';
            else if (catRaw.includes('school') || catRaw.includes('university')) category = 'EDUCATION';

            results.push({
              name: name,
              category: category,
              address: item.display_name,
              city: (item.address && (item.address.city || item.address.town || item.address.county)) || city || '',
              state: (item.address && item.address.state) || '',
              latitude: latVal,
              longitude: lngVal,
              source: 'OpenStreetMap'
            });
          }

          if (results.length > 0) {
            placeSearchCache.set(cacheKey, results);
            return results;
          }
        }
      }
    } catch (e2) {
      console.warn('Nominatim search notice:', e2);
    }

    // 3. Fallback: Backend Curated Municipal Dataset
    try {
      const localResults = await SmartShieldAPI.getDestinationSuggestions(city, trimmedQuery, lat, lng);
      if (Array.isArray(localResults) && localResults.length > 0) {
        const mapped = localResults.map(r => ({
          ...r,
          source: 'Municipal Dataset'
        }));
        placeSearchCache.set(cacheKey, mapped);
        return mapped;
      }
    } catch (e3) {
      console.warn('Local municipal fallback notice:', e3);
    }

    return [];
  }

  // 5. Destination Suggestions (Nearby Emergency, Transit & Municipal Landmarks)
  async function loadDestinationSuggestions(city) {
    if (!quickDestChips) return;

    if (quickCityTag) {
      if (city) {
        quickCityTag.textContent = city;
        quickCityTag.style.display = 'inline-block';
      } else {
        quickCityTag.style.display = 'none';
      }
    }

    if (quickDestLabel) {
      quickDestLabel.textContent = city
        ? `Nearby places in ${city}:`
        : 'Nearby destinations:';
    }

    quickDestChips.innerHTML = '<span class="suggestions-state"><span class="spinner-sm"></span> Finding nearby places...</span>';

    const oLat = parseFloat(originLatInput?.value) || 28.6692;
    const oLng = parseFloat(originLngInput?.value) || 77.4538;

    let items = [];

    // Attempt real OpenStreetMap POI discovery for emergency & transit hubs
    try {
      const emergencyQuery = city ? `hospital police metro in ${city}` : 'hospital police station metro';
      const realPlaces = await searchOpenStreetMapPlaces(emergencyQuery, oLat, oLng, city);
      if (realPlaces && realPlaces.length > 0) {
        items = realPlaces;
      }
    } catch (err) {
      console.warn('Live nearby POI discovery notice:', err);
    }

    // Supplement or fallback with curated municipal registry
    try {
      const municipalPlaces = await SmartShieldAPI.getDestinationSuggestions(city, '', oLat, oLng);
      if (Array.isArray(municipalPlaces) && municipalPlaces.length > 0) {
        const existingNames = new Set(items.map(i => (i.name || '').toLowerCase()));
        for (const m of municipalPlaces) {
          if (!existingNames.has((m.name || '').toLowerCase())) {
            items.push({ ...m, source: 'Municipal Dataset' });
          }
        }
      }
    } catch (mErr) {
      console.warn('Municipal landmark fetch notice:', mErr);
    }

    quickDestChips.innerHTML = '';

    if (!items || items.length === 0) {
      const emptySpan = document.createElement('span');
      emptySpan.className = 'suggestions-empty';
      emptySpan.innerHTML = 'Nearby place discovery unavailable. Enter a destination manually above. <button type="button" class="btn-retry-chips" id="quickDestRetryBtn">↻ Retry</button>';
      quickDestChips.appendChild(emptySpan);
      const retryBtn = document.getElementById('quickDestRetryBtn');
      if (retryBtn) {
        retryBtn.addEventListener('click', () => loadDestinationSuggestions(city));
      }
      return;
    }

    // Display up to 8 real destination chips
    items.slice(0, 8).forEach(item => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'chip-btn';
      const icon = getCategoryIcon(item.category);
      btn.textContent = `${icon} ${item.name}`;
      btn.title = `${item.name} (${item.address || item.city || ''}) - ${item.source || 'OpenStreetMap'}`;
      btn.addEventListener('click', () => {
        selectDestination(item);
      });
      quickDestChips.appendChild(btn);
    });
  }

  function selectDestination(item) {
    if (destInput) destInput.value = item.name;
    if (destLatInput) destLatInput.value = item.latitude;
    if (destLngInput) destLngInput.value = item.longitude;
    const hint = document.getElementById('destCoordHint');
    if (hint) {
      const locStr = item.address || item.city || '';
      hint.textContent = `Destination: ${item.name}${locStr ? ' (' + locStr + ')' : ''} [${item.latitude.toFixed(6)}, ${item.longitude.toFixed(6)}]`;
    }
    if (destSuggestionsDropdown) {
      destSuggestionsDropdown.style.display = 'none';
    }
  }

  // Live Destination Search with OpenStreetMap Provider & Municipal Fallback
  if (destInput && destSuggestionsDropdown) {
    destInput.addEventListener('input', () => {
      const query = destInput.value.trim();
      clearTimeout(destSearchDebounceTimer);

      if (query.length < 2) {
        destSuggestionsDropdown.style.display = 'none';
        return;
      }

      destSearchDebounceTimer = setTimeout(async () => {
        destSuggestionsDropdown.innerHTML = '<div class="suggestions-state"><span class="spinner-sm"></span> Searching places via OpenStreetMap...</div>';
        destSuggestionsDropdown.style.display = 'block';

        const oLat = parseFloat(originLatInput?.value) || 28.6692;
        const oLng = parseFloat(originLngInput?.value) || 77.4538;

        try {
          const results = await searchOpenStreetMapPlaces(query, oLat, oLng, detectedCity);
          destSuggestionsDropdown.innerHTML = '';

          if (!results || results.length === 0) {
            const emptyEl = document.createElement('div');
            emptyEl.className = 'suggestions-state suggestions-empty';
            emptyEl.textContent = `No matching places found for "${query}" near your location. You may enter any destination address manually.`;
            destSuggestionsDropdown.appendChild(emptyEl);
            destSuggestionsDropdown.style.display = 'block';
            return;
          }

          results.forEach(res => {
            const itemEl = document.createElement('div');
            itemEl.className = 'suggestion-item';

            const mainEl = document.createElement('div');
            mainEl.className = 'suggestion-main';

            const iconEl = document.createElement('span');
            iconEl.className = 'suggestion-icon';
            iconEl.textContent = getCategoryIcon(res.category);

            const textEl = document.createElement('div');
            textEl.className = 'suggestion-text';

            const nameEl = document.createElement('span');
            nameEl.className = 'suggestion-name';
            nameEl.textContent = res.name;

            const addrEl = document.createElement('span');
            addrEl.className = 'suggestion-address';
            addrEl.textContent = res.address || `${res.city || ''}, ${res.state || ''}`;

            textEl.appendChild(nameEl);
            textEl.appendChild(addrEl);

            mainEl.appendChild(iconEl);
            mainEl.appendChild(textEl);

            const metaEl = document.createElement('div');
            metaEl.style.display = 'flex';
            metaEl.style.alignItems = 'center';
            metaEl.style.gap = '6px';

            const tagEl = document.createElement('span');
            tagEl.className = `suggestion-cat-tag ${res.category || ''}`;
            tagEl.textContent = res.category || 'PLACE';

            const srcEl = document.createElement('span');
            srcEl.className = 'suggestion-src-badge';
            srcEl.textContent = res.source === 'OpenStreetMap' ? 'OSM' : 'MUNICIPAL';

            metaEl.appendChild(tagEl);
            metaEl.appendChild(srcEl);

            itemEl.appendChild(mainEl);
            itemEl.appendChild(metaEl);

            itemEl.addEventListener('click', () => {
              selectDestination(res);
            });

            destSuggestionsDropdown.appendChild(itemEl);
          });

          destSuggestionsDropdown.style.display = 'block';
        } catch (err) {
          console.warn('Destination search error:', err);
          destSuggestionsDropdown.innerHTML = '<div class="suggestions-state suggestions-error">Search service temporarily unavailable. <button type="button" class="btn-retry-dest" id="destSearchRetryBtn">↻ Retry</button></div>';
          destSuggestionsDropdown.style.display = 'block';
          const retryBtn = document.getElementById('destSearchRetryBtn');
          if (retryBtn) {
            retryBtn.addEventListener('click', () => {
              destInput.dispatchEvent(new Event('input'));
            });
          }
        }
      }, 300);
    });

    // Close dropdown on click outside
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.input-with-dropdown')) {
        destSuggestionsDropdown.style.display = 'none';
      }
    });
  }

  // 6. Calculate Safe Route Submission (Requirement 7)
  if (routeForm) {
    routeForm.addEventListener('submit', (e) => {
      e.preventDefault();

      if (!token) {
        showToast('Please sign in to calculate and save safe routes', 'error');
        return;
      }

      hideMapFallbackBanner();

      const oLat = parseFloat(originLatInput.value) || 28.6692;
      const oLng = parseFloat(originLngInput.value) || 77.4538;
      const dLat = parseFloat(destLatInput.value) || 28.6672;
      const dLng = parseFloat(destLngInput.value) || 77.4328;

      if (Math.abs(oLat - dLat) < 0.0001 && Math.abs(oLng - dLng) < 0.0001) {
        showToast('Origin and destination coordinates cannot be identical', 'error');
        return;
      }

      setCalculatingState(true);

      // 1. Fetch real road geometry from OpenStreetMap OSRM routing service
      fetchRoadRoute(oLat, oLng, dLat, dLng, currentTravelMode)
        .then(routingData => {
          // 2. Send actual origin, destination, and road metrics to backend
          const payload = {
            originLatitude: oLat,
            originLongitude: oLng,
            originAddress: originInput.value.trim() || 'Current Location',
            destinationLatitude: dLat,
            destinationLongitude: dLng,
            destinationAddress: destInput.value.trim() || 'Destination Location',
            travelMode: currentTravelMode,
            distanceMeters: routingData.distanceMeters,
            durationSeconds: routingData.durationSeconds
          };

          return SmartShieldAPI.calculateSafeRoute(payload)
            .then(route => {
              currentRouteId = route.safeRouteId;
              currentRouteData = route;
              // Attach real road coordinates from OSRM for Leaflet drawing
              currentRouteData.roadLatLngs = routingData.latLngs;
              currentRouteData.distanceMeters = routingData.distanceMeters;
              currentRouteData.durationSeconds = routingData.durationSeconds;

              renderRouteResults(currentRouteData);
              showToast('Safe Route evaluated with genuine OpenStreetMap road navigation', 'success');
              routeResultsSection.scrollIntoView({ behavior: 'smooth' });
            });
        })
        .catch(err => {
          console.warn('Road routing or safety evaluation issue:', err);
          showMapFallbackBanner(
            'OSRM Routing Service Notice',
            'Could not retrieve real road geometry from the free routing service: ' + (err.message || 'Network timeout') + '. Click Retry to try again.'
          );
          if (mapStatusPill) {
            mapStatusPill.textContent = 'Routing Provider Notice — Retry';
            mapStatusPill.className = 'map-status-pill fallback';
          }
          showToast(err.message || 'Routing service unavailable. Please retry.', 'error');
        })
        .finally(() => {
          setCalculatingState(false);
        });
    });
  }

  function setCalculatingState(isCalculating) {
    if (calculateBtn) calculateBtn.disabled = isCalculating;
    if (calcBtnText) calcBtnText.textContent = isCalculating ? 'Evaluating Route Safety...' : '🛡 Calculate Safe Route';
    if (calcBtnSpinner) calcBtnSpinner.style.display = isCalculating ? 'inline-block' : 'none';
  }

  // 7. Render Route Results & Inspection (Requirement 8, 9, 10, 11)
  function renderRouteResults(route) {
    if (!routeResultsSection) return;
    routeResultsSection.style.display = 'block';

    // Route ID
    if (routeIdBadge) routeIdBadge.textContent = route.safeRouteId || 'SR-ACTIVE';

    // Metrics
    const distKm = (route.distanceMeters / 1000).toFixed(1);
    const durMins = Math.round(route.durationSeconds / 60);
    if (metricDistance) metricDistance.textContent = `${distKm} km`;
    if (metricDuration) metricDuration.textContent = `${durMins} mins`;

    const mapDist = document.getElementById('mapDistanceBadge');
    const mapDur = document.getElementById('mapDurationBadge');
    if (mapDist) mapDist.textContent = `${distKm} km`;
    if (mapDur) mapDur.textContent = `${durMins} mins`;

    // Google Maps-Style Route Inspection Summary Card
    if (inspectStartName) inspectStartName.textContent = route.originAddress || 'Current Location';
    if (inspectStartSub) {
      inspectStartSub.textContent = detectedCity
        ? (detectedState ? `${detectedCity}, ${detectedState}` : detectedCity)
        : 'Verified Origin Corridor';
    }
    if (inspectEndName) inspectEndName.textContent = route.destinationAddress || 'Destination Place';
    if (inspectEndSub) inspectEndSub.textContent = 'Verified Destination';
    if (inspectModeIcon) inspectModeIcon.textContent = currentTravelMode === 'DRIVE' ? '🚗' : '🚶';
    if (inspectDistanceText) inspectDistanceText.textContent = `${distKm} km`;
    if (inspectDurationText) inspectDurationText.textContent = `${durMins} mins`;
    if (routeEngineBadge) {
      routeEngineBadge.textContent = 'OSRM & OpenStreetMap — Live Road Navigation';
      routeEngineBadge.className = 'engine-badge google-live';
    }

    // Community Ratings Metric
    if (metricRating) {
      metricRating.textContent = route.averageRating > 0 ? `${route.averageRating.toFixed(1)} / 5` : 'New';
    }
    if (metricReviewsCount) {
      metricReviewsCount.textContent = `${route.ratingCount || 0} reviews`;
    }

    // Requirement 10 & 11: Safety Score & Zero-Evidence Behavior
    const isInsufficient = route.safetyLevel === 'INSUFFICIENT_DATA';

    if (safetyScoreNum) {
      safetyScoreNum.textContent = isInsufficient ? '--' : route.safetyScore;
    }

    if (safetyLevelTag) {
      if (isInsufficient) {
        safetyLevelTag.textContent = 'INSUFFICIENT DATA';
        safetyLevelTag.className = 'level-tag INSUFFICIENT_DATA';
      } else {
        safetyLevelTag.textContent = route.safetyLevel ? route.safetyLevel.replace('_', ' ') : 'SAFE';
        safetyLevelTag.className = `level-tag ${route.safetyLevel || 'SAFE'}`;
      }
    }

    if (scoreCircle) {
      const colorMap = {
        VERY_SAFE: '#10b981',
        SAFE: '#3b82f6',
        MODERATE: '#f59e0b',
        RISKY: '#f97316',
        HIGH_RISK: '#ef4444',
        INSUFFICIENT_DATA: '#64748b'
      };
      const activeColor = colorMap[route.safetyLevel] || '#64748b';
      scoreCircle.style.borderColor = activeColor;
      scoreCircle.style.color = isInsufficient ? '#475569' : activeColor;
    }

    if (safetyLevelDesc) {
      const descMap = {
        VERY_SAFE: 'High safety index. Clean history with accessible emergency posts and positive community ratings.',
        SAFE: 'Safe passage corridor. Low active public safety complaints and standard emergency coverage.',
        MODERATE: 'Moderate transit caution advised. Some civic infrastructure reports in neighboring areas.',
        RISKY: 'Caution: Reduced street lighting or elevated emergency incident reports recorded.',
        HIGH_RISK: 'Warning: Multiple active public safety hazard reports along this corridor. Transit alternative recommended.',
        INSUFFICIENT_DATA: 'No verified citizen safety reports, emergency incidents, or community reviews are currently available for this route.'
      };
      safetyLevelDesc.textContent = descMap[route.safetyLevel] || 'Corridor evaluated against verified civic safety records.';
    }

    // Data Confidence
    if (dataConfidenceBadge) {
      dataConfidenceBadge.textContent = `${route.dataConfidence || 'LOW'} CONFIDENCE`;
      dataConfidenceBadge.className = `badge-confidence ${route.dataConfidence || 'LOW'}`;
    }

    // Warnings List
    if (warningsCard && warningsList) {
      if (route.warnings && route.warnings.length > 0) {
        warningsCard.style.display = 'block';
        warningsList.innerHTML = '';
        route.warnings.forEach(w => {
          const li = document.createElement('li');
          li.textContent = w;
          warningsList.appendChild(li);
        });
      } else {
        warningsCard.style.display = 'none';
      }
    }

    // Factors Breakdown (Safe text insertion)
    if (factorsContainer) {
      factorsContainer.innerHTML = '';
      if (route.factors && route.factors.length > 0) {
        route.factors.forEach(f => {
          const item = document.createElement('div');
          item.className = 'factor-item';

          const header = document.createElement('div');
          header.className = 'factor-header';

          const name = document.createElement('span');
          name.className = 'factor-name';
          name.textContent = f.factorName;

          const badge = document.createElement('span');
          badge.className = `factor-badge ${f.impact}`;
          const sign = f.scoreContribution > 0 ? `+${f.scoreContribution}` : `${f.scoreContribution}`;
          badge.textContent = `${f.impact} (${sign})`;

          header.appendChild(name);
          header.appendChild(badge);

          const desc = document.createElement('p');
          desc.className = 'factor-desc';
          desc.textContent = f.description;

          item.appendChild(header);
          item.appendChild(desc);
          factorsContainer.appendChild(item);
        });
      }
    }

    // Facilities Section
    if (facilitiesSection && facilitiesList) {
      if (route.nearbyFacilities && route.nearbyFacilities.length > 0) {
        facilitiesSection.style.display = 'block';
        facilitiesList.innerHTML = '';
        route.nearbyFacilities.forEach(fac => {
          const el = document.createElement('div');
          el.className = 'facility-item';

          const meta = document.createElement('div');
          meta.className = 'facility-meta';

          const icon = document.createElement('span');
          icon.className = 'facility-icon';
          icon.textContent = fac.type === 'POLICE' ? '👮' : '🏥';

          const info = document.createElement('div');
          const facName = document.createElement('div');
          facName.className = 'facility-name';
          facName.textContent = fac.name;

          const facType = document.createElement('div');
          facType.className = 'facility-type';
          facType.textContent = fac.type === 'POLICE' ? 'Police Station' : 'Hospital / Clinic';

          info.appendChild(facName);
          info.appendChild(facType);
          meta.appendChild(icon);
          meta.appendChild(info);

          const dist = document.createElement('span');
          dist.className = 'facility-distance';
          dist.textContent = `${fac.distanceMeters}m away`;

          el.appendChild(meta);
          el.appendChild(dist);
          facilitiesList.appendChild(el);
        });
      } else {
        facilitiesSection.style.display = 'none';
      }
    }

    // Render Genuine OpenStreetMap Interactive Route on Leaflet
    renderLeafletMapRoute(route);

    // Load Existing Reviews
    loadRouteReviews(route.safeRouteId);
  }

  // 8. Render Live Road-Following Route on Leaflet with OpenStreetMap Tiles
  function renderLeafletMapRoute(route) {
    initLeafletMap();
    if (!leafletMapInstance) return;

    // Remove previous polylines & markers
    if (leafletRoutePolyline) {
      leafletMapInstance.removeLayer(leafletRoutePolyline);
      leafletRoutePolyline = null;
    }
    if (leafletRouteCasing) {
      leafletMapInstance.removeLayer(leafletRouteCasing);
      leafletRouteCasing = null;
    }
    if (leafletOriginMarker) {
      leafletMapInstance.removeLayer(leafletOriginMarker);
      leafletOriginMarker = null;
    }
    if (leafletDestMarker) {
      leafletMapInstance.removeLayer(leafletDestMarker);
      leafletDestMarker = null;
    }
    leafletFacilityMarkers.forEach(m => {
      try {
        leafletMapInstance.removeLayer(m);
      } catch (e) {}
    });
    leafletFacilityMarkers = [];

    const originLat = route.originLatitude;
    const originLng = route.originLongitude;
    const destLat = route.destinationLatitude;
    const destLng = route.destinationLongitude;

    // Use road-following coordinates from OSRM, or decoded polyline
    let pathLatLngs = [];
    if (route.roadLatLngs && route.roadLatLngs.length > 0) {
      pathLatLngs = route.roadLatLngs;
    } else if (route.encodedPolyline) {
      pathLatLngs = decodePolyline(route.encodedPolyline).map(p => [p.lat, p.lng]);
    } else {
      pathLatLngs = [[originLat, originLng], [destLat, destLng]];
    }

    // 1. Draw Casing (Dark outline for visual separation on varied street tiles)
    leafletRouteCasing = L.polyline(pathLatLngs, {
      color: '#7f1d1d',
      weight: 9,
      opacity: 0.55,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(leafletMapInstance);

    // 2. Draw Prominent Red Road Route Line
    leafletRoutePolyline = L.polyline(pathLatLngs, {
      color: '#dc2626',
      weight: 6,
      opacity: 0.95,
      lineCap: 'round',
      lineJoin: 'round'
    }).addTo(leafletMapInstance);

    // 3. Blue Origin Marker for Starting / Current Location
    const bluePinIcon = L.divIcon({
      className: 'custom-leaflet-marker marker-origin',
      html: `<div class="marker-pin marker-pin-blue" title="Start / Current Location">
               <span class="marker-icon">A</span>
             </div>`,
      iconSize: [30, 30],
      iconAnchor: [15, 30],
      popupAnchor: [0, -28]
    });

    leafletOriginMarker = L.marker([originLat, originLng], { icon: bluePinIcon })
      .bindPopup(`<strong>Origin:</strong> ${escapeHtml(route.originAddress || 'Current Location')}`)
      .addTo(leafletMapInstance);

    // 4. Red Destination Marker
    const redPinIcon = L.divIcon({
      className: 'custom-leaflet-marker marker-dest',
      html: `<div class="marker-pin marker-pin-red" title="Destination">
               <span class="marker-icon">B</span>
             </div>`,
      iconSize: [30, 30],
      iconAnchor: [15, 30],
      popupAnchor: [0, -28]
    });

    leafletDestMarker = L.marker([destLat, destLng], { icon: redPinIcon })
      .bindPopup(`<strong>Destination:</strong> ${escapeHtml(route.destinationAddress || 'Selected Destination')}`)
      .addTo(leafletMapInstance);

    // 5. Emergency Infrastructure Posts (Police & Hospital)
    if (route.nearbyFacilities && route.nearbyFacilities.length > 0) {
      route.nearbyFacilities.forEach(fac => {
        const isPolice = fac.type === 'POLICE';
        const facIcon = L.divIcon({
          className: 'custom-leaflet-marker marker-fac',
          html: `<div class="marker-facility ${isPolice ? 'marker-facility-police' : 'marker-facility-hospital'}" title="${escapeHtml(fac.name)}">
                   ${isPolice ? '👮' : '🏥'}
                 </div>`,
          iconSize: [26, 26],
          iconAnchor: [13, 13],
          popupAnchor: [0, -14]
        });

        const m = L.marker([fac.latitude, fac.longitude], { icon: facIcon })
          .bindPopup(`<strong>${isPolice ? 'Police Station' : 'Emergency Center'}:</strong> ${escapeHtml(fac.name)}<br><small style="color:#64748b;">${fac.distanceMeters}m from corridor</small>`)
          .addTo(leafletMapInstance);

        leafletFacilityMarkers.push(m);
      });
    }

    // 6. Zoom & Pan to Fit Bounds with comfortable padding
    if (pathLatLngs.length > 0) {
      const bounds = L.latLngBounds(pathLatLngs);
      leafletMapInstance.fitBounds(bounds, { padding: [40, 40] });
    }

    if (mapStatusPill) {
      mapStatusPill.textContent = 'OpenStreetMap — Live Streets';
      mapStatusPill.className = 'map-status-pill live';
    }

    // Invalidate size to ensure crisp tile calculation
    setTimeout(() => {
      if (leafletMapInstance) {
        leafletMapInstance.invalidateSize();
      }
    }, 200);
  }

  // 9. Interactive Star Rating & Observation Tags
  if (starRating) {
    const stars = starRating.querySelectorAll('.star');
    stars.forEach(star => {
      star.addEventListener('click', () => {
        const val = parseInt(star.getAttribute('data-val'), 10);
        selectedStarRatingInput.value = val;
        updateStarDisplay(val);
      });
      star.addEventListener('mouseover', () => {
        const val = parseInt(star.getAttribute('data-val'), 10);
        highlightStars(val);
      });
    });

    starRating.addEventListener('mouseleave', () => {
      const currentVal = parseInt(selectedStarRatingInput.value, 10);
      highlightStars(currentVal);
    });
  }

  function updateStarDisplay(val) {
    highlightStars(val);
    const textMap = {
      1: '1 - High Risk / Hazardous',
      2: '2 - Needs Precaution',
      3: '3 - Moderate Transit',
      4: '4 - Safe Corridor',
      5: '5 - Very Safe & Well Lit'
    };
    if (starText) starText.textContent = textMap[val] || `${val} Stars`;
  }

  function highlightStars(count) {
    if (!starRating) return;
    const stars = starRating.querySelectorAll('.star');
    stars.forEach(s => {
      const val = parseInt(s.getAttribute('data-val'), 10);
      if (val <= count) {
        s.classList.add('active');
      } else {
        s.classList.remove('active');
      }
    });
  }

  // Quick observation tags
  document.querySelectorAll('.tag-chip').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tag-chip').forEach(c => c.classList.remove('active'));
      btn.classList.add('active');
      const tag = btn.getAttribute('data-tag');
      if (selectedSafetyTagInput) selectedSafetyTagInput.value = tag;
    });
  });

  // Character counter
  if (routeComment && charCount) {
    routeComment.addEventListener('input', () => {
      charCount.textContent = routeComment.value.length;
    });
  }

  // 10. Submit Rating
  if (ratingForm) {
    ratingForm.addEventListener('submit', (e) => {
      e.preventDefault();

      if (!token) {
        showToast('Please sign in to submit a route review', 'error');
        return;
      }

      if (!currentRouteId) {
        showToast('No active route selected to review', 'error');
        return;
      }

      const ratingVal = parseInt(selectedStarRatingInput.value, 10) || 5;
      const tagVal = selectedSafetyTagInput ? selectedSafetyTagInput.value : 'WELL_LIT';
      const commentVal = routeComment ? routeComment.value.trim() : '';

      const payload = {
        rating: ratingVal,
        comment: commentVal,
        safetyTag: tagVal
      };

      const submitBtn = document.getElementById('submitReviewBtn');
      if (submitBtn) submitBtn.disabled = true;

      SmartShieldAPI.submitRouteRating(currentRouteId, payload)
        .then(res => {
          showToast('Thank you! Your safety review was posted.', 'success');
          if (routeComment) routeComment.value = '';
          if (charCount) charCount.textContent = '0';
          // Refresh reviews and route metrics
          loadRouteReviews(currentRouteId);
          SmartShieldAPI.getSafeRoute(currentRouteId).then(renderRouteResults);
        })
        .catch(err => {
          console.error('Rating submission error:', err);
          showToast(err.message || 'Failed to submit review', 'error');
        })
        .finally(() => {
          if (submitBtn) submitBtn.disabled = false;
        });
    });
  }

  // 11. Load Community Reviews
  function loadRouteReviews(safeRouteId) {
    if (!reviewsList || !safeRouteId) return;

    SmartShieldAPI.getRouteRatings(safeRouteId)
      .then(reviews => {
        reviewsList.innerHTML = '';
        if (!reviews || reviews.length === 0) {
          reviewsList.innerHTML = '<p class="empty-state">No reviews yet for this route. Be the first citizen to review!</p>';
          return;
        }

        reviews.forEach(r => {
          const entry = document.createElement('div');
          entry.className = 'review-entry';

          const meta = document.createElement('div');
          meta.className = 'review-meta';

          const author = document.createElement('span');
          author.className = 'review-author';
          author.textContent = r.citizenName || 'Verified Citizen';

          const stars = document.createElement('span');
          stars.className = 'review-stars';
          stars.textContent = '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating);

          meta.appendChild(author);
          meta.appendChild(stars);
          entry.appendChild(meta);

          if (r.safetyTag) {
            const tag = document.createElement('div');
            tag.className = 'review-tag';
            tag.textContent = `Tag: ${r.safetyTag.replace('_', ' ')}`;
            entry.appendChild(tag);
          }

          if (r.comment) {
            const comment = document.createElement('p');
            comment.className = 'review-comment';
            // XSS-Safe textContent assignment
            comment.textContent = r.comment;
            entry.appendChild(comment);
          }

          if (r.createdAt) {
            const date = document.createElement('div');
            date.className = 'review-date';
            date.textContent = new Date(r.createdAt).toLocaleDateString('en-US', {
              year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
            });
            entry.appendChild(date);
          }

          reviewsList.appendChild(entry);
        });
      })
      .catch(err => {
        console.warn('Reviews load error:', err);
      });
  }

  // Toast Notification Utility
  function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast show ${type}`;
    setTimeout(() => {
      toast.className = 'toast';
    }, 4000);
  }

  // Initial Boot
  loadConfigStatus();
  loadDestinationSuggestions(null);
});
