// ============================================================
// SAFESIGNAL - MAIN JAVASCRIPT
// ============================================================


// ============================================================
// MAP INITIALIZATION
// ============================================================

const map = L.map("map");


L.tileLayer(
    "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",
    {
        attribution: "&copy; OpenStreetMap contributors"
    }
).addTo(map);


// ============================================================
// STORE MAP LAYERS
// ============================================================

const routeLayers = {};

const incidentMarkers = [];

const streetlightMarkers = [];

const streetlightCoverageLayers = [];

let originMarker = null;

let destinationMarker = null;


// ============================================================
// STREETLIGHT VISUALIZATION SETTINGS
// ============================================================

// This is NOT the physical illumination radius.
// It is only used to determine whether a route section
// is close enough to a streetlight observation.

const LIGHTING_ROUTE_MATCH_METERS = 120;


// ============================================================
// STREETLIGHT COLORS
// ============================================================

const LIGHTING_COLORS = {

    WORKING: "#38bdf8",

    DIM: "#f59e0b",

    NOT_WORKING: "#64748b",

    UNKNOWN: "#a78bfa"

};


// ============================================================
// REPORT LOCATION STATE
// ============================================================

let reportLocationMarker = null;

let selectingReportLocation = false;


// ============================================================
// STREETLIGHT REPORT LOCATION STATE
// ============================================================

let streetlightReportLocationMarker = null;

let selectingStreetlightLocation = false;


// ============================================================
// LOCATION SEARCH
// OpenStreetMap Nominatim - India only
// ============================================================

async function searchLocation(
    query,
    suggestionsContainer
) {

    if (
        !query ||
        query.trim().length < 3
    ) {

        suggestionsContainer.innerHTML = "";

        suggestionsContainer.style.display =
            "none";

        return;

    }


    try {

        const response =
            await fetch(
                "https://nominatim.openstreetmap.org/search" +
                "?format=json" +
                "&q=" +
                encodeURIComponent(query) +
                "&limit=5" +
                "&addressdetails=1" +
                "&countrycodes=in"
            );


        if (!response.ok) {

            throw new Error(
                "Location search failed."
            );

        }


        const results =
            await response.json();


        suggestionsContainer.innerHTML =
            "";


        if (
            results.length === 0
        ) {

            suggestionsContainer.innerHTML = `
                <div class="location-suggestion">
                    <div class="location-suggestion-title">
                        No locations found
                    </div>
                </div>
            `;

            suggestionsContainer.style.display =
                "block";

            return;

        }


        results.forEach(
            result => {

                const suggestion =
                    document.createElement(
                        "div"
                    );


                suggestion.className =
                    "location-suggestion";


                const displayName =
                    result.display_name;


                const parts =
                    displayName.split(",");


                const title =
                    parts
                        .slice(0, 2)
                        .join(",")
                        .trim();


                const address =
                    parts
                        .slice(2)
                        .join(",")
                        .trim();


                suggestion.innerHTML = `

                    <div class="location-suggestion-title">
                        ${escapeHtml(title)}
                    </div>

                    <div class="location-suggestion-address">
                        ${escapeHtml(address)}
                    </div>

                `;


                suggestion.addEventListener(
                    "click",
                    () => {

                        selectLocation(
                            result,
                            suggestionsContainer
                        );

                    }
                );


                suggestionsContainer.appendChild(
                    suggestion
                );

            }
        );


        suggestionsContainer.style.display =
            "block";


    } catch (error) {

        console.error(
            "Location search error:",
            error
        );

    }

}


// ============================================================
// SELECT LOCATION
// ============================================================

function selectLocation(
    result,
    suggestionsContainer
) {

    const isOrigin =
        suggestionsContainer.id ===
        "originSuggestions";


    const input =
        document.getElementById(
            isOrigin
                ? "originSearch"
                : "destinationSearch"
        );


    const latitudeInput =
        document.getElementById(
            isOrigin
                ? "originLatitude"
                : "destinationLatitude"
        );


    const longitudeInput =
        document.getElementById(
            isOrigin
                ? "originLongitude"
                : "destinationLongitude"
        );


    input.value =
        result.display_name;


    latitudeInput.value =
        result.lat;


    longitudeInput.value =
        result.lon;


    input.classList.add(
        "location-selected"
    );


    suggestionsContainer.innerHTML =
        "";

    suggestionsContainer.style.display =
        "none";


    const coordinates = [
        Number(result.lat),
        Number(result.lon)
    ];


    if (isOrigin) {

        if (originMarker) {

            map.removeLayer(
                originMarker
            );

        }


        originMarker =
            L.marker(
                coordinates
            )
                .addTo(map)
                .bindPopup(
                    `
                    <b>Starting Point</b>
                    <br><br>
                    ${escapeHtml(
                        result.display_name
                    )}
                    `
                );


    } else {

        if (destinationMarker) {

            map.removeLayer(
                destinationMarker
            );

        }


        destinationMarker =
            L.marker(
                coordinates
            )
                .addTo(map)
                .bindPopup(
                    `
                    <b>Destination</b>
                    <br><br>
                    ${escapeHtml(
                        result.display_name
                    )}
                    `
                );

    }


    if (
        originMarker &&
        destinationMarker
    ) {

        const bounds =
            L.latLngBounds([
                originMarker.getLatLng(),
                destinationMarker.getLatLng()
            ]);


        map.fitBounds(
            bounds,
            {
                padding: [40, 40]
            }
        );


    } else {

        map.setView(
            coordinates,
            14
        );

    }

}


// ============================================================
// ESCAPE HTML
// ============================================================

function escapeHtml(value) {

    if (!value) {

        return "";

    }


    return String(value)
        .replaceAll(
            "&",
            "&amp;"
        )
        .replaceAll(
            "<",
            "&lt;"
        )
        .replaceAll(
            ">",
            "&gt;"
        )
        .replaceAll(
            '"',
            "&quot;"
        )
        .replaceAll(
            "'",
            "&#039;"
        );

}


// ============================================================
// LOCATION SEARCH ELEMENTS
// ============================================================

const originSearch =
    document.getElementById(
        "originSearch"
    );


const destinationSearch =
    document.getElementById(
        "destinationSearch"
    );


const originSuggestions =
    document.getElementById(
        "originSuggestions"
    );


const destinationSuggestions =
    document.getElementById(
        "destinationSuggestions"
    );


// ============================================================
// SEARCH DEBOUNCE
// ============================================================

let originSearchTimer = null;

let destinationSearchTimer = null;


// ============================================================
// STARTING POINT SEARCH
// ============================================================

if (originSearch) {

    originSearch.addEventListener(
        "input",
        () => {

            document.getElementById(
                "originLatitude"
            ).value = "";


            document.getElementById(
                "originLongitude"
            ).value = "";


            originSearch.classList.remove(
                "location-selected"
            );


            clearTimeout(
                originSearchTimer
            );


            originSearchTimer =
                setTimeout(
                    () => {

                        searchLocation(
                            originSearch.value,
                            originSuggestions
                        );

                    },
                    500
                );

        }
    );

}


// ============================================================
// DESTINATION SEARCH
// ============================================================

