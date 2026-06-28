package com.hexa.map

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hexa.HexaApplication
import com.hexa.core.geo.LatLng
import com.hexa.location.ChaseCameraConfig
import com.hexa.location.PositionSource
import com.hexa.player.PlacedBuilding
import com.hexa.world.TileContent
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.AnnotatedFeature
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.ViewAnnotationOptions
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.rememberMapState
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.generated.GesturesSettings
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Durée d'interpolation de la caméra vers chaque nouvelle pose. Proche de la cadence d'échantillonnage
 * du cap (~100 ms) : assez courte pour que chaque animation aboutisse avant la suivante (sinon
 * l'ease-in-out, sans cesse relancé, figerait la caméra) et donne un suivi fluide.
 */
private const val FOLLOW_EASE_MS = 200L

/** Nanosecondes par milliseconde — convertit le temps de frame ([withFrameNanos]) en millisecondes. */
private const val NANOS_PER_MS = 1_000_000.0

/**
 * Écran carte : la **caméra de poursuite** à la troisième personne.
 *
 * La permission de localisation **et** le premier fix GPS sont garantis **en amont** par la machine à
 * états de démarrage ([com.hexa.startup.startupStage], cf. [com.hexa.MainActivity]) : cet écran n'est
 * composé qu'une fois la position du joueur connue, ce qui permet d'amorcer le viewport directement
 * sur lui (jamais de centre arbitraire visible).
 *
 * @param placedBuildings flux des bâtiments posés, rendus en **modèles 3D** sur la carte (cf.
 *   [com.hexa.player.PlayerViewModel.placedBuildings]).
 * @param extractorStock flux du nombre d'extracteurs prêts à poser, qui conditionne l'apparition du
 *   marqueur « + » sur la tuile courante (cf. [com.hexa.player.PlayerViewModel.extractorStock]).
 * @param onPlaceExtracteur pose un extracteur sur la tuile dont l'index H3 est fourni.
 */
@Composable
fun MapScreen(
    placedBuildings: Flow<List<PlacedBuilding>>,
    extractorStock: StateFlow<Int>,
    onPlaceExtracteur: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChaseCameraMap(
        placedBuildings = placedBuildings,
        extractorStock = extractorStock,
        onPlaceExtracteur = onPlaceExtracteur,
        modifier = modifier,
    )
}

/**
 * Carte Mapbox plein écran avec **caméra de poursuite à la troisième personne**.
 *
 * La caméra suit la position GPS filtrée fournie par [ChaseCameraViewModel], inclinée et **centrée en
 * permanence sur le joueur**. Le cap n'est plus piloté par la boussole (retiré en #96) : un **glisser
 * à un doigt** fait pivoter la caméra autour du joueur (cf. [detectDragRotation]), le joueur restant
 * au centre. Pan et inclinaison manuels restent désactivés ; le zoom au pincement reste actif et borné
 * à [MapConfig.MIN_ZOOM]–[MapConfig.MAX_ZOOM].
 *
 * Le token public est fourni au SDK en amont (cf. [com.hexa.MainActivity]).
 */
@Composable
private fun ChaseCameraMap(
    placedBuildings: Flow<List<PlacedBuilding>>,
    extractorStock: StateFlow<Int>,
    onPlaceExtracteur: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as HexaApplication
    // Seuil de désambiguïsation tap ↔ rotation (px) : en-deçà, le geste reste un tap (inspection) ;
    // au-delà, le geste bascule en rotation de la caméra (cf. [detectDragRotation]).
    val touchSlopPx = LocalViewConfiguration.current.touchSlop
    // Référence native de la carte, captée par un MapEffect, dont le glisser de rotation se sert pour
    // piloter la caméra en direct ; `null` jusqu'à la première composition de la carte.
    var mapboxMap by remember { mutableStateOf<MapboxMap?>(null) }
    // Rotation au glisser en cours : tant qu'elle dure, le suivi GPS est suspendu (le geste est seul
    // maître de la caméra), pour que le cap n'oscille pas entre le geste et la pose suivie.
    var rotating by remember { mutableStateOf(false) }
    // Taille de la vue carte (px), captée par `onSizeChanged` ; son centre sert de point focal des
    // gestes (cf. le `LaunchedEffect` plus bas).
    var mapViewSize by remember { mutableStateOf(IntSize.Zero) }
    val viewModel: ChaseCameraViewModel =
        viewModel(factory = chaseCameraViewModelFactory(app.sharedPositionSource))
    val gridViewModel: HexGridViewModel =
        viewModel(factory = hexGridViewModelFactory(app.sharedCurrentTile, app.sharedGrid))
    val inspectionViewModel: TileInspectionViewModel =
        viewModel(
            factory = tileInspectionViewModelFactory(
                app.sharedGrid,
                app.sharedWorldGenerator::contentOf,
                app.sharedCurrentTile,
            ),
        )

    val camera by viewModel.cameraState.collectAsStateWithLifecycle()
    val gridCells by gridViewModel.cells.collectAsStateWithLifecycle()
    val inspection by inspectionViewModel.inspection.collectAsStateWithLifecycle()

    // Bâtiments posés → placements de modèles 3D : la résolution cellule → centre (H3 natif) passe par
    // l'intégration partagée de l'application, la même que la grille et l'inspection.
    val buildings by placedBuildings.collectAsStateWithLifecycle(initialValue = emptyList())
    val placements = remember(buildings) { buildingPlacements(buildings, app.centerOfCell) }

    // Pose d'un extracteur : la tuile courante porte un marqueur « + » dès qu'elle est libre et qu'il
    // reste du stock. La décision est purement dérivée de l'état observé (cf. extractorPlacementCell) ;
    // un tap sur le marqueur ouvre la liste des bâtiments à poser.
    val currentTile by app.sharedCurrentTile.collectAsStateWithLifecycle()
    val stock by extractorStock.collectAsStateWithLifecycle()
    val placementCell =
        remember(currentTile, buildings, stock) {
            extractorPlacementCell(currentTile, buildings, stock, app.sharedGrid::toH3String)
        }
    var placementSheetOpen by rememberSaveable { mutableStateOf(false) }

    // Position de l'avatar : la **même** position GPS filtrée partagée que la caméra (une seule
    // trajectoire), suivie indépendamment de la pose de poursuite.
    val avatarPosition by remember { app.sharedPositionSource.positions() }
        .collectAsStateWithLifecycle(initialValue = null)

    // La grille suit le palier d'anneaux du zoom de poursuite courant (et du zoom au pincement, qui
    // se répercute sur la pose).
    LaunchedEffect(camera?.zoomLevel) {
        camera?.zoomLevel?.let(gridViewModel::onZoomChanged)
    }

    // Viewport amorcé **directement sur le joueur** : cet écran n'étant composé qu'une fois le premier
    // fix connu (cf. machine à états de démarrage), la position partagée est déjà disponible, et la
    // carte s'affiche centrée sur lui sans glisser depuis un centre de repli. [MapConfig.DEFAULT_CENTER]
    // ne sert que de garde défensive si la valeur manquait — jamais montré en pratique.
    val initialCenter = app.premierFix.value
        ?: LatLng(MapConfig.DEFAULT_CENTER_LAT, MapConfig.DEFAULT_CENTER_LON)
    val viewportState =
        rememberMapViewportState {
            setCameraOptions {
                center(Point.fromLngLat(initialCenter.lngDeg, initialCenter.latDeg))
                zoom(MapConfig.DEFAULT_ZOOM)
            }
        }

    // Caméra centrée sur le joueur : on coupe via les **réglages de gestes** tout geste natif qui la
    // décentrerait ou la ferait « sauter » (cf. [playerLockedGestures]). Seul le pincement à deux
    // doigts subsiste pour zoomer ; la **rotation au glisser** est fournie à part par un `pointerInput`
    // posé sur la carte (cf. [detectDragRotation]). Les réglages **doivent** passer par [MapState] : le
    // wrapper Compose les applique et les ré-applique en continu, alors qu'un `gestures.updateSettings`
    // ponctuel serait réinitialisé.
    val mapState = rememberMapState { gesturesSettings = playerLockedGestures() }

    // Point focal des gestes = centre de la vue (le joueur) dès que sa taille est connue, et à chaque
    // changement (ex. rotation d'écran) : le pincement zoome alors **vers le joueur**, pas vers les
    // doigts, et la caméra ne glisse plus pour le recentrer après coup.
    LaunchedEffect(mapViewSize) {
        if (mapViewSize != IntSize.Zero) {
            mapState.gesturesSettings = playerLockedGestures(
                ScreenCoordinate(mapViewSize.width / 2.0, mapViewSize.height / 2.0),
            )
        }
    }

    Box(modifier = modifier) {
        MapboxMap(
            // Rotation au glisser posée **sur la carte elle-même** (pas un overlay frère) : un glisser à
            // un doigt qui tourne autour du joueur fait pivoter la caméra (cf. [detectDragRotation]).
            // Sous le seuil [touchSlopPx], rien n'est consommé → le tap descend naturellement à Mapbox
            // (inspection de tuile, marqueur « + ») ; au-delà, il est consommé (le tap est annulé). La
            // caméra est pilotée **en direct** pendant le geste (comme un geste natif), le cap n'étant
            // persisté dans la pose qu'au relâcher → pas de tempête de recomposition par frame.
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { mapViewSize = it }
                .pointerInput(viewModel, mapboxMap) {
                    val map = mapboxMap ?: return@pointerInput
                    detectDragRotation(
                        mapboxMap = map,
                        touchSlopPx = touchSlopPx,
                        onRotatingChange = { rotating = it },
                        onBearingCommitted = viewModel::onUserBearing,
                    )
                },
            mapViewportState = viewportState,
            mapState = mapState,
            style = { MapStyle(style = MapConfig.STYLE_URL) },
            // Ornements parasites masqués par lambda vide : la barre d'échelle n'apporte rien au jeu,
            // et la boussole est masquée par choix produit (#96 : pas de retour-au-nord, la rotation au
            // glisser reste libre). Logo et attribution Mapbox laissés au défaut → restent affichés (CGU Mapbox).
            scaleBar = {},
            compass = {},
            // Un tap ouvre l'inspection de la tuile touchée : Mapbox fournit la coordonnée carte du
            // point, le ViewModel en résout la cellule H3 et en recalcule le contenu à la volée. Le
            // clic passe par le paramètre du composable (et non le plugin gestures, que le wrapper
            // Compose réinitialise) ; `true` consomme l'événement.
            onMapClickListener = OnMapClickListener { point ->
                inspectionViewModel.inspectAt(LatLng(latDeg = point.latitude(), lngDeg = point.longitude()))
                true
            },
        ) {
            // Suivi : à chaque nouvelle pose, glisser la caméra vers elle (centre, zoom, **cap**) ; tant
            // que la première pose n'est pas connue (pose nulle, avant le premier fix), ne rien imposer.
            // Le cap, persisté au relâcher du glisser, est ré-appliqué ici pour survivre au suivi GPS.
            // **Suspendu pendant une rotation** ([rotating]) : le geste est alors seul maître de la
            // caméra, sinon chaque point GPS ré-imposerait l'ancien cap et le ferait osciller.
            MapEffect(camera, rotating) { mapView ->
                if (rotating) return@MapEffect
                val pose = camera ?: return@MapEffect
                mapView.mapboxMap.easeTo(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(pose.center.lngDeg, pose.center.latDeg))
                        .zoom(pose.zoomLevel)
                        .pitch(pose.pitchDeg)
                        .bearing(pose.bearingDeg)
                        .build(),
                    MapAnimationOptions.Builder().duration(FOLLOW_EASE_MS).build(),
                )
            }
            // Redessine la grille hexagonale à chaque nouvel ensemble de cellules (changement de
            // tuile courante ou de palier de zoom) ; la source GeoJSON n'est créée qu'une fois.
            MapEffect(gridCells) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showHexGrid(gridCells) }
            }
            // Redessine les modèles 3D des bâtiments à chaque changement de la liste des placements
            // (apparition d'un bâtiment, changement de tuile) ; la source/couche n'est créée qu'une fois.
            MapEffect(placements) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showBuildingModels(placements) }
            }
            // Repose le modèle de l'avatar à chaque nouvelle position lissée ; la source/couche n'est
            // créée qu'une fois, ensuite seul le point est réinjecté (suivi fluide en marche).
            MapEffect(avatarPosition) { mapView ->
                mapView.mapboxMap.getStyle { style -> style.showAvatar(avatarPosition) }
            }
            // Driver de flottement (effet fantôme) : à chaque frame, recalcule le décalage vertical
            // sinusoïdal et le ré-applique au seul Z du modèle, **indépendamment** de la position GPS
            // ci-dessus. Le flottement tourne donc en boucle, joueur immobile ou en marche. La boucle
            // est portée par un MapEffect (accès au style + horloge de frame Compose) et s'annule
            // proprement quand l'écran quitte la composition.
            MapEffect(Unit) { mapView ->
                val startNanos = withFrameNanos { it }
                while (true) {
                    val elapsedMs = (withFrameNanos { it } - startNanos) / NANOS_PER_MS
                    val offsetM = avatarFloatOffsetMeters(
                        elapsedMs = elapsedMs,
                        amplitudeM = MapConfig.AVATAR_FLOAT_AMPLITUDE_M,
                        periodMs = MapConfig.AVATAR_FLOAT_PERIOD_MS.toDouble(),
                    )
                    mapView.mapboxMap.getStyle { style -> style.floatAvatar(offsetM) }
                }
            }
            // Marqueur « + » de pose, ancré au centre de la tuile courante tant qu'une pose est possible
            // ([placementCell] non nul). Un tap ouvre la liste des bâtiments ; la pose le fait disparaître
            // (tuile devenue occupée / stock épuisé), la décision étant recalculée à chaque émission.
            placementCell?.let { cell ->
                val center = app.centerOfCell(cell)
                ViewAnnotation(
                    options = ViewAnnotationOptions.Builder()
                        .annotatedFeature(AnnotatedFeature(Point.fromLngLat(center.lngDeg, center.latDeg)))
                        .allowOverlap(true)
                        .build(),
                ) {
                    ExtractorPlacementMarker(onClick = { placementSheetOpen = true })
                }
            }
            // Initialisation native one-time : on capture la référence [MapboxMap] (dont le glisser de
            // rotation se sert pour piloter la caméra en direct), on borne durement le zoom à
            // [MIN_ZOOM, MAX_ZOOM] (aucun pincement n'en sort) et on écoute le pincement pour répercuter
            // le zoom dans la pose. Le verrou des gestes natifs (pan, rotation native…) vit, lui, dans
            // `mapState.gesturesSettings` ci-dessus.
            MapEffect(Unit) { mapView ->
                mapboxMap = mapView.mapboxMap
                mapView.mapboxMap.setBounds(
                    CameraBoundsOptions.Builder()
                        .minZoom(MapConfig.MIN_ZOOM)
                        .maxZoom(MapConfig.MAX_ZOOM)
                        .build(),
                )
                mapView.gestures.addOnScaleListener(
                    object : OnScaleListener {
                        override fun onScaleBegin(detector: StandardScaleGestureDetector) = Unit

                        override fun onScale(detector: StandardScaleGestureDetector) = Unit

                        // Le pincement ajuste le zoom sans décentrer ; on répercute la valeur (bornée
                        // par le contrôleur) dans la pose pour qu'elle persiste à la prochaine poursuite.
                        override fun onScaleEnd(detector: StandardScaleGestureDetector) =
                            viewModel.onUserZoom(mapView.mapboxMap.cameraState.zoom)
                    },
                )
            }
        }

        inspection?.let { open ->
            TileInspectionSheet(inspection = open, onDismiss = inspectionViewModel::dismiss)
        }

        // Liste des bâtiments à poser, ouverte par le marqueur « + ». Bornée à une pose possible
        // ([placementCell] non nul) : la pose referme le panneau et le marqueur s'efface de lui-même.
        if (placementSheetOpen) {
            placementCell?.let { cell ->
                BuildingPlacementSheet(
                    extractorStock = stock,
                    onPlaceExtracteur = {
                        onPlaceExtracteur(cell)
                        placementSheetOpen = false
                    },
                    onDismiss = { placementSheetOpen = false },
                )
            }
        }
    }
}

