package com.example.pzrymyo_ai

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Query
import androidx.compose.ui.res.painterResource
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // Diamond, Lightbulb, History buradan gelecek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch





//VERİ MODELLERİ
data class Message(
    val text: String,
    val isUser: Boolean,
    val sessionId: String = "", // Hangi sohbete ait? (Varsayılan boş)
    val date: Long = 0L         // Ne zaman atıldı? (Varsayılan 0)
)
data class ChatSession(val id: Int, val title: String)
// Sol menüdeki başlıklar için gerekli
data class ChatSessionSummary(
    val sessionId: String,
    val title: String,
    val date: Long
)

enum class AvatarOption(val resId: Int, val color: Color) {
    WOMAN(R.drawable.kadin, Color(0xFFE91E63)),
    MAN(R.drawable.erkek, Color(0xFF2196F3)),
    CHICKEN(R.drawable.tavuk, Color(0xFFFFC107))
}

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    val savedName = sharedPreferences.getString("userName", null)

    var isOnboardingCompleted by remember { mutableStateOf(savedName != null) }
    var userName by remember { mutableStateOf(savedName ?: "") }
    var selectedAvatar by remember { mutableStateOf(AvatarOption.CHICKEN) }

    if (!isOnboardingCompleted) {
        OnboardingScreen(
            onStartClicked = { name, avatar ->
                sharedPreferences.edit().putString("userName", name).apply()
                userName = name
                selectedAvatar = avatar
                isOnboardingCompleted = true
            }
        )
    } else {
        MainChatInterface(
            userName = userName,
            userAvatar = selectedAvatar,
            onLogout = {
                sharedPreferences.edit().clear().apply()
                userName = ""
                isOnboardingCompleted = false
            }
        )
    }
}

//GİRİŞ EKRANI
@Composable
fun OnboardingScreen(onStartClicked: (String, AvatarOption) -> Unit) {
    var nameInput by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(AvatarOption.CHICKEN) }