if (destinationSearch) {

    destinationSearch.addEventListener(
        "input",
        () => {

            document.getElementById(
                "destinationLatitude"
            ).value = "";


            document.getElementById(
                "destinationLongitude"
            ).value = "";


            destinationSearch.classList.remove(
                "location-selected"
            );


            clearTimeout(
                destinationSearchTimer
            );


            destinationSearchTimer =
                setTimeout(
                    () => {

                        searchLocation(
                            destinationSearch.value,
                            destinationSuggestions
                        );

                    },
                    500
                );

        }
    );

}


// ============================================================
// CLOSE LOCATION SUGGESTIONS
// ============================================================

document.addEventListener(
    "click",
    event => {

        if (
            !event.target.closest(
                ".location-field"
            )
        ) {

            if (originSuggestions) {

                originSuggestions.style.display =
                    "none";

            }


            if (destinationSuggestions) {

                destinationSuggestions.style.display =
                    "none";

            }

        }

    }
);


// ============================================================
// REPORT LOCATION ELEMENTS
// ============================================================

const chooseReportLocationButton =
    document.getElementById(
        "chooseReportLocation"
    );


const selectedReportLocation =
    document.getElementById(
        "selectedReportLocation"
    );


// ============================================================
// STREETLIGHT REPORT ELEMENTS
// ============================================================

const chooseStreetlightLocationButton =
    document.getElementById(
        "chooseStreetlightLocation"
    );


const selectedStreetlightLocation =
    document.getElementById(
        "selectedStreetlightLocation"
    );


const submitStreetlightReportButton =
    document.getElementById(
        "submitStreetlightReport"
    );


const streetlightMessage =
    document.getElementById(
        "streetlightMessage"
    );


// ============================================================
// ENABLE REPORT LOCATION SELECTION
// ============================================================

if (chooseReportLocationButton) {

    chooseReportLocationButton.addEventListener(
        "click",
        () => {

            selectingStreetlightLocation =
                false;

            selectingReportLocation = true;


            selectedReportLocation.textContent =
                "Click the affected location on the map.";


            selectedReportLocation.classList.add(
                "active"
            );


            map.getContainer().style.cursor =
                "crosshair";

        }
    );

}


// ============================================================
// ENABLE STREETLIGHT LOCATION SELECTION
// ============================================================

if (chooseStreetlightLocationButton) {

    chooseStreetlightLocationButton.addEventListener(
        "click",
        () => {

            selectingReportLocation =
                false;

            selectingStreetlightLocation =
                true;


            selectedStreetlightLocation.textContent =
                "Click the streetlight location on the map.";


            selectedStreetlightLocation.classList.add(
                "active"
            );


            map.getContainer().style.cursor =
                "crosshair";

        }
    );

}


// ============================================================
// REVERSE GEOCODE LOCATION
// ============================================================

async function reverseGeocodeLocation(
    latitude,
    longitude
) {

    const response =
        await fetch(
            "https://nominatim.openstreetmap.org/reverse" +
            "?format=json" +
            "&lat=" +
            latitude +
            "&lon=" +
            longitude +
            "&zoom=18" +
            "&addressdetails=1"
        );


    if (!response.ok) {

        throw new Error(
            "Could not identify the street."
        );

    }


    const data =
        await response.json();


    const address =
        data.address || {};


    const streetName =
        address.road ||
        address.pedestrian ||
        address.footway ||
        address.residential ||
        "Selected Road";


    const areaName =
        address.suburb ||
        address.neighbourhood ||
        address.city_district ||
        address.city ||
        "Selected Area";


    return {
        streetName,
        areaName
    };

}


// ============================================================
// HANDLE MAP CLICK
// ============================================================

map.on(
    "click",
    async event => {

        // ====================================================
        // SAFETY REPORT LOCATION
        // ====================================================

        if (
            selectingReportLocation
        ) {

            selectingReportLocation =
                false;


            map.getContainer().style.cursor =
                "";


            const latitude =
                event.latlng.lat;


            const longitude =
                event.latlng.lng;


            document.getElementById(
                "reportLatitude"
            ).value =
                latitude;


            document.getElementById(
                "reportLongitude"
            ).value =
                longitude;


            if (reportLocationMarker) {

                map.removeLayer(
                    reportLocationMarker
                );

            }


            reportLocationMarker =
                L.marker(
                    [
                        latitude,
                        longitude
                    ]
                )
                    .addTo(map)
                    .bindPopup(
                        "<b>Safety report location</b>"
                    )
                    .openPopup();


            selectedReportLocation.textContent =
                "Finding street...";


            try {

                const location =
                    await reverseGeocodeLocation(
                        latitude,
                        longitude
                    );


                document.getElementById(
                    "reportStreetName"
                ).value =
                    location.streetName;


                document.getElementById(
                    "reportAreaName"
                ).value =
                    location.areaName;


                selectedReportLocation.textContent =
                    `📍 ${location.streetName}, ${location.areaName}`;


            } catch (error) {

                console.error(
                    "Report reverse geocoding error:",
                    error
                );


                selectedReportLocation.textContent =
                    "Location selected, but street name could not be determined.";


                document.getElementById(
                    "reportStreetName"
                ).value =
                    "Selected Road";


                document.getElementById(
                    "reportAreaName"
                ).value =
                    "Selected Area";

            }


            return;

        }


        // ====================================================
        // STREETLIGHT LOCATION
        // ====================================================

        if (
            selectingStreetlightLocation
        ) {

            selectingStreetlightLocation =
                false;


            map.getContainer().style.cursor =
                "";


            const latitude =
                event.latlng.lat;


            const longitude =
                event.latlng.lng;


            document.getElementById(
                "streetlightLatitude"
            ).value =
                latitude;


            document.getElementById(
                "streetlightLongitude"
            ).value =
                longitude;


            if (streetlightReportLocationMarker) {

                map.removeLayer(
                    streetlightReportLocationMarker
                );

            }


            streetlightReportLocationMarker =
                L.marker(
                    [
                        latitude,
                        longitude
                    ]
                )
                    .addTo(map)
                    .bindPopup(
                        "<b>💡 Streetlight observation</b>"
                    )
                    .openPopup();


            selectedStreetlightLocation.textContent =
                "Finding street...";


            try {

                const location =
                    await reverseGeocodeLocation(
                        latitude,
                        longitude
                    );


                document.getElementById(
                    "streetlightStreetName"
                ).value =
                    location.streetName;


                document.getElementById(
                    "streetlightAreaName"
                ).value =
                    location.areaName;


                selectedStreetlightLocation.textContent =
                    `📍 ${location.streetName}, ${location.areaName}`;


            } catch (error) {

                console.error(
                    "Streetlight reverse geocoding error:",
                    error
                );


                selectedStreetlightLocation.textContent =
                    "Location selected, but street name could not be determined.";


                document.getElementById(
                    "streetlightStreetName"
                ).value =
                    "Selected Road";


                document.getElementById(
                    "streetlightAreaName"
                ).value =
                    "Selected Area";

            }

        }

    }
);


// ============================================================
// DRAW REAL OSRM ROUTES
// ============================================================

