package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.FitnessRepository
import com.example.ui.navigation.FitCalendarApp
import com.example.ui.theme.FitCalendarTheme
import com.example.ui.viewmodel.FitnessViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = FitnessRepository(database.fitnessDao())
        val factory = FitnessViewModel.Factory(repository)

        setContent {
            FitCalendarTheme {
                val viewModel: FitnessViewModel = viewModel(factory = factory)
                FitCalendarApp(viewModel = viewModel)
            }
        }
    }
}
