package com.lvngx.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import org.json.JSONArray

private val Bg = Color(0xFF080A11)
private val Surface = Color(0xFF141A29)
private val Surface2 = Color(0xFF1B2335)
private val Red = Color(0xFFF20D50)
private val TextMain = Color(0xFFF4F5F8)
private val Muted = Color(0xFF9AA4B8)

data class GameApp(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LVNGApp() }
    }
}

@Composable
private fun LVNGApp() {
    val context = LocalContext.current
    var page by remember { mutableStateOf("home") }
    var selectedGames by remember { mutableStateOf(loadGames(context)) }
    var allApps by remember { mutableStateOf(emptyList<GameApp>()) }
    var showPicker by remember { mutableStateOf(false) }
    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    fun refreshOverlay() { overlayGranted = Settings.canDrawOverlays(context) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val pkg = result.data?.getStringExtra("package")
            if (pkg != null) {
                val app = allApps.firstOrNull { it.packageName == pkg }
                if (app != null && selectedGames.none { it.packageName == pkg }) {
                    selectedGames = selectedGames + app
                    saveGames(context, selectedGames)
                }
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Red, background = Bg, surface = Surface,
        onPrimary = Color.White, onBackground = TextMain, onSurface = TextMain
    )) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101522), Bg, Color(0xFF07090F))))) {
            Column(Modifier.fillMaxSize()) {
                when (page) {
                    "home" -> HomePage(
                        games = selectedGames,
                        onAdd = {
                            allApps = installedApps(context)
                            showPicker = true
                        },
                        onLaunch = { game -> launchGame(context, game.packageName) },
                        onSpace = {
                            if (overlayGranted) {
                                startOverlayService(context)
                            } else {
                                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                            }
                        },
                        onDiagnostics = { page = "diagnostics" }
                    )
                    "library" -> LibraryPage(selectedGames, onAdd = {
                        allApps = installedApps(context); showPicker = true
                    }, onLaunch = { launchGame(context, it.packageName) }, onRemove = {
                        selectedGames = selectedGames.filterNot { g -> g.packageName == it.packageName }
                        saveGames(context, selectedGames)
                    })
                    "diagnostics" -> DiagnosticsPage(overlayGranted, onPermission = {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                    })
                }
                Spacer(Modifier.weight(1f))
                BottomBar(page, onPage = { page = it })
            }
            if (showPicker) {
                GamePickerDialog(
                    apps = allApps,
                    selected = selectedGames.map { it.packageName }.toSet(),
                    onDismiss = { showPicker = false },
                    onSelect = { app ->
                        if (selectedGames.none { it.packageName == app.packageName }) {
                            selectedGames = selectedGames + app
                            saveGames(context, selectedGames)
                        }
                        showPicker = false
                    }
                )
            }
        }
    }
}

@Composable
private fun HomePage(
    games: List<GameApp>, onAdd: () -> Unit, onLaunch: (GameApp) -> Unit,
    onSpace: () -> Unit, onDiagnostics: () -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LVNG -X", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.6.sp)
                Text("GAME SPACE", color = Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.4.sp)
            }
            Image(androidx.compose.ui.res.painterResource(R.drawable.lvngx_logo), null, Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)))
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF291522), Color(0xFF171B2B), Surface)))
                .clickable { onSpace() }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Red.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Text("LX", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text("Masuk ke", color = Muted, fontSize = 12.sp)
                Text("Game Space", color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            Text("›", color = TextMain, fontSize = 34.sp, modifier = Modifier.padding(end = 4.dp))
        }
        Row(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Perpustakaan Game", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${games.size}", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.width(12.dp))
            Text("Tambah  +", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(Surface2).clickable { onAdd() }.padding(horizontal = 15.dp, vertical = 9.dp))
        }
        if (games.isEmpty()) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Surface).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Belum ada game", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Tambahkan game dari aplikasi yang terpasang.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(14.dp))
                Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Tambah Game") }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(games, key = { it.packageName }) { game ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Surface).clickable { onLaunch(game) }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(game, Modifier.size(54.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                            Text(game.label, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Game tersimpan", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Button(onClick = { onLaunch(game) }, colors = ButtonDefaults.buttonColors(containerColor = Red), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)) {
                            Text("Main", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Pengaturan akses  ›", color = Muted, fontSize = 12.sp, modifier = Modifier.clickable { onDiagnostics() }.padding(vertical = 10.dp))
    }
}