/**
 * Réglages de gestes d'une caméra **verrouillée sur le joueur** : tous les gestes natifs qui la
 * décentreraient, la désorienteraient ou la feraient « sauter » sont coupés (pan, pan au pincement,
 * rotation native, inclinaison, zooms double-tap / quick-zoom). Seul le pincement à deux doigts
 * subsiste pour zoomer.
 *
 * @param focal point focal des gestes (centre de rotation/zoom). Fixé au centre de la vue (le joueur),
 *   le pincement zoome **vers le joueur** ; `null` (taille de vue encore inconnue) → focal du geste.
 */
private fun playerLockedGestures(focal: ScreenCoordinate? = null) = GesturesSettings {
    scrollEnabled = false
    pinchScrollEnabled = false
    rotateEnabled = false
    pitchEnabled = false
    doubleTapToZoomInEnabled = false
    doubleTouchToZoomOutEnabled = false
    quickZoomEnabled = false
    focalPoint = focal
}

/**
 * Détecte le glisser à un doigt qui fait **pivoter la caméra** autour du joueur, en distinguant le
 * **tap** de la **rotation** par le seuil [touchSlopPx] :
 * - tant que le doigt n'a pas dépassé [touchSlopPx], rien n'est consommé → un simple tap descend à
 *   Mapbox (inspection de tuile, marqueur « + ») ;
 * - dès le seuil franchi, le geste bascule en rotation : il est **consommé** (le tap est annulé) et le
 *   doigt qui tourne **autour du centre** (le joueur) pilote le cap (cf. [CameraRotationGesture]).
 *
 * La caméra est pilotée **en direct** sur [mapboxMap] (comme un geste natif), sans round-trip par
 * l'état Compose. Pendant toute la rotation, [onRotatingChange]`(true)` **suspend le suivi GPS** (cf.
 * `MapEffect` de pose) : sinon, à chaque point GPS le suivi ré-appliquerait l'ancien cap et le ferait
 * osciller (carte qui flippe, avatar qui clignote). Le cap final n'est persisté dans la pose qu'au
 * **relâcher**, via [onBearingCommitted] (puis [onRotatingChange]`(false)` rend la main au suivi). Un
 * second doigt (pincement) suspend la rotation pour laisser Mapbox zoomer.
 *
 * @param mapboxMap carte native, dont on lit/écrit le cap directement pendant le geste.
 * @param touchSlopPx distance (px) au-delà de laquelle le glisser cesse d'être un tap.
 * @param onRotatingChange notifie le début (`true`) et la fin (`false`) d'une rotation effective.
 * @param onBearingCommitted reçoit le cap final (déjà normalisé) au relâcher, pour le persister.
 */
