package com.example.docsach.ui.reader

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.material3.IconButton
import com.example.docsach.R
import com.example.docsach.data.model.Book
import com.example.docsach.ui.UiState
import com.example.docsach.ui.components.DocSachTopAppBar
import com.example.docsach.ui.components.ErrorState
import com.example.docsach.ui.components.LoadingState

private const val TAG = "DocSachReader"

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(factory = ReaderViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val book = (uiState.book as? UiState.Success)?.data
    val decreaseLabel = stringResource(R.string.reader_font_decrease)
    val increaseLabel = stringResource(R.string.reader_font_increase)

    // Google can still refuse a volume the metadata claimed was embeddable; when its viewer reports
    // "not found" we drop to the text reader rather than leaving the user on a blank page.
    var viewerUnavailable by remember(book?.id) { mutableStateOf(false) }
    val useTextReader = book != null && (!book.hasReadablePreview || viewerUnavailable)

    Scaffold(
        modifier = modifier,
        topBar = {
            DocSachTopAppBar(
                title = book?.title ?: stringResource(R.string.app_name),
                onBack = onBack,
                actions = {
                    // Font controls only mean something for the native text reader.
                    if (useTextReader) {
                        IconButton(onClick = { viewModel.changeFontScale(-ReaderViewModel.SCALE_STEP) }) {
                            Text(
                                text = "A-",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { contentDescription = decreaseLabel },
                            )
                        }
                        IconButton(onClick = { viewModel.changeFontScale(ReaderViewModel.SCALE_STEP) }) {
                            Text(
                                text = "A+",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { contentDescription = increaseLabel },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when (val state = uiState.book) {
            UiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is UiState.Error -> ErrorState(
                messageRes = state.messageRes,
                onRetry = viewModel::load,
                modifier = Modifier.padding(innerPadding),
            )
            is UiState.Success -> {
                val current = state.data
                if (current.hasReadablePreview && !viewerUnavailable) {
                    BookPagesViewer(
                        volumeId = current.id,
                        onBack = onBack,
                        onUnavailable = { viewerUnavailable = true },
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                } else {
                    NativeTextReader(
                        book = current,
                        fontScalePercent = uiState.fontScalePercent,
                        restoredScrollOffset = uiState.restoredScrollOffset,
                        onPersistProgress = viewModel::persistProgress,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                }
            }
        }
    }
}

/**
 * Renders **only** the scanned book pages.
 *
 * Google's preview page is the one surface that reliably paints real pages on a phone, but it
 * arrives wrapped in the whole site: a top nav bar, a buy panel, "Các trang được chọn", the
 * bibliographic tables and a footer. Google's own Embedded Viewer API avoids that chrome but does
 * not lay its canvas out at phone width. So we load the preview page and, once it settles, collapse
 * everything that is not the `#viewport` pages container.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
private fun BookPagesViewer(
    volumeId: String,
    onBack: () -> Unit,
    onUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var loading by remember(volumeId) { mutableStateOf(true) }
    var failed by remember(volumeId) { mutableStateOf(false) }

    val webView = remember(volumeId) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            // Lay the page out at its natural desktop width, then zoom out so a whole book page
            // fits the screen instead of showing a cropped corner of it.
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                    Log.d(TAG, "js: ${message.message()}")
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // The preview page redirects itself to ?printsec=frontcover, so this fires more
                    // than once; the script is idempotent and re-runs after every settle.
                    view?.evaluateJavascript(STRIP_CHROME_JS, null)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?,
                ) {
                    // Sub-resource failures are noise; only a failed main frame is a real problem.
                    if (request?.isForMainFrame == true) {
                        Log.w(TAG, "main frame failed: ${error?.description}")
                        loading = false
                        failed = true
                    }
                }
            }
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onReady() {
                        post {
                            Log.d(TAG, "pages visible for $volumeId")
                            loading = false
                        }
                    }

                    @JavascriptInterface
                    fun onNotFound() {
                        post {
                            Log.w(TAG, "no page viewport for $volumeId")
                            loading = false
                            onUnavailable()
                        }
                    }
                },
                JS_BRIDGE,
            )
            loadUrl("https://books.google.com/books?id=$volumeId&printsec=frontcover")
        }
    }

    LaunchedEffect(volumeId, loading) {
        if (loading) {
            delay(VIEWER_TIMEOUT_MS)
            if (loading) {
                Log.w(TAG, "viewer did not answer within ${VIEWER_TIMEOUT_MS}ms for $volumeId")
                loading = false
                onUnavailable()
            }
        }
    }

    BackHandler {
        if (webView.canGoBack()) webView.goBack() else onBack()
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        when {
            failed -> ErrorState(
                messageRes = R.string.error_network,
                onRetry = {
                    failed = false
                    loading = true
                    webView.reload()
                },
                modifier = Modifier.align(Alignment.Center),
            )
            loading -> LoadingState()
        }
    }
}

private const val JS_BRIDGE = "DocSachBridge"

/** Generous on purpose: Google's preview page is slow to settle on a cold WebView. */
private const val VIEWER_TIMEOUT_MS = 25_000L

/**
 * Hides every element that is not an ancestor of `#viewport` — the pages container — then stretches
 * it over the whole screen. Nothing is removed from the DOM, because Google's own paging script
 * keeps reading the nodes around it.
 */
private val STRIP_CHROME_JS = """
(function () {
  var vp = document.getElementById('viewport');
  if (!vp) { $JS_BRIDGE.onNotFound(); return; }

  if (!document.getElementById('docsach-style')) {
    var style = document.createElement('style');
    style.id = 'docsach-style';
    style.textContent =
      'html, body { margin:0 !important; padding:0 !important; height:100% !important;' +
      ' overflow:hidden !important; background:#fff !important; }' +
      '#viewport { position:fixed !important; top:0 !important; left:0 !important;' +
      ' right:0 !important; bottom:0 !important; width:100% !important; height:100% !important;' +
      ' margin:0 !important; padding:0 !important; overflow:auto !important; }';
    document.documentElement.appendChild(style);
  }

  // Walk up from the pages container, hiding each sibling on the way to <body>.
  var node = vp;
  while (node && node.parentNode && node !== document.body) {
    var siblings = node.parentNode.children;
    for (var i = 0; i < siblings.length; i++) {
      if (siblings[i] !== node) { siblings[i].style.display = 'none'; }
    }
    node = node.parentNode;
  }

  $JS_BRIDGE.onReady();
})();
""".trimIndent()

/**
 * Fallback reader for volumes Google will not let us embed. It shows the book's own text with the
 * font controls and scroll restore the mockup's reading screen calls for.
 */
@Composable
private fun NativeTextReader(
    book: Book,
    fontScalePercent: Int,
    restoredScrollOffset: Int,
    onPersistProgress: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(book.id) {
        scrollState.scrollTo(restoredScrollOffset)
    }

    // Read the offset inside onDispose, not during composition — scroll position changes every frame.
    DisposableEffect(book.id) {
        onDispose { onPersistProgress(scrollState.value) }
    }

    Column(modifier = modifier.verticalScroll(scrollState).padding(16.dp)) {
        Text(
            text = stringResource(R.string.reader_no_preview),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = book.description ?: stringResource(R.string.detail_no_description),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = (16 * fontScalePercent / 100f).sp,
                lineHeight = (26 * fontScalePercent / 100f).sp,
            ),
        )
    }
}
