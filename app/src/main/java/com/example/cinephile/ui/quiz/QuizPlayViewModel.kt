package com.example.cinephile.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizQuestionUiModel
import com.example.cinephile.domain.repository.QuizDifficulty
import com.example.cinephile.domain.repository.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionAnswer(
    val questionIndex: Int,
    val isCorrect: Boolean,
    val timeLeft: Int,
    val timeLimit: Int
)

data class QuizPlayState(
    val questions: List<QuizQuestionUiModel> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val timeRemaining: Int = 0,
    val isTimerRunning: Boolean = false,
    val quizDifficulty: QuizDifficulty = QuizDifficulty.EASY,
    val quizMode: QuizMode = QuizMode.TIMED,
    val quizId: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isFinished: Boolean = false,
    val startTime: Long = 0L,
    val answerHistory: List<QuestionAnswer> = emptyList()
)

sealed class QuizPlayEvent {
    object AnswerSelected : QuizPlayEvent()
    object NextQuestion : QuizPlayEvent()
    object SubmitAnswer : QuizPlayEvent()
    object TimerExpired : QuizPlayEvent()
    object QuizFinished : QuizPlayEvent()
}

@HiltViewModel
class QuizPlayViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: Long = savedStateHandle.get<Long>("quizId") ?: 0L

    private val _state = MutableStateFlow(QuizPlayState(quizId = quizId))
    val state: StateFlow<QuizPlayState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<QuizPlayEvent>()
    val events: SharedFlow<QuizPlayEvent> = _events.asSharedFlow()

    private var timerJob: Job? = null
    private var quizStartTime: Long = 0L
    private var currentQuestionStartTime: Long = 0L
    private var currentTimeLimit: Int = 0

    init {
        loadQuizAndQuestions()
    }

    private fun loadQuizAndQuestions() {
        viewModelScope.launch {
            // Load quiz to get difficulty and mode
            quizRepository.getQuiz(quizId).firstOrNull()?.let { quiz ->
                if (quiz != null) {
                    _state.update { it.copy(quizDifficulty = quiz.difficulty, quizMode = quiz.mode) }
                    
                    // Load questions
                    quizRepository.getQuizQuestions(quizId).firstOrNull()?.let { questions ->
                        if (questions.isNotEmpty()) {
                            val timeLimit = getTimeLimitForDifficulty(quiz.difficulty)
                            quizStartTime = System.currentTimeMillis()
                            currentQuestionStartTime = quizStartTime
                            currentTimeLimit = timeLimit
                            _state.update {
                                it.copy(
                                    questions = questions,
                                    timeRemaining = timeLimit,
                                    startTime = quizStartTime,
                                    isTimerRunning = true
                                )
                            }
                            startTimer()
                        }
                    }
                }
            }
        }
    }

    private fun getTimeLimitForDifficulty(difficulty: QuizDifficulty): Int {
        return when (difficulty) {
            QuizDifficulty.EASY -> 20
            QuizDifficulty.MEDIUM -> 15
            QuizDifficulty.HARD -> 10
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timeRemaining > 0 && _state.value.isTimerRunning && !_state.value.isFinished) {
                delay(1000)
                val newTime = _state.value.timeRemaining - 1
                _state.update { it.copy(timeRemaining = newTime) }
                
                if (newTime <= 0) {
                    handleTimerExpired()
                }
            }
        }
    }

    fun selectAnswer(answer: String) {
        if (_state.value.selectedAnswer != null || _state.value.isFinished) return
        _state.update { it.copy(selectedAnswer = answer) }
        viewModelScope.launch {
            _events.emit(QuizPlayEvent.AnswerSelected)
        }
    }

    fun submitAnswer() {
        val currentState = _state.value
        if (currentState.selectedAnswer == null || currentState.isFinished) return

        val currentQuestion = currentState.questions.getOrNull(currentState.currentIndex) ?: return
        val isCorrect = currentState.selectedAnswer == currentQuestion.correctAnswer
        val timeLeft = currentState.timeRemaining

        // Record answer
        val answerRecord = QuestionAnswer(
            questionIndex = currentState.currentIndex,
            isCorrect = isCorrect,
            timeLeft = timeLeft,
            timeLimit = currentTimeLimit
        )
        val newAnswerHistory = currentState.answerHistory + answerRecord

        val newCorrectCount = if (isCorrect) currentState.correctCount + 1 else currentState.correctCount
        val newWrongCount = if (!isCorrect) currentState.wrongCount + 1 else currentState.wrongCount

        // In survival mode, stop at first wrong answer
        if (currentState.quizMode == QuizMode.SURVIVAL && !isCorrect) {
            finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
            return
        }

        // Move to next question or finish
        if (currentState.currentIndex < currentState.questions.size - 1) {
            val nextIndex = currentState.currentIndex + 1
            val timeLimit = getTimeLimitForDifficulty(currentState.quizDifficulty)
            currentTimeLimit = timeLimit
            currentQuestionStartTime = System.currentTimeMillis()
            _state.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedAnswer = null,
                    timeRemaining = timeLimit,
                    correctCount = newCorrectCount,
                    wrongCount = newWrongCount,
                    isTimerRunning = true,
                    answerHistory = newAnswerHistory
                )
            }
            startTimer()
            viewModelScope.launch {
                _events.emit(QuizPlayEvent.NextQuestion)
            }
        } else {
            finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
        }
    }

    private fun handleTimerExpired() {
        val currentState = _state.value
        val currentQuestion = currentState.questions.getOrNull(currentState.currentIndex) ?: return
        
        // If no answer selected, count as wrong (timeLeft = 0)
        val isCorrect = currentState.selectedAnswer == currentQuestion.correctAnswer
        val timeLeft = 0
        
        // Record answer
        val answerRecord = QuestionAnswer(
            questionIndex = currentState.currentIndex,
            isCorrect = isCorrect,
            timeLeft = timeLeft,
            timeLimit = currentTimeLimit
        )
        val newAnswerHistory = currentState.answerHistory + answerRecord

        val newCorrectCount = if (isCorrect) currentState.correctCount + 1 else currentState.correctCount
        val newWrongCount = if (!isCorrect) currentState.wrongCount + 1 else currentState.wrongCount

        // In survival mode, stop at first wrong answer
        if (currentState.quizMode == QuizMode.SURVIVAL && !isCorrect) {
            finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
            return
        }

        // Move to next question or finish
        if (currentState.currentIndex < currentState.questions.size - 1) {
            val nextIndex = currentState.currentIndex + 1
            val timeLimit = getTimeLimitForDifficulty(currentState.quizDifficulty)
            currentTimeLimit = timeLimit
            currentQuestionStartTime = System.currentTimeMillis()
            _state.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedAnswer = null,
                    timeRemaining = timeLimit,
                    correctCount = newCorrectCount,
                    wrongCount = newWrongCount,
                    isTimerRunning = true,
                    answerHistory = newAnswerHistory
                )
            }
            startTimer()
            viewModelScope.launch {
                _events.emit(QuizPlayEvent.TimerExpired)
            }
        } else {
            finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
        }
    }

    private fun finishQuiz(correctCount: Int, wrongCount: Int, answerHistory: List<QuestionAnswer>) {
        timerJob?.cancel()
        val duration = ((System.currentTimeMillis() - quizStartTime) / 1000).toInt()
        val score = calculateScore(answerHistory, _state.value.quizMode, _state.value.quizDifficulty)

        _state.update {
            it.copy(
                isFinished = true,
                isTimerRunning = false,
                correctCount = correctCount,
                wrongCount = wrongCount,
                answerHistory = answerHistory
            )
        }

        viewModelScope.launch {
            // Save result
            quizRepository.saveQuizResult(
                quizId = quizId,
                score = score,
                durationSec = duration,
                correctCount = correctCount,
                wrongCount = wrongCount,
                mode = _state.value.quizMode
            )
            _events.emit(QuizPlayEvent.QuizFinished)
        }
    }

    private fun calculateScore(
        answerHistory: List<QuestionAnswer>,
        mode: QuizMode,
        difficulty: QuizDifficulty
    ): Int {
        return when (mode) {
            QuizMode.TIMED -> calculateTimedScore(answerHistory, difficulty)
            QuizMode.SURVIVAL -> calculateSurvivalScore(answerHistory)
        }
    }

    private fun calculateTimedScore(
        answerHistory: List<QuestionAnswer>,
        difficulty: QuizDifficulty
    ): Int {
        if (answerHistory.isEmpty()) return 0

        val difficultyMultiplier = when (difficulty) {
            QuizDifficulty.EASY -> 1.0
            QuizDifficulty.MEDIUM -> 1.3
            QuizDifficulty.HARD -> 1.6
        }

        val totalScore = answerHistory.sumOf { answer ->
            if (answer.isCorrect) {
                val timeRatio = answer.timeLeft.toDouble() / answer.timeLimit.toDouble()
                (1000.0 * timeRatio * difficultyMultiplier).toInt()
            } else {
                0
            }
        }

        return totalScore
    }

    private fun calculateSurvivalScore(answerHistory: List<QuestionAnswer>): Int {
        if (answerHistory.isEmpty()) return 0

        var score = 0
        var streak = 0

        answerHistory.forEach { answer ->
            if (answer.isCorrect) {
                streak++
                score += 200 // Base points per correct answer
                score += streak * 50 // Streak bonus (+50 per streak)
            } else {
                streak = 0 // Reset streak on wrong answer
            }
        }

        return score
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

