package fr.bonobo.privacy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF07111F)
private val Surface = Color(0xFF101D30)
private val Cyan = Color(0xFF42D9FF)
private val Green = Color(0xFF25F2AF)
private val Violet = Color(0xFF9B7BFF)
private val Soft = Color(0xFFB8C5D9)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BonoboPrivacyApp() }
    }
}

data class SensitiveApp(
    val label: String,
    val packageName: String,
    val declaredPermissions: List<String>,
    val grantedPermissions: List<String>,
    val icon: Drawable?
)

@Composable
private fun BonoboPrivacyApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableStateOf("home") }
    var auditedApps by remember { mutableStateOf(emptyList<SensitiveApp>()) }
    LaunchedEffect(Unit) { auditedApps = scanSensitiveApps(context) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Violet, background = Night, surface = Surface)) {
        Surface(color = Night, modifier = Modifier.fillMaxSize()) {
            when (page) {
                "audit" -> AuditScreen(auditedApps, onBack = { page = "home" }, onOpenApp = { openAppSettings(context, it) })
                "silence" -> SilenceScreen(onBack = { page = "home" }, onOpenPrivacy = { context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) })
                "microphone" -> SensorDetailScreen("Microphone", "Applications qui ont déclaré l’accès au microphone", appsFor(auditedApps, "Microphone"), onBack = { page = "home" }, onOpenApp = { openAppSettings(context, it) })
                "camera" -> SensorDetailScreen("Caméra", "Applications qui ont déclaré l’accès à la caméra", appsFor(auditedApps, "Caméra"), onBack = { page = "home" }, onOpenApp = { openAppSettings(context, it) })
                "location" -> SensorDetailScreen("Localisation", "Applications qui ont déclaré l’accès à votre position", appsFor(auditedApps, "Position"), onBack = { page = "home" }, onOpenApp = { openAppSettings(context, it) })
                "clipboard" -> ClipboardScreen(onBack = { page = "home" }, onOpenPrivacy = { context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS)) })
                else -> HomeScreen(auditedApps, onAudit = { page = "audit" }, onSilence = { page = "silence" }, onSensor = { page = it })
            }
        }
    }
}

@Composable
private fun HomeScreen(apps: List<SensitiveApp>, onAudit: () -> Unit, onSilence: () -> Unit, onSensor: (String) -> Unit) {
    val microphone = apps.count { it.grantedPermissions.contains("Microphone") }
    val camera = apps.count { it.grantedPermissions.contains("Caméra") }
    val location = apps.count { it.grantedPermissions.contains("Position") }
    val score = privacyScore(microphone, camera, location)
    LazyColumn(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Security, null, tint = Green, modifier = Modifier.size(31.dp)); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Row { Text("BONOBO ", fontSize = 19.sp, fontWeight = FontWeight.Bold); Text("PRIVACY", color = Cyan, fontSize = 19.sp, fontWeight = FontWeight.Bold) }; Text("Votre téléphone vous appartient.", color = Soft, fontSize = 12.sp) }; IconButton(onClick = onAudit) { Icon(Icons.Default.Settings, "Applications", tint = Soft) } } }
        item { ProtectionSummary(score, microphone == 0) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SensorTile(Icons.Default.Mic, "Microphone", "$microphone autorisée(s)", if (microphone == 0) "Aucun accès" else "Voir et agir", Cyan, Modifier.weight(1f)) { onSensor("microphone") }; SensorTile(Icons.Default.Videocam, "Caméra", "$camera autorisée(s)", if (camera == 0) "Aucun accès" else "Voir et agir", Green, Modifier.weight(1f)) { onSensor("camera") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SensorTile(Icons.Default.LocationOn, "Localisation", "$location autorisée(s)", if (location == 0) "Aucun accès" else "Voir et agir", Color(0xFFFFA46A), Modifier.weight(1f)) { onSensor("location") }; SensorTile(Icons.Default.ContentPaste, "Presse-papiers", "Surveillance active", "Comprendre", Cyan, Modifier.weight(1f)) { onSensor("clipboard") } } }
        item { Button(onClick = onSilence, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168BFF), contentColor = Color.White)) { Icon(Icons.Default.MicOff, null); Spacer(Modifier.width(9.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("MODE SILENCE", fontWeight = FontWeight.ExtraBold); Text("Couper l’accès au micro et à la caméra", fontSize = 11.sp, color = Color(0xFFD7EBFF)) } } }
        item { TextButton(onClick = onAudit, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.ManageSearch, null); Spacer(Modifier.width(6.dp)); Text("Voir toutes les applications") } }
    }
}

