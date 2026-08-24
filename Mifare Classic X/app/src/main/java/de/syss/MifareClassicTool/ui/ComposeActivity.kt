package de.syss.MifareClassicTool.ui

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.core.content.edit
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.syss.MifareClassicTool.ui.auth.AuthResult
import de.syss.MifareClassicTool.ui.auth.BiometricAuthManager
import de.syss.MifareClassicTool.ui.auth.LockScreen
import de.syss.MifareClassicTool.ui.navigation.*
import de.syss.MifareClassicTool.ui.onboarding.OnboardingScreen
import de.syss.MifareClassicTool.ui.theme.MctxTheme
import de.syss.MifareClassicTool.ui.usermode.VendorWriteViewModel

class ComposeActivity : FragmentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcFilters: Array<IntentFilter>? = null

    private val vendorWriteViewModel: VendorWriteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setupNfcForegroundDispatch()

        val prefs = getSharedPreferences("mfarex_prefs", MODE_PRIVATE)
        val onboardingDone = prefs.getBoolean("onboarding_done", false)

        setContent {
            MctxTheme {
                var showOnboarding by remember { mutableStateOf(!onboardingDone) }
                var isUnlocked by remember { mutableStateOf(!BiometricAuthManager.isAvailable(this)) }
                var authError by remember { mutableStateOf<String?>(null) }

                when {
                    showOnboarding -> {
                        OnboardingScreen(onFinish = {
                            prefs.edit { putBoolean("onboarding_done", true) }
                            showOnboarding = false
                        })
                    }
                    !isUnlocked -> {
                        LockScreen(
                            errorMessage = authError,
                            onUnlockClick = {
                                authError = null
                                BiometricAuthManager.authenticate(this) { result ->
                                    when (result) {
                                        is AuthResult.Success -> isUnlocked = true
                                        is AuthResult.Cancelled -> Unit
                                        is AuthResult.Error -> authError = result.message
                                    }
                                }
                            }
                        )
                        LaunchedEffect(Unit) {
                            BiometricAuthManager.authenticate(this@ComposeActivity) { result ->
                                when (result) {
                                    is AuthResult.Success -> isUnlocked = true
                                    is AuthResult.Cancelled -> Unit
                                    is AuthResult.Error -> authError = result.message
                                }
                            }
                        }
                    }
                    else -> MctxApp(vendorWriteViewModel = vendorWriteViewModel)
                }
            }
        }

        // Handle NFC tag delivered via cold-start intent (activity not yet in foreground)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        enableNfcForegroundDispatch()
    }

    override fun onPause() {
        super.onPause()
        disableNfcForegroundDispatch()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun setupNfcForegroundDispatch() {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        val ndef = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        nfcFilters = arrayOf(ndef)
    }

    private fun enableNfcForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent, nfcFilters,
            arrayOf(
                arrayOf("android.nfc.tech.MifareClassic"),
                arrayOf("android.nfc.tech.NfcA")
            )
        )
    }

    private fun disableNfcForegroundDispatch() {
        try {
            nfcAdapter?.disableForegroundDispatch(this)
        } catch (_: IllegalStateException) {}
    }

    @Suppress("DEPRECATION")
    private fun handleNfcIntent(intent: Intent) {
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }
        tag?.let { vendorWriteViewModel.onTagDiscovered(it) }
    }
}

@Composable
fun MctxApp(vendorWriteViewModel: VendorWriteViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.VENDOR_GRID,
        Routes.AUTO_MODE_SCREEN,
        Routes.ADMIN_HUB
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = when (item.route) {
                            Routes.USER_MODE -> currentRoute == Routes.VENDOR_GRID ||
                                    currentRoute?.startsWith("vendor_detail") == true
                            Routes.AUTO_MODE -> currentRoute == Routes.AUTO_MODE_SCREEN
                            Routes.ADMIN_MODE -> currentRoute?.startsWith("vendor_editor") == true ||
                                    currentRoute == Routes.IMPORT_EXPORT ||
                                    currentRoute == Routes.UID_MANAGER ||
                                    currentRoute == Routes.ADMIN_HUB
                            else -> false
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val targetRoute = when (item.route) {
                                    Routes.USER_MODE -> Routes.VENDOR_GRID
                                    Routes.AUTO_MODE -> Routes.AUTO_MODE_SCREEN
                            Routes.ADMIN_MODE -> Routes.ADMIN_HUB
                                    else -> Routes.VENDOR_GRID
                                }
                                navController.navigate(targetRoute) {
                                    popUpTo(Routes.VENDOR_GRID) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        MctxNavGraph(
            navController = navController,
            vendorWriteViewModel = vendorWriteViewModel,
            onNavigateToVendorDetail = { vendorId ->
                navController.navigate(Routes.vendorDetail(vendorId))
            },
            onNavigateToVendorEditor = { vendorId ->
                navController.navigate(Routes.vendorEditor(vendorId))
            },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
