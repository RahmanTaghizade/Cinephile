package com.example.cinephile.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinephile.domain.repository.QuizRepository
import com.example.cinephile.domain.repository.QuizQuestionUiModel
import com.example.cinephile.domain.repository.QuizMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionAnswer(
    val questionIndex: Int,
    val isCorrect: Boolean
)

data class QuizPlayState(
    val questions: List<QuizQuestionUiModel> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val submittedAnswer: String? = null, 
    val isAnswerCorrect: Boolean? = null, 
    val quizId: Long = 0L,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val isFinished: Boolean = false,
    val startTime: Long = 0L,
    val answerHistory: List<QuestionAnswer> = emptyList(),
    val timeRemaining: Int = 30,
    val questionStartTime: Long = 0L,
    val questionTimeSpent: Long = 0L,
    val quizMode: QuizMode = QuizMode.TIMED,
    val totalTimeSpent: Long = 0L
)

sealed class QuizPlayEvent {
    object AnswerSelected : QuizPlayEvent()
    object NextQuestion : QuizPlayEvent()
    object SubmitAnswer : QuizPlayEvent()
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

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        loadQuizAndQuestions()
    }

    private fun loadQuizAndQuestions() {
        viewModelScope.launch {
            quizRepository.getQuiz(quizId).firstOrNull()?.let { quiz ->
                quizRepository.getQuizQuestions(quizId).firstOrNull()?.let { questions ->
                    if (questions.isNotEmpty()) {
                        val startTime = System.currentTimeMillis()
                        val mode = try {
                            QuizMode.valueOf(quiz.mode ?: "TIMED")
                        } catch (e: Exception) {
                            QuizMode.TIMED
                        }
                        val timePerQuestion = when (mode) {
                            QuizMode.TIMED -> 30
                            QuizMode.SURVIVAL -> 60
                        }
                        _state.update {
                            it.copy(
                                questions = questions,
                                startTime = startTime,
                                questionStartTime = startTime,
                                timeRemaining = timePerQuestion,
                                quizMode = mode
                            )
                        }
                        startTimer()
                    }
                }
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val currentState = _state.value
                if (currentState.isFinished || currentState.submittedAnswer != null) {
                    continue
                }
                
                val newTimeRemaining = currentState.timeRemaining - 1
                if (newTimeRemaining <= 0) {
                    handleTimeUp()
                    break
                } else {
                    _state.update { it.copy(timeRemaining = newTimeRemaining) }
                }
            }
        }
    }

    private fun handleTimeUp() {
        val currentState = _state.value
        if (currentState.submittedAnswer != null || currentState.isFinished) return
        
        val currentQuestion = currentState.questions.getOrNull(currentState.currentIndex) ?: return
        
        val questionTimeSpent = System.currentTimeMillis() - currentState.questionStartTime
        
        if (currentState.quizMode == QuizMode.SURVIVAL) {
            finishQuiz(currentState.correctCount, currentState.wrongCount + 1, currentState.answerHistory)
        } else {
            submitAnswer(null, questionTimeSpent)
        }
    }

    fun selectAnswer(answer: String) {
        
        if (_state.value.submittedAnswer != null || _state.value.isFinished) return
        _state.update { it.copy(selectedAnswer = answer) }
        viewModelScope.launch {
            _events.emit(QuizPlayEvent.AnswerSelected)
        }
    }

    fun submitAnswer() {
        val currentState = _state.value
        if (currentState.selectedAnswer == null || currentState.isFinished) return
        
        if (currentState.submittedAnswer != null) return

        val questionTimeSpent = System.currentTimeMillis() - currentState.questionStartTime
        submitAnswer(currentState.selectedAnswer, questionTimeSpent)
    }

    private fun submitAnswer(answer: String?, questionTimeSpent: Long) {
        val currentState = _state.value
        if (currentState.isFinished) return
        
        if (currentState.submittedAnswer != null) return

        val currentQuestion = currentState.questions.getOrNull(currentState.currentIndex) ?: return
        val isCorrect = answer == currentQuestion.correctAnswer

        timerJob?.cancel()

        val newTotalTimeSpent = currentState.totalTimeSpent + questionTimeSpent
        
        _state.update {
            it.copy(
                submittedAnswer = answer,
                isAnswerCorrect = isCorrect,
                questionTimeSpent = questionTimeSpent,
                totalTimeSpent = newTotalTimeSpent
            )
        }

        val answerRecord = QuestionAnswer(
            questionIndex = currentState.currentIndex,
            isCorrect = isCorrect
        )
        val newAnswerHistory = currentState.answerHistory + answerRecord

        val newCorrectCount = if (isCorrect) currentState.correctCount + 1 else currentState.correctCount
        val newWrongCount = if (!isCorrect) currentState.wrongCount + 1 else currentState.wrongCount

        if (currentState.quizMode == QuizMode.SURVIVAL && !isCorrect) {
            finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
            return
        }

        viewModelScope.launch {
            delay(1500) 
            
            if (currentState.currentIndex < currentState.questions.size - 1) {
                val nextIndex = currentState.currentIndex + 1
                val nextQuestionStartTime = System.currentTimeMillis()
                val timePerQuestion = when (currentState.quizMode) {
                    QuizMode.TIMED -> 30
                    QuizMode.SURVIVAL -> 60
                }
                _state.update {
                    it.copy(
                        currentIndex = nextIndex,
                        selectedAnswer = null,
                        submittedAnswer = null,
                        isAnswerCorrect = null,
                        correctCount = newCorrectCount,
                        wrongCount = newWrongCount,
                        answerHistory = newAnswerHistory,
                        questionStartTime = nextQuestionStartTime,
                        timeRemaining = timePerQuestion,
                        questionTimeSpent = 0L
                    )
                }
                startTimer()
                _events.emit(QuizPlayEvent.NextQuestion)
            } else {
                finishQuiz(newCorrectCount, newWrongCount, newAnswerHistory)
            }
        }
    }

    private fun finishQuiz(correctCount: Int, wrongCount: Int, answerHistory: List<QuestionAnswer>) {
        timerJob?.cancel()
        
        val startTime = _state.value.startTime
        val totalTimeSpent = _state.value.totalTimeSpent
        val duration = if (startTime > 0) {
            ((System.currentTimeMillis() - startTime) / 1000).toInt()
        } else {
            0
        }
        
        val baseXP = correctCount * 10
        val timeBonus = calculateTimeBonus(totalTimeSpent, correctCount)
        val xpEarned = baseXP + timeBonus

        _state.update {
            it.copy(
                isFinished = true,
                correctCount = correctCount,
                wrongCount = wrongCount,
                answerHistory = answerHistory
            )
        }

        viewModelScope.launch {
            try {
                android.util.Log.d("QuizPlayViewModel", "Saving quiz result: quizId=$quizId, correct=$correctCount, wrong=$wrongCount, xp=$xpEarned, duration=$duration")
                quizRepository.saveQuizResult(
                    quizId = quizId,
                    xpEarned = xpEarned,
                    durationSec = duration,
                    correctCount = correctCount,
                    wrongCount = wrongCount
                )
                android.util.Log.d("QuizPlayViewModel", "Quiz result saved successfully")
                _events.emit(QuizPlayEvent.QuizFinished)
            } catch (e: Exception) {
                android.util.Log.e("QuizPlayViewModel", "Error saving quiz result", e)
                // Still emit the event so the UI can show the summary
                _events.emit(QuizPlayEvent.QuizFinished)
            }
        }
    }

    private fun calculateTimeBonus(totalTimeMs: Long, correctCount: Int): Int {
        if (correctCount == 0) return 0
        val avgTimePerQuestion = totalTimeMs / correctCount
        val avgTimeSeconds = avgTimePerQuestion / 1000
        
        return when {
            avgTimeSeconds <= 5 -> correctCount * 5
            avgTimeSeconds <= 10 -> correctCount * 3
            avgTimeSeconds <= 15 -> correctCount * 2
            else -> 0
        }
    }
}