function drawOsrmRoutes(routes) {

    Object.values(routeLayers).forEach(
        routeLayer => {

            map.removeLayer(
                routeLayer
            );

        }
    );


    Object.keys(routeLayers).forEach(
        key => {

            delete routeLayers[key];

        }
    );


    const allCoordinates = [];


    routes.forEach(
        route => {

            if (
                !route.geometry ||
                route.geometry.length === 0
            ) {

                return;

            }


            const coordinates =
                route.geometry.map(
                    point => [
                        Number(point.latitude),
                        Number(point.longitude)
                    ]
                );


            coordinates.forEach(
                coordinate => {

                    allCoordinates.push(
                        coordinate
                    );

                }
            );


            const routeLine =
                L.polyline(
                    coordinates,
                    {
                        color: "#64748b",
                        weight: 6,
                        opacity: 0.75
                    }
                ).addTo(map);


            routeLine.bindPopup(
                `
                <b>${escapeHtml(
                    route.routeName
                )}</b>

                <br><br>

                Travel time:
                ${route.estimatedDurationMinutes}
                min

                <br>

                Distance:
                ${route.distanceMeters}
                m
                `
            );


            routeLayers[
                route.routeId
                ] =
                routeLine;

        }
    );


    if (
        allCoordinates.length > 0
    ) {

        map.fitBounds(
            allCoordinates,
            {
                padding: [40, 40]
            }
        );

    }

}


// ============================================================
// CALCULATE DISTANCE BETWEEN TWO COORDINATES
// HAVERSINE FORMULA
// ============================================================

function distanceInMeters(
    lat1,
    lon1,
    lat2,
    lon2
) {

    const EARTH_RADIUS_METERS =
        6_371_000;


    const lat1Radians =
        lat1 * Math.PI / 180;


    const lat2Radians =
        lat2 * Math.PI / 180;


    const deltaLat =
        (lat2 - lat1) * Math.PI / 180;


    const deltaLon =
        (lon2 - lon1) * Math.PI / 180;


    const a =
        Math.sin(deltaLat / 2) *
        Math.sin(deltaLat / 2)
        +
        Math.cos(lat1Radians) *
        Math.cos(lat2Radians) *
        Math.sin(deltaLon / 2) *
        Math.sin(deltaLon / 2);


    const c =
        2 *
        Math.atan2(
            Math.sqrt(a),
            Math.sqrt(1 - a)
        );


    return EARTH_RADIUS_METERS * c;

}


// ============================================================
// DISTANCE FROM POINT TO ROUTE SEGMENT
// ============================================================

function distanceFromPointToSegment(
    pointLat,
    pointLon,
    startLat,
    startLon,
    endLat,
    endLon
) {

    const latitudeScale =
        111_320;


    const longitudeScale =
        111_320 *
        Math.cos(
            pointLat * Math.PI / 180
        );


    const px =
        pointLon *
        longitudeScale;


    const py =
        pointLat *
        latitudeScale;


    const ax =
        startLon *
        longitudeScale;


    const ay =
        startLat *
        latitudeScale;


    const bx =
        endLon *
        longitudeScale;


    const by =
        endLat *
        latitudeScale;


    const dx =
        bx - ax;


    const dy =
        by - ay;


    if (
        dx === 0 &&
        dy === 0
    ) {

        return Math.sqrt(
            Math.pow(
                px - ax,
                2
            ) +
            Math.pow(
                py - ay,
                2
            )
        );

    }


    const t =
        (
            (px - ax) * dx +
            (py - ay) * dy
        ) /
        (
            dx * dx +
            dy * dy
        );


    const clampedT =
        Math.max(
            0,
            Math.min(
                1,
                t
            )
        );


    const closestX =
        ax +
        clampedT * dx;


    const closestY =
        ay +
        clampedT * dy;


    return Math.sqrt(
        Math.pow(
            px - closestX,
            2
        ) +
        Math.pow(
            py - closestY,
            2
        )
    );

}


// ============================================================
// CHECK WHETHER STREETLIGHT IS NEAR DISPLAYED ROUTES
// ============================================================

function isStreetlightNearRoutes(
    streetlight,
    routes
) {

    if (
        !routes ||
        routes.length === 0
    ) {

        return false;

    }


    const latitude =
        Number(
            streetlight.latitude
        );


    const longitude =
        Number(
            streetlight.longitude
        );


    if (
        !Number.isFinite(latitude) ||
        !Number.isFinite(longitude)
    ) {

        return false;

    }


    // IMPORTANT:
    // This is route matching tolerance only.
    // It is NOT the physical illumination radius.

    const MAX_DISTANCE_METERS =
        300;


    for (
        const route of routes
        ) {

        if (
            !route.geometry ||
            route.geometry.length === 0
        ) {

            continue;

        }


        const geometry =
            route.geometry;


        for (
            let i = 0;
            i < geometry.length - 1;
            i++
        ) {

            const start =
                geometry[i];


            const end =
                geometry[i + 1];


            const startLatitude =
                Number(
                    start.latitude
                );


            const startLongitude =
                Number(
                    start.longitude
                );


            const endLatitude =
                Number(
                    end.latitude
                );


            const endLongitude =
                Number(
                    end.longitude
                );


            if (
                !Number.isFinite(
                    startLatitude
                ) ||
                !Number.isFinite(
                    startLongitude
                ) ||
                !Number.isFinite(
                    endLatitude
                ) ||
                !Number.isFinite(
                    endLongitude
                )
            ) {

                continue;

            }


            const distance =
                distanceFromPointToSegment(
                    latitude,
                    longitude,
                    startLatitude,
                    startLongitude,
                    endLatitude,
                    endLongitude
                );


            if (
                distance <=
                MAX_DISTANCE_METERS
            ) {

                return true;

            }

        }

    }


    return false;

}


// ============================================================
// GET STREETLIGHT STATUS LABEL
// ============================================================

function getLightingStatusLabel(
    condition
) {

    switch (
        condition
        ) {

        case "WORKING":

            return "Working";


        case "DIM":

            return "Dim";


        case "NOT_WORKING":

            return "Not working / observed off";


        case "UNKNOWN":

            return "Unknown";


        default:

            return "Unknown";

    }

}


// ============================================================
// GET NEAREST STREETLIGHT
// ============================================================

function getNearestStreetlight(
    latitude,
    longitude,
    streetlights,
    maxDistanceMeters
) {

    let nearestLight =
        null;


    let nearestDistance =
        Infinity;


    for (
        const light of streetlights
        ) {

        const lightLatitude =
            Number(
                light.latitude
            );


        const lightLongitude =
            Number(
                light.longitude
            );


        if (
            !Number.isFinite(
                lightLatitude
            ) ||
            !Number.isFinite(
                lightLongitude
            )
        ) {

            continue;

        }


        const distance =
            distanceInMeters(
                latitude,
                longitude,
                lightLatitude,
                lightLongitude
            );


        if (
            distance <
            nearestDistance
        ) {

            nearestDistance =
                distance;

            nearestLight =
                light;

        }

    }


    if (
        nearestLight &&
        nearestDistance <=
        maxDistanceMeters
    ) {

        return {

            light:
            nearestLight,

            distance:
            nearestDistance

        };

    }


    return null;

}


