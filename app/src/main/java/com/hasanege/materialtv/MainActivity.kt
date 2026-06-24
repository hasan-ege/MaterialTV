
package com.hasanege.materialtv

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.hasanege.materialtv.data.M3uRepository
import com.hasanege.materialtv.network.CredentialsManager
import com.hasanege.materialtv.network.SessionManager
import com.hasanege.materialtv.ui.theme.MaterialTVTheme
import com.hasanege.materialtv.R
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.staticCompositionLocalOf
import android.content.res.Configuration
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedButton
import android.app.PictureInPictureParams
import android.util.Rational

val LocalPipMode = staticCompositionLocalOf { false }
val LocalPipAction = staticCompositionLocalOf<((String) -> Unit)?> { null }

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var isInPipModeState = mutableStateOf(false)
    private var activePipReceiver: ((String) -> Unit)? = null

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // If we want auto-pip on home press, we can check if player is active.
        // For now, PlayerScreen will handle enterPictureInPictureMode manually or set auto-pip.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val credentialsManager = (application as MainApplication).credentialsManager
        val settingsRepo = com.hasanege.materialtv.data.SettingsRepository.getInstance(applicationContext)

        setContent {
            val navController = androidx.navigation.compose.rememberNavController()
            val customName by settingsRepo.userName.collectAsState(initial = null)
            val customAvatar by settingsRepo.userAvatarPath.collectAsState(initial = null)
            val isDisclaimerAccepted by settingsRepo.isDisclaimerAccepted.collectAsState(initial = null)
            var startDestination by remember { mutableStateOf<String?>(null) }
            val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

            LaunchedEffect(Unit) {
                startDestination = checkAutoLoginDest(credentialsManager)
            }

            MaterialTVTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalPipMode provides isInPipModeState.value,
                    LocalPipAction provides { action -> activePipReceiver?.invoke(action) }
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        if (isDisclaimerAccepted == null || startDestination == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (!isDisclaimerAccepted!!) {
                            DisclaimerScreen(
                                onAccept = {
                                    coroutineScope.launch {
                                        settingsRepo.setDisclaimerAccepted(true)
                                    }
                                },
                                onDecline = {
                                    finish()
                                }
                            )
                        } else {
                            com.hasanege.materialtv.navigation.AppNavigation(
                                navController = navController,
                                startDestination = startDestination!!,
                                mainViewModel = viewModel,
                                customName = customName,
                                customAvatar = customAvatar
                            )
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun checkAutoLoginDest(credentialsManager: CredentialsManager): String {
        val serverUrl = credentialsManager.getServerUrl()
        val username = credentialsManager.getUsername()
        val password = credentialsManager.getPassword()
        val m3uUrl = credentialsManager.getM3uUrl()
        
        if (!m3uUrl.isNullOrBlank()) {
            SessionManager.initializeM3u(m3uUrl)
            return try {
                M3uRepository.fetchPlaylist(m3uUrl, applicationContext)
                com.hasanege.materialtv.navigation.Screen.Main.route
            } catch (e: Exception) {
                credentialsManager.clearCredentials()
                com.hasanege.materialtv.navigation.Screen.Login.route
            }
        }
        
        if (!serverUrl.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
            SessionManager.initialize(serverUrl, username, password)
            return com.hasanege.materialtv.navigation.Screen.Main.route
        }

        return com.hasanege.materialtv.navigation.Screen.Login.route
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel, 
    customName: String?, 
    customAvatar: String?, 
    onLoginSuccess: () -> Unit
) {
    LaunchedEffect(viewModel.error) {
        if (viewModel.error == null && SessionManager.isInitialized()) {
            onLoginSuccess()
        }
    }
    
    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it / 3 },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!customName.isNullOrBlank()) {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!customAvatar.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = customAvatar,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = customName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = stringResource(R.string.profile_welcome, customName),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = stringResource(R.string.profile_welcome_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Original Logo / Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name_material),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.app_name_tv),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.login_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Tab Row with M3 styling
                    SecondaryTabRow(selectedTabIndex = if (viewModel.isM3uLogin) 1 else 0) {
                        Tab(
                            selected = !viewModel.isM3uLogin,
                            onClick = { viewModel.isM3uLogin = false },
                            text = { Text(stringResource(R.string.login_tab_xtream)) }
                        )
                        Tab(
                            selected = viewModel.isM3uLogin,
                            onClick = { viewModel.isM3uLogin = true },
                            text = { Text(stringResource(R.string.login_tab_m3u)) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Animated content switch
                    AnimatedContent(
                        targetState = viewModel.isM3uLogin,
                        transitionSpec = {
                            (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + slideInHorizontally(
                                initialOffsetX = { if (targetState) it else -it },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )).togetherWith(
                                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                    targetOffsetX = { if (targetState) -it else it },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            )
                        },
                        label = "LoginTypeSwitch"
                    ) { isM3u ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (isM3u) {
                                OutlinedTextField(
                                    value = viewModel.m3uUrl,
                                    onValueChange = { viewModel.m3uUrl = it },
                                    label = { Text(stringResource(R.string.login_m3u_url_label)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                                    singleLine = true
                                )
                            } else {
                                XtreamLoginFields(viewModel)
                            }
                        }
                    }

                    // Error message with animation
                    AnimatedVisibility(
                        visible = viewModel.error != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        viewModel.error?.let {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Small
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Login button with animation
                    val buttonScale by animateFloatAsState(
                        targetValue = if (viewModel.isLoading) 0.95f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "buttonScale"
                    )
                    
                    FilledTonalButton(
                        onClick = { viewModel.onLoginClick(onLoginSuccess) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .graphicsLayer {
                                scaleX = buttonScale
                                scaleY = buttonScale
                            },
                        enabled = !viewModel.isLoading,
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Large
                    ) {
                        if (viewModel.isLoading) {
                            androidx.compose.material3.CircularWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = with(LocalDensity.current) { 2.dp.toPx() }),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text(
                                text = if (viewModel.isM3uLogin) {
                                    stringResource(R.string.login_button_load_playlist)
                                } else {
                                    stringResource(R.string.login_button_sign_in)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XtreamLoginFields(viewModel: MainViewModel) {
    OutlinedTextField(
        value = viewModel.serverUrl,
        onValueChange = { viewModel.serverUrl = it },
        label = { Text(stringResource(R.string.login_server_url_label)) },
        modifier = Modifier.fillMaxWidth(),
        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = viewModel.username,
        onValueChange = { viewModel.username = it },
        label = { Text(stringResource(R.string.login_username_label)) },
        modifier = Modifier.fillMaxWidth(),
        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = viewModel.password,
        onValueChange = { viewModel.password = it },
        label = { Text(stringResource(R.string.login_password_label)) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
        singleLine = true
    )
}

@Composable
fun DisclaimerScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .widthIn(max = 650.dp)
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.ExtraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.disclaimer_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.disclaimer_decline),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    FilledTonalButton(
                        onClick = onAccept,
                        shape = com.hasanege.materialtv.ui.theme.ExpressiveShapes.Medium,
                        modifier = Modifier
                            .height(48.dp)
                            .focusRequester(focusRequester)
                    ) {
                        Text(
                            text = stringResource(R.string.disclaimer_accept),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

