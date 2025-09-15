// 6.10 이은상 얼굴 네모 ui 추가 (StartModelScreen)  33 -> 31
// 얼굴 네모 모양을 위해 StartModelViewModel.kt 함수 및 변수 추가
// 6.10 김봉국 눈감김시 알림소리와 경고화면추가 MainActivity.kt 함수 및 변수추가
//
// 6.11 김봉국
// - Room 데이터베이스 연동 (DrowsinessRepository, AppDatabase, Dao, Entity)
// - '기록' 화면(RecordsScreen) 및 관련 기능(차트, 목록 UI) 추가
// - '기록' 화면으로 이동하기 위한 네비게이션 경로 및 메뉴 버튼 추가
// - 운전 시작 시 EAR 값 및 경고 횟수를 DB에 저장하도록 ViewModel 연동
// - 전체적인 import 구문 정리 및 빌드 오류 수정

package com.example.dpa // 실제 프로젝트의 패키지 이름으로 되어있는지 확인하세요!

// [6.13 김봉국 추가] 기능 구현에 필요한 모든 import 문
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.dpa.data.AppDatabase
import com.example.dpa.data.DrowsinessDao
import com.example.dpa.data.EarRecord
import com.example.dpa.data.WarningRecord
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import com.example.dpa.ui.theme.DPATheme
//추가 6.13김봉국
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import io.github.boguszpawlowski.composecalendar.selection.SelectionState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollState

// [6.13 오태성 추가]
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight


// [6.11 김봉국 수정] 'Records' 화면 경로 추가
sealed class Screen(val route: String) {
    data object Menu : Screen("menu_screen")
    data object StartModel : Screen("start_model_screen")
    data object TestScreen : Screen("test_screen")
    data object Settings : Screen("settings_screen")
    data object BugReport : Screen("bug_report_screen")
    data object About : Screen("about_screen")
    data object Records : Screen("records_screen") // 기록 화면 경로
}

// [6.11 김봉국 추가] 데이터 저장을 위한 Repository 클래스
class DrowsinessRepository(private val drowsinessDao: DrowsinessDao) {
    fun getAllWarningRecords(): Flow<List<WarningRecord>> = drowsinessDao.getAllWarningRecords()
    fun getEarRecordsByDate(date: String): Flow<List<EarRecord>> = drowsinessDao.getEarRecordsByDate(date)

    suspend fun incrementWarningCountForToday() {
        withContext(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentRecord = drowsinessDao.getWarningRecordByDate(today)
            if (currentRecord == null) {
                drowsinessDao.insertOrUpdateWarning(WarningRecord(date = today, count = 1))
            } else {
                drowsinessDao.insertOrUpdateWarning(currentRecord.copy(count = currentRecord.count + 1))
            }
        }
    }

