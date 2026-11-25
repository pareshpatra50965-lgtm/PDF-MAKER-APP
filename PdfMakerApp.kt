package com.example.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.model.PdfToolCatalog
import com.example.model.PdfToolId
import com.example.model.PdfToolSpec
import com.example.ui.components.PdfBrandMark
import com.example.ui.components.sharePdfUri
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageToPdfScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolInfoScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedSoft
import com.example.viewmodel.ImageToPdfViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.PdfReaderViewModel

private data class BottomTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val mainTabs = listOf(
    BottomTabItem(
        route = Routes.HOME,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomTabItem(
        route = Routes.TOOLS,
        labelRes = R.string.nav_tools,
        selectedIcon = Icons.Rounded.Build,
        unselectedIcon = Icons.Outlined.Build
    ),
    BottomTabItem(
        route = Routes.FILES,
        labelRes = R.string.nav_files,
        selectedIcon = Icons.Rounded.Folder,
        unselectedIcon = Icons.Outlined.Folder
    ),
    BottomTabItem(
        route = Routes.SETTINGS,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)

@Composable
fun PdfMakerApp(
    mainViewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val homeUiState by mainViewModel.homeUiState.collectAsStateWithLifecycle()
    val filesUiState by mainViewModel.filesUiState.collectAsStateWithLifecycle()
    val selectedCategory by mainViewModel.selectedCategory.collectAsStateWithLifecycle()
    val categorizedTools by mainViewModel.filteredToolsByCategory.collectAsStateWithLifecycle()
    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val statusMessage by mainViewModel.statusMessage.collectAsStateWithLifecycle()

    val readerViewModel: PdfReaderViewModel = viewModel()

    var pendingExportPath by remember { mutableStateOf<String?>(null) }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { destinationUri ->
            val source = pendingExportPath
            pendingExportPath = null
            if (destinationUri != null && source != null) {
                mainViewModel.exportPdfToUri(source, destinationUri)
            }
        }
    )

    val openExternalPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                readerViewModel.openPdfFromUri(uri)
                navController.navigate(Routes.PDF_READER_PICKER)
            }
        }
    )

    LaunchedEffect(statusMessage) {
        val message = statusMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            mainViewModel.consumeStatusMessage()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showNavigation = currentRoute in mainTabs.map { it.route }

    val openImageToPdf: () -> Unit = {
        navController.navigate(Routes.IMAGE_TO_PDF) {
            launchSingleTop = true
        }
    }

    val navigateToTool: (PdfToolSpec) -> Unit = { tool ->
        when (tool.id) {
            PdfToolId.IMAGE_TO_PDF -> openImageToPdf()
            PdfToolId.PDF_READER -> {
                readerViewModel.clearDocument()
                navController.navigate(Routes.PDF_READER_PICKER) {
                    launchSingleTop = true
                }
            }
            else -> navController.navigate(Routes.toolInfo(tool.id)) {
                launchSingleTop = true
            }
        }
    }

    val shareFilePath: (String, String) -> Unit = { filePath, fileName ->
        try {
            val shareUri = mainViewModel.pdfRepository.getShareUri(filePath)
            sharePdfUri(context, shareUri, fileName) { err ->
                mainViewModel.showStatusMessage(err)
            }
        } catch (t: Throwable) {
            mainViewModel.showStatusMessage(t.message ?: "Unable to share PDF file.")
        }
    }

    val saveCopyFilePath: (String, String) -> Unit = { filePath, fileName ->
        pendingExportPath = filePath
        createDocumentLauncher.launch(fileName)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showNavigation && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PdfRed,
                                    selectedTextColor = PdfRed,
                                    indicatorColor = PdfRedSoft,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("bottom_nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showNavigation && useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Spacer(modifier = Modifier.height(12.dp))
                            PdfBrandMark()
                        }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        mainTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PdfRed,
                                    selectedTextColor = PdfRed,
                                    indicatorColor = PdfRedSoft,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_nav_${tab.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                uiState = homeUiState,
                                onSearchQueryChange = mainViewModel::updateHomeSearch,
                                onToolClick = navigateToTool,
                                onOpenPdfClick = { record ->
                                    navController.navigate(Routes.pdfReaderForFile(record.filePath))
                                },
                                onSharePdfClick = { record ->
                                    shareFilePath(record.filePath, record.name)
                                },
                                onSaveCopyClick = { record ->
                                    saveCopyFilePath(record.filePath, record.name)
                                },
                                onRenamePdf = mainViewModel::renamePdf,
                                onDeletePdf = mainViewModel::deletePdf,
                                onViewAllFilesClick = {
                                    navController.navigate(Routes.FILES) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                onQuickCreatePdfClick = openImageToPdf
                            )
                        }

                        composable(Routes.TOOLS) {
                            ToolsScreen(
                                selectedCategory = selectedCategory,
                                tools = categorizedTools,
                                onSelectCategory = mainViewModel::selectToolCategory,
                                onToolClick = navigateToTool
                            )
                        }

                        composable(Routes.FILES) {
                            FilesScreen(
                                uiState = filesUiState,
                                onSearchQueryChange = mainViewModel::updateFilesSearch,
                                onSortOptionChange = mainViewModel::updateSortOption,
                                onOpenPdfClick = { record ->
                                    navController.navigate(Routes.pdfReaderForFile(record.filePath))
                                },
                                onSharePdfClick = { record ->
                                    shareFilePath(record.filePath, record.name)
                                },
                                onSaveCopyClick = { record ->
                                    saveCopyFilePath(record.filePath, record.name)
                                },
                                onRenamePdf = mainViewModel::renamePdf,
                                onDeletePdf = mainViewModel::deletePdf,
                                onBrowseDevicePdfClick = {
                                    openExternalPdfLauncher.launch(arrayOf("application/pdf"))
                                },
                                onCreateNewPdfClick = openImageToPdf
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                savedFileCount = filesUiState.files.size,
                                totalStorageBytes = filesUiState.totalStorageBytes,
                                onUpdatePageSize = mainViewModel::updateDefaultPageSize,
                                onUpdateMargin = mainViewModel::updateDefaultMargin,
                                onUpdateQuality = mainViewModel::updateImageQuality,
                                onDeleteAllPdfs = mainViewModel::deleteAllPdfs
                            )
                        }

                        composable(Routes.IMAGE_TO_PDF) {
                            val imageToPdfViewModel: ImageToPdfViewModel = viewModel()
                            ImageToPdfScreen(
                                viewModel = imageToPdfViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onPdfCreatedRefresh = { mainViewModel.refreshFiles() },
                                onOpenPdf = { record ->
                                    navController.navigate(Routes.pdfReaderForFile(record.filePath))
                                },
                                onSharePdf = { record ->
                                    shareFilePath(record.filePath, record.name)
                                },
                                onDownloadPdf = { record ->
                                    mainViewModel.downloadPdfToPublicStorage(
                                        sourcePath = record.filePath,
                                        displayName = record.name,
                                        onLegacyPickDestination = {
                                            saveCopyFilePath(record.filePath, record.name)
                                        }
                                    )
                                }
                            )
                        }

                        composable(Routes.PDF_READER_PICKER) {
                            PdfReaderScreen(
                                initialFilePath = null,
                                recentFiles = filesUiState.files,
                                viewModel = readerViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onShareFilePath = shareFilePath,
                                onSaveCopyFilePath = saveCopyFilePath
                            )
                        }

                        composable(
                            route = Routes.PDF_READER_FILE,
                            arguments = listOf(navArgument("encodedPath") { type = NavType.StringType })
                        ) { entry ->
                            val encoded = entry.arguments?.getString("encodedPath").orEmpty()
                            val decodedPath = Uri.decode(encoded)
                            PdfReaderScreen(
                                initialFilePath = decodedPath,
                                recentFiles = filesUiState.files,
                                viewModel = readerViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onShareFilePath = shareFilePath,
                                onSaveCopyFilePath = saveCopyFilePath
                            )
                        }

                        composable(
                            route = Routes.TOOL_INFO,
                            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
                        ) { entry ->
                            val rawId = entry.arguments?.getString("toolId")
                            val toolId = PdfToolId.entries.firstOrNull { it.name == rawId }
                                ?: PdfToolId.SCAN_TO_PDF
                            val toolSpec = PdfToolCatalog.findById(toolId)
                            ToolInfoScreen(
                                tool = toolSpec,
                                onNavigateBack = { navController.popBackStack() },
                                onOpenImageToPdf = {
                                    navController.navigate(Routes.IMAGE_TO_PDF) {
                                        popUpTo(Routes.TOOL_INFO) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