// ============================================================
// CLEAR STREETLIGHT COVERAGE
// ============================================================

function clearStreetlightCoverage() {

    streetlightCoverageLayers.forEach(
        layer => {

            map.removeLayer(
                layer
            );

        }
    );


    streetlightCoverageLayers.length =
        0;

}


// ============================================================
// DRAW STREETLIGHT COVERAGE ALONG ROUTES
// ============================================================

function drawStreetlightCoverage(
    routes,
    streetlights
) {

    clearStreetlightCoverage();


    if (
        !routes ||
        routes.length === 0
    ) {

        return;

    }


    if (
        !streetlights ||
        streetlights.length === 0
    ) {

        return;

    }


    routes.forEach(
        route => {

            if (
                !route.geometry ||
                route.geometry.length < 2
            ) {

                return;

            }


            let currentCondition =
                null;


            let currentCoordinates =
                [];


            function flushCoverageSegment() {

                if (
                    !currentCondition ||
                    currentCoordinates.length < 2
                ) {

                    currentCoordinates =
                        [];

                    return;

                }


                let color =
                    LIGHTING_COLORS.UNKNOWN;


                let dashArray =
                    null;


                let weight =
                    5;


                if (
                    currentCondition ===
                    "WORKING"
                ) {

                    color =
                        LIGHTING_COLORS.WORKING;

                    weight =
                        5;

                }

                else if (
                    currentCondition ===
                    "DIM"
                ) {

                    color =
                        LIGHTING_COLORS.DIM;

                    dashArray =
                        "8 6";

                    weight =
                        5;

                }

                else if (
                    currentCondition ===
                    "NOT_WORKING"
                ) {

                    color =
                        LIGHTING_COLORS.NOT_WORKING;

                    dashArray =
                        "4 7";

                    weight =
                        5;

                }

                else {

                    color =
                        LIGHTING_COLORS.UNKNOWN;

                    dashArray =
                        "3 6";

                    weight =
                        4;

                }


                const coverageLine =
                    L.polyline(
                        currentCoordinates,
                        {
                            color:
                            color,

                            weight:
                            weight,

                            opacity:
                                0.9,

                            dashArray:
                            dashArray,

                            lineCap:
                                "round",

                            lineJoin:
                                "round"
                        }
                    ).addTo(map);


                coverageLine.bindPopup(
                    `
                    <div>

                        <b>💡 Lighting coverage</b>

                        <br><br>

                        <b>Status:</b>
                        ${escapeHtml(
                        getLightingStatusLabel(
                            currentCondition
                        )
                    )}

                        <br>

                        <b>Route:</b>
                        ${escapeHtml(
                        route.routeName
                    )}

                        <br><br>

                        <span style="color:#64748b;">
                            Highlighted section is based on
                            nearby streetlight observations.
                            It is not a measured illumination
                            radius.
                        </span>

                    </div>
                    `
                );


                streetlightCoverageLayers.push(
                    coverageLine
                );


                currentCoordinates =
                    [];

            }


            for (
                let i = 0;
                i < route.geometry.length - 1;
                i++
            ) {

                const start =
                    route.geometry[i];


                const end =
                    route.geometry[i + 1];


                const startLatitude =
                    Number(
                        start.latitude
                    );


                const startLongitude =
                    Number(
                        start.longitude
                    );


                const endLatitude =
                    Number(
                        end.latitude
                    );


                const endLongitude =
                    Number(
                        end.longitude
                    );


                if (
                    !Number.isFinite(
                        startLatitude
                    ) ||
                    !Number.isFinite(
                        startLongitude
                    ) ||
                    !Number.isFinite(
                        endLatitude
                    ) ||
                    !Number.isFinite(
                        endLongitude
                    )
                ) {

                    continue;

                }


                const midpointLatitude =
                    (
                        startLatitude +
                        endLatitude
                    ) / 2;


                const midpointLongitude =
                    (
                        startLongitude +
                        endLongitude
                    ) / 2;


                const nearest =
                    getNearestStreetlight(
                        midpointLatitude,
                        midpointLongitude,
                        streetlights,
                        LIGHTING_ROUTE_MATCH_METERS
                    );


                const condition =
                    nearest
                        ? (
                            nearest.light.condition ||
                            "UNKNOWN"
                        )
                        : null;


                if (
                    condition !==
                    currentCondition
                ) {

                    flushCoverageSegment();


                    currentCondition =
                        condition;

                }


                if (
                    currentCondition
                ) {

                    if (
                        currentCoordinates.length === 0
                    ) {

                        currentCoordinates.push(
                            [
                                startLatitude,
                                startLongitude
                            ]
                        );

                    }


                    currentCoordinates.push(
                        [
                            endLatitude,
                            endLongitude
                        ]
                    );

                }

            }


            flushCoverageSegment();

        }
    );


    console.log(
        "Streetlight coverage segments:",
        streetlightCoverageLayers.length
    );

}


// ============================================================
// ADD SINGLE STREETLIGHT MARKER
// ============================================================

function addStreetlightMarker(
    light
) {

    const latitude =
        Number(
            light.latitude
        );


    const longitude =
        Number(
            light.longitude
        );


    if (
        !Number.isFinite(latitude) ||
        !Number.isFinite(longitude)
    ) {

        return null;

    }


    let markerColor =
        LIGHTING_COLORS.WORKING;


    let conditionLabel =
        "Working";


    if (
        light.condition ===
        "DIM"
    ) {

        markerColor =
            LIGHTING_COLORS.DIM;

        conditionLabel =
            "Dim";

    }

    else if (
        light.condition ===
        "NOT_WORKING"
    ) {

        markerColor =
            LIGHTING_COLORS.NOT_WORKING;

        conditionLabel =
            "Not working / observed off";

    }

    else if (
        light.condition ===
        "UNKNOWN"
    ) {

        markerColor =
            LIGHTING_COLORS.UNKNOWN;

        conditionLabel =
            "Unknown";

    }


    const marker =
        L.circleMarker(
            [
                latitude,
                longitude
            ],
            {
                radius: 9,

                color:
                markerColor,

                fillColor:
                markerColor,

                fillOpacity: 1,

                weight: 3
            }
        ).addTo(map);


    marker.bindPopup(
        `
        <div>

            <b>💡 Streetlight</b>

            <br><br>

            <b>Condition:</b>
            ${escapeHtml(
            conditionLabel
        )}

            <br>

            <b>Last observed:</b>
            ${
            light.lastObservedAt
                ?
                escapeHtml(
                    String(
                        light.lastObservedAt
                    ).replace(
                        "T",
                        " "
                    )
                )
                :
                "Unknown"
        }

            <br>

            <b>Source:</b>
            ${
            light.source
                ?
                escapeHtml(
                    String(
                        light.source
                    ).replaceAll(
                        "_",
                        " "
                    )
                )
                :
                "Unknown"
        }

            ${
            light.notes
                ?
                `
                        <br><br>
                        ${escapeHtml(
                    light.notes
                )}
                    `
                :
                ""
        }

        </div>
        `
    );


    streetlightMarkers.push(
        marker
    );


    return marker;

}


