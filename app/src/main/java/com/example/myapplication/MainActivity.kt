package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.AurafitTheme
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class DietItem(val name: String, val protein: String, val desc: String)
data class HostelFood(val name: String, val price: Int, val protein: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AurafitTheme {
                AurafitApp()
            }
        }
    }
}

@Composable
fun AurafitApp(viewModel: AurafitViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf("welcome", "nutrition", "user_profile", "budget_nutrition", "meal_guardian", "chat_bro", "progress_vault") || 
                       currentRoute?.startsWith("muscle_selection") == true ||
                       currentRoute?.startsWith("workout") == true || 
                       currentRoute?.startsWith("exercise_detail") == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AurafitBottomBar(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("splash") {
                SplashScreen {
                    navController.navigate("user_profile") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
            composable("user_profile") {
                UserProfileScreen(viewModel, onUserSelected = {
                    navController.navigate("welcome")
                })
            }
            composable("welcome") { WelcomeScreen(navController, viewModel) }
            composable("muscle_selection/{level}") { backStackEntry ->
                val level = backStackEntry.arguments?.getString("level") ?: "Beginner"
                MuscleSelectionScreen(level, navController)
            }
            composable("workout/{level}/{category}") { backStackEntry ->
                val level = backStackEntry.arguments?.getString("level") ?: "Beginner"
                val category = backStackEntry.arguments?.getString("category") ?: "Chest"
                ExerciseListScreen(level, category, viewModel, navController)
            }
            composable("exercise_detail/{exerciseId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("exerciseId")?.toIntOrNull() ?: 0
                ExerciseDetailScreen(id, viewModel, navController)
            }
            composable("nutrition") { NutritionScreen(navController, viewModel) }
            composable("budget_nutrition") { BudgetNutritionScreen(navController) }
            composable("meal_guardian") { MealGuardianScreen(navController, viewModel) }
            composable("chat_bro") { ChatBroScreen(navController, viewModel) }
            composable("progress_vault") { ProgressVaultScreen(navController, viewModel) }
        }
    }
}

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.8f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color(0xFF00FF88),
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .shadow(20.dp, CircleShape, spotColor = Color(0xFF00FF88))
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "AURAFIT",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 10.sp
            )
            Text(
                text = "EVOLVE YOUR STRENGTH",
                fontSize = 12.sp,
                color = Color(0xFF00FF88),
                letterSpacing = 4.sp
            )
        }
    }
}

@Composable
fun UserProfileScreen(viewModel: AurafitViewModel, onUserSelected: () -> Unit) {
    val users by viewModel.allUsers.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Select Athlete",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier.padding(top = 80.dp, bottom = 48.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(users) { user ->
                UserCard(user) {
                    viewModel.selectUser(user)
                    onUserSelected()
                }
            }
            if (users.size < 3) {
                item {
                    AddUserCard { showAddDialog = true }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1E1E1E),
            title = { Text("New User Profile", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = newUserName,
                    onValueChange = { newUserName = it },
                    label = { Text("Athlete Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00FF88),
                        focusedLabelColor = Color(0xFF00FF88),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            viewModel.addUser(newUserName)
                            newUserName = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))
                ) { Text("Create", color = Color.Black, fontWeight = FontWeight.Bold) }
            }
        )
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    val gradient = Brush.linearGradient(listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5)))
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1A1A1A))
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(gradient, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(user.name.take(1).uppercase(), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(user.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddUserCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .border(2.dp, Color(0xFF333333), RoundedCornerShape(32.dp))
            .clickable(onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(90.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Add New", color = Color.Gray, fontSize = 18.sp)
    }
}

@Composable
fun WelcomeScreen(navController: NavHostController, viewModel: AurafitViewModel) {
    val selectedUser by viewModel.selectedUser.collectAsState()
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "angle"
    )

    val streak = selectedUser?.streakCount ?: 0
    val auraColors = when {
        streak < 3 -> listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5), Color(0xFF00D2FF))
        streak <= 7 -> listOf(Color(0xFF00FF88), Color(0xFF00C853), Color(0xFF00FF88))
        else -> listOf(Color(0xFF8E2DE2), Color(0xFFFFD700), Color(0xFF8E2DE2))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F0F))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Welcome back,", fontSize = 18.sp, color = Color.Gray)
                Text(
                    text = selectedUser?.name ?: "Athlete",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            
            Box(
                modifier = Modifier
                    .size(85.dp)
                    .drawBehind {
                        rotate(angle) {
                            drawCircle(
                                brush = Brush.sweepGradient(auraColors, center = center),
                                style = Stroke(width = 5.dp.toPx()),
                                alpha = 0.9f
                            )
                        }
                    }
                    .padding(8.dp)
                    .clickable { navController.navigate("user_profile") },
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = Color(0xFF1A1A1A), modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "$streak🔥", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF252525))
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CURRENT PROGRESS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "$streak Day Streak", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF00FF88), modifier = Modifier.size(45.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { navController.navigate("progress_vault") },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF252525))
        ) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TRANSFORMATION VAULT", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Track your body gains", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFF00D2FF), modifier = Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text("SELECT YOUR TRACK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))

        LevelCard("Beginner", "Foundational Strength", Color(0xFF00F260), Color(0xFF0575E6)) {
            navController.navigate("muscle_selection/Beginner")
        }
        LevelCard("Intermediate", "Hypertrophy Mastery", Color(0xFF8E2DE2), Color(0xFF4A00E0)) {
            navController.navigate("muscle_selection/Intermediate")
        }
        LevelCard("Advanced", "Elite Performance", Color(0xFFFF416C), Color(0xFFFF4B2B)) {
            navController.navigate("muscle_selection/Advanced")
        }
    }
}