@Composable
private fun ProtectionSummary(score: Int, microphoneProtected: Boolean) = Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
    Icon(Icons.Default.GppGood, null, tint = Green, modifier = Modifier.size(78.dp))
    Text(if (microphoneProtected) "PROTÉGÉ" else "À VÉRIFIER", fontWeight = FontWeight.ExtraBold, fontSize = 25.sp, color = if (microphoneProtected) Green else Color(0xFFFFB15E))
    Text(if (microphoneProtected) "Microphone sécurisé" else "Accès microphone autorisé", color = if (microphoneProtected) Green else Color(0xFFFFB15E), fontSize = 13.sp)
    Spacer(Modifier.height(9.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Indice de confidentialité", fontWeight = FontWeight.Bold, fontSize = 13.sp); Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Green, trackColor = Color(0xFF213653)) }; Spacer(Modifier.width(12.dp)); Text("$score/100", color = Cyan, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
    }
}

@Composable
private fun SensorTile(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String, state: String, tint: Color, modifier: Modifier, onClick: () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = modifier.height(140.dp).clickable(onClick = onClick)) {
    Column(Modifier.fillMaxSize().padding(13.dp), verticalArrangement = Arrangement.SpaceBetween) { Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }; Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(detail, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(state, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) } }
}

@Composable
private fun AuditScreen(apps: List<SensitiveApp>, onBack: () -> Unit, onOpenApp: (String) -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
    Header("Applications à vérifier", "Autorisations réelles et capacités déclarées", onBack)
    if (apps.isEmpty()) EmptyState("Aucune application avec permission sensible détectée.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(apps, key = { it.packageName }) { app -> Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onOpenApp(app.packageName) }) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Icon(rememberDrawablePainter(app.icon), null, Modifier.size(42.dp), tint = Color.Unspecified) else Icon(Icons.Default.Android, null, Modifier.size(42.dp), tint = Cyan); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.Bold); Text(permissionSummary(app), color = Soft, fontSize = 13.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Cyan) } } } }
}

@Composable
private fun SilenceScreen(onBack: () -> Unit, onOpenPrivacy: () -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
    Header("Mode Silence", "Protéger micro et caméra", onBack)
    Spacer(Modifier.height(28.dp)); Icon(Icons.Default.MicOff, null, tint = Cyan, modifier = Modifier.size(92.dp).align(Alignment.CenterHorizontally)); Spacer(Modifier.height(20.dp)); Text("Quand vous posez le téléphone sur la table", fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.height(8.dp)); Text("Android permet de désactiver globalement l’accès au microphone et à la caméra depuis ses réglages de confidentialité. Pour votre sécurité, Bonobo Privacy ouvre le réglage officiel : vous gardez toujours le contrôle.", color = Soft, lineHeight = 21.sp); Spacer(Modifier.height(28.dp)); Button(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(10.dp)); Text("OUVRIR LES RÉGLAGES ANDROID") }
}

@Composable
private fun Header(title: String, subtitle: String, onBack: () -> Unit) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 20.dp)) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Retour") }; Spacer(Modifier.width(8.dp)); Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text(subtitle, color = Soft, fontSize = 13.sp) } } }

