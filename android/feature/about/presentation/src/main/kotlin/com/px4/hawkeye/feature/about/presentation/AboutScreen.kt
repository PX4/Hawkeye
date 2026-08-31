package com.px4.hawkeye.feature.about.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.px4.hawkeye.core.designsystem.HawkeyeDimens
import com.px4.hawkeye.core.designsystem.HawkeyeTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun AboutRoot(
    onBack: () -> Unit,
    viewModel: AboutViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AboutScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    state: AboutState,
    onAction: (AboutAction) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.about_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(HawkeyeDimens.contentPadding),
        ) {
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = if (state.versionName.isNotEmpty()) {
                    stringResource(R.string.about_version, state.versionName)
                } else {
                    stringResource(R.string.about_version_unknown)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = HawkeyeDimens.captionSpacing),
            )

            Section(
                header = stringResource(R.string.about_official_header),
                body = stringResource(R.string.about_official_body),
            )
            Section(
                header = stringResource(R.string.about_disclaimer_header),
                body = stringResource(R.string.about_disclaimer_body),
            )
            PrivacySection()

            HorizontalDivider(modifier = Modifier.padding(top = HawkeyeDimens.sectionSpacing))
            LicensesSection(state = state, onAction = onAction)
        }
    }
}

@Composable
private fun PrivacySection() {
    val uriHandler = LocalUriHandler.current
    val url = stringResource(R.string.about_privacy_url)

    Section(
        header = stringResource(R.string.about_privacy_header),
        body = stringResource(R.string.about_privacy_body),
    )
    Text(
        text = stringResource(R.string.about_privacy_link),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { uriHandler.openUri(url) }
            .padding(vertical = HawkeyeDimens.itemSpacing),
    )
}

@Composable
private fun Section(header: String, body: String) {
    Text(
        text = header,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(
            top = HawkeyeDimens.sectionSpacing,
            bottom = HawkeyeDimens.titleSpacing,
        ),
    )
    Text(text = body, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun LicensesSection(
    state: AboutState,
    onAction: (AboutAction) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(AboutAction.OnToggleLicenses) }
            .padding(vertical = HawkeyeDimens.itemSpacing),
    ) {
        Text(
            text = stringResource(R.string.about_licenses_header),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = if (state.isLicensesExpanded) {
                stringResource(R.string.about_licenses_hide)
            } else {
                stringResource(R.string.about_licenses_show)
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Text(
        text = stringResource(R.string.about_licenses_summary),
        style = MaterialTheme.typography.bodyMedium,
    )

    if (!state.isLicensesExpanded) return

    val notices = state.notices
    if (notices == null) {
        Text(
            text = stringResource(R.string.about_licenses_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = HawkeyeDimens.itemSpacing),
        )
        return
    }

    // The notices are shipped verbatim from the repository's NOTICE.md and fonts/OFL.txt, so
    // they carry hard-wrapped paragraphs and a table. Monospace plus a horizontal scroll keeps
    // that alignment intact rather than reflowing it into nonsense on a narrow screen.
    Text(
        text = notices,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
            .padding(top = HawkeyeDimens.itemSpacing)
            .horizontalScroll(rememberScrollState()),
    )
}

@Preview(showBackground = true)
@Composable
private fun AboutScreenPreview() {
    HawkeyeTheme {
        AboutScreen(
            state = AboutState(versionName = "0.4.0", notices = "raylib\nzlib/libpng license"),
            onAction = {},
            onBack = {},
        )
    }
}