@Composable
fun LevelCard(title: String, desc: String, c1: Color, c2: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(15.dp)
    ) {
        Box(modifier = Modifier.background(Brush.horizontalGradient(listOf(c1, c2))).padding(32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text(desc, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun MuscleSelectionScreen(level: String, navController: NavHostController) {
    val muscles = listOf(
        Triple("Chest", Icons.Default.VerticalAlignTop, Color(0xFFFF5252)),
        Triple("Back", Icons.Default.LineWeight, Color(0xFF448AFF)),
        Triple("Shoulders", Icons.Default.FitnessCenter, Color(0xFFFFD740)),
        Triple("Legs", Icons.AutoMirrored.Filled.DirectionsRun, Color(0xFF69F0AE)),
        Triple("Core", Icons.Default.CenterFocusStrong, Color(0xFFE040FB))
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 32.dp)) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Target Muscle", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(1), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(muscles) { (name, icon, color) ->
                MuscleCard(name, icon, color) {
                    navController.navigate("workout/$level/$name")
                }
            }
        }
    }
}

@Composable
fun MuscleCard(name: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(60.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(32.dp)) }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ExerciseListScreen(level: String, category: String, viewModel: AurafitViewModel, navController: NavHostController) {
    val allExercises by viewModel.getExercises(level).collectAsState(initial = emptyList())
    val filteredExercises = allExercises.filter { it.category.equals(category, ignoreCase = true) }
    
    val completedCount = filteredExercises.count { it.isCompleted }
    val totalCount = filteredExercises.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Column {
                Text(text = "$category Routine", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = level, color = Color(0xFF00FF88), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Workout Progress", color = Color.Gray, fontSize = 12.sp)
                    Text("$completedCount/$totalCount Done", color = Color(0xFF00FF88), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = Color(0xFF00FF88),
                    trackColor = Color(0xFF333333)
                )
            }
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filteredExercises) { exercise ->
                ExerciseListItem(exercise) { navController.navigate("exercise_detail/${exercise.id}") }
            }
        }
    }
}