//PMYO Renk Paleti
    val pmyoRed = Color(0xFFB71C1C)
    val pmyoOrange = Color(0xFFF57F17)
    val pmyoWhite = Color(0xFFFFFFFF)
    val pmyoTextGrey = Color(0xFF212121)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pmyoWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("PmyoAI'a Hoş Geldin", color = pmyoRed, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Bir avatar seç", color = pmyoTextGrey, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AvatarOption.values().forEach { avatar ->
                val borderColor = if (selectedAvatar == avatar) pmyoOrange else Color.Transparent
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatar.color)
                        .border(
                            width = if (selectedAvatar == avatar) 4.dp else 0.dp,
                            color = borderColor,
                            shape = CircleShape
                        )
                        .clickable { selectedAvatar = avatar },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = avatar.resId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(75.dp).clip(CircleShape)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Adın ne?") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF212121),
                unfocusedTextColor = Color(0xFF212121),
                focusedBorderColor = Color(0xFFB71C1C),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFFB71C1C),
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color(0xFFB71C1C)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nameInput.isNotBlank()) {
                    onStartClicked(nameInput, selectedAvatar)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C),
                disabledContainerColor = Color.Gray,
                disabledContentColor = Color.White)
        ) {
            Text("Sohbete Başla", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

//ANA SOHBET ARAYÜZÜ
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatInterface(userName: String, userAvatar: AvatarOption, onLogout: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    //TÜM MESAJLARI TUTAN LİSTE
    val allMessages = remember { mutableStateListOf<Message>() }

    var currentMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val geminiHelper = remember { GeminiHelper() }

    //VERİTABANI BAĞLANTISI
    val db = remember { com.google.firebase.Firebase.firestore }

    //ŞU ANKİ SOHBETİN KİMLİĞİ
    var currentSessionId by remember { mutableStateOf(System.currentTimeMillis().toString()) }

    //DİNLEME CİHAZI: Veritabanını Canlı Takip Et
    LaunchedEffect(userName) {
        if (userName.isNotBlank()) {
            db.collection("sohbetler")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        println("HATA: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        allMessages.clear() // Listeyi temizle
                        for (document in snapshot.documents) {
                            val msgOwner = document.getString("userName")
                            // Sadece BENİM mesajlarımı al
                            if (msgOwner == userName) {
                                val text = document.getString("text") ?: ""
                                val isUser = document.getBoolean("isUser") ?: true
                                val sId = document.getString("sessionId") ?: ""
                                val date = document.getLong("date") ?: 0L

                                allMessages.add(Message(text, isUser, sId, date))
                            }
                        }
                    }
                }
        }
    }

    //HESAPLAMALAR

    // 1. Sol Menü İçin Tarihçe Listesi (Gruplama)
    val chatHistoryList = remember(allMessages.size) { // Liste boyutu değişince tekrar hesapla
        allMessages.groupBy { it.sessionId }
            .map { (sId, msgs) ->
                // O sohbetteki ilk kullanıcı mesajını başlık yap
                val firstMsg = msgs.firstOrNull { it.isUser }?.text ?: "Yeni Sohbet"
                val title = if (firstMsg.length > 25) firstMsg.take(25) + "..." else firstMsg
                val lastDate = msgs.maxOfOrNull { it.date } ?: 0L
                ChatSessionSummary(sId, title, lastDate)
            }
            .sortedByDescending { it.date } // En yeni en üstte
    }

    // 2. Ekranda Gösterilecek Mesajlar (Filtreleme)
    val displayMessages = allMessages.filter { it.sessionId == currentSessionId }
    val isChatStarted = displayMessages.isNotEmpty()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFFFFFFFF),
                drawerContentColor = Color(0xFF212121)
            ) {
                // BAŞLIK
                Text("Merhaba, $userName", modifier = Modifier.padding(16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))

                //YENİ SOHBET BUTONU
                ExtendedFloatingActionButton(
                    onClick = {
                        currentSessionId = System.currentTimeMillis().toString() // Yeni sayfa aç
                        scope.launch { drawerState.close() }
                    },
                    containerColor = Color(0xFFB71C1C),
                    contentColor = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text("Yeni Sohbet")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Geçmiş Sohbetler", modifier = Modifier.padding(horizontal = 16.dp), color = Color.Gray, fontSize = 14.sp)

                // --- TARİHÇE LİSTESİ ---
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(chatHistoryList) { session ->
                        val isSelected = session.sessionId == currentSessionId
                        NavigationDrawerItem(
                            label = { Text(session.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = isSelected,
                            onClick = {
                                currentSessionId = session.sessionId
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                // İkon rengi: Seçiliyse BEYAZ, değilse GRİ
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = Color(0xFFB71C1C), // Seçiliyken KIRMIZI zemin
                                selectedTextColor = Color.White,            // Seçiliyken BEYAZ yazı
                                unselectedContainerColor = Color.Transparent, // Seçili değilse şeffaf
                                unselectedTextColor = Color.Black           // Seçili değilse Siyah yazı
                            )
                        )
                    }
                }

                HorizontalDivider()

                //ÇIKIŞ BUTONU
                NavigationDrawerItem(
                    label = { Text("Çıkış Yap", fontWeight = FontWeight.Bold) },
                    selected = false, // Bu hep false kalır, buton gibi davranır
                    onClick = {
                        scope.launch { drawerState.close() }
                        onLogout()
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White) }, // İkon BEYAZ
                    modifier = Modifier.padding(16.dp),
                    //ÇIKIŞ BUTONU RENKLERİ
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color(0xFFB71C1C), // Zemin KIRMIZI
                        unselectedTextColor = Color.White,            // Yazı BEYAZ
                        unselectedIconColor = Color.White             // İkon BEYAZ
                    )
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PmyoAI", fontSize = 18.sp, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                        }
                    },
                    //SAĞ TARAFA AVATAR
                    actions = {
                        Image(
                            painter = painterResource(id = userAvatar.resId),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop, // Resmi daireye sığdır
                            modifier = Modifier
                                .padding(end = 16.dp) // Sağdan biraz boşluk bırak
                                .size(40.dp)          // Boyutu
                                .clip(CircleShape)    // Yuvarlak kes
                                .background(Color.White) // Arkasına beyaz fon (resim şeffafsa)
                        )
                    },
                    // ---------------------------------------------
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFFB71C1C),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.White
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color.White)) {
                Column(modifier = Modifier.fillMaxSize()) {

                    Box(modifier = Modifier.weight(1f)) {
                        if (!isChatStarted) {
                            // HOŞGELDİN EKRANI
                            WelcomeContent(
                                userName = userName,
                                onSuggestionClick = { prompt ->
                                    isLoading = true
                                    // Mesajı Kaydet
                                    saveMsg(db, prompt, true, currentSessionId, userName)
                                    // Gemini
                                    scope.launch {
                                        val response = geminiHelper.askGemini(prompt)
                                        isLoading = false
                                        saveMsg(db, response, false, currentSessionId, userName)
                                    }
                                }
                            )
                        } else {
                            // MESAJ LİSTESİ
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp)
                            ) {
                                items(displayMessages) { message ->
                                    MessageBubble(message)
                                }
                            }
                        }
                    }

                    // MESAJ GÖNDERME
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFFF5F5F5), RoundedCornerShape(24.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = currentMessage,
                            onValueChange = { currentMessage = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("PmyoAI'a sorun", color = Color.Gray) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,      // Yazarken yazı SİYAH olsun
                                unfocusedTextColor = Color.Black,    // Yazmazken de SİYAH olsun
                                focusedContainerColor = Color(0xFFF5F5F5), // Kutunun içi hafif GRİ olsun (belli olsun)
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                cursorColor = Color(0xFFB71C1C),     // Yanıp sönen çubuk KIRMIZI olsun
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        IconButton(
                            onClick = {
                                if (currentMessage.isNotBlank()) {
                                    val userMsg = currentMessage
                                    currentMessage = ""
                                    isLoading = true
                                    saveMsg(db, userMsg, true, currentSessionId, userName)
                                    scope.launch {
                                        val response = geminiHelper.askGemini(userMsg)
                                        isLoading = false
                                        saveMsg(db, response, false, currentSessionId, userName)
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = Color(0xFFB71C1C))
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
                        // Senin Okul Logon
                        Image(
                            painter = painterResource(id = R.drawable.pmyo),
                            contentDescription = "Loading",
                            modifier = Modifier.size(60.dp).clip(CircleShape)
                        )
                        CircularProgressIndicator(modifier = Modifier.size(80.dp), color = Color(0xFFB71C1C), strokeWidth = 4.dp)
                    }
                }
            }
        }
    }
}

