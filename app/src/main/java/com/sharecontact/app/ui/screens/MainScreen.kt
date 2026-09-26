package com.sharecontact.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sharecontact.app.model.AppSettings
import com.sharecontact.app.model.CardType
import com.sharecontact.app.model.ShareCard
import com.sharecontact.app.util.QrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    cards: List<ShareCard>,
    settings: AppSettings,
    onEditCard: (String) -> Unit,
    onAddNewCard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNfcWriter: (ShareCard) -> Unit,
    onOpenReorderCards: () -> Unit,
    onOpenNfcGuide: () -> Unit,
    onCardChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    // Determine initial page
    val initialPage = remember(cards, settings) {
        val targetId = settings.lastViewedCardId
        val idx = cards.indexOfFirst { it.id == targetId }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (cards.size - 1).coerceAtLeast(0)),
        pageCount = { cards.size }
    )

    val currentCard = cards.getOrNull(pagerState.currentPage) ?: cards.firstOrNull()

    // Notify repository of current card change for 'Last Used' memory
    LaunchedEffect(pagerState.currentPage, currentCard) {
        if (currentCard != null) {
            onCardChanged(currentCard.id)
        }
    }

    // Fullscreen QR Modal state
    var isFullscreenQrOpen by remember { mutableStateOf(false) }

    // Screen brightness control
    var isBrightnessBoosted by remember { mutableStateOf(settings.boostBrightnessOnQr) }

    DisposableEffect(isBrightnessBoosted) {
        val activity = context as? Activity
        val window = activity?.window
        val originalBrightness = window?.attributes?.screenBrightness ?: -1f

        if (isBrightnessBoosted && window != null) {
            val lp = window.attributes
            lp.screenBrightness = 1.0f
            window.attributes = lp
        }

        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = originalBrightness
                window.attributes = lp
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentCard?.displayName ?: "ShareCard",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Prominent "Write NFC" button with explicit text
                    if (currentCard != null) {
                        FilledTonalButton(
                            onClick = { onOpenNfcWriter(currentCard) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Nfc,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Write NFC", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        // System Share
                        IconButton(onClick = {
                            val payload = QrCodeGenerator.getPayload(currentCard)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, payload)
                                type = if (currentCard.type == CardType.VCARD) "text/x-vcard" else "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Card Content"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                    }

                    // Hamburger / Overflow Menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (currentCard != null) {
                                DropdownMenuItem(
                                    text = { Text("Write to NFC Tag") },
                                    leadingIcon = { Icon(Icons.Default.Nfc, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenNfcWriter(currentCard)
                                    }
                                )
                            }
                            if (cards.size > 1) {
                                DropdownMenuItem(
                                    text = { Text("Rearrange Cards") },
                                    leadingIcon = { Icon(Icons.Default.SwapVert, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onOpenReorderCards()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("NFC Buyer's Guide & How-To") },
                                leadingIcon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenNfcGuide()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Settings & Backup") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (currentCard != null) {
                Column(horizontalAlignment = Alignment.End) {
                    // Quick Brightness Toggle FAB
                    SmallFloatingActionButton(
                        onClick = { isBrightnessBoosted = !isBrightnessBoosted },
                        containerColor = if (isBrightnessBoosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isBrightnessBoosted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(
                            imageVector = if (isBrightnessBoosted) Icons.Default.BrightnessHigh else Icons.Default.BrightnessMedium,
                            contentDescription = "Toggle Brightness Boost"
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Edit Card FAB
                    FloatingActionButton(
                        onClick = { onEditCard(currentCard.id) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Card")
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Pager for multiple QR cards
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val card = cards[page]
                QrCardItem(
                    card = card,
                    onQrClick = { isFullscreenQrOpen = true }
                )
            }

            // Pager Page Indicator Dots
            if (cards.size > 1) {
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(cards.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                        )
                    }
                }
            }

            // Bottom controls: Add Card & Reorder link
            Row(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddNewCard) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card / Wi-Fi", fontSize = 13.sp)
                }

                if (cards.size > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onOpenReorderCards) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rearrange", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Fullscreen QR Modal on tap
    if (isFullscreenQrOpen && currentCard != null) {
        Dialog(
            onDismissRequest = { isFullscreenQrOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { isFullscreenQrOpen = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = currentCard.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    QrCodeImage(
                        content = QrCodeGenerator.getPayload(currentCard),
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Tap anywhere to exit full screen",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun QrCardItem(
    card: ShareCard,
    onQrClick: () -> Unit
) {
    val payload = remember(card) { QrCodeGenerator.getPayload(card) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Prominent Card Title / Name Header
        Text(
            text = card.title.ifBlank { card.displayName },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        // High-Contrast QR Code Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f)
                .clickable { onQrClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                QrCodeImage(
                    content = payload,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tap QR to expand full screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(content) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(content) {
        withContext(Dispatchers.Default) {
            bitmap = QrCodeGenerator.generateBitmap(content, sizePixels = 1024, margin = 1)
        }
    }

    bitmap?.let { b ->
        Image(
            bitmap = b.asImageBitmap(),
            contentDescription = "Scannable QR Code",
            modifier = modifier
        )
    } ?: Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(36.dp))
    }
}