private suspend fun PointerInputScope.detectDragRotation(
    mapboxMap: MapboxMap,
    touchSlopPx: Float,
    onRotatingChange: (Boolean) -> Unit,
    onBearingCommitted: (Double) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        var engaged = false
        var startBearingDeg = 0.0
        var startAngleDeg = 0.0
        var bearingDeg = 0.0
        while (true) {
            val event = awaitPointerEvent()
            val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!pointer.pressed) break
            // Pincement (≥ 2 doigts) : laisser Mapbox zoomer, ne pas engager ni maintenir la rotation.
            if (event.changes.count { it.pressed } > 1) continue
            val angleDeg = CameraRotationGesture.angleDeg(centerX, centerY, pointer.position.x, pointer.position.y)
            if (!engaged && (pointer.position - down.position).getDistance() > touchSlopPx) {
                engaged = true
                onRotatingChange(true)
                startBearingDeg = mapboxMap.cameraState.bearing
                startAngleDeg = angleDeg
            }
            if (engaged) {
                bearingDeg = CameraRotationGesture.rotate(startBearingDeg, startAngleDeg, angleDeg)
                mapboxMap.setCamera(CameraOptions.Builder().bearing(bearingDeg).build())
                pointer.consume()
            }
        }
        if (engaged) {
            onBearingCommitted(bearingDeg)
            onRotatingChange(false)
        }
    }
}

