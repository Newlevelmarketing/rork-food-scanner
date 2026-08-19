package com.rork.calzyandroid.ui.sheets

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.rork.calzyandroid.AppViewModel
import com.rork.calzyandroid.data.AiService
import com.rork.calzyandroid.data.EntrySource
import com.rork.calzyandroid.data.ImageUtils
import com.rork.calzyandroid.ui.components.LocalAppLanguage
import com.rork.calzyandroid.ui.components.Pressable
import com.rork.calzyandroid.ui.navigation.MealDraft
import com.rork.calzyandroid.ui.theme.CalzyColors
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Camera scan flow: live CameraX viewfinder, shutter + library picker, then
 * AI analysis. Detects camera availability at runtime and shows a clean
 * placeholder when no camera is present.
 */
@Composable
fun ScanSheet(
    open: Boolean,
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onResult: (MealDraft) -> Unit,
) {
    AnimatedVisibility(
        visible = open,
        enter = fadeIn(tween(240)),
        exit = fadeOut(tween(200)),
        modifier = Modifier.zIndex(12f),
    ) {
        BackHandler(onBack = onClose)
        ScanContent(viewModel = viewModel, onClose = onClose, onResult = onResult)
    }
}

@Composable
private fun ScanContent(
    viewModel: AppViewModel,
    onClose: () -> Unit,
    onResult: (MealDraft) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var analyzing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    fun analyze(dataUrl: String) {
        if (analyzing) return
        analyzing = true
        error = null
        scope.launch {
            try {
                val result = AiService.analyzeImage(
                    dataUrl = dataUrl,
                    jesterMode = data.profile.jesterMode,
                    languageName = language.englishName,
                )
                analyzing = false
                onResult(MealDraft(result = result, photo = dataUrl, source = EntrySource.photo))
            } catch (failure: Exception) {
                analyzing = false
                error = AiService.messageFor(failure)
            }
        }
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            ImageUtils.uriToDataUrl(context, uri)?.let { analyze(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val previewView = remember {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

        if (hasPermission) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            LaunchedEffect(hasPermission) {
                val providerFuture = ProcessCameraProvider.getInstance(context)
                providerFuture.addListener(
                    {
                        try {
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build()
                            preview.setSurfaceProvider(previewView.surfaceProvider)
                            provider.unbindAll()
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                            cameraReady = true
                        } catch (failure: Exception) {
                            cameraFailed = true
                        }
                    },
                    ContextCompat.getMainExecutor(context),
                )
            }
            DisposableEffect(Unit) {
                onDispose {
                    try {
                        ProcessCameraProvider.getInstance(context).get().unbindAll()
                    } catch (ignored: Exception) {
                        // Provider may already be released.
                    }
                }
            }
        }

        // Framing guide
        if (cameraReady && !analyzing) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(300.dp)
                    .border(
                        2.5.dp,
                        Color.White.copy(alpha = 0.85f),
                        RoundedCornerShape(32.dp),
                    ),
            )
        }

        // No-camera / permission-denied placeholder
        if (cameraFailed || permissionDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NoPhotography,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
                Text(
                    text = if (permissionDenied) {
                        "Camera access is off. Enable it in Settings, or pick a photo from your library."
                    } else {
                        "No camera available. Pick a photo from your library instead."
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WindowInsets.statusBars.asPaddingValues())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pressable(onClick = onClose) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close scanner",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "Point at your meal",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Box(modifier = Modifier.size(40.dp))
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(WindowInsets.navigationBars.asPaddingValues())
                .padding(bottom = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Pressable(onClick = {
                pickLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            }) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Choose a meal photo from your library to scan",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Pressable(
                onClick = {
                    if (!cameraReady || analyzing) return@Pressable
                    imageCapture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val rotation = image.imageInfo.rotationDegrees.toFloat()
                                val bitmap = ImageUtils.rotate(image.toBitmap(), rotation)
                                image.close()
                                analyze(ImageUtils.bitmapToDataUrl(bitmap))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                error = "Could not capture that photo. Try again."
                            }
                        },
                    )
                },
                enabled = cameraReady && !analyzing,
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (cameraReady) Color.White else Color.White.copy(alpha = 0.4f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription =
                        "Scan meal — take a photo and analyze its calories and macros",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Box(modifier = Modifier.size(52.dp))
        }

        // Analyzing overlay
        if (analyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CircularProgressIndicator(color = CalzyColors.flame, strokeWidth = 4.dp)
                    Text(
                        text = "Counting your calories…",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Error toast
        error?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 150.dp)
                    .padding(horizontal = 32.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
