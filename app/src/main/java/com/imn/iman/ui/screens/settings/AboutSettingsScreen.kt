package com.imn.iman.ui.screens.settings

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport


import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold

import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource


import androidx.compose.ui.unit.dp
import com.imn.iman.BuildConfig
import com.imn.iman.R

private const val IMAN_REPO_URL = "https://github.com/farhun/iman"
private const val IMAN_ISSUES_URL = "https://github.com/farhun/iman/issues"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    onNavigateCredits: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { SettingsTopBarTitle(stringResource(R.string.about_title), scrollBehavior) },
                navigationIcon = { SettingsBackButton(onBack) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            SectionHeader(stringResource(R.string.about_story))
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
//            Text(
//                text = "This first version is the foundation. Prayer times, reminders, and reading are being shaped into something more disciplined and more beautiful over time. If you see rough edges, that is part of building in public and tightening the product with real use.",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
//            )

            SectionHeader(stringResource(R.string.about_links))

            MenuSectionItem(
                title = stringResource(R.string.about_project_page),
                detail = stringResource(R.string.about_project_page_detail),
                imageVector = Icons.Default.Language,
                onClick = { openUrl(IMAN_REPO_URL) }
            )

            SectionHeader(stringResource(R.string.about_support))
            MenuSectionItem(
                title = stringResource(R.string.about_report_bug),
                detail = stringResource(R.string.about_report_bug_detail),
                imageVector = Icons.Default.BugReport,
                onClick = { openUrl(IMAN_ISSUES_URL) }
            )



            MenuSectionItem(
                title = stringResource(R.string.about_credits_title),
                detail = stringResource(R.string.about_credits_detail),
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                onClick = onNavigateCredits
            )

            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}