@Composable
private fun LibraryPage(
    games: List<GameApp>, onAdd: () -> Unit, onLaunch: (GameApp) -> Unit, onRemove: (GameApp) -> Unit
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {
        Text("Game Library", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 22.dp, bottom = 18.dp))
        Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Tambah Game") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
            items(games, key = { it.packageName }) { game ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(game, Modifier.size(44.dp))
                    Text(game.label, color = TextMain, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Main", color = Red, modifier = Modifier.clickable { onLaunch(game) }.padding(8.dp))
                    Text("Hapus", color = Muted, modifier = Modifier.clickable { onRemove(game) }.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsPage(overlayGranted: Boolean, onPermission: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(20.dp)) {
        Text("Access & Diagnostics", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        Text("Status izin perangkat", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 20.dp))
        DiagnosticRow("Display over other apps", if (overlayGranted) "Granted" else "Needs permission", overlayGranted)
        DiagnosticRow("Shizuku integration", "Belum diintegrasikan", false)
        DiagnosticRow("Macro Accessibility", "Belum diintegrasikan", false)
        Spacer(Modifier.height(16.dp))
        if (!overlayGranted) Button(onClick = onPermission, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("Aktifkan izin overlay") }
        Text("LVNG -X hanya menampilkan status yang dapat diverifikasi. Fitur yang belum tersedia tidak ditandai aktif.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 18.dp))
    }
}

@Composable
private fun DiagnosticRow(name: String, status: String, good: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(status, color = if (good) Color(0xFF70D6A1) else Red, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (good) Color(0xFF70D6A1) else Red))
    }
    HorizontalDivider(color = Surface2)
}

@Composable
private fun BottomBar(page: String, onPage: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 26.dp, vertical = 10.dp)
        .clip(CircleShape).background(Color(0xFF171922)).padding(7.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        BottomItem("Home", page == "home") { onPage("home") }
        BottomItem("Library", page == "library") { onPage("library") }
        BottomItem("Access", page == "diagnostics") { onPage("diagnostics") }
    }
}

@Composable
private fun BottomItem(label: String, active: Boolean, onClick: () -> Unit) {
    Text(label, color = if (active) TextMain else Muted, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier.clip(CircleShape).background(if (active) Red.copy(alpha = 0.20f) else Color.Transparent).clickable { onClick() }.padding(horizontal = 17.dp, vertical = 12.dp))
}

@Composable
private fun AppIcon(game: GameApp, modifier: Modifier = Modifier) {
    val bitmap = remember(game.packageName) {
        val drawable = game.icon
        if (drawable is BitmapDrawable && drawable.bitmap != null) drawable.bitmap
        else {
            val bmp = android.graphics.Bitmap.createBitmap(96, 96, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, 96, 96)
            drawable.draw(canvas)
            bmp
        }
    }
    Image(bitmap.asImageBitmap(), contentDescription = game.label, modifier = modifier.clip(RoundedCornerShape(14.dp)))
}

@Composable
private fun GamePickerDialog(apps: List<GameApp>, selected: Set<String>, onDismiss: () -> Unit, onSelect: (GameApp) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        title = { Text("Tambah Game", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            if (apps.isEmpty()) Text("Tidak ada aplikasi yang dapat ditampilkan.", color = Muted)
            else LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = app.packageName !in selected) { onSelect(app) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app, Modifier.size(38.dp))
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(app.label, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, color = Muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(if (app.packageName in selected) "Ditambahkan" else "Tambah", color = if (app.packageName in selected) Muted else Red, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai", color = Red) } }
    )
}

private fun installedApps(context: Context): List<GameApp> {
    val pm = context.packageManager
    return pm.getInstalledApplications(PackageManager.GET_META_DATA)
        .filter { it.packageName != context.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { GameApp(pm.getApplicationLabel(it).toString(), it.packageName, pm.getApplicationIcon(it)) }
        .sortedBy { it.label.lowercase() }
}

private fun launchGame(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        if (Settings.canDrawOverlays(context)) {
            startOverlayService(context)
        }
    }
}

private fun saveGames(context: Context, games: List<GameApp>) {
    val array = JSONArray()
    games.forEach { array.put(it.packageName) }
    context.getSharedPreferences("lvngx", Context.MODE_PRIVATE).edit().putString("games", array.toString()).apply()
}

private fun loadGames(context: Context): List<GameApp> {
    val pm = context.packageManager
    val raw = context.getSharedPreferences("lvngx", Context.MODE_PRIVATE).getString("games", "[]") ?: "[]"
    val array = JSONArray(raw)
    val result = mutableListOf<GameApp>()
    for (i in 0 until array.length()) {
        val pkg = array.optString(i)
        try {
            val info = pm.getApplicationInfo(pkg, 0)
            if (pm.getLaunchIntentForPackage(pkg) != null) result += GameApp(pm.getApplicationLabel(info).toString(), pkg, pm.getApplicationIcon(info))
        } catch (_: Exception) { }
    }
    return result
}

private fun startOverlayService(context: Context) {
    if (!Settings.canDrawOverlays(context)) return
    try { context.startService(Intent(context, OverlayService::class.java)) } catch (_: Exception) { }
}
