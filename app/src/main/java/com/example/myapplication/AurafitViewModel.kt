package com.example.myapplication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.*

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class AurafitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val exerciseDao = db.exerciseDao()
    private val userDao = db.userDao()
    private val messMealDao = db.messMealDao()
    private val progressPhotoDao = db.progressPhotoDao()

    val allUsers: Flow<List<User>> = userDao.getAllUsers()
    val allMessMeals: Flow<List<MessMeal>> = messMealDao.getAllMeals()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    val progressPhotos: Flow<List<ProgressPhoto>> = _selectedUser.flatMapLatest { user ->
        user?.let { progressPhotoDao.getPhotosForUser(it.id) } ?: flowOf(emptyList())
    }

    // --- AI Chat Logic ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Yo! Aurafit-Bot here. Kya haal hai brute? Muscle build karna hai ya weight loss? Poochh jo poochhna hai!", false)
    ))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_GEMINI_API_KEY",
        systemInstruction = content { text("You are Aurafit-Bot, a blunt, scientific, and motivating gym-bro for Indian college students. Answer fitness and diet questions in a mix of Hindi and English (Hinglish). Keep answers short and actionable. Support 'Bulking' and 'Cutting' phases. Be strict about consistency.") }
    )
    private val chat = generativeModel.startChat()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        
        val userMsg = ChatMessage(userText, true)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userText)
                val botMsg = ChatMessage(response.text ?: "Bhai, network issue hai shayad. Fir se bol.", false)
                _chatMessages.value = _chatMessages.value + botMsg
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage("Error: ${e.message}. API Key check karle bhai.", false)
            }
        }
    }

    fun selectUser(user: User) {
        _selectedUser.value = user
    }

    fun addUser(name: String) {
        viewModelScope.launch {
            if (userDao.getUserCount() < 3) {
                userDao.insertUser(User(name = name))
            }
        }
    }

    fun addProgressPhoto(filePath: String) {
        val user = _selectedUser.value ?: return
        viewModelScope.launch {
            progressPhotoDao.insertPhoto(ProgressPhoto(userId = user.id, filePath = filePath))
        }
    }

    fun deleteProgressPhoto(photo: ProgressPhoto) {
        viewModelScope.launch {
            progressPhotoDao.deletePhoto(photo)
        }
    }

    fun getExercises(level: String): Flow<List<Exercise>> = exerciseDao.getExercisesByLevel(level)
    fun getExerciseById(id: Int): Flow<Exercise?> = exerciseDao.getExerciseById(id)
    fun getProgress(level: String): Flow<Int> = exerciseDao.getCompletedCount(level)
    fun getTotal(level: String): Flow<Int> = exerciseDao.getTotalCount(level)

    fun toggleExercise(exercise: Exercise) {
        viewModelScope.launch {
            exerciseDao.updateExercise(exercise.copy(isCompleted = !exercise.isCompleted))
            if (!exercise.isCompleted) { updateStreak() }
        }
    }

    fun updateMessMeal(day: String, mealName: String, isHighProtein: Boolean) {
        viewModelScope.launch { messMealDao.insertMeal(MessMeal(day, mealName, isHighProtein)) }
    }

    suspend fun getTodayMeal(): MessMeal? {
        val calendar = Calendar.getInstance()
        val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val dayName = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        return messMealDao.getMealByDay(dayName)
    }

    private fun updateStreak() {
        val user = _selectedUser.value ?: return
        val currentTime = System.currentTimeMillis()
        val lastWorkout = Calendar.getInstance().apply { timeInMillis = user.lastWorkoutDate }
        val today = Calendar.getInstance().apply { timeInMillis = currentTime }
        if (lastWorkout.get(Calendar.YEAR) == today.get(Calendar.YEAR) && lastWorkout.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) return 
        val yesterday = Calendar.getInstance().apply { timeInMillis = currentTime; add(Calendar.DAY_OF_YEAR, -1) }
        val wasYesterday = lastWorkout.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && lastWorkout.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
        val newStreak = if (wasYesterday) user.streakCount + 1 else 1
        viewModelScope.launch {
            val updatedUser = user.copy(streakCount = newStreak, lastWorkoutDate = currentTime)
            userDao.insertUser(updatedUser)
            _selectedUser.value = updatedUser
        }
    }

    init {
        viewModelScope.launch {
            if (exerciseDao.getExerciseCount() == 0) {
                val initialExercises = mutableListOf<Exercise>()
                
                val levels = listOf("Beginner", "Intermediate", "Advanced")
                val categories = listOf("Chest", "Back", "Shoulders", "Legs", "Core")

                levels.forEach { level ->
                    categories.forEach { category ->
                        val data = when (category) {
                            "Chest" -> when (level) {
                                "Beginner" -> listOf(
                                    Triple("Wall Push-Ups", "Great for building initial strength without overtaxing joints.", "• Stand facing wall, arms length away.\n• Place hands flat on wall at chest height.\n• Lean in slowly, keeping body straight.\n• Push back to start.\n• PRO TIP: Squeeze your pecs at the top.\n• 3 Sets of 15-20 reps."),
                                    Triple("Knee Push-Ups", "The bridge between wall push-ups and standard ones.", "• Get on hands and knees.\n• Hands slightly wider than shoulders.\n• Keep hips down, forming a straight line from head to knees.\n• Lower chest to floor.\n• 3 Sets of 12 reps."),
                                    Triple("Incline Push-Ups", "Targets the lower chest and builds stability.", "• Use a sturdy bench or bed.\n• Keep body rigid.\n• Push through your palms.\n• 3 Sets of 10 reps."),
                                    Triple("Floor Press", "Safe alternative to bench press for beginners.", "• Lie on back, knees bent.\n• Press dumbbells or water bottles up.\n• Don't let elbows touch floor hard.\n• 3 Sets of 15 reps."),
                                    Triple("Wide Push-Ups", "Focuses on the outer chest fibers.", "• Normal push-up position but hands wider.\n• Control the descent.\n• 3 Sets of 10 reps.")
                                )
                                "Intermediate" -> listOf(
                                    Triple("Standard Push-Ups", "The absolute gold standard for chest hypertrophy.", "• High plank position.\n• Elbows at 45 degrees.\n• Full range of motion.\n• PRO TIP: Keep core tight to prevent sagging.\n• 4 Sets of 12-15 reps."),
                                    Triple("Diamond Push-Ups", "Intense focus on triceps and inner chest.", "• Thumbs and index fingers form diamond.\n• Keep elbows close to ribs.\n• 3 Sets of 10-12 reps."),
                                    Triple("Chest Dips", "Excellent for overall mass and lower pec development.", "• Lean forward slightly.\n• Go deep enough to feel a stretch.\n• Drive up explosively.\n• 3 Sets of 8-10 reps."),
                                    Triple("Decline Push-Ups", "Shifts emphasis to the difficult-to-target upper chest.", "• Feet on a chair or bench.\n• Keep neck neutral.\n• 3 Sets of 12 reps."),
                                    Triple("Archer Push-Ups", "Builds serious unilateral strength.", "• Wide hands.\n• Shift weight to one side while other arm stays straight.\n• Switch sides.\n• 3 Sets of 6-8 per side.")
                                )
                                else -> listOf( // Advanced
                                    Triple("Pseudo Planche Push-Ups", "Extreme shoulder and chest loading.", "• Lean far forward over your hands.\n• Perform push-ups while maintaining lean.\n• PRO TIP: Brace your core like crazy.\n• 3 Sets of 8-10 reps."),
                                    Triple("Clap Push-Ups", "Maximum power and explosive recruitment.", "• Push up hard to leave the ground.\n• Clap mid-air.\n• 3 Sets of 10 reps."),
                                    Triple("Weighted Dips", "For pure mass and raw strength.", "• Add 10-20kg using a belt or bag.\n• Maintain strict form.\n• 4 Sets of 6-8 reps."),
                                    Triple("One-Arm Push-Ups", "Elite level control and strength.", "• Feet wide for balance.\n• Keep shoulders square to floor.\n• 3 Sets of 5 reps per side."),
                                    Triple("Muscle-Up Transition", "The hardest part of the muscle-up.", "• Focus on the 'turnover' from pull to dip.\n• Use a slow controlled motion.\n• 5 Attempts.")
                                )
                            }
                            "Back" -> when (level) {
                                "Beginner" -> listOf(
                                    Triple("Superman Hold", "Strengthens entire posterior chain.", "• Lie on belly.\n• Lift arms, chest, and legs.\n• Squeeze back hard.\n• 3 Sets of 30s holds."),
                                    Triple("Bird-Dog", "Improves spinal stability and back health.", "• On hands and knees.\n• Opposite arm and leg reach out.\n• Switch sides.\n• 3 Sets of 12 per side."),
                                    Triple("Table Rows", "Effective way to pull without a gym.", "• Get under a very sturdy table.\n• Pull chest to the edge.\n• Keep body like a plank.\n• 3 Sets of 10 reps."),
                                    Triple("Wall Slides", "Fixes posture and activates upper back.", "• Back against wall.\n• Slide arms up and down.\n• Keep elbows touching wall.\n• 15 Reps."),
                                    Triple("Door Frame Rows", "Isolate the lats using a simple door.", "• Grab both sides of door frame.\n• Lean back and pull yourself in.\n• Squeeze lats.\n• 3 Sets of 15 reps.")
                                )
                                "Intermediate" -> listOf(
                                    Triple("Pull-Ups", "The best move for a wide V-taper back.", "• Overhand wide grip.\n• Pull chin over bar.\n• Squeeze shoulder blades down.\n• 4 Sets of 8 reps."),
                                    Triple("Chin-Ups", "Heavy bicep and lat involvement.", "• Underhand grip.\n• Pull chest to bar.\n• 3 Sets of 10 reps."),
                                    Triple("Inverted Rows", "Develops middle back thickness.", "• Use a low bar.\n• Pull bar to lower chest.\n• 3 Sets of 12 reps."),
                                    Triple("Single-Arm Rows", "Fixes back imbalances.", "• Use a dumbbell or heavy bag.\n• Pull to hip, not chest.\n• 3 Sets of 12 per side."),
                                    Triple("Face Pulls", "Crucial for rear delts and posture.", "• Use a resistance band.\n• Pull toward forehead.\n• Pull ends apart.\n• 3 Sets of 15 reps.")
                                )
                                else -> listOf( // Advanced
                                    Triple("Muscle-Ups", "Total upper body dominance.", "• Explosive pull into a dip.\n• Lean over bar during transition.\n• 3 Sets of 5 reps."),
                                    Triple("Front Lever Hold", "Elite gymnastic back strength.", "• Back parallel to ground while hanging.\n• Tight core and lats.\n• 3 Sets of 10s holds."),
                                    Triple("Weighted Pull-Ups", "For advanced muscle recruitment.", "• Add 10-20kg weight.\n• Dead hang to full chin over bar.\n• 4 Sets of 5 reps."),
                                    Triple("Explosive Pull-Ups", "Builds raw pulling power.", "• Pull bar to belly button.\n• Fast upward phase.\n• 3 Sets of 8 reps."),
                                    Triple("Wide Grip Muscle-Ups", "Increased difficulty for the lats.", "• Ultra-wide grip.\n• Clean transition over bar.\n• 3 Sets of 3 reps.")
                                )
                            }
                            "Shoulders" -> when (level) {
                                "Beginner" -> listOf(
                                    Triple("Shoulder Taps", "Stability and initial delt strength.", "• High plank position.\n• Tap opposite shoulder.\n• PRO TIP: No hip rocking allowed.\n• 3 Sets of 20 taps."),
                                    Triple("Pike Push-Ups", "The bodyweight version of overhead press.", "• Hips high, form a V-shape.\n• Lower head toward floor.\n• 3 Sets of 10 reps."),
                                    Triple("Lateral Raises", "Isolates the side deltoids for width.", "• Use water bottles.\n• Lift arms to side slowly.\n• 3 Sets of 15 reps."),
                                    Triple("Front Raises", "Targets the front deltoids.", "• Lift weights straight forward.\n• Control the descent.\n• 3 Sets of 15 reps."),
                                    Triple("Plank Circles", "Shoulder mobility and core combo.", "• In plank, rotate body in small circles.\n• Stay rigid.\n• 10 Reps each way.")
                                )
                                "Intermediate" -> listOf(
                                    Triple("Elevated Pike Push-Ups", "Increases load on the deltoids.", "• Feet on a chair.\n• Form V-shape.\n• Lower head slowly.\n• 3 Sets of 10 reps."),
                                    Triple("Arnold Press", "Full range of motion for all delt heads.", "• Rotate palms during press.\n• Control the weights.\n• 3 Sets of 12 reps."),
                                    Triple("Wall Walk-Ups", "Builds handstand endurance.", "• Walk feet up wall into handstand.\n• Hold, then walk down.\n• 5 Reps."),
                                    Triple("Rear Delt Flys", "Essential for that 3D shoulder look.", "• Lean forward, back flat.\n• Open arms wide like wings.\n• 3 Sets of 15 reps."),
                                    Triple("Dumbbell Overhead Press", "The heavy mass builder for shoulders.", "• Stand tall.\n• Press heavy weights overhead.\n• 4 Sets of 10 reps.")
                                )
                                else -> listOf( // Advanced
                                    Triple("Handstand Push-Ups", "Ultimate bodyweight shoulder exercise.", "• Against wall.\n• Lower head to floor.\n• Press up explosively.\n• 3 Sets of 5-8 reps."),
                                    Triple("Wall Walks (Sideways)", "Extreme time under tension.", "• Side-walk in handstand along wall.\n• Keep body tight.\n• 10 Meters each way."),
                                    Triple("Planche Lean", "Maximum front delt loading.", "• Lean as far forward as possible in plank.\n• Keep arms locked.\n• 3 Sets of 30s holds."),
                                    Triple("Freestanding Handstand", "Elite balance and control.", "• Balance without wall support.\n• Focus on core.\n• 3 Sets of 20s."),
                                    Triple("Weighted HSPU", "The final boss of shoulder strength.", "• HSPU with 5-10kg vest.\n• Strict form only.\n• 3 Sets of 5 reps.")
                                )
                            }
                            "Legs" -> when (level) {
                                "Beginner" -> listOf(
                                    Triple("Air Squats", "The base of all lower body training.", "• Feet shoulder width.\n• Sit back into your heels.\n• 3 Sets of 20 reps."),
                                    Triple("Reverse Lunges", "Better for knees than forward lunges.", "• Step back, drop knee.\n• Stay upright.\n• 3 Sets of 12 per side."),
                                    Triple("Glute Bridges", "Isolates the glutes and hamstrings.", "• Lie on back, lift hips.\n• Squeeze glutes at top.\n• 3 Sets of 20 reps."),
                                    Triple("Calf Raises", "Builds lower leg endurance.", "• Stand on a step edge.\n• Full stretch and squeeze.\n• 3 Sets of 25 reps."),
                                    Triple("Wall Sit", "Builds mental toughness and endurance.", "• Back flat on wall.\n• 90 degree knees.\n• Hold 1 min.")
                                )
                                "Intermediate" -> listOf(
                                    Triple("Bulgarian Split Squats", "The most effective unilateral leg move.", "• Back foot on bench.\n• Squat deep on front leg.\n• 3 Sets of 12 per side."),
                                    Triple("Jump Squats", "Develops explosive power.", "• Squat then explode into jump.\n• Land soft.\n• 3 Sets of 15 reps."),
                                    Triple("Single Leg Deadlifts", "Hamstring focus and balance.", "• Reach forward, kick one leg back.\n• Back flat.\n• 3 Sets of 12 per side."),
                                    Triple("Step-Ups", "Building powerful quads and glutes.", "• Step onto high surface.\n• Drive through the heel.\n• 3 Sets of 15 per side."),
                                    Triple("Sumo Squats", "Emphasis on inner thighs and glutes.", "• Wide stance, toes out.\n• Sit deep.\n• 3 Sets of 15 reps.")
                                )
                                else -> listOf( // Advanced
                                    Triple("Pistol Squats", "Elite balance and raw leg strength.", "• One leg squat to floor.\n• Other leg in front.\n• 3 Sets of 5 per side."),
                                    Triple("Nordic Curls", "The absolute best for hamstrings.", "• Kneel, anchor heels.\n• Fall forward slowly.\n• 3 Sets of 8 reps."),
                                    Triple("Shrimp Squats", "Highly complex unilateral stability.", "• Grab back foot with hand.\n• Squat until knee touches.\n• 3 Sets of 8 per side."),
                                    Triple("Explosive Lunges", "Switch legs in mid-air jump.", "• Maximum force.\n• Controlled landing.\n• 3 Sets of 20 switches."),
                                    Triple("Sprinting Burpees", "Total leg and lung incinerator.", "• Burpee into 5s sprint.\n• Repeat non-stop.\n• 3 rounds of 1 min.")
                                )
                            }
                            "Core" -> when (level) {
                                "Beginner" -> listOf(
                                    Triple("Plank Hold", "Building foundational isometric core strength.", "• Straight body from head to heels.\n• Squeeze glutes and abs.\n• 3 Sets of 45s."),
                                    Triple("Dead Bug", "Inner abs and coordination.", "• On back, opposite arm/leg move.\n• Lower back pressed to floor.\n• 3 Sets of 12 per side."),
                                    Triple("Leg Raises", "Targets the lower abdominal region.", "• On back, lift straight legs.\n• Don't touch floor at bottom.\n• 3 Sets of 15 reps."),
                                    Triple("Bird-Dog", "Core stability and spinal health.", "• Hands and knees.\n• Reach out slowly.\n• 12 Reps per side."),
                                    Triple("Russian Twists", "Targets the obliques.", "• Sit, feet up, rotate torso.\n• Move slow.\n• 3 Sets of 30 twists.")
                                )
                                "Intermediate" -> listOf(
                                    Triple("Hanging Leg Raises", "Serious lower ab developer.", "• Hang from bar, lift legs.\n• No swinging.\n• 3 Sets of 12 reps."),
                                    Triple("Bicycle Crunches", "Metabolic core destruction.", "• Pedal legs, touch elbow to knee.\n• Constant tension.\n• 3 Sets of 40 reps."),
                                    Triple("Hollow Body Hold", "Gymnastic core tension.", "• Lift head and legs in banana shape.\n• Low back stays flat.\n• 3 Sets of 30s."),
                                    Triple("Side Plank with Dip", "Deep oblique isolation.", "• Lower hips then lift high.\n• Keep body straight.\n• 3 Sets of 15 per side."),
                                    Triple("Mountain Climbers", "Core and conditioning combo.", "• Run knees to chest in plank.\n• Keep hips low.\n• 3 rounds of 45s.")
                                )
                                else -> listOf( // Advanced
                                    Triple("Dragon Flags", "Legendary core tension move.", "• Entire body rigid like a pole.\n• Lower as one unit.\n• 3 Sets of 5 reps."),
                                    Triple("L-Sit Hold", "Elite static strength.", "• Support weight on hands.\n• Legs straight out.\n• 3 Sets of 15s hold."),
                                    Triple("Ab Wheel Rollouts", "Maximum anti-extension load.", "• Roll out until flat.\n• Use abs to pull back.\n• 3 Sets of 12 reps."),
                                    Triple("Toes to Bar", "Full body pulling and ab power.", "• Hang from bar, touch bar with toes.\n• No momentum.\n• 3 Sets of 10 reps."),
                                    Triple("Windshield Wipers", "Extreme oblique rotational force.", "• Hang from bar, rotate legs 180 deg.\n• Keep legs straight.\n• 3 Sets of 10 per side.")
                                )
                            }
                            else -> emptyList()
                        }

                        data.forEach { (name, desc, instructions) ->
                            initialExercises.add(Exercise(
                                name = name,
                                level = level,
                                category = category,
                                description = desc,
                                instructions = instructions
                            ))
                        }
                    }
                }
                exerciseDao.insertExercises(initialExercises)
            }
            
            // Initialize default mess meals if empty
            val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            dayNames.forEach { day ->
                if (messMealDao.getMealByDay(day) == null) {
                    messMealDao.insertMeal(MessMeal(day = day, mealName = "General Meal", isHighProtein = true))
                }
            }
        }
    }
}