@Composable
private fun SensorDetailScreen(title: String, subtitle: String, apps: List<SensitiveApp>, onBack: () -> Unit, onOpenApp: (String) -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
    Header(title, subtitle, onBack)
    Text("Touchez une application pour ouvrir sa fiche Android et retirer ou limiter l’autorisation.", color = Soft, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
    if (apps.isEmpty()) EmptyState("Aucune application à afficher.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(apps, key = { it.packageName }) { app -> AppActionCard(app, onOpenApp) } }
}

@Composable
private fun AppActionCard(app: SensitiveApp, onOpenApp: (String) -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onOpenApp(app.packageName) }) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { if (app.icon != null) Icon(rememberDrawablePainter(app.icon), null, Modifier.size(42.dp), tint = Color.Unspecified) else Icon(Icons.Default.Android, null, Modifier.size(42.dp), tint = Cyan); Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text(app.label, fontWeight = FontWeight.Bold); Text(permissionSummary(app), color = Soft, fontSize = 13.sp) }; Icon(Icons.Default.Settings, null, tint = Cyan) } }

@Composable
private fun ClipboardScreen(onBack: () -> Unit, onOpenPrivacy: () -> Unit) = Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(20.dp)) {
    Header("Presse-papiers", "Ce qu’Android contrôle", onBack)
    Text("Android affiche un avertissement lorsqu’une application lit le presse-papiers. Les applications ne déclarent pas une autorisation séparée pour cette fonction : il n’existe donc pas de liste fiable à afficher ici.", color = Soft, lineHeight = 21.sp)
    Spacer(Modifier.height(24.dp)); Button(onClick = onOpenPrivacy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(10.dp)); Text("OUVRIR LES RÉGLAGES ANDROID") }
}

@Composable private fun EmptyState(text: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = Soft) }

private fun scanSensitiveApps(context: Context): List<SensitiveApp> {
    val pm = context.packageManager
    val sensitive = mapOf(Manifest.permission.RECORD_AUDIO to "Microphone", Manifest.permission.CAMERA to "Caméra", Manifest.permission.ACCESS_FINE_LOCATION to "Position", Manifest.permission.ACCESS_COARSE_LOCATION to "Position")
    val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION") pm.getInstalledApplications(0)
    }
    return installed.mapNotNull { info ->
        val requested = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(info.packageName, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())).requestedPermissions?.toList().orEmpty()
            } else {
                @Suppress("DEPRECATION") pm.getPackageInfo(info.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.toList().orEmpty()
            }
        }.getOrDefault(emptyList())
        val found = requested.mapNotNull { sensitive[it] }.distinct()
        val granted = sensitive.filterKeys { permission -> pm.checkPermission(permission, info.packageName) == PackageManager.PERMISSION_GRANTED }.values.distinct()
        val isSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        if (found.isEmpty() || isSystemApp || info.packageName == context.packageName) null else SensitiveApp(pm.getApplicationLabel(info).toString(), info.packageName, found, granted, pm.getApplicationIcon(info))
    }.sortedBy { it.label.lowercase() }
}

private fun openAppSettings(context: Context, packageName: String) { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
private fun appsFor(apps: List<SensitiveApp>, permission: String) = apps.filter { it.declaredPermissions.contains(permission) }
private fun permissionSummary(app: SensitiveApp): String = app.declaredPermissions.joinToString(" · ") { permission ->
    "$permission : ${if (app.grantedPermissions.contains(permission)) "autorisé" else "non autorisé"}"
}

/**
 * Indicateur volontairement modéré : il résume les accès sensibles accordés,
 * sans assimiler une permission utile à un comportement dangereux. Chaque
 * famille de capteurs est donc plafonnée à 12 points.
 */
private fun privacyScore(microphone: Int, camera: Int, location: Int): Int {
    val microphoneImpact = microphone.coerceAtMost(12)
    val cameraImpact = camera.coerceAtMost(12)
    val locationImpact = ((location * 2) / 3).coerceAtMost(12)
    return (100 - microphoneImpact - cameraImpact - locationImpact).coerceIn(0, 100)
}