@Composable
fun ExerciseListItem(exercise: Exercise, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(54.dp).background(Color(0xFF252525), CircleShape), contentAlignment = Alignment.Center) {
                Icon(imageVector = if(exercise.isCompleted) Icons.Default.CheckCircle else Icons.Default.PlayCircleFilled, null, tint = if(exercise.isCompleted) Color(0xFF00FF88) else Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Form & Manual", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ExerciseDetailScreen(id: Int, viewModel: AurafitViewModel, navController: NavHostController) {
    val exercise by viewModel.getExerciseById(id).collectAsState(initial = null)

    exercise?.let { ex ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(Color(0xFF00FF88).copy(alpha = 0.6f), Color(0xFF0F0F0F)))), contentAlignment = Alignment.Center) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, tint = Color.White) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Bolt, null, tint = Color(0xFF00FF88), modifier = Modifier.size(60.dp))
                    Text(text = ex.name.uppercase(), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp) )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row {
                    Surface(color = Color(0xFF252525), shape = RoundedCornerShape(12.dp)) { Text(ex.category, color = Color(0xFF00FF88), modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(color = Color(0xFF252525), shape = RoundedCornerShape(12.dp)) { Text(ex.level, color = Color.White, modifier = Modifier.padding(12.dp)) }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("EXERCISE GOAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text(ex.description, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp), lineHeight = 26.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text("STEP-BY-STEP MANUAL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Card(modifier = Modifier.padding(top = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFF252525))) {
                    Text(text = ex.instructions, color = Color.White.copy(alpha = 0.9f), fontSize = 17.sp, modifier = Modifier.padding(24.dp), lineHeight = 28.sp)
                }
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { viewModel.toggleExercise(ex); navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(65.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)), shape = RoundedCornerShape(20.dp)) {
                    Text(if (ex.isCompleted) "RESET PROGRESS" else "MARK AS COMPLETED", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun NutritionScreen(navController: NavHostController, viewModel: AurafitViewModel) {
    val todayMeal by produceState<MessMeal?>(initialValue = null) { value = viewModel.getTodayMeal() }
    var location by remember { mutableStateOf("Hostel") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(24.dp).verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(text = "Smart Nutrition", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White)
        
        Row(modifier = Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = location == "Hostel", onClick = { location = "Hostel" }, label = { Text("Hostel Mode") })
            FilterChip(selected = location == "Home", onClick = { location = "Home" }, label = { Text("Home Mode") })
        }

        todayMeal?.let { meal ->
            if (!meal.isHighProtein) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.1f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFFF5252))) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        val supplement = if(location == "Hostel") "50g Soya Chunks or 4 Eggs" else "100g Paneer or Greek Yogurt"
                        Text(text = "Meal Guardian: Today's meal (${meal.mealName}) is low in protein. Supplement with $supplement!", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f).clickable { navController.navigate("budget_nutrition") }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Wallet, null, tint = Color(0xFF00FF88))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Budget Optimizer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f).clickable { navController.navigate("meal_guardian") }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Shield, null, tint = Color(0xFF00D2FF))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Meal Guardian", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth().clickable { navController.navigate("chat_bro") }, colors = CardDefaults.cardColors(containerColor = Color(0xFF00FF88).copy(alpha = 0.1f)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFF00FF88))) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bro-Science AI", color = Color(0xFF00FF88), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Ask anything to your gym-bro bot.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00FF88), modifier = Modifier.size(32.dp))
            }
        }

        DietSection("PROTEIN BLUEPRINTS", listOf(
            DietItem("Paneer Stuffed Paratha", "18g P", "Recipe: Use 50g grated paneer in whole wheat dough."),
            DietItem("Soya Chunk Pulao", "28g P", "Recipe: Sauté 50g soya chunks with brown rice."),
            DietItem("Rajma Chawal + Curd", "22g P", "Recipe: Rajma curry with cooked rice and curd."),
            DietItem("Paneer Bhurji / Tofu", "24g P", "Recipe: Scramble 100g paneer with capsicum.")
        ), Color(0xFF00FF88))
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun ChatBroScreen(navController: NavHostController, viewModel: AurafitViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(messages.size) { scrollState.animateScrollTo(scrollState.maxValue) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp)) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Bro-Science AI", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                messages.forEach { msg -> ChatBubble(msg) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        Surface(color = Color(0xFF1A1A1A), tonalElevation = 8.dp) {
            Row(modifier = Modifier.padding(16.dp).navigationBarsPadding().imePadding(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = inputText, onValueChange = { inputText = it }, placeholder = { Text("Ask bro something...", color = Color.Gray) }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF00FF88), unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White), shape = RoundedCornerShape(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                FloatingActionButton(onClick = { if (inputText.isNotBlank()) { viewModel.sendMessage(inputText); inputText = "" } }, containerColor = Color(0xFF00FF88), contentColor = Color.Black, shape = CircleShape, modifier = Modifier.size(56.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null) }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start) {
        Surface(color = if (msg.isUser) Color(0xFF00FF88) else Color(0xFF252525), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (msg.isUser) 20.dp else 4.dp, bottomEnd = if (msg.isUser) 4.dp else 20.dp)) {
            Text(text = msg.text, color = if (msg.isUser) Color.Black else Color.White, modifier = Modifier.padding(16.dp), fontSize = 16.sp)
        }
    }
}

