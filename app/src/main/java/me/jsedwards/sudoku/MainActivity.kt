package me.jsedwards.sudoku

import Grid
import Solver
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.jsedwards.sudoku.ui.theme.SudokuSolverTheme
import kotlin.math.floor

const val MARGIN = 20f

private val grid = Grid()
private val selectedSquare = arrayOf(0, 0)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SudokuSolverTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column {
                        var refresh by remember { mutableStateOf(false) }
                        val textMeasurer = rememberTextMeasurer(9)
                        BoxWithConstraints {
                            Canvas(Modifier.size(maxWidth, maxWidth).pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    selectedSquare[0] = floor((offset.x - MARGIN) / (size.width - MARGIN) * 9).toInt()
                                    selectedSquare[1] = floor((offset.y - MARGIN) / (size.width - MARGIN) * 9).toInt()
                                    refresh = !refresh
                                }
                            }) {
                                refresh.apply {}
                                val gridSize = size.width - MARGIN * 2
                                val squareSize = gridSize / 9
                                val gridEnd = size.width - MARGIN
                                // Selected square
                                drawRect(
                                    Color(250, 218, 74),
                                    Offset(
                                        MARGIN + selectedSquare[0] * squareSize,
                                        MARGIN + selectedSquare[1] * squareSize
                                    ),
                                    Size(squareSize, squareSize),
                                    style = Fill
                                )
                                // Main square
                                drawRect(
                                    Color.Black,
                                    Offset(MARGIN, MARGIN),
                                    Size(gridSize, gridSize),
                                    style = Stroke(width = 20f)
                                )
                                // Other lines
                                for (i in 1..8) {
                                    val strokeWidth = if (i % 3 == 0) 10f else 5f
                                    val offset = MARGIN + i * squareSize
                                    drawLine(Color.Black, Offset(offset, MARGIN), Offset(offset, gridEnd), strokeWidth)
                                    drawLine(Color.Black, Offset(MARGIN, offset), Offset(gridEnd, offset), strokeWidth)
                                }
                                // Numbers
                                Grid.locations().associateWith { grid.get(it) }
                                    .filterValues { it?.size == 1 }
                                    .forEach { (x, y), list ->
                                        val value = list!!.first()
                                        val layoutResult = textMeasurer.measure(
                                            value.toString(),
                                            style = TextStyle(
                                                fontSize = TextUnit(36f, TextUnitType.Sp),
                                                fontFamily = FontFamily(Font(R.font.karnak_regular, FontWeight.Normal))
                                            )
                                        )
                                        drawText(
                                            layoutResult, Color.Black, topLeft = Offset(
                                                (MARGIN + (x + 0.5) * squareSize - layoutResult.size.width / 2.0).toFloat(),
                                                (MARGIN + (y + 0.5) * squareSize - layoutResult.size.height / 2.0).toFloat()
                                            )
                                        )
                                    }
                            }
                        }
                        // 1 to 5
                        Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                            for (i in 1..5) {
                                Button({
                                    grid.set(selectedSquare[0], selectedSquare[1], mutableListOf(i))
                                    refresh = !refresh
                                }, modifier = Modifier.fillMaxWidth(1f / (6 - i))) { Text(i.toString()) }
                            }
                        }
                        // 6 to 9
                        Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                            for (i in 6..9) {
                                Button({
                                    grid.set(selectedSquare[0], selectedSquare[1], mutableListOf(i))
                                    refresh = !refresh
                                }, modifier = Modifier.fillMaxWidth(1f / (11 - i))) { Text(i.toString()) }
                            }
                            // Clear
                            Button({
                                grid.remove(selectedSquare[0] to selectedSquare[1])
                                refresh = !refresh
                            }, modifier = Modifier.fillMaxWidth()) { Text("X") }
                        }
                        // Solve
                        Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                            Button({
                                val solver = Solver(grid)
                                solver.solve()
                                refresh = !refresh
                            }, modifier = Modifier.fillMaxWidth()) { Text("Solve") }
                        }
                    }
                }
            }
        }
    }
}