    suspend fun saveEarValue(ear: Float) {
        withContext(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val earRecord = EarRecord(timestamp = System.currentTimeMillis(), earValue = ear, date = today)
            drowsinessDao.insertEarRecord(earRecord)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    // [6.11 김봉국 추가] DB와 Repository 인스턴스를 Activity 범위에서 생성
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { DrowsinessRepository(database.drowsinessDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DPATheme {
                val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
                when {
                    cameraPermissionState.status.isGranted -> {
                        // [6.11 김봉국 수정] 필요한 모든 ViewModel을 여기서 생성하여 하위 Composable에 전달
                        val settingsDataStore = remember { SettingsDataStore(applicationContext) }
                        val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(settingsDataStore))
                        val recordsViewModel: RecordsViewModel = viewModel(factory = RecordsViewModelFactory(repository))
                        val startModelViewModel: StartModelViewModel = viewModel(factory = StartModelViewModelFactory(application, repository))

                        AppNavigation(settingsViewModel, recordsViewModel, startModelViewModel)
                    }
                    cameraPermissionState.status.shouldShowRationale -> {
                        PermissionRationaleScreen { cameraPermissionState.launchPermissionRequest() }
                    }
                    else -> {
                        PermissionRequestScreen(
                            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                            onGoToSettings = {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                                    it.data = Uri.fromParts("package", packageName, null)
                                    startActivity(it)
                                }
                            }
                        )
                        LaunchedEffect(cameraPermissionState.status) {
                            if (!cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale) {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        }
                    }
                }
            }
        }
    }
}

// [6.11 김봉국 추가] ViewModel을 생성하기 위한 Factory 클래스들
class StartModelViewModelFactory(private val application: Application, private val repository: DrowsinessRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StartModelViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StartModelViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// [6.13 김봉국수정] RecordsViewModel에 캘린더용 데이터 추가
class RecordsViewModel(private val repository: DrowsinessRepository) : ViewModel() {
    val allWarningRecords: StateFlow<List<WarningRecord>> = repository.getAllWarningRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val warningRecordsMap: StateFlow<Map<LocalDate, WarningRecord>> =
        allWarningRecords.map { records ->
            records.mapNotNull { record ->
                try {
                    LocalDate.parse(record.date, DateTimeFormatter.ISO_LOCAL_DATE) to record
                } catch (e: Exception) {
                    Log.e("RecordsViewModel", "Invalid date format for record: $record", e)
                    null
                }
            }.toMap()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate
    val earRecordsForSelectedDate = MutableStateFlow<List<EarRecord>>(emptyList())

    // [수정] 날짜 선택 및 해제를 모두 처리하도록 함수 로직 변경
    fun selectDate(date: String?) { // String? nullable 타입으로 변경
        if (_selectedDate.value == date) return // 같은 날짜를 다시 누른 경우, 불필요한 작업 방지

        _selectedDate.value = date
        if (date != null) {
            viewModelScope.launch {
                repository.getEarRecordsByDate(date).collect { earRecordsForSelectedDate.value = it }
            }
        } else {
            // 날짜 선택이 해제되면 EAR 기록도 비웁니다.
            earRecordsForSelectedDate.value = emptyList()
        }
    }
}

class RecordsViewModelFactory(private val repository: DrowsinessRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// 기존 SettingsViewModelFactory
class SettingsViewModelFactory(private val settingsDataStore: SettingsDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(settingsDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun PermissionRationaleScreen(onRetryPermissionRequest: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
            Text("카메라 권한이 필요합니다.", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("이 앱은 졸음 감지 기능을 제공하기 위해 카메라를 사용합니다. 원활한 서비스 이용을 위해 카메라 권한을 허용해주세요.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryPermissionRequest) {
                Text("권한 다시 요청")
            }
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit, onGoToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(16.dp)) {
            Text("카메라 권한 안내", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("졸음운전 방지 기능 사용을 위해 카메라 접근 권한이 필요합니다.", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onRequestPermission() }) {
                Text("권한 요청하기")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onGoToSettings) {
                Text("앱 설정으로 이동 (권한 직접 설정)")
            }
        }
    }
}

// [6.11 김봉국 수정] AppNavigation에 ViewModel 파라미터 추가 및 'Records' 경로 추가
@Composable
fun AppNavigation(
    settingsViewModel: SettingsViewModel,
    recordsViewModel: RecordsViewModel,
    startModelViewModel: StartModelViewModel
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Menu.route) {
        composable(Screen.Menu.route) {
            MenuScreen(navController = navController)
        }
        composable(Screen.StartModel.route) {
            Scaffold { innerPadding ->
                StartModelScreen(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    viewModel = startModelViewModel
                )
            }
        }
        composable(Screen.TestScreen.route) {
            Scaffold { innerPadding ->
                DrowsinessAppScreen(modifier = Modifier.padding(innerPadding), settingsViewModel = settingsViewModel)
            }
        }
        composable(Screen.Settings.route) {
            Scaffold { innerPadding ->
                SettingsScreen(navController, Modifier.padding(innerPadding), settingsViewModel)
            }
        }
        composable(Screen.BugReport.route) {
            Scaffold { innerPadding ->
                BugReportScreen(navController, Modifier.padding(innerPadding))
            }
        }
        composable(Screen.About.route) {
            Scaffold { innerPadding ->
                AboutScreen(Modifier.padding(innerPadding))
            }
        }
        // [6.11 김봉국 추가] 기록 화면으로 이동하는 composable 정의
        composable(Screen.Records.route) {
            Scaffold { innerPadding ->
                RecordsScreen(
                    navController = navController,
                    modifier = Modifier.padding(innerPadding),
                    viewModel = recordsViewModel
                )
            }
        }
    }
}

// [6.11 김봉국 수정] MenuScreen에 '기록' 버튼 추가
@Composable
fun MenuScreen(navController: NavController, modifier: Modifier = Modifier) {
    var buttonsEnabled by remember { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(navBackStackEntry) {
        buttonsEnabled = navBackStackEntry?.destination?.route == Screen.Menu.route
    }

    Box(
        modifier = modifier
            .fillMaxSize()
//            .background(MaterialTheme.colorScheme.background)
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF42A5F5).copy(alpha = 0.08f),
                        androidx.compose.ui.graphics.Color.White,
                        androidx.compose.ui.graphics.Color(0xFF81D4FA).copy(alpha = 0.05f)
                    )
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // [6.13 오태성 수정] 로고 변경
            Card(
                modifier = Modifier
                    .size(160.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.dpa_icon),
                        contentDescription = "앱 로고",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "졸음운전 방지 앱",
                fontSize = 32.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = androidx.compose.ui.graphics.Color.Black,
                textAlign = TextAlign.Center
            )


            Spacer(modifier = Modifier.height(24.dp))

            val buttonModifier = Modifier
                .fillMaxWidth(0.85f)
                .height(56.dp)

//            val buttonModifier = Modifier.fillMaxWidth(0.7f)
//            val buttonFontSize = 18.sp
            val buttonSpacing = 1.dp

            Button(
                onClick = { if (buttonsEnabled) navController.navigate(Screen.StartModel.route) },
                enabled = buttonsEnabled,
                modifier = buttonModifier,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFF42A5F5),
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp
                )
            ) {
                Text(
                text = "▶ 운전 시작",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ) }
//            Spacer(modifier = Modifier.height(buttonSpacing))

            // [6.11 김봉국 추가] 기록 화면으로 가는 버튼
            Button(
                onClick = { if (buttonsEnabled) navController.navigate(Screen.Records.route) },
                enabled = buttonsEnabled,
                modifier = buttonModifier,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) { Text(
                text = "📅 기록",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ) }
//            Spacer(modifier = Modifier.height(buttonSpacing))

            Button(
                onClick = { if (buttonsEnabled) navController.navigate(Screen.Settings.route) },
                enabled = buttonsEnabled,
                modifier = buttonModifier,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) { Text(
                text = "⚙️ 설정",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ) }
//            Spacer(modifier = Modifier.height(buttonSpacing))
            Button(
                onClick = { if (buttonsEnabled) navController.navigate(Screen.BugReport.route) },
                enabled = buttonsEnabled,
                modifier = buttonModifier,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) { Text(
                text = "✉️ 오류신고",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ) }
//            Spacer(modifier = Modifier.height(buttonSpacing))
            Button(
                onClick = { if (buttonsEnabled) navController.navigate(Screen.About.route) },
                enabled = buttonsEnabled,
                modifier = buttonModifier,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp
                )
            ) { Text(
                text = "ℹ️ 만든이",
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            ) }
        }
        // [6.13 오태성 수정] 개선된 버전 정보
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF5F5F5).copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "ver 0.1",
                    fontSize = 14.sp,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// [6.11 김봉국 수정] StartModelScreen이 ViewModel을 외부에서 주입받도록 수정