@Composable
fun ProgressVaultScreen(navController: NavHostController, viewModel: AurafitViewModel) {
    val photos by viewModel.progressPhotos.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var currentPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if (success && currentPhotoUri != null) viewModel.addProgressPhoto(currentPhotoUri.toString()) }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 32.dp)) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Progress Vault", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            val photoFile = File(context.filesDir, "images/progress_${System.currentTimeMillis()}.jpg")
            photoFile.parentFile?.mkdirs()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            currentPhotoUri = uri
            cameraLauncher.launch(uri)
        }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D2FF)), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.CameraAlt, null, tint = Color.Black); Spacer(modifier = Modifier.width(12.dp)); Text("Take Photo", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(photos) { photo ->
                Card(modifier = Modifier.aspectRatio(0.8f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                    Box {
                        AsyncImage(model = photo.filePath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        IconButton(onClick = { viewModel.deleteProgressPhoto(photo) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape).size(32.dp)) { Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MealGuardianScreen(navController: NavHostController, viewModel: AurafitViewModel) {
    val meals by viewModel.allMessMeals.collectAsState(initial = emptyList())
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 32.dp)) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Meal Guardian", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(meals) { meal ->
                var mealName by remember { mutableStateOf(meal.mealName) }
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(meal.day, color = Color(0xFF00D2FF), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Switch(checked = meal.isHighProtein, onCheckedChange = { viewModel.updateMessMeal(meal.day, mealName, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF88)))
                        }
                        OutlinedTextField(value = mealName, onValueChange = { mealName = it; viewModel.updateMessMeal(meal.day, it, meal.isHighProtein) }, placeholder = { Text("What's on the menu?") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color(0xFF333333), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    }
                }
            }
        }
    }
}

@Composable
fun DietSection(title: String, items: List<DietItem>, accent: Color) {
    Text(title.uppercase(), color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, modifier = Modifier.padding(vertical = 16.dp))
    for (item in items) {
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp)) {
            Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = 0.2f), shape = CircleShape) { Text(text = item.protein, color = accent, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontWeight = FontWeight.Black, fontSize = 14.sp) }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(item.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(item.desc, color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun BudgetNutritionScreen(navController: NavHostController) {
    var budget by remember { mutableFloatStateOf(50f) }
    val hostelFoods = listOf(HostelFood("Soya Chunks", 10, 26), HostelFood("Paneer", 40, 18), HostelFood("Eggs", 14, 12), HostelFood("Milk", 15, 8), HostelFood("Sprouts", 15, 8), HostelFood("Peanut Butter", 20, 8), HostelFood("Roasted Chana", 10, 9), HostelFood("Greek Yogurt", 50, 10), HostelFood("Dal", 10, 7))
    val selectedCombination = remember(budget) {
        val n = hostelFoods.size; val wCap = budget.toInt(); val dp = Array(n + 1) { IntArray(wCap + 1) }
        for (i in 1..n) { val food = hostelFoods[i - 1]; for (w in 0..wCap) { if (food.price <= w) dp[i][w] = maxOf(dp[i - 1][w], dp[i - 1][w - food.price] + food.protein) else dp[i][w] = dp[i - 1][w] } }
        val result = mutableListOf<HostelFood>(); var res = dp[n][wCap]; var currentW = wCap
        for (i in n downTo 1) { if (res <= 0) break; if (res != dp[i - 1][currentW]) { val food = hostelFoods[i - 1]; result.add(food); res -= food.protein; currentW -= food.price } }
        result
    }
    val totalProtein = selectedCombination.sumOf { it.protein }; val totalCost = selectedCombination.sumOf { it.price }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F)).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 32.dp)) {
            IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text("Budget Optimizer", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFF252525))) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WALLET BALANCE", color = Color(0xFF00FF88), fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Slider(value = budget, onValueChange = { budget = it }, valueRange = 10f..100f, steps = 17, colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF88), activeTrackColor = Color(0xFF00FF88), inactiveTrackColor = Color(0xFF333333)))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF252525)).padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("MAX PROTEIN", color = Color.Gray, fontSize = 10.sp); Text("${totalProtein}g", color = Color(0xFF00FF88), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                        VerticalDivider(modifier = Modifier.height(40.dp), color = Color.DarkGray)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("TOTAL SPEND", color = Color.Gray, fontSize = 10.sp); Text("₹$totalCost", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("AI SUGGESTED COMBO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 2.sp)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)) {
            items(selectedCombination) { food ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(24.dp)) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFF00FF88).copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) { Text(text = "${food.protein}g P", color = Color(0xFF00FF88), modifier = Modifier.padding(10.dp), fontWeight = FontWeight.Black, fontSize = 14.sp) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) { Text(food.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("₹${food.price}", color = Color.Gray, fontSize = 12.sp) }
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF00FF88).copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun AurafitBottomBar(navController: NavHostController) {
    NavigationBar(containerColor = Color(0xFF121212), tonalElevation = 8.dp) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        NavigationBarItem(selected = currentRoute == "welcome" || currentRoute?.startsWith("muscle_selection") == true || currentRoute?.startsWith("workout") == true || currentRoute?.startsWith("exercise_detail") == true || currentRoute == "progress_vault", onClick = { navController.navigate("welcome") }, icon = { Icon(Icons.Default.Dashboard, null) }, label = { Text("Plan") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FF88), selectedTextColor = Color(0xFF00FF88), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF00FF88).copy(alpha = 0.1f)))
        NavigationBarItem(selected = currentRoute == "nutrition" || currentRoute == "budget_nutrition" || currentRoute == "meal_guardian" || currentRoute == "chat_bro", onClick = { navController.navigate("nutrition") }, icon = { Icon(Icons.Default.RestaurantMenu, null) }, label = { Text("Diet") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FF88), selectedTextColor = Color(0xFF00FF88), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF00FF88).copy(alpha = 0.1f)))
        NavigationBarItem(selected = currentRoute == "user_profile", onClick = { navController.navigate("user_profile") }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFF00FF88), selectedTextColor = Color(0xFF00FF88), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray, indicatorColor = Color(0xFF00FF88).copy(alpha = 0.1f)))
    }
}