/**
 * Fabrique le [ChaseCameraViewModel] en câblant la position GPS filtrée **partagée**
 * ([positionSource], cf. [HexaApplication.sharedPositionSource]) avec les réglages de [MapConfig]. Le
 * cap n'est plus piloté par la boussole (retiré en #96) : il part au nord et est piloté par le
 * glisser de l'utilisateur. La permission de localisation est garantie en amont par
 * [LocationPermissionGate].
 */
private fun chaseCameraViewModelFactory(positionSource: PositionSource) = viewModelFactory {
    initializer {
        ChaseCameraViewModel(
            positionSource = positionSource,
            config = ChaseCameraConfig(
                pitchDeg = MapConfig.PITCH,
                followZoom = MapConfig.FOLLOW_ZOOM,
                minZoom = MapConfig.MIN_ZOOM,
                maxZoom = MapConfig.MAX_ZOOM,
            ),
        )
    }
}

/**
 * Fabrique le [HexGridViewModel] en câblant la **tuile courante partagée** ([currentTile]) et
 * l'intégration H3 de production partagée ([grid]) — les mêmes que celles servies à l'inspection de
 * tuile, pour un suivi unique.
 */
private fun hexGridViewModelFactory(currentTile: StateFlow<Long?>, grid: HexGrid) = viewModelFactory {
    initializer {
        HexGridViewModel(currentTile = currentTile, grid = grid)
    }
}

/**
 * Fabrique le [TileInspectionViewModel] en câblant la **même** intégration H3 partagée ([grid]), le
 * **même** générateur de monde partagé ([contentOf], cf. [com.hexa.HexaApplication.sharedWorldGenerator])
 * et la **même** tuile courante partagée ([currentTile]) que la grille : la résolution tap → cellule,
 * le contenu de tuile et la récolte partagent une seule façade native et un seul générateur.
 */
private fun tileInspectionViewModelFactory(
    grid: HexGrid,
    contentOf: (Long) -> TileContent,
    currentTile: StateFlow<Long?>,
) = viewModelFactory {
    initializer {
        TileInspectionViewModel(grid = grid, contentOf = contentOf, currentTile = currentTile)
    }
}
