package de.ritzelprimpf.toniqo.ui.info

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import de.ritzelprimpf.toniqo.R
import de.ritzelprimpf.toniqo.ui.components.ScreenHeader
import de.ritzelprimpf.toniqo.ui.theme.Tq

/**
 * Tally.so form URL for the "Report a Bug" menu item.
 *
 * Configurable in one place — swap this constant to point at a different bug-report form
 * without touching navigation or UI code.
 */
private const val BUG_REPORT_URL = "https://tally.so/r/WO64gv"

/**
 * Tally.so form URL for the "Request a Feature" menu item.
 *
 * Configurable in one place — swap this constant to point at a different feature-request form
 * without touching navigation or UI code.
 */
private const val FEATURE_REQUEST_URL = "https://tally.so/r/RGQOlj"

/**
 * Data privacy policy document URL for the "Data Privacy" menu item.
 *
 * Plain-text response (not HTML) — renders fine in a [WebView], same as any other page.
 */
private const val DATA_PRIVACY_URL = "https://toniqo.ritzelprimpf.de/datenschutz.txt"

/** Wires [WebViewScreen] to [BUG_REPORT_URL] for the "Report a Bug" nav destination. */
@Composable
fun BugReportScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    WebViewScreen(
        title = stringResource(R.string.feedback_bug_report_title),
        kicker = stringResource(R.string.feedback_bug_report_kicker),
        url = BUG_REPORT_URL,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Wires [WebViewScreen] to [FEATURE_REQUEST_URL] for the "Request a Feature" nav destination. */
@Composable
fun FeatureRequestScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    WebViewScreen(
        title = stringResource(R.string.feedback_feature_request_title),
        kicker = stringResource(R.string.feedback_feature_request_kicker),
        url = FEATURE_REQUEST_URL,
        onBack = onBack,
        modifier = modifier,
    )
}

/** Wires [WebViewScreen] to [DATA_PRIVACY_URL] for the "Data Privacy" nav destination. */
@Composable
fun DataPrivacyScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    WebViewScreen(
        title = stringResource(R.string.data_privacy_title),
        kicker = stringResource(R.string.data_privacy_kicker),
        url = DATA_PRIVACY_URL,
        onBack = onBack,
        modifier = modifier,
    )
}

/**
 * Loads a URL ([url]) inside an in-app [WebView], behind the same [ScreenHeader] chrome the
 * other Info sub-screens use ([HelpScreen], [LicensesScreen]) — shared by the feedback web
 * forms ([BugReportScreen], [FeatureRequestScreen]) and the static [DataPrivacyScreen] document.
 *
 * The system/gesture back action navigates the WebView's own history first (so following a link
 * inside the page doesn't strand the user); only once the WebView has no more history does back
 * fall through to [onBack] and pop this screen.
 *
 * ### File upload
 *
 * A plain [WebView] silently no-ops on `<input type="file">` — Android requires the host app to
 * implement [WebChromeClient.onShowFileChooser] itself and launch a system file picker on the
 * page's behalf, then feed the chosen [Uri]s back through [filePathCallback]. [fileChooserLauncher]
 * does that: it launches whatever intent [WebChromeClient.FileChooserParams.createIntent] builds
 * (the standard system document/content picker) and resolves the pending callback with the result.
 * Only [BugReportScreen]/[FeatureRequestScreen]'s Tally forms actually use this; it's harmless
 * (never triggered) for [DataPrivacyScreen]'s plain-text document.
 */
@SuppressLint("SetJavaScriptEnabled") // Required: Tally's form renderer needs JS to function.
@Composable
private fun WebViewScreen(
    title: String,
    kicker: String,
    url: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        val uris = if (result.resultCode == Activity.RESULT_OK && data != null) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        } else {
            null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Tq.Color.BgBase),
    ) {
        Spacer(modifier = Modifier.height(Tq.Sp.s2))

        ScreenHeader(
            title = title,
            kicker = {
                Text(
                    text = kicker,
                    style = Tq.Type.Kicker,
                    color = Tq.Color.FgTertiary,
                )
            },
            onBack = onBack,
            modifier = Modifier.padding(start = Tq.Sp.s3, end = Tq.Sp.s5),
        )

        Spacer(modifier = Modifier.height(Tq.Sp.s3))

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                isLoading = false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowFileChooser(
                                webView: WebView,
                                callback: ValueCallback<Array<Uri>>,
                                fileChooserParams: FileChooserParams,
                            ): Boolean {
                                // Cancel any still-pending chooser before replacing it, so a
                                // stale callback is never left unresolved.
                                filePathCallback?.onReceiveValue(null)
                                filePathCallback = callback
                                return try {
                                    fileChooserLauncher.launch(fileChooserParams.createIntent())
                                    true
                                } catch (e: ActivityNotFoundException) {
                                    filePathCallback = null
                                    false
                                }
                            }
                        }
                        loadUrl(url)
                    }.also { webView = it }
                },
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Tq.Color.SignalMint,
                    trackColor = Tq.Color.BgElev2,
                )
            }
        }
    }

    // WebView holds native resources tied to this composition; release them explicitly on
    // dispose rather than waiting for GC.
    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
            webView = null
        }
    }
}
