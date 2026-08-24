package de.syss.MifareClassicTool.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.syss.MifareClassicTool.ui.components.MctxModeBadge
import de.syss.MifareClassicTool.ui.components.PremiumScreenBackground
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val description: String,
    val detail: String
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Filled.Badge,
        eyebrow = "Profili verificabili",
        title = "Configura una volta. Opera con precisione.",
        description = "Ogni vendor raccoglie tipo di tag, chiavi e payload in un profilo controllato.",
        detail = "Le configurazioni restano locali sul dispositivo."
    ),
    OnboardingPage(
        icon = Icons.Filled.Nfc,
        eyebrow = "Flusso guidato",
        title = "Avvicina il tag solo quando richiesto.",
        description = "MiFareX verifica compatibilità, contesto e risultato durante ogni operazione NFC.",
        detail = "Mantieni il tag fermo fino alla conferma finale."
    ),
    OnboardingPage(
        icon = Icons.Filled.Security,
        eyebrow = "Ruoli separati",
        title = "Operatore, Admin ed Expert.",
        description = "La modalità Operatore è essenziale; configurazione e strumenti raw sono isolati nelle aree dedicate.",
        detail = "Expert Mode modifica dati a basso livello: usala consapevolmente."
    )
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    PremiumScreenBackground(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex])
        }

        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MiFareX", style = MaterialTheme.typography.titleLarge)
                Text("NFC OPERATIONS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onFinish) { Text("Salta introduzione") }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    val width by animateDpAsState(if (selected) 28.dp else 8.dp, spring(), label = "page_indicator_$index")
                    Box(
                        Modifier.height(7.dp).width(width).clip(CircleShape).background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            }
            val lastPage = pagerState.currentPage == pages.lastIndex
            Button(
                onClick = {
                    if (lastPage) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp).height(52.dp)
            ) {
                AnimatedContent(lastPage, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "onboarding_action") {
                    Text(if (it) "Accedi a MiFareX" else "Continua")
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 104.dp)) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                OnboardingVisual(page, Modifier.weight(0.9f).fillMaxHeight())
                OnboardingCopy(page, Modifier.weight(1.1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                OnboardingVisual(page, Modifier.fillMaxWidth().weight(0.9f))
                OnboardingCopy(page, Modifier.fillMaxWidth().weight(1.1f))
            }
        }
    }
}

@Composable
private fun OnboardingVisual(page: OnboardingPage, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(76.dp))
        }
    }
}

@Composable
private fun OnboardingCopy(page: OnboardingPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MctxModeBadge(page.eyebrow)
        Spacer(Modifier.height(18.dp))
        Text(page.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(page.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(page.detail, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