@SuppressLint("DefaultLocale")
@Composable
fun StartModelScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: StartModelViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val earValue by viewModel.earState.collectAsStateWithLifecycle()
    val surfaceRequest by viewModel.surfaceRequest.collectAsState()
    val faceBox by viewModel.faceBox
    val imageWidth by viewModel.imageWidth
    val imageHeight by viewModel.imageHeight
    val showWarningOverlay by viewModel.isWarningActive.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val job = lifecycleOwner.lifecycleScope.launch {
            viewModel.bindToCamera(lifecycleOwner)
        }
        onDispose {
            job.cancel()
            viewModel.releaseResources()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        surfaceRequest?.let { request ->
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        this.surfaceProvider.onSurfaceRequested(request)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        faceBox?.let { rect ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (imageWidth > 0f && imageHeight > 0f) {
                    val scaleX = size.width / imageWidth
                    val scaleY = size.height / imageHeight

                    // --- 여기부터 수정 ---
                    // 좌우 반전(미러 모드)된 카메라에 맞게 x 좌표를 뒤집어줍니다.
                    // 원본의 '오른쪽'이 화면의 '왼쪽'이 되고, 원본의 '왼쪽'이 화면의 '오른쪽'이 됩니다.
                    val left = size.width - (rect.right * scaleX)
                    val top = rect.top * scaleY
                    val right = size.width - (rect.left * scaleX)
                    val bottom = rect.bottom * scaleY
                    // --- 여기까지 수정 ---

                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(left, top), // 새로 계산된 좌표 사용
                        size = Size(right - left, bottom - top),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        if (showWarningOverlay) {
            val infiniteTransition = rememberInfiniteTransition(label = "BlinkingEffect")
            val blinkingAlpha by infiniteTransition.animateFloat(
                initialValue = 0.0f,
                targetValue = 0.5f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                label = "BlinkingAlpha"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = blinkingAlpha))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val earText = String.format("EAR: %.3f", earValue) + if (showWarningOverlay) " (경고!)" else ""
            Text(
                text = earText,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

// [6.13 김봉국 추가] 캘린더의 각 날짜 셀 UI를 정의하는 Composable
@Composable
fun <T : SelectionState> CalendarDay(
    state: DayState<T>, // 제네릭으로 수정
    warningRecord: WarningRecord?,
    onClick: () -> Unit // 추가!
) {
    val date = state.date
    // 수정: state.selectionState를 통해 선택 여부 확인
    val isSelected = state.selectionState.isDateSelected(date)

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        state.isCurrentDay -> MaterialTheme.colorScheme.primary // 현재 날짜 강조
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp, horizontal = 4.dp)
            .background(color = backgroundColor, shape = RoundedCornerShape(25))
            .aspectRatio(1f)
            .clickable { onClick() }, // clickable 적용
        contentAlignment = Alignment.Center // 수정: 불필요한 fully qualified 제거
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            if (warningRecord != null) {
                Text(
                    text = "${warningRecord.count}회",
                    style = MaterialTheme.typography.labelSmall,
                    color = if(isSelected) MaterialTheme.colorScheme.onPrimary else Color.Red
                )
            }
        }
    }
}
// [6.13 김봉국 수정] RecordsScreen Composable 전체를 캘린더 UI로 변경
@Composable
fun RecordsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel
) {
    val warningRecordsMap by viewModel.warningRecordsMap.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val earRecords by viewModel.earRecordsForSelectedDate.collectAsState()
    val chartEntryModelProducer = remember { ChartEntryModelProducer() }

    val calendarState = rememberSelectableCalendarState(
        initialSelection = selectedDate?.let { listOf(LocalDate.parse(it)) } ?: emptyList(),
    )

    // (UI -> 데이터) 캘린더 클릭을 ViewModel에 전달
    LaunchedEffect(calendarState.selectionState.selection) {
        val userClickedDate = calendarState.selectionState.selection.firstOrNull()?.toString()
        if (userClickedDate != selectedDate) {
            viewModel.selectDate(userClickedDate)
        }
    }

    // (데이터 -> UI) ViewModel의 상태 변화를 캘린더 UI에 반영
    LaunchedEffect(selectedDate) {
        val date = selectedDate?.let { LocalDate.parse(it) }
        if (calendarState.selectionState.selection.firstOrNull() != date) {
            calendarState.selectionState.selection = if (date != null) listOf(date) else emptyList()
        }
    }

    LaunchedEffect(earRecords) {
        val chartData = earRecords.mapIndexed { index, record ->
            FloatEntry(index.toFloat(), record.earValue)
        }
        chartEntryModelProducer.setEntries(chartData)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "운전 기록",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
        Text(text = "날짜별 경고 횟수", style = MaterialTheme.typography.titleLarge)

        SelectableCalendar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            calendarState = calendarState,
            dayContent = { dayState ->
                // [핵심 수정] dayState를 그대로 전달하여 타입 불일치 문제를 해결합니다.
                CalendarDay(
                    state = dayState,
                    warningRecord = warningRecordsMap[dayState.date],
                    onClick = { calendarState.selectionState.onDateSelected(dayState.date) } // onClick 연결
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedDate != null) {
            Text(text = "$selectedDate EAR 값 변화", style = MaterialTheme.typography.titleLarge)
            if (earRecords.isNotEmpty()) {
                Chart(
                    chart = lineChart(),
                    chartModelProducer = chartEntryModelProducer,
                    startAxis = rememberStartAxis(),
                    bottomAxis = rememberBottomAxis(),
                    chartScrollState = rememberChartScrollState(), // 패키지명 명시
                    isZoomEnabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("해당 날짜의 EAR 기록이 없습니다.")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("메뉴로 돌아가기")
        }
    }
}
// [6.13 김봉국 수정] WarningRecordItem은 캘린더 UI로 대체되었으므로 주석 처리 또는 삭제
/*
@Composable
fun WarningRecordItem(record: WarningRecord, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = record.date, style = MaterialTheme.typography.bodyLarge)
            Text(text = "경고 ${record.count}회", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
*/

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar, Card 사용을 위한 @OptIn 추가
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel
) {
    val currentSettings by settingsViewModel.settings.collectAsState()
    val scrollState = rememberScrollState() // 스크롤 상태 변수 추가

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState) // 스크롤 수식어 적용
        ) {
            // 설정 버튼 정리
            // 슬라이더 그룹
            Text(
                text = "조절 설정",
                style = MaterialTheme.typography.titleLarge, // 제목 크기 조정
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 경고음 볼륨 슬라이더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("경고음 볼륨", modifier = Modifier.weight(0.3f), fontSize = 16.sp)
                        Slider(
                            value = currentSettings.volumeLevel,
                            onValueChange = { settingsViewModel.setVolumeLevel(it) },
                            valueRange = 0f..1f,
                            steps = 9, // 0.1f 단위
                            enabled = currentSettings.isSoundEnabled,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Text(
                        "현재 볼륨: ${(currentSettings.volumeLevel * 100).toInt()}%",
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider() // 구분선 추가
                    Spacer(modifier = Modifier.height(16.dp))

                    // 진동 강도 슬라이더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("진동 강도", modifier = Modifier.weight(0.3f), fontSize = 16.sp)
                        Slider(
                            value = currentSettings.vibrationStrength.toFloat(),
                            onValueChange = { settingsViewModel.setVibrationStrength(it.toInt()) },
                            valueRange = 0f..255f,
                            steps = 254,
                            enabled = currentSettings.isVibrationEnabled,
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Text(
                        "현재 진동 강도: ${currentSettings.vibrationStrength}",
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider() // 구분선 추가
                    Spacer(modifier = Modifier.height(16.dp))

                    // EAR 임계값 슬라이더
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("EAR 임계값", modifier = Modifier.weight(0.3f), fontSize = 16.sp)
                        Slider(
                            value = currentSettings.earThreshold,
                            onValueChange = { settingsViewModel.setEarThreshold(it) },
                            valueRange = 0.0f..0.3f, // EAR 값 범위
                            steps = 29, // 0.01f 단위 (0.00 ~ 0.29)
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Text(
                        "현재 EAR 임계값: %.2f".format(currentSettings.earThreshold),
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider() // 구분선 추가
                    Spacer(modifier = Modifier.height(16.dp))

                    // 눈 감김 지속 시간 슬라이더 (ms를 초로 표시)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("졸음 시간(초)", modifier = Modifier.weight(0.3f), fontSize = 16.sp)
                        Slider(
                            value = currentSettings.drowsinessDurationThreshold.toFloat() / 1000f, // ms를 초로 변환
                            onValueChange = { settingsViewModel.setDrowsinessDurationThreshold((it * 1000).toLong()) },
                            valueRange = 0.5f..10f, // 0.5초에서 10초 사이로 조정
                            steps = 19, // 0.5초 단위 (0.5, 1.0, 1.5, ..., 10.0)
                            modifier = Modifier.weight(0.7f)
                        )
                    }
                    Text(
                        "현재 지속 시간: ${currentSettings.drowsinessDurationThreshold / 1000}초", // 초 단위로 표시
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 4.dp),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 토글/스위치 그룹
            Text(
                text = "알림 설정",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 소리 알림 스위치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("소리 알림", fontSize = 16.sp)
                        Switch(
                            checked = currentSettings.isSoundEnabled,
                            onCheckedChange = { settingsViewModel.setSoundEnabled(it) }
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 화면 플래싱 알림 스위치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("화면 플래싱", fontSize = 16.sp)
                        Switch(
                            checked = currentSettings.isFlashingEnabled,
                            onCheckedChange = { settingsViewModel.setFlashingEnabled(it) }
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // 진동 알림 스위치
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("진동 알림", fontSize = 16.sp)
                        Switch(
                            checked = currentSettings.isVibrationEnabled,
                            onCheckedChange = { settingsViewModel.setVibrationEnabled(it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate(Screen.TestScreen.route) },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .align(Alignment.CenterHorizontally)
            ) { Text("기능 테스트 화면으로 이동", fontSize = 16.sp) }
            Spacer(Modifier.weight(1f)) // Spacer를 추가하여 버튼이 하단에 정렬되도록 함 (스크롤 시에도 적용)
            Button(
                onClick = {
                    Toast.makeText(navController.context, "설정 적용됨", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) { Text("확인 (메뉴로 돌아가기)") }
        }
    }
}


//@Composable
//fun SettingItemRow(text: String, isChecked: Boolean, onCheckedChanged: (Boolean) -> Unit) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(text, fontSize = 18.sp)
//        Switch(checked = isChecked, onCheckedChange = onCheckedChanged)
//    }
//}

@SuppressLint("UseKtx")
@Composable
fun BugReportScreen(navController: NavController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("오류 신고", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
        Text(
            "앱 사용 중 발생한 문제나 개선사항이 있다면\n아래 버튼을 눌러 메일을 보내주세요.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("y.enter00@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "졸음운전 방지 앱 (DPA) 오류 신고")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "메일 앱을 실행할 수 없습니다.", Toast.LENGTH_LONG).show()
            }
        }) { Text("오류 메일 보내기") }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) { Text("메뉴로 돌아가기") }
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("만든이", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("4조", fontSize = 18.sp)
            Text("버전 1.0", fontSize = 16.sp)
        }
    }
}

@Composable
fun DrowsinessAppScreen(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel,
    isPreview: Boolean = false
) {
    val currentSettings by settingsViewModel.settings.collectAsState() // [수정] settings 사용
    val context = LocalContext.current
    var isTestingActive by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
    val vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) { Text(if(isPreview) "테스트 화면입니다" else "테스트 화면 입니다", color = Color.White) }

            if (isTestingActive) {
                val statusText = when {
                    currentSettings.isSoundEnabled && mediaPlayer?.isPlaying == true -> "경고음 및 깜빡임 테스트 중..."
                    currentSettings.isFlashingEnabled -> "화면 깜빡임 테스트 중..."
                    currentSettings.isVibrationEnabled -> "진동 테스트 중..."
                    else -> "테스트 중 (활성화된 효과 없음)"
                }
                Text(text = statusText, fontSize = 24.sp, modifier = Modifier.padding(vertical = 16.dp))
            }

            Spacer(modifier = Modifier.weight(if (isTestingActive) 0.2f else 0.4f))

            Button(
                onClick = {
                    isTestingActive = if (isTestingActive) {
                        mediaPlayer?.stop(); mediaPlayer?.release(); mediaPlayer = null
                        vibrator.cancel()
                        false
                    } else {
                        var anyEffectWillPlay = false
                        if (currentSettings.isSoundEnabled) {
                            try {
                                mediaPlayer = MediaPlayer.create(context, R.raw.alarm_sound)?.apply {
                                    isLooping = true
                                    setVolume(currentSettings.volumeLevel, currentSettings.volumeLevel)
                                    start()
                                }
                                anyEffectWillPlay = true
                            } catch (e: Exception) {
                                Toast.makeText(context, "경고음 파일 재생 중 오류", Toast.LENGTH_SHORT).show()
                            }
                        }
                        if (currentSettings.isFlashingEnabled) anyEffectWillPlay = true
                        if (currentSettings.isVibrationEnabled) {
                            anyEffectWillPlay = true
                            val pattern = longArrayOf(0, 200, 100, 200, 100, 200)
                            val amplitudes = intArrayOf(0, 150, 0, 200, 0, currentSettings.vibrationStrength)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(pattern, -1)
                            }
                        }
                        anyEffectWillPlay
                    }
                },
                modifier = Modifier.padding(all = 16.dp)
            ) { Text(if (isTestingActive) "테스트 중지" else "테스트 시작") }
        }

        if (isTestingActive && currentSettings.isFlashingEnabled) {
            val infiniteTransition = rememberInfiniteTransition(label = "")
            val blinkingAlpha by infiniteTransition.animateFloat(
                initialValue = 0.0f, targetValue = 0.3f,
                animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = ""
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = blinkingAlpha))
            )
        }
    }
}

@Preview(showBackground = true, name = "Menu Screen Preview")
@Composable
fun MenuScreenPreview() { DPATheme { MenuScreen(rememberNavController()) } }

@Preview(showBackground = true, name = "Settings Screen Preview")
@Composable
fun SettingsScreenPreview() {
    DPATheme {
        val context = LocalContext.current
        val settingsDataStore = remember { SettingsDataStore(context) }
        val settingsViewModel = remember { SettingsViewModel(settingsDataStore) }
        SettingsScreen(
            navController = rememberNavController(),
            settingsViewModel = settingsViewModel
        )
    }
}

@Preview(showBackground = true, name = "Drowsiness App Screen Preview")
@Composable
fun DrowsinessAppScreenPreview() {
    DPATheme {
        val context = LocalContext.current
        val settingsDataStore = remember { SettingsDataStore(context) }
        val settingsViewModel = remember { SettingsViewModel(settingsDataStore) }
        DrowsinessAppScreen(
            settingsViewModel = settingsViewModel,
            isPreview = true
        )
    }
}

@Preview(showBackground = true, name = "StartModelScreen Preview")
@Composable
fun StartModelScreenPreview() {
    DPATheme {
        val fakeEarValue = remember { mutableStateOf(0.15f) }
        val fakeIsWarningActive = remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text("카메라 미리보기 영역 (Preview)", color = Color.White, fontSize = 20.sp)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.Green,
                        topLeft = Offset(size.width * 0.2f, size.height * 0.3f),
                        size = Size(size.width * 0.6f, size.height * 0.4f),
                        style = Stroke(width = 4f)
                    )
                }
            }

            if (fakeIsWarningActive.value) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val blinkingAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.0f, targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = ""
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = blinkingAlpha)))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = String.format("EAR: %.3f", fakeEarValue.value) + if (fakeIsWarningActive.value) " (경고!)" else "",
                    color = Color.White,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