// ============================================================
// LOAD SAFETY INCIDENT + STREETLIGHT OVERLAYS
// ============================================================

async function loadMapOverlays(
    originLat,
    originLon,
    destinationLat,
    destinationLon,
    travelDateTime,
    routes
) {

    // ========================================================
    // REMOVE OLD INCIDENT MARKERS
    // ========================================================

    incidentMarkers.forEach(
        marker =>
            map.removeLayer(
                marker
            )
    );


    incidentMarkers.length =
        0;


    // ========================================================
    // REMOVE OLD STREETLIGHT MARKERS
    // ========================================================

    streetlightMarkers.forEach(
        marker =>
            map.removeLayer(
                marker
            )
    );


    streetlightMarkers.length =
        0;


    // ========================================================
    // REMOVE OLD STREETLIGHT COVERAGE
    // ========================================================

    clearStreetlightCoverage();


    // ========================================================
    // LOAD SAFETY INCIDENT OVERLAYS
    // ========================================================

    try {

        const response =
            await fetch(
                `/api/map-overlays` +
                `?originLat=${originLat}` +
                `&originLon=${originLon}` +
                `&destinationLat=${destinationLat}` +
                `&destinationLon=${destinationLon}` +
                `&at=${encodeURIComponent(
                    travelDateTime
                )}`
            );


        if (!response.ok) {

            console.warn(
                "Safety overlay endpoint unavailable. HTTP status:",
                response.status
            );

        } else {

            const data =
                await response.json();


            if (data.incidents) {

                data.incidents.forEach(
                    incident => {

                        const latitude =
                            Number(
                                incident.latitude
                            );


                        const longitude =
                            Number(
                                incident.longitude
                            );


                        if (
                            !Number.isFinite(
                                latitude
                            ) ||
                            !Number.isFinite(
                                longitude
                            )
                        ) {

                            return;

                        }


                        const marker =
                            L.circleMarker(
                                [
                                    latitude,
                                    longitude
                                ],
                                {
                                    radius: 9,
                                    color: "#ef4444",
                                    fillColor: "#ef4444",
                                    fillOpacity: 0.85,
                                    weight: 2
                                }
                            ).addTo(map);


                        marker.bindPopup(
                            `
                            <b>⚠ Safety Incident</b>

                            <br><br>

                            <b>Street:</b>
                            ${escapeHtml(
                                incident.streetName
                            )}

                            <br>

                            <b>Type:</b>
                            ${escapeHtml(
                                incident.incidentType
                                    .replaceAll(
                                        "_",
                                        " "
                                    )
                            )}

                            <br>

                            <b>Severity:</b>
                            ${incident.severity}/5

                            <br>

                            <b>Reported:</b>
                            ${escapeHtml(
                                incident.occurredAt
                                    .replace(
                                        "T",
                                        " "
                                    )
                            )}

                            <br><br>

                            ${escapeHtml(
                                incident.description || ""
                            )}
                            `
                        );


                        incidentMarkers.push(
                            marker
                        );

                    }
                );

            }

        }

    } catch (error) {

        console.warn(
            "Safety incident overlay could not be loaded:",
            error
        );

    }


    // ========================================================
    // LOAD STREETLIGHTS
    // ========================================================

    try {

        const streetlightResponse =
            await fetch(
                "/api/streetlights"
            );


        if (!streetlightResponse.ok) {

            throw new Error(
                "Could not load streetlights. HTTP " +
                streetlightResponse.status
            );

        }


        const streetlights =
            await streetlightResponse.json();


        console.log(
            "Total streetlights from database:",
            streetlights.length
        );


        console.log(
            "Routes received for streetlight filtering:",
            routes ? routes.length : 0
        );


        const routeRelevantStreetlights =
            streetlights.filter(
                light =>
                    isStreetlightNearRoutes(
                        light,
                        routes
                    )
            );


        console.log(
            "Streetlights near current routes:",
            routeRelevantStreetlights.length
        );


        routeRelevantStreetlights.forEach(
            light => {

                addStreetlightMarker(
                    light
                );

            }
        );


        console.log(
            "Streetlight markers displayed:",
            streetlightMarkers.length
        );


        // ====================================================
        // DRAW LIGHTING COVERAGE
        // ====================================================

        drawStreetlightCoverage(
            routes,
            routeRelevantStreetlights
        );


    } catch (error) {

        console.error(
            "Streetlight loading error:",
            error
        );

    }

}


// ============================================================
// CHANGE ROUTE COLORS USING SAFETY RESULTS
// ============================================================

function updateMapColors(
    routes
) {

    routes.forEach(
        route => {

            const routeLine =
                routeLayers[
                    route.routeId
                    ];


            if (!routeLine) {

                return;

            }


            let color =
                "#64748b";


            if (
                route.recommended
            ) {

                color =
                    "#22c55e";

            }

            else if (
                route.safetyCategory ===
                "HIGHER_REPORTED_CONCERN"
            ) {

                color =
                    "#ef4444";

            }

            else if (
                route.safetyCategory ===
                "MODERATE_REPORTED_CONCERN"
            ) {

                color =
                    "#f59e0b";

            }

            else {

                color =
                    "#3b82f6";

            }


            routeLine.setStyle({

                color: color,

                weight:
                    route.recommended
                        ? 8
                        : 6,

                opacity:
                    route.recommended
                        ? 1
                        : 0.75

            });


            routeLine.bindPopup(
                `
                <b>${escapeHtml(
                    route.routeName
                )}</b>

                <br><br>

                Travel time:
                ${route.estimatedDurationMinutes}
                min

                <br>

                Distance:
                ${route.distanceMeters}
                m

                <br>

                Concern score:
                ${route.reportedConcernScore}

                <br>

                Status:
                ${escapeHtml(
                    route.safetyCategory
                        .replaceAll(
                            "_",
                            " "
                        )
                )}
                `
            );

        }
    );

}


// ============================================================
// EXISTING ELEMENTS
// ============================================================

const findRoutesButton =
    document.getElementById(
        "findRoutes"
    );


const routeList =
    document.getElementById(
        "routeList"
    );


// ============================================================
// SET TODAY'S DATE
// ============================================================

const travelDateInput =
    document.getElementById(
        "travelDate"
    );


const today =
    new Date();


const year =
    today.getFullYear();


const month =
    String(
        today.getMonth() + 1
    ).padStart(
        2,
        "0"
    );


const day =
    String(
        today.getDate()
    ).padStart(
        2,
        "0"
    );


travelDateInput.value =
    `${year}-${month}-${day}`;


// ============================================================
// DEFAULT TRAVEL TIME
// ============================================================

document.getElementById(
    "travelTime"
).value =
    "23:00";


// ============================================================
// DEFAULT REPORT DATE
// ============================================================

const reportDateInput =
    document.getElementById(
        "reportDate"
    );


if (reportDateInput) {

    reportDateInput.value =
        `${year}-${month}-${day}`;

}


// ============================================================
// DEFAULT REPORT TIME
// ============================================================

const reportTimeInput =
    document.getElementById(
        "reportTime"
    );


