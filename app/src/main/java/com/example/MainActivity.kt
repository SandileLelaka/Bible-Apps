package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.BibleStoryRepository
import com.example.ui.BibleStoriesViewModel
import com.example.ui.HomeScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup local database access
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.storyStateDao()
        val repository = BibleStoryRepository(dao)
        
        // Instantiate the presentation viewmodel via factory
        val viewModel: BibleStoriesViewModel by viewModels {
            BibleStoriesViewModel.Factory(repository, applicationContext)
        }

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