fun saveMsg(db: com.google.firebase.firestore.FirebaseFirestore, text: String, isUser: Boolean, sessionId: String, userName: String) {
    val msg = hashMapOf(
        "text" to text,
        "isUser" to isUser,
        "sessionId" to sessionId,
        "userName" to userName,
        "date" to System.currentTimeMillis()
    )
    db.collection("sohbetler").add(msg)
}
@Composable
fun WelcomeContent(userName: String, onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        val gradientColors = listOf(
            Color(0xFFB71C1C),
            Color(0xFFF57F17),
            Color(0xFFFBC02D))
        Text(
            text = "Merhaba $userName",
            style = MaterialTheme.typography.displaySmall.copy(
                brush = Brush.linearGradient(colors = gradientColors)
            ),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Nereden başlayalım?",
            style = MaterialTheme.typography.displaySmall.copy(color = Color(0xFF757575)),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ORİJİNAL İKON: LIGHTBULB (AMPUL)
        SuggestionChip(
            onClick = { onSuggestionClick("Benim için bir şeyler yaz") },
            //Etiket: Beyaz yazı
            label = {
                Text(
                    text = "Benim için bir şeyler yaz",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            },
            //İkon: PMYO Sarısı
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBC02D)
                )
            },
            // "Dolu" görünüm için ana numara
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color(0xFFB71C1C), // PMYO Kırmızı Arka Plan
                labelColor = Color.White
            ),
            // Dolu blok gibi görünmesi için kenarlığı kaldır (siyah olandaki gibi)
            border = null,
            // Şekil: Karemsi, tam yuvarlak değil
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

// 2. CHIP
        SuggestionChip(
            onClick = { onSuggestionClick("Bana yeni bir şeyler öğret") },
            label = {
                Text(
                    text = "Öğrenmeme yardım et",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBC02D)
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color(0xFFB71C1C),
                labelColor = Color.White
            ),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

// 3. CHIP
        SuggestionChip(
            onClick = { onSuggestionClick("Sınavlarıma çalıştır") },
            label = {
                Text(
                    text = "Sınava Hazırlık",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFFFBC02D)
                )
            },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = Color(0xFFB71C1C),
                labelColor = Color.White
            ),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit, icon: ImageVector) {
    Surface(
        onClick = onClick,
        color = Color(0xFFB71C1C),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, color = Color.White,fontWeight = FontWeight.Medium)
        }
    }
}


@Composable
fun MessageBubble(message: Message) {
    val isUser = message.isUser

    // Satır yapısı
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top // Üstten hizala
    ) {
        // EĞER YAPAY ZEKA İSE: En başa logo koy
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.pmyo), // OKul logosu
                contentDescription = "AI Logo",
                modifier = Modifier
                    .size(40.dp) // Gemini ikon boyutu
                    .padding(end = 4.dp) // Balonla arası açılsın
                    .clip(CircleShape) // Yuvarlak olsun
            )
        }

        // MESAJ BALONU
        Surface(
            color = if (isUser) Color(0xFFB71C1C) else Color(0xFFF5F5F5),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
// MessageBubble içindeki Text bileşeni:
            Text(
                text = parseBoldText(message.text),
                modifier = Modifier.padding(16.dp),
                color = if (isUser) Color.White else Color.Black,
                fontSize = 16.sp
            )
        }
    }
}
@Composable
fun parseBoldText(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val parts = text.split("**")

        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}