if (reportTimeInput) {

    const currentHours =
        String(
            today.getHours()
        ).padStart(
            2,
            "0"
        );


    const currentMinutes =
        String(
            today.getMinutes()
        ).padStart(
            2,
            "0"
        );


    reportTimeInput.value =
        `${currentHours}:${currentMinutes}`;

}


// ============================================================
// FIND ROUTES BUTTON
// ============================================================

findRoutesButton.addEventListener(
    "click",
    async () => {

        // ----------------------------------------------------
        // GET LOCATIONS
        // ----------------------------------------------------

        const originInput =
            document.getElementById(
                "originSearch"
            );


        const destinationInput =
            document.getElementById(
                "destinationSearch"
            );


        const originLatitude =
            document.getElementById(
                "originLatitude"
            ).value;


        const originLongitude =
            document.getElementById(
                "originLongitude"
            ).value;


        const destinationLatitude =
            document.getElementById(
                "destinationLatitude"
            ).value;


        const destinationLongitude =
            document.getElementById(
                "destinationLongitude"
            ).value;


        const travelDate =
            document.getElementById(
                "travelDate"
            ).value;


        const travelTime =
            document.getElementById(
                "travelTime"
            ).value;


        // ----------------------------------------------------
        // VALIDATE LOCATIONS
        // ----------------------------------------------------

        if (
            !originLatitude ||
            !originLongitude
        ) {

            routeList.innerHTML = `
                <div class="error-message">
                    Please search for and select
                    a starting point.
                </div>
            `;

            originInput.focus();

            return;

        }


        if (
            !destinationLatitude ||
            !destinationLongitude
        ) {

            routeList.innerHTML = `
                <div class="error-message">
                    Please search for and select
                    a destination.
                </div>
            `;

            destinationInput.focus();

            return;

        }


        // ----------------------------------------------------
        // VALIDATE DATE + TIME
        // ----------------------------------------------------

        if (
            !travelDate ||
            !travelTime
        ) {

            routeList.innerHTML = `
                <div class="error-message">
                    Please select both a travel date
                    and travel time.
                </div>
            `;

            return;

        }


        // ----------------------------------------------------
        // COMBINE DATE + TIME
        // ----------------------------------------------------

        const travelDateTime =
            `${travelDate}T${travelTime}:00`;


        // ----------------------------------------------------
        // BACKEND URL
        // ----------------------------------------------------

        const url =
            `/api/route-recommendations` +
            `?originLat=${originLatitude}` +
            `&originLon=${originLongitude}` +
            `&destinationLat=${destinationLatitude}` +
            `&destinationLon=${destinationLongitude}` +
            `&at=${encodeURIComponent(
                travelDateTime
            )}`;


        // ----------------------------------------------------
        // LOADING STATE
        // ----------------------------------------------------

        findRoutesButton.disabled =
            true;


        findRoutesButton.textContent =
            "Analyzing routes";


        routeList.innerHTML = `
            <div class="loading">
                Finding routes and analyzing
                reported safety information...
            </div>
        `;


        try {

            // ------------------------------------------------
            // FETCH ROUTES
            // ------------------------------------------------

            const response =
                await fetch(url);


            if (!response.ok) {

                throw new Error(
                    "Could not fetch route recommendations."
                );

            }


            const data =
                await response.json();


            // ------------------------------------------------
            // DRAW ROUTES
            // ------------------------------------------------

            if (
                data.routes &&
                data.routes.length > 0
            ) {

                drawOsrmRoutes(
                    data.routes
                );

            }


            // ------------------------------------------------
            // UPDATE COLORS
            // ------------------------------------------------

            if (data.routes) {

                updateMapColors(
                    data.routes
                );

            }


            // ------------------------------------------------
            // LOAD OVERLAYS
            // ------------------------------------------------

            await loadMapOverlays(
                originLatitude,
                originLongitude,
                destinationLatitude,
                destinationLongitude,
                travelDateTime,
                data.routes || []
            );


            // ------------------------------------------------
            // CLEAR OLD RESULTS
            // ------------------------------------------------

            routeList.innerHTML =
                "";


            // ------------------------------------------------
            // DISPLAY ROUTES
            // ------------------------------------------------

            if (
                data.routes &&
                data.routes.length > 0
            ) {

                // ====================================================
                // FIND FASTEST ROUTE
                // ====================================================

                const fastestRoute =
                    data.routes.reduce(
                        (fastest, route) => {

                            if (!fastest) {

                                return route;

                            }


                            return route.estimatedDurationMinutes <
                            fastest.estimatedDurationMinutes
                                ? route
                                : fastest;

                        },
                        null
                    );


                // ====================================================
                // FIND SAFEST ROUTE
                // ====================================================

                const safestRoute =
                    data.routes.find(
                        route =>
                            route.recommended === true
                    ) ||
                    data.routes.reduce(
                        (safest, route) => {

                            if (!safest) {

                                return route;

                            }


                            return route.reportedConcernScore <
                            safest.reportedConcernScore
                                ? route
                                : safest;

                        },
                        null
                    );


                // ====================================================
                // FASTEST VS SAFEST SUMMARY
                // ====================================================

                if (
                    fastestRoute &&
                    safestRoute
                ) {

                    const timeDifference =
                        safestRoute.estimatedDurationMinutes -
                        fastestRoute.estimatedDurationMinutes;


                    let comparisonMessage;


                    if (
                        safestRoute.routeId ===
                        fastestRoute.routeId
                    ) {

                        comparisonMessage =
                            "The fastest route is also the safest available route.";

                    }

                    else if (
                        timeDifference > 0
                    ) {

                        comparisonMessage =
                            `The safest route takes ${timeDifference} more minute${timeDifference === 1 ? "" : "s"} than the fastest route, but has a lower reported safety concern.`;

                    }

                    else if (
                        timeDifference === 0
                    ) {

                        comparisonMessage =
                            "The safest route takes the same estimated time as the fastest route.";

                    }

                    else {

                        comparisonMessage =
                            "The recommended route is both safer and faster based on the current route analysis.";

                    }


                    const comparisonCard =
                        document.createElement(
                            "div"
                        );


                    comparisonCard.className =
                        "route-comparison";


                    comparisonCard.innerHTML = `

                        <div class="comparison-title">
                            Route Comparison
                        </div>


                        <div class="comparison-grid">

                            <div class="comparison-item fastest">

                                <div class="comparison-icon">
                                    🏎️
                                </div>

                                <div>

                                    <div class="comparison-label">
                                        Fastest Route
                                    </div>

                                    <div class="comparison-value">
                                        ${fastestRoute.estimatedDurationMinutes}
                                        min
                                    </div>

                                </div>

                            </div>


                            <div class="comparison-item safest">

                                <div class="comparison-icon">
                                    🛡️
                                </div>

                                <div>

                                    <div class="comparison-label">
                                        Safest Route
                                    </div>

                                    <div class="comparison-value">
                                        ${safestRoute.estimatedDurationMinutes}
                                        min
                                    </div>

                                </div>

                            </div>

                        </div>


                        <div class="comparison-message">

                            ⭐
                            ${comparisonMessage}

                        </div>

                    `;


                    routeList.appendChild(
                        comparisonCard
                    );

                }


                // ====================================================
                // DISPLAY INDIVIDUAL ROUTE CARDS
                // ====================================================

                data.routes.forEach(
                    route => {

                        const routeCard =
                            document.createElement(
                                "div"
                            );


                        routeCard.className =
                            "route-card";


                        // ------------------------------------------------
                        // RECOMMENDED ROUTE
                        // ------------------------------------------------

                        if (
                            route.recommended
                        ) {

                            routeCard.classList.add(
                                "recommended"
                            );

                        }


                        // ------------------------------------------------
                        // FASTEST ROUTE
                        // ------------------------------------------------

                        const isFastest =
                            fastestRoute &&
                            route.routeId ===
                            fastestRoute.routeId;


                        // ------------------------------------------------
                        // SAFEST ROUTE
                        // ------------------------------------------------

                        const isSafest =
                            safestRoute &&
                            route.routeId ===
                            safestRoute.routeId;


                        // ------------------------------------------------
                        // SAFETY CATEGORY CLASS
                        // ------------------------------------------------

                        let categoryClass =
                            "category-low";


                        if (
                            route.safetyCategory ===
                            "MODERATE_REPORTED_CONCERN"
                        ) {

                            categoryClass =
                                "category-moderate";

                        }


                        if (
                            route.safetyCategory ===
                            "HIGHER_REPORTED_CONCERN"
                        ) {

                            categoryClass =
                                "category-high";

                        }


                        // ------------------------------------------------
                        // SAFETY CATEGORY TEXT
                        // ------------------------------------------------

                        const categoryText =
                            route.safetyCategory
                                ? route.safetyCategory.replaceAll(
                                    "_",
                                    " "
                                )
                                : "LOW CONCERN";


                        // ------------------------------------------------
                        // BADGES
                        // ------------------------------------------------

                        let badgesHTML =
                            "";


                        if (isFastest) {

                            badgesHTML += `
                                <span class="route-badge fastest-badge">
                                    🏎️ Fastest
                                </span>
                            `;

                        }


                        if (isSafest) {

                            badgesHTML += `
                                <span class="route-badge safest-badge">
                                    🛡️ Safest
                                </span>
                            `;

                        }


                        if (route.recommended) {

                            badgesHTML += `
                                <span class="route-badge recommended-badge">
                                    ⭐ Recommended
                                </span>
                            `;

                        }


                        // ------------------------------------------------
                        // ROUTE CARD
                        // ------------------------------------------------

                        routeCard.innerHTML = `

                            <div class="route-top">

                                <div class="route-name">

                                    ${escapeHtml(
                            route.routeName
                        )}

                                </div>


                                <div class="route-badges">

                                    ${badgesHTML}

                                </div>

                            </div>


                            <div class="route-stats">

                                <div class="stat">

                                    <span class="stat-label">
                                        Travel time
                                    </span>

                                    <span class="stat-value">
                                        ${route.estimatedDurationMinutes}
                                        min
                                    </span>

                                </div>


                                <div class="stat">

                                    <span class="stat-label">
                                        Distance
                                    </span>

                                    <span class="stat-value">
                                        ${route.distanceMeters}
                                        m
                                    </span>

                                </div>


                                <div class="stat">

                                    <span class="stat-label">
                                        Concern score
                                    </span>

                                    <span class="stat-value">
                                        ${route.reportedConcernScore}
                                    </span>

                                </div>

                            </div>


                            <div
                                class="safety-category ${categoryClass}"
                            >

                                ${categoryText}

                            </div>


                            <p class="route-explanation">

                                ${escapeHtml(
                            route.explanation
                        )}

                            </p>

                        `;


                        routeList.appendChild(
                            routeCard
                        );

                    }
                );


            } else {

                routeList.innerHTML = `

                    <div class="empty-state">

                        <h3>
                            No routes found
                        </h3>

                        <p>
                            Try searching for other
                            locations in India.
                        </p>

                    </div>

                `;

            }


        } catch (error) {

            routeList.innerHTML = `

                <div class="error-message">

                    Error:
                    ${escapeHtml(
                error.message
            )}

                </div>

            `;


            console.error(
                "Route recommendation error:",
                error
            );


        } finally {

            findRoutesButton.disabled =
                false;


            findRoutesButton.textContent =
                "Find safer routes";

        }

    }
);


