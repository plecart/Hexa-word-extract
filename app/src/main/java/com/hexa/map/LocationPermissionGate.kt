package com.hexa.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hexa.R

/**
 * Porte d'accès à la localisation : n'affiche [granted] que lorsque `ACCESS_FINE_LOCATION` est
 * accordée.
 *
 * Au premier affichage, demande la permission. En cas de refus, présente un état explicite (message
 * + bouton « Réessayer ») : le bouton **re-demande** la permission, ou **ouvre les réglages système**
 * si le refus est devenu permanent (re-demander serait alors sans effet). La permission est
 * re-vérifiée au retour au premier plan, pour prendre en compte un accord donné depuis les réglages.
 *
 * @param granted contenu affiché une fois la permission accordée (la carte de poursuite).
 */
@Composable
fun LocationPermissionGate(modifier: Modifier = Modifier, granted: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasFineLocationPermission()) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasPermission = isGranted
        }

    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermission = context.hasFineLocationPermission()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasPermission) {
        granted()
    } else {
        LocationDenied(
            modifier = modifier,
            onRetry = {
                val activity = context.findActivity()
                val permanentlyDenied =
                    activity != null &&
                        !activity.shouldShowRequestPermissionRationale(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                if (permanentlyDenied) {
                    context.openAppSettings()
                } else {
                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
        )
    }
}

@Composable
private fun LocationDenied(modifier: Modifier, onRetry: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.location_permission_rationale),
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.location_permission_retry))
            }
        }
    }
}

/** Vrai si `ACCESS_FINE_LOCATION` est déjà accordée. */
private fun Context.hasFineLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Ouvre l'écran de réglages de l'app, pour réactiver une permission refusée définitivement. */
private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Remonte la chaîne de `ContextWrapper` jusqu'à l'`Activity` hôte, ou `null` si introuvable. */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
