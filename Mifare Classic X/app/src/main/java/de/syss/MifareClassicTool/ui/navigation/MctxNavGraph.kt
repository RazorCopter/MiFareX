package de.syss.MifareClassicTool.ui.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import de.syss.MifareClassicTool.ui.usermode.AutoModeScreen
import de.syss.MifareClassicTool.ui.usermode.VendorGridScreen
import de.syss.MifareClassicTool.ui.usermode.VendorDetailScreen
import de.syss.MifareClassicTool.ui.usermode.VendorWriteViewModel
import de.syss.MifareClassicTool.ui.adminmode.VendorEditorScreen
import de.syss.MifareClassicTool.ui.adminmode.ImportExportScreen
import de.syss.MifareClassicTool.ui.adminmode.UidManagerScreen
import de.syss.MifareClassicTool.ui.adminmode.AdminHubScreen
import de.syss.MifareClassicTool.Activities.MainMenu
import de.syss.MifareClassicTool.Activities.Preferences

object Routes {
    const val USER_MODE = "user_mode"
    const val AUTO_MODE = "auto_mode"
    const val ADMIN_MODE = "admin_mode"
    const val ADMIN_HUB = "admin_hub"

    const val VENDOR_GRID = "vendor_grid"
    const val VENDOR_DETAIL = "vendor_detail/{vendorId}"
    fun vendorDetail(vendorId: String) = "vendor_detail/$vendorId"

    const val AUTO_MODE_SCREEN = "auto_mode_screen"

    const val VENDOR_EDITOR = "vendor_editor?vendorId={vendorId}"
    fun vendorEditor(vendorId: String? = null) =
        if (vendorId != null) "vendor_editor?vendorId=$vendorId"
        else "vendor_editor"
    const val IMPORT_EXPORT = "import_export"
    const val UID_MANAGER = "uid_manager"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Routes.USER_MODE,
        label = "Operatore",
        selectedIcon = Icons.Filled.Nfc,
        unselectedIcon = Icons.Outlined.Nfc
    ),
    BottomNavItem(
        route = Routes.AUTO_MODE,
        label = "Auto",
        selectedIcon = Icons.Filled.FlashOn,
        unselectedIcon = Icons.Filled.FlashOn
    ),
    BottomNavItem(
        route = Routes.ADMIN_MODE,
        label = "Admin",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun MctxNavGraph(
    navController: NavHostController,
    vendorWriteViewModel: VendorWriteViewModel,
    onNavigateToVendorDetail: (String) -> Unit,
    onNavigateToVendorEditor: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToImportExport: () -> Unit = { navController.navigate(Routes.IMPORT_EXPORT) }
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = Routes.VENDOR_GRID,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { it / 4 }
        },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it / 4 }
        },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        // === User Mode ===
        composable(Routes.VENDOR_GRID) {
            VendorGridScreen(
                onVendorClick = onNavigateToVendorDetail,
                onAddVendorClick = { onNavigateToVendorEditor(null) },
                onEditVendorClick = { vendorId -> onNavigateToVendorEditor(vendorId) },
                onImportExportClick = onNavigateToImportExport
            )
        }

        composable(
            route = Routes.VENDOR_DETAIL,
            arguments = listOf(navArgument("vendorId") { type = NavType.StringType })
        ) { backStackEntry ->
            val vendorId = backStackEntry.arguments?.getString("vendorId") ?: return@composable
            VendorDetailScreen(
                vendorId = vendorId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { onNavigateToVendorEditor(vendorId) },
                viewModel = vendorWriteViewModel
            )
        }

        // === Auto Mode ===
        composable(Routes.AUTO_MODE_SCREEN) {
            AutoModeScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = vendorWriteViewModel
            )
        }

        // === Admin Mode ===
        composable(Routes.ADMIN_HUB) {
            AdminHubScreen(
                onCreateVendor = { onNavigateToVendorEditor(null) },
                onImportExport = onNavigateToImportExport,
                onUidManager = { navController.navigate(Routes.UID_MANAGER) },
                onExpertMode = { context.startActivity(Intent(context, MainMenu::class.java)) },
                onSettings = { context.startActivity(Intent(context, Preferences::class.java)) }
            )
        }

        composable(
            route = Routes.VENDOR_EDITOR,
            arguments = listOf(
                navArgument("vendorId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val vendorId = backStackEntry.arguments?.getString("vendorId")
            VendorEditorScreen(
                vendorId = vendorId,
                onBackClick = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                // After delete, pop editor AND VendorDetailScreen to land on grid
                onDeleted = {
                    navController.popBackStack()
                    if (vendorId != null) navController.popBackStack()
                }
            )
        }

        composable(Routes.IMPORT_EXPORT) {
            ImportExportScreen(
                onBackClick = { navController.popBackStack() },
                onUidManagerClick = { navController.navigate(Routes.UID_MANAGER) }
            )
        }

        composable(Routes.UID_MANAGER) {
            UidManagerScreen(onBackClick = { navController.popBackStack() })
        }

    }
}