// ============================================================
// CROWD-SOURCED SAFETY REPORTING
// ============================================================

const submitReportButton =
    document.getElementById(
        "submitReport"
    );


const reportMessage =
    document.getElementById(
        "reportMessage"
    );


if (submitReportButton) {

    submitReportButton.addEventListener(
        "click",
        async () => {

            const latitude =
                document.getElementById(
                    "reportLatitude"
                ).value;


            const longitude =
                document.getElementById(
                    "reportLongitude"
                ).value;


            const streetName =
                document.getElementById(
                    "reportStreetName"
                ).value;


            const areaName =
                document.getElementById(
                    "reportAreaName"
                ).value;


            const incidentType =
                document.getElementById(
                    "incidentType"
                ).value;


            const severity =
                document.getElementById(
                    "severity"
                ).value;


            const reportDate =
                document.getElementById(
                    "reportDate"
                ).value;


            const reportTime =
                document.getElementById(
                    "reportTime"
                ).value;


            const description =
                document.getElementById(
                    "reportDescription"
                ).value.trim();


            if (
                !latitude ||
                !longitude
            ) {

                showReportMessage(
                    "Please choose the affected location on the map.",
                    true
                );

                return;

            }


            if (!streetName) {

                showReportMessage(
                    "Please select a valid location on the map.",
                    true
                );

                return;

            }


            if (!incidentType) {

                showReportMessage(
                    "Please select an issue type.",
                    true
                );

                return;

            }


            if (
                !reportDate ||
                !reportTime
            ) {

                showReportMessage(
                    "Please select the date and time.",
                    true
                );

                return;

            }


            if (!description) {

                showReportMessage(
                    "Please enter a short description.",
                    true
                );

                return;

            }


            const occurredAt =
                `${reportDate}T${reportTime}`;


            try {

                submitReportButton.disabled =
                    true;


                submitReportButton.textContent =
                    "Finding street...";


                const segmentResponse =
                    await fetch(
                        "/api/street-segments/resolve",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify({

                                    latitude:
                                        Number(
                                            latitude
                                        ),

                                    longitude:
                                        Number(
                                            longitude
                                        ),

                                    streetName:
                                    streetName,

                                    areaName:
                                    areaName

                                })
                        }
                    );


                if (!segmentResponse.ok) {

                    let errorMessage =
                        "Could not create the street segment.";


                    try {

                        const errorData =
                            await segmentResponse.json();


                        if (
                            errorData.message
                        ) {

                            errorMessage =
                                errorData.message;

                        }

                    } catch (error) {

                    }


                    throw new Error(
                        errorMessage
                    );

                }


                const segment =
                    await segmentResponse.json();


                const reportData = {

                    streetSegmentId:
                        Number(
                            segment.id
                        ),

                    incidentType:
                    incidentType,

                    severity:
                        Number(
                            severity
                        ),

                    occurredAt:
                    occurredAt,

                    description:
                    description

                };


                submitReportButton.textContent =
                    "Submitting...";


                const response =
                    await fetch(
                        "/api/safety-reports",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify(
                                    reportData
                                )

                        }
                    );


                if (!response.ok) {

                    let errorMessage =
                        "Unable to submit the report.";


                    try {

                        const errorData =
                            await response.json();


                        if (
                            errorData.message
                        ) {

                            errorMessage =
                                errorData.message;

                        }

                    } catch (error) {

                    }


                    throw new Error(
                        errorMessage
                    );

                }


                await response.json();


                showReportMessage(
                    "Report successfully recorded. Thank you for making your community safer.",
                    false
                );


                document.getElementById(
                    "incidentType"
                ).value =
                    "";


                document.getElementById(
                    "severity"
                ).value =
                    "3";


                document.getElementById(
                    "reportDescription"
                ).value =
                    "";


                document.getElementById(
                    "reportLatitude"
                ).value =
                    "";


                document.getElementById(
                    "reportLongitude"
                ).value =
                    "";


                document.getElementById(
                    "reportStreetName"
                ).value =
                    "";


                document.getElementById(
                    "reportAreaName"
                ).value =
                    "";


                selectedReportLocation.textContent =
                    "No location selected";


                selectedReportLocation.classList.remove(
                    "active"
                );


                if (reportLocationMarker) {

                    map.removeLayer(
                        reportLocationMarker
                    );


                    reportLocationMarker =
                        null;

                }


            } catch (error) {

                console.error(
                    "Safety report error:",
                    error
                );


                showReportMessage(
                    error.message ||
                    "Failed to submit safety report.",
                    true
                );


            } finally {

                submitReportButton.disabled =
                    false;


                submitReportButton.textContent =
                    "Submit anonymous report";

            }

        }
    );

}


