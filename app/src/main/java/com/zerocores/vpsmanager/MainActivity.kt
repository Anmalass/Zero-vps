package com.zerocores.vpsmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val bg = Color(0xFF0D1119)
private val panel = Color(0xFF171E2A)
private val accent = Color(0xFF8AAEFF)
private data class Host(val name:String,val host:String,val port:String,val user:String,val kind:String="VPS")

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("zero_manager", MODE_PRIVATE)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=accent,background=bg,surface=panel,secondary=Color(0xFF83DDB6))) {
   var tab by remember { mutableStateOf(0) }
   var hosts by remember { mutableStateOf(listOf<Host>()) }
   var showAdd by remember { mutableStateOf(false) }
   var selected by remember { mutableStateOf<Host?>(null) }
   var hostSettings by remember { mutableStateOf<Host?>(null) }
   var agentUrl by remember { mutableStateOf(prefs.getString("url", "") ?: "") }
   var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
   var status by remember { mutableStateOf("Belum terhubung") }
   Surface(Modifier.fillMaxSize(), color=bg) {
    Scaffold(containerColor=bg, bottomBar={ NavigationBar(containerColor=panel) {
      NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Icon(Icons.Default.Home,null)},label={Text("Beranda")})
      NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Icon(Icons.Default.Dns,null)},label={Text("VPS")})
      NavigationBarItem(selected=tab==2,onClick={tab=2},icon={Icon(Icons.Default.DesktopWindows,null)},label={Text("Desktop")})
      NavigationBarItem(selected=tab==3,onClick={tab=3},icon={Icon(Icons.Default.Settings,null)},label={Text("Setelan")})
    }}) { pad ->
     Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       Box(Modifier.size(46.dp).background(panel,RoundedCornerShape(14.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.Terminal,null,tint=accent,modifier=Modifier.size(27.dp))}
       Column(Modifier.weight(1f)) { Text("ZERO",style=MaterialTheme.typography.headlineMedium,color=Color.White); Text("VPS MANAGER",style=MaterialTheme.typography.labelMedium,color=accent) }
       IconButton(onClick={showAdd=true}) { Icon(Icons.Default.AddCircleOutline,null,tint=accent) }
      }
      when(tab) {
       0 -> {
        Text("Semua perangkat",style=MaterialTheme.typography.titleLarge)
        Text("Kelola server dan desktop dari satu tempat.",color=Color.LightGray)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Metric("VPS tersimpan",hosts.count{it.kind=="VPS"}.toString(),Modifier.weight(1f)); Metric("Desktop",hosts.count{it.kind=="Desktop"}.toString(),Modifier.weight(1f)) }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
         FeatureCard("VPS Manager","File · Kode · Project",Icons.Default.Dns,Modifier.weight(1f)){tab=1}
         FeatureCard("Remote Desktop","Layar seperti PC",Icons.Default.DesktopWindows,Modifier.weight(1f)){tab=2}
        }
        Row(verticalAlignment=Alignment.CenterVertically) { Text("Koneksi tersimpan",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f)); TextButton(onClick={showAdd=true}){Text("+ Tambah") } }
        if(hosts.isEmpty()) EmptyCard("Belum ada koneksi","Tambahkan IP/hostname, port, dan username VPS atau Desktop.")
        hosts.forEach { HostCard(it, onClick={selected=it;tab=if(it.kind=="VPS")1 else 2}, onDelete={hosts=hosts-it}) }
        selected?.let { Text("Dipilih: ${it.name}",color=accent) }
       }
       1 -> {
        Text("VPS Manager",style=MaterialTheme.typography.headlineSmall)
        Text("Pilih koneksi untuk mengelola server.",color=Color.LightGray)
        if(hosts.filter{it.kind=="VPS"}.isEmpty()) EmptyCard("Belum ada VPS","Tambahkan koneksi dari tombol + di atas.")
        hosts.filter{it.kind=="VPS"}.forEach { host -> HostCard(host,{selected=host;hostSettings=host}, {hosts=hosts-host;if(selected==host)selected=null}) }
        selected?.takeIf{it.kind=="VPS"}?.let { active ->
         Card(colors=CardDefaults.cardColors(containerColor=panel)) { Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Koneksi dipilih",style=MaterialTheme.typography.titleMedium); Text(active.name,color=accent); Text("${active.user}@${active.host}:${active.port}",color=Color.LightGray); Button(onClick={hostSettings=active},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Settings,null);Spacer(Modifier.width(8.dp));Text("Pengaturan koneksi VPS")} } }
        }
        Text("Alat server",style=MaterialTheme.typography.titleLarge)
        val tools=listOf(Triple("File Manager","Jelajah dan edit file",Icons.Default.Folder),Triple("Code Editor","Edit kode dan konfigurasi",Icons.Default.Code),Triple("Projects","Jalankan dan pantau project",Icons.Default.PlayCircle),Triple("Docker & Services","Kontainer dan service",Icons.Default.Inventory2),Triple("Logs & Monitoring","Status, log, CPU, RAM, disk",Icons.Default.MonitorHeart),Triple("Terminal","Perintah lanjutan",Icons.Default.Terminal))
        tools.forEach { (title,desc,icon)-> Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(14.dp)).clickable{status=if(selected?.kind=="VPS") "$title dipilih untuk ${selected?.name}; backend SSH belum diintegrasikan" else "Pilih VPS terlebih dahulu untuk membuka $title"}.padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)){Icon(icon,null,tint=accent);Column(Modifier.weight(1f)){Text(title);Text(desc,color=Color.LightGray,style=MaterialTheme.typography.bodySmall)};Icon(Icons.Default.ChevronRight,null)} }
        Text(status,color=Color.LightGray,style=MaterialTheme.typography.bodySmall)
       }
       2 -> {
        Text("Desktop",style=MaterialTheme.typography.headlineSmall)
        Text("Pilih desktop yang ingin dibuka atau diatur.",color=Color.LightGray)
        val desktops=hosts.filter{it.kind=="Desktop"}
        if(desktops.isEmpty()) EmptyCard("Belum ada Desktop","Tambahkan koneksi RDP. Setelah tersimpan, profil Desktop akan tampil di sini.")
        desktops.forEach { host -> HostCard(host,{selected=host;hostSettings=host},{hosts=hosts-host;if(selected==host)selected=null}) }
        selected?.takeIf{it.kind=="Desktop"}?.let { active -> Card(colors=CardDefaults.cardColors(containerColor=panel)) { Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Desktop dipilih",style=MaterialTheme.typography.titleMedium); Text(active.name,color=accent); Text("${active.user}@${active.host}:${active.port}",color=Color.LightGray); Button(onClick={hostSettings=active},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Settings,null);Spacer(Modifier.width(8.dp));Text("Pengaturan koneksi Desktop")} } } }
        Box(Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF202A3B),RoundedCornerShape(20.dp)),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.DesktopWindows,null,tint=accent,modifier=Modifier.size(54.dp));Text("Remote Desktop",style=MaterialTheme.typography.titleLarge);Text("Layar PC akan tampil di sini",color=Color.LightGray)} }
        Button(onClick={showAdd=true},modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Tambah koneksi Desktop")}
        Text("Fitur target",style=MaterialTheme.typography.titleLarge)
        listOf("Tampilan desktop penuh","Mouse / touchpad dan keyboard","Zoom, rotasi, layar penuh","Toolbar koneksi dan putuskan sesi").forEach { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Icon(Icons.Default.CheckCircle,null,tint=Color(0xFF83DDB6));Text(it)} }
        EmptyCard("Desktop belum tersambung","Tampilan ini adalah kerangka UI. Streaming RDP belum diintegrasikan.")
       }
       else -> {
        Text("Pengaturan",style=MaterialTheme.typography.headlineSmall)
        Text("Agent lama · Tailscale",style=MaterialTheme.typography.titleMedium)
        OutlinedTextField(agentUrl,{agentUrl=it},label={Text("URL agent, contoh http://100.x.y.z:8765")},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri))
        OutlinedTextField(token,{token=it},label={Text("Bearer token agent")},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=PasswordVisualTransformation())
        Button(onClick={prefs.edit().putString("url",agentUrl.trim().trimEnd('/')).putString("token",token.trim()).apply();status="Pengaturan agent disimpan di preferensi aplikasi"},modifier=Modifier.fillMaxWidth()){Text("Simpan pengaturan agent")}
        Text(status,color=Color.LightGray)
        EmptyCard("Keamanan","Jangan masukkan password VPS ke catatan atau chat. Profil koneksi di layar ini hanya tersimpan sementara selama aplikasi terbuka; agent token disimpan di preferensi Android.")
       }
      }
     }
    }
   }
   if(showAdd) AddConnectionDialog(onDismiss={showAdd=false},onSave={h->hosts=hosts+h;showAdd=false})
   hostSettings?.let { target -> HostSettingsDialog(host=target,onDismiss={hostSettings=null},onSave={updated->hosts=hosts.map{if(it==target)updated else it};selected=updated;hostSettings=null},onDelete={hosts=hosts-target;selected=null;hostSettings=null}) }
  } }
 }
}

