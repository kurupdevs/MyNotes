package com.kurupdevs.mynotes.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.data.model.NoteType
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.ui.archive.ArchiveScreen
import com.kurupdevs.mynotes.ui.editor.EditorScreen
import com.kurupdevs.mynotes.ui.home.HomeScreen
import com.kurupdevs.mynotes.ui.labels.LabelsScreen
import com.kurupdevs.mynotes.ui.nav.Routes
import com.kurupdevs.mynotes.ui.onboarding.OnboardingScreen
import com.kurupdevs.mynotes.ui.reminders.RemindersScreen
import com.kurupdevs.mynotes.ui.search.SearchScreen
import com.kurupdevs.mynotes.ui.settings.SettingsScreen
import com.kurupdevs.mynotes.ui.share.ShareScreen
import com.kurupdevs.mynotes.ui.theme.MyNotesTheme
import com.kurupdevs.mynotes.ui.trash.TrashScreen
import com.kurupdevs.mynotes.ui.voice.VoiceRecorderSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyNotesAppRoot(openNoteId = intent.getStringExtra("open_note"))
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MyNotesAppRoot(openNoteId: String? = null) {
    val context = LocalContext.current
    val container = (context.applicationContext as NotesApp).container
    val themePref by container.prefs.theme.collectAsState(initial = "dark")
    val onboardingDone by container.prefs.onboardingDone.collectAsState(initial = false)
    val dark = when (themePref) {
        "light" -> false
        "system" -> androidx.compose.foundation.isSystemInDarkTheme()
        else -> true
    }
    val nav = rememberNavController()
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    var quickVoiceFor by remember { mutableStateOf<String?>(null) }
    var deepLinkHandled by remember { mutableStateOf(false) }

    // reminder notification deep link
    androidx.compose.runtime.LaunchedEffect(openNoteId, onboardingDone) {
        if (!deepLinkHandled && openNoteId != null && onboardingDone) {
            deepLinkHandled = true
            nav.navigate(Routes.editor(openNoteId))
        }
    }

    fun createNote(type: String, then: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val noteType = when (type) {
                "checklist" -> NoteType.CHECKLIST
                "photo" -> NoteType.IMAGE
                "voice" -> NoteType.VOICE
                else -> NoteType.TEXT
            }
            val blocks = when (type) {
                "checklist" -> listOf(Block(UUID.randomUUID().toString(), BlockKind.TODO))
                else -> emptyList()
            }
            val n = container.notes.createNote(type = noteType, blocks = blocks)
            scope.launch(Dispatchers.Main) { then(n.id) }
        }
    }

    MyNotesTheme(dark = dark) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {
            SharedTransitionLayout {
                NavHost(
                    navController = nav,
                    startDestination = if (onboardingDone) Routes.HOME else Routes.ONBOARDING
                ) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(dark = dark, onFinish = {
                            scope.launch(Dispatchers.IO) { container.prefs.setOnboardingDone() }
                            nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                        })
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            dark = dark,
                            sharedScope = this@SharedTransitionLayout,
                            animScope = this,
                            onOpenNote = { nav.navigate(Routes.editor(it)) },
                            onOpenSearch = { nav.navigate(Routes.SEARCH) },
                            onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                            onOpenLabels = { nav.navigate(Routes.LABELS) },
                            onOpenReminders = { nav.navigate(Routes.REMINDERS) },
                            onOpenTrash = { nav.navigate(Routes.TRASH) },
                            onOpenArchive = { nav.navigate(Routes.ARCHIVE) },
                            onNewNote = { type -> createNote(type) { id -> nav.navigate(Routes.editor(id)) } },
                            onQuickVoice = {
                                createNote("voice") { id ->
                                    quickVoiceFor = id
                                    nav.navigate(Routes.editor(id))
                                }
                            },
                            onLinkGoogle = {
                                scope.launch {
                                    container.auth.linkGoogle()
                                        .onFailure { /* stay anonymous, no nag */ }
                                }
                            }
                        )
                    }
                    composable(
                        Routes.EDITOR,
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments!!.getString("noteId")!!
                        EditorScreen(
                            noteId = id,
                            dark = dark,
                            sharedScope = this@SharedTransitionLayout,
                            animScope = this,
                            onBack = { nav.popBackStack() },
                            onShare = { nav.navigate(Routes.share(it)) },
                            onDuplicate = { nav.navigate(Routes.editor(it)) }
                        )
                    }
                    composable(Routes.SEARCH) {
                        SearchScreen(dark = dark, onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.editor(it)) })
                    }
                    composable(Routes.LABELS) {
                        LabelsScreen(dark = dark, onBack = { nav.popBackStack() }, onOpenLabel = { nav.popBackStack() })
                    }
                    composable(Routes.REMINDERS) {
                        RemindersScreen(dark = dark, onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.editor(it)) })
                    }
                    composable(Routes.TRASH) {
                        TrashScreen(dark = dark, onBack = { nav.popBackStack() })
                    }
                    composable(Routes.ARCHIVE) {
                        ArchiveScreen(dark = dark, onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.editor(it)) })
                    }
                    composable(Routes.SETTINGS) {
                        SettingsScreen(dark = dark, onBack = { nav.popBackStack() }, onThemeChange = { t ->
                            scope.launch(Dispatchers.IO) { container.prefs.setTheme(t) }
                        })
                    }
                    composable(
                        Routes.SHARE,
                        arguments = listOf(navArgument("noteId") { type = NavType.StringType })
                    ) { entry ->
                        val id = entry.arguments!!.getString("noteId")!!
                        ShareScreen(noteId = id, dark = dark, onBack = { nav.popBackStack() })
                    }
                }
            }
        }
    }

    quickVoiceFor?.let { id ->
        VoiceRecorderSheet(dark = dark, onDismiss = { quickVoiceFor = null }, onDone = { file, dur ->
            quickVoiceFor = null
            scope.launch(Dispatchers.IO) {
                container.notes.attachAudio(id, file, dur)
            }
        })
    }
}