// ============================================================
// STREETLIGHT REPORTING
// ============================================================

if (submitStreetlightReportButton) {

    submitStreetlightReportButton.addEventListener(
        "click",
        async () => {

            // ------------------------------------------------
            // GET LOCATION
            // ------------------------------------------------

            const latitude =
                document.getElementById(
                    "streetlightLatitude"
                ).value;


            const longitude =
                document.getElementById(
                    "streetlightLongitude"
                ).value;


            const streetName =
                document.getElementById(
                    "streetlightStreetName"
                ).value;


            const areaName =
                document.getElementById(
                    "streetlightAreaName"
                ).value;


            // ------------------------------------------------
            // GET FORM VALUES
            // ------------------------------------------------

            const condition =
                document.getElementById(
                    "streetlightCondition"
                ).value;


            const notes =
                document.getElementById(
                    "streetlightNotes"
                ).value.trim();


            // ------------------------------------------------
            // VALIDATE LOCATION
            // ------------------------------------------------

            if (
                !latitude ||
                !longitude
            ) {

                showStreetlightMessage(
                    "Please choose the streetlight location on the map.",
                    true
                );

                return;

            }


            if (!streetName) {

                showStreetlightMessage(
                    "Please select a valid streetlight location.",
                    true
                );

                return;

            }


            // ------------------------------------------------
            // VALIDATE CONDITION
            // ------------------------------------------------

            if (!condition) {

                showStreetlightMessage(
                    "Please select the streetlight condition.",
                    true
                );

                return;

            }


            try {

                submitStreetlightReportButton.disabled =
                    true;


                submitStreetlightReportButton.textContent =
                    "Saving observation...";


                // ====================================================
                // SAVE COMMUNITY STREETLIGHT OBSERVATION
                // ====================================================

                const response =
                    await fetch(
                        "/api/streetlights",
                        {
                            method: "POST",

                            headers: {
                                "Content-Type":
                                    "application/json"
                            },

                            body:
                                JSON.stringify({

                                    latitude:
                                        Number(
                                            latitude
                                        ),

                                    longitude:
                                        Number(
                                            longitude
                                        ),

                                    streetName:
                                    streetName,

                                    areaName:
                                    areaName,

                                    condition:
                                    condition,

                                    notes:
                                    notes

                                })
                        }
                    );


                if (!response.ok) {

                    let errorMessage =
                        "Unable to save the streetlight observation.";


                    try {

                        const errorData =
                            await response.json();


                        if (
                            errorData.message
                        ) {

                            errorMessage =
                                errorData.message;

                        }

                    } catch (error) {

                        // Ignore JSON parsing errors

                    }


                    throw new Error(
                        errorMessage
                    );

                }


                const savedStreetlight =
                    await response.json();


                // ====================================================
                // SUCCESS
                // ====================================================

                showStreetlightMessage(
                    "Streetlight observation recorded successfully.",
                    false
                );


                // ====================================================
                // REMOVE TEMPORARY LOCATION MARKER
                // ====================================================

                if (
                    streetlightReportLocationMarker
                ) {

                    map.removeLayer(
                        streetlightReportLocationMarker
                    );


                    streetlightReportLocationMarker =
                        null;

                }


                // ====================================================
                // RESET FORM
                // ====================================================

                document.getElementById(
                    "streetlightLatitude"
                ).value =
                    "";


                document.getElementById(
                    "streetlightLongitude"
                ).value =
                    "";


                document.getElementById(
                    "streetlightStreetName"
                ).value =
                    "";


                document.getElementById(
                    "streetlightAreaName"
                ).value =
                    "";


                document.getElementById(
                    "streetlightCondition"
                ).value =
                    "";


                document.getElementById(
                    "streetlightNotes"
                ).value =
                    "";


                selectedStreetlightLocation.textContent =
                    "No location selected";


                selectedStreetlightLocation.classList.remove(
                    "active"
                );


                // ====================================================
                // SHOW NEW MARKER IMMEDIATELY
                // ====================================================

                addStreetlightMarker(
                    savedStreetlight
                );


            } catch (error) {

                console.error(
                    "Streetlight report error:",
                    error
                );


                showStreetlightMessage(
                    error.message ||
                    "Failed to save streetlight observation.",
                    true
                );


            } finally {

                submitStreetlightReportButton.disabled =
                    false;


                submitStreetlightReportButton.innerHTML = `
                    <span>💡</span>

                    Submit lighting observation

                    <span class="button-arrow">
                        →
                    </span>
                `;

            }

        }
    );

}


// ============================================================
// SHOW STREETLIGHT MESSAGE
// ============================================================

function showStreetlightMessage(
    text,
    isError
) {

    if (!streetlightMessage) {

        return;

    }


    streetlightMessage.style.display =
        "block";


    streetlightMessage.textContent =
        text;


    streetlightMessage.className =
        "report-message " +
        (
            isError
                ? "error-message"
                : ""
        );

}


// ============================================================
// SHOW REPORT MESSAGE
// ============================================================

function showReportMessage(
    text,
    isError
) {

    if (!reportMessage) {

        return;

    }


    reportMessage.style.display =
        "block";


    reportMessage.textContent =
        text;


    reportMessage.className =
        "report-message " +
        (
            isError
                ? "error-message"
                : ""
        );

}