package com.prowllabs.prowl.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prowllabs.prowl.Prowl
import com.prowllabs.prowl.applyProwl
import com.prowllabs.prowl.core.mocking.ProwlMockRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                SampleScreen()
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val scope = rememberCoroutineScope()
    val client = remember {
        OkHttpClient.Builder()
            .applyProwl()
            .build()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("ProwlKit Android Sample", style = MaterialTheme.typography.headlineSmall)
            Text("Tap a request, then open the Prowl notification to inspect traffic or create mocks.")

            Button(
                onClick = {
                    scope.launch {
                        fetch(client, "https://jsonplaceholder.typicode.com/posts/1")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fetch Post #1")
            }

            Button(
                onClick = {
                    scope.launch {
                        fetch(client, "https://jsonplaceholder.typicode.com/users")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Fetch Users")
            }

            Button(
                onClick = {
                    Prowl.addMockRule(
                        ProwlMockRule(
                            targetUrlPattern = "jsonplaceholder.typicode.com/posts/1",
                            targetMethod = "GET",
                            mockStatusCode = 200,
                            mockBody = """{"id":1,"title":"Mocked by Prowl","body":"Hello Android"}"""
                                .toByteArray(),
                        ),
                    )
                    scope.launch {
                        fetch(client, "https://jsonplaceholder.typicode.com/posts/1")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add Mock + Fetch Post #1")
            }

            Button(
                onClick = { Prowl.show() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open Inspector")
            }
        }
    }
}

private suspend fun fetch(client: OkHttpClient, url: String) {
    withContext(Dispatchers.IO) {
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                response.body?.string()
            }
        }
    }
}
