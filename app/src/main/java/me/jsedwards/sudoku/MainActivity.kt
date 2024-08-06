package me.jsedwards.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import me.jsedwards.sudoku.ui.theme.SudokuSolverTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuSolverTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SudokuCanvas()
                }
            }
        }
    }
}

@Composable
fun SudokuCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val path = Path()
        path.moveTo(0f, 0f)
        path.lineTo(50f, 0f)
        path.lineTo(50f, 50f)
        path.lineTo(0f, 50f)
        drawPath(path = path, brush = SolidColor(Color.Red))
    }
}