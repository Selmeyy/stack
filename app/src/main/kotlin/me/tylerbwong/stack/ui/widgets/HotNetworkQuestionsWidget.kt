package me.tylerbwong.stack.ui.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.tylerbwong.stack.R
import me.tylerbwong.stack.api.model.NetworkHotQuestion
import me.tylerbwong.stack.data.repository.NetworkHotQuestionsRepository
import me.tylerbwong.stack.data.site.normalizeSite
import me.tylerbwong.stack.ui.questions.detail.QuestionDetailActivity
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class HotNetworkQuestionsWidget @OptIn(DelicateCoroutinesApi::class) constructor(
    private val externalScope: CoroutineScope = GlobalScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppWidgetProvider() {

    @Inject
    lateinit var networkHotQuestionsRepository: NetworkHotQuestionsRepository

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var json: Json

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        refreshWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshWidgets(
                context,
                currentQuestionId = intent.getIntExtra(CURRENT_HOT_QUESTION_ID, -1)
            )
        }
    }

    private fun refreshWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager? = null,
        appWidgetIds: IntArray? = null,
        currentQuestionId: Int = -1
    ) {
        val manager = appWidgetManager ?: AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetIds ?: manager.getAppWidgetIds(
            ComponentName(context, HotNetworkQuestionsWidget::class.java)
        )
        widgetIds.forEach { appWidgetId ->
            externalScope.launch {
                val question = getRandomHotNetworkQuestion(context, currentQuestionId)
                val remoteViews = buildRemoteViews(context, question)
                manager.updateAppWidget(appWidgetId, remoteViews)
            }
        }
    }

    private suspend fun getHotNetworkQuestions(context: Context): List<NetworkHotQuestion> {
        val sharedPreferences =
            context.getSharedPreferences(CACHE_PREFERENCE_NAME, Context.MODE_PRIVATE)
        val type = ListSerializer(NetworkHotQuestion.serializer())

        sharedPreferences.getString(CACHE_QUESTIONS_KEY, null)?.let {
            val expiresAfter = sharedPreferences.getLong(CACHE_EXPIRES_AFTER_KEY, -1)

            if (expiresAfter > System.currentTimeMillis()) {
                Timber.d("hot network questions: cache hit")

                val questions = json.decodeFromString(type, it)

                if (questions.isNotEmpty()) {
                    return questions
                }
            }
        }

        Timber.d("hot network questions: cache miss")

        return networkHotQuestionsRepository.getHotNetworkQuestions().also {
            sharedPreferences.edit().apply {
                putString(CACHE_QUESTIONS_KEY, json.encodeToString(type, it))
                putLong(
                    CACHE_EXPIRES_AFTER_KEY,
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
                        CACHE_EXPIRES_AFTER_MINUTES
                    )
                )

                apply()
            }
        }
    }

    private suspend fun getRandomHotNetworkQuestion(
        context: Context,
        currentQuestionId: Int
    ): NetworkHotQuestion? {
        return withContext(ioDispatcher) {
            val questions = getHotNetworkQuestions(context)
                .also { Timber.d("hot network questions count is ${it.size}") }

            if (questions.isEmpty()) return@withContext null

            var question = questions.random()

            // the exchange typically provides 100 hot network questions, so explicitly checking that the question
            // id is different two times should ensure the odds of the same hot question being picked are very low
            // without requiring us to worry about handling edge cases that could cause infinite loops
            if (questions.size > 1 && question.questionId == currentQuestionId) {
                question = questions.random()

                if (question.questionId == currentQuestionId) {
                    question = questions.random()
                }
            }

            question
        }
    }

    private suspend fun buildRemoteViews(
        context: Context,
        question: NetworkHotQuestion?
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.hot_network_questions_widget).apply {
            if (question != null) {
                // Set the question title
                setTextViewText(R.id.hotNetworkQuestionTitleTextView, question.title)

                ImageRequest.Builder(context)
                    .data(question.iconUrl)
                    .target(
                        onSuccess = {
                            setImageViewBitmap(R.id.hotQuestionIcon, it.toBitmap())
                            setViewVisibility(R.id.hotQuestionIcon, View.VISIBLE)
                        },
                        onError = {
                            setViewVisibility(R.id.hotQuestionIcon, View.GONE)
                        }
                    )
                    .build()
                    .let { imageLoader.execute(it) }

                // Set click listeners for the widget and refresh button
                setOnClickPendingIntent(
                    android.R.id.background,
                    getOpenQuestionIntent(context, question)
                )
                setOnClickPendingIntent(
                    R.id.fetchNewHotQuestionButton,
                    getFetchNewHotQuestionIntent(context, question)
                )
            } else {
                setTextViewText(
                    R.id.hotNetworkQuestionTitleTextView,
                    context.resources.getString(R.string.hot_network_questions_unable_to_load),
                )
                setOnClickPendingIntent(
                    R.id.fetchNewHotQuestionButton,
                    getFetchNewHotQuestionIntent(context)
                )
            }
        }
    }

    private fun getOpenQuestionIntent(
        context: Context,
        question: NetworkHotQuestion
    ): PendingIntent {
        val intent = QuestionDetailActivity.makeIntent(
            context = context,
            questionId = question.questionId,
            deepLinkSite = question.site.normalizeSite(),
        )

        return PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent.setAction("OPEN_QUESTION_${question.questionId}_ON_${question.site}"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getFetchNewHotQuestionIntent(
        context: Context,
        currentQuestion: NetworkHotQuestion? = null
    ): PendingIntent {
        val intent = Intent(context, HotNetworkQuestionsWidget::class.java)

        intent.action = ACTION_REFRESH
        intent.putExtra(CURRENT_HOT_QUESTION_ID, currentQuestion?.questionId)

        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val ACTION_REFRESH = "me.tylerbwong.stack.widget.ACTION_REFRESH"
        private const val CURRENT_HOT_QUESTION_ID = "current_hot_question_id"

        private const val CACHE_EXPIRES_AFTER_MINUTES = 5L

        private const val CACHE_PREFERENCE_NAME = "hot_network_questions_widget_cache"
        private const val CACHE_QUESTIONS_KEY = "hot_network_questions"
        private const val CACHE_EXPIRES_AFTER_KEY = "hot_network_questions_expires_after"
    }
}