@Composable private fun Metric(title:String,value:String,modifier:Modifier=Modifier){Card(modifier,colors=CardDefaults.cardColors(containerColor=panel)){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(title,color=Color.LightGray,style=MaterialTheme.typography.bodySmall);Text(value,style=MaterialTheme.typography.headlineSmall,color=Color.White)}}}
@Composable private fun FeatureCard(title:String,desc:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,onClick:()->Unit){Card(modifier.clickable{onClick()},colors=CardDefaults.cardColors(containerColor=panel)){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Icon(icon,null,tint=accent,modifier=Modifier.size(28.dp));Text(title,style=MaterialTheme.typography.titleMedium);Text(desc,color=Color.LightGray,style=MaterialTheme.typography.bodySmall)}}}
@Composable private fun EmptyCard(title:String,desc:String){Card(colors=CardDefaults.cardColors(containerColor=panel)){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(desc,color=Color.LightGray,style=MaterialTheme.typography.bodyMedium)}}}
@Composable private fun HostCard(host:Host,onClick:()->Unit,onDelete:()->Unit){Card(Modifier.fillMaxWidth().clickable{onClick()},colors=CardDefaults.cardColors(containerColor=panel)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Icon(if(host.kind=="VPS")Icons.Default.Dns else Icons.Default.DesktopWindows,null,tint=accent,modifier=Modifier.size(30.dp));Column(Modifier.weight(1f)){Text(host.name,style=MaterialTheme.typography.titleMedium);Text("${host.user}@${host.host}:${host.port}",color=Color.LightGray,style=MaterialTheme.typography.bodySmall);Text("${host.kind} · belum diuji koneksi",color=Color(0xFFFFD28A),style=MaterialTheme.typography.labelSmall)};IconButton(onClick=onDelete){Icon(Icons.Default.DeleteOutline,null,tint=Color.LightGray)}}}}
@Composable private fun AddConnectionDialog(onDismiss:()->Unit,onSave:(Host)->Unit){var name by remember{mutableStateOf("")};var host by remember{mutableStateOf("")};var port by remember{mutableStateOf("22")};var user by remember{mutableStateOf("root")};var kind by remember{mutableStateOf("VPS")};var password by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("Tambah koneksi")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(9.dp)){OutlinedTextField(name,{name=it},label={Text("Nama koneksi")},singleLine=true);OutlinedTextField(host,{host=it},label={Text("IP / hostname")},singleLine=true);OutlinedTextField(port,{port=it},label={Text("Port SSH / RDP")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number));OutlinedTextField(user,{user=it},label={Text("Username")},singleLine=true);Text("Jenis koneksi");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=kind=="VPS",onClick={kind="VPS"},label={Text("VPS / SSH")});FilterChip(selected=kind=="Desktop",onClick={kind="Desktop"},label={Text("Desktop / RDP")})};OutlinedTextField(password,{password=it},label={Text("Password (tidak disimpan)")},singleLine=true,visualTransformation=PasswordVisualTransformation());Text("Form ini belum melakukan login SSH/RDP. Password tidak disimpan.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton(onClick={if(name.isNotBlank()&&host.isNotBlank()&&port.toIntOrNull()!=null&&user.isNotBlank())onSave(Host(name.trim(),host.trim(),port.trim(),user.trim(),kind))}){Text("Simpan profil")}},dismissButton={TextButton(onClick=onDismiss){Text("Batal")}})}

@Composable private fun HostSettingsDialog(host:Host,onDismiss:()->Unit,onSave:(Host)->Unit,onDelete:()->Unit){
 var name by remember(host){mutableStateOf(host.name)};var address by remember(host){mutableStateOf(host.host)};var port by remember(host){mutableStateOf(host.port)};var user by remember(host){mutableStateOf(host.user)}
 AlertDialog(onDismissRequest=onDismiss,title={Text("Pengaturan ${host.kind}")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Profil koneksi",style=MaterialTheme.typography.titleSmall,color=accent);OutlinedTextField(name,{name=it},label={Text("Nama koneksi")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(address,{address=it},label={Text("IP / hostname")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(port,{port=it},label={Text("Port")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth());OutlinedTextField(user,{user=it},label={Text("Username")},singleLine=true,modifier=Modifier.fillMaxWidth());Text("Jenis: ${host.kind} · password tidak disimpan. Koneksi SSH/RDP langsung belum diaktifkan.",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton(onClick={if(name.isNotBlank()&&address.isNotBlank()&&port.toIntOrNull()!=null&&user.isNotBlank())onSave(host.copy(name=name.trim(),host=address.trim(),port=port.trim(),user=user.trim()))}){Text("Simpan")}},dismissButton={Row{TextButton(onClick=onDelete){Text("Hapus",color=Color(0xFFFF8A8A))};TextButton(onClick=onDismiss){Text("Tutup")}}})
}
