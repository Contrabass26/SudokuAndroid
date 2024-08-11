package me.jsedwards.sudoku

import Grid
import Solver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import then
import kotlin.math.floor
import kotlin.math.pow

const val MARGIN = 20f

private val grid = Grid()
private val selectedSquare = arrayOf(0, 0)
private val cornerPositions = arrayOf(100f to 100f, 500f to 100f, 500f to 500f, 100f to 500f)

const val GridRoute = "grid"
const val SelectBoundsRoute = "selectBounds"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SudokuApp() }
    }

    @Composable
    fun SudokuApp() {
        val navController = rememberNavController()
        NavHost(navController, startDestination = GridRoute) {
            composable(GridRoute) { GridScreen { navController.navigate(SelectBoundsRoute) } }
            composable(SelectBoundsRoute) { SelectBounds { navController.navigate(GridRoute) } }
        }
    }

    fun Context.getImageFile() = externalCacheDir!!.then("latest_image.jpg")

    @Composable
    fun SelectBounds(toGrid: () -> Unit) {
        Column {
            // Confirm button
            Button(modifier = Modifier.fillMaxWidth(), onClick = {
                println("Confirmed")
                toGrid()
            }) { Text("Confirm") }
            Box {
                // State for refreshing
                var refresh by remember { mutableStateOf(false) }
                var draggingCorner: Int =
                    -1 // Index of the corner that is being dragged, or -1 if no corner is being dragged
                // Get, rotate and display image
                val image = BitmapFactory.decodeFile(getImageFile().absolutePath)
                val matrix = Matrix()
                matrix.postRotate(90f)
                val rotated = Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, false)
                Image(rotated.asImageBitmap(), null, modifier = Modifier.fillMaxWidth())
                // Canvas over the top - calculate height for button below
                Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                    // Get height
                    detectDragGestures(onDragStart = { (x, y) ->
                        draggingCorner = cornerPositions.indexOfFirst { (x1, y1) ->
                            val squaredDistance = (x1 - x).pow(2) + (y1 - y).pow(2)
//                    println(squaredDistance)
                            squaredDistance <= 3600
                        }
                    }, onDrag = { change, _ ->
                        if (draggingCorner != -1) {
                            val position = change.position
                            cornerPositions[draggingCorner] = position.x to position.y
                            refresh = !refresh
                        }
                    }, onDragEnd = {
                        draggingCorner = -1
                    })
                }) {
                    refresh.apply {}
                    // Function to scale from image coordinates to canvas coordinates
                    val imageToCanvas = { it: Float -> it * size.width / rotated.width }
                    // Draw corners
                    cornerPositions.forEach { (x, y) ->
                        val circle =
                            { r: Float, a: Float ->
                                drawOval(
                                    Color.Red,
                                    Offset(x - r, y - r),
                                    Size(r * 2, r * 2),
                                    a,
                                    Fill
                                )
                            }
                        circle(30f, 0.5f)
                        circle(10f, 1f)
                    }
                }
            }
        }
    }

    @Composable
    fun GridScreen(toSelectBounds: () -> Unit) {
        val textMeasurer = rememberTextMeasurer(9)
        // Init camera stuff
        val context = LocalContext.current
        val file = context.getImageFile()
        val uri = FileProvider.getUriForFile(
            context,
            "sudoku.provider", file
        )
        var capturedImageUri by remember { mutableStateOf(Uri.EMPTY) }
        val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            capturedImageUri = uri
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        Column {
            var refresh by remember { mutableStateOf(false) }
            // Canvas container
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
            // Buttons 1 to 5
            Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                for (i in 1..5) {
                    Button({
                        grid.set(selectedSquare[0], selectedSquare[1], mutableListOf(i))
                        refresh = !refresh
                    }, modifier = Modifier.fillMaxWidth(1f / (6 - i))) { Text(i.toString()) }
                }
            }
            Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                // Buttons 6 to 9
                for (i in 6..9) {
                    Button({
                        grid.set(selectedSquare[0], selectedSquare[1], mutableListOf(i))
                        refresh = !refresh
                    }, modifier = Modifier.fillMaxWidth(1f / (11 - i))) { Text(i.toString()) }
                }
                // Clear button
                Button({
                    grid.remove(selectedSquare[0] to selectedSquare[1])
                    refresh = !refresh
                }, modifier = Modifier.fillMaxWidth()) { Text("X") }
            }
            Row(modifier = Modifier.absolutePadding(left = 5.dp, right = 5.dp)) {
                // Solve button
                Button({
                    val solver = Solver(grid)
                    solver.solve()
                    refresh = !refresh
                }, modifier = Modifier.fillMaxWidth(0.5f)) { Text("Solve") }
                // Scan button
                Button({
                    val permissionCheckResult =
                        ContextCompat.checkSelfPermission(context, "android.permission.CAMERA")
                    if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(uri)
                    } else {
                        // Request a permission
                        permissionLauncher.launch("android.permission.CAMERA")
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text("Scan") }
            }
        }
        // Check captured image
        if (capturedImageUri.path?.isNotEmpty() == true) {
            toSelectBounds()
        }
    }
}