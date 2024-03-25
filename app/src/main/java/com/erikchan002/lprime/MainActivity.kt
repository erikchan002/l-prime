package com.erikchan002.lprime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.navigation
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import androidx.wear.tooling.preview.devices.WearDevices
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import com.erikchan002.lprime.theme.WearAppTheme
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.ambient.AmbientAware
import com.google.android.horologist.compose.layout.AppScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberColumnState
import com.google.android.horologist.compose.material.Button
import com.google.android.horologist.compose.material.Chip
import com.google.android.horologist.compose.material.ToggleChip
import com.google.android.horologist.compose.material.ToggleChipToggleControl
import com.google.android.horologist.images.base.paintable.ImageVectorPaintable.Companion.asPaintable
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.worldcubeassociation.tnoodle.puzzle.ClockPuzzle
import org.worldcubeassociation.tnoodle.scrambles.InvalidScrambleException
import org.worldcubeassociation.tnoodle.scrambles.PuzzleRegistry
import org.worldcubeassociation.tnoodle.svglite.Dimension
import org.worldcubeassociation.tnoodle.svglite.Svg

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }
}

sealed class Screens(val route: String) {
    data object Scramble: Screens("scramble")
    data object Settings: Screens("settings"){
        data object SettingsList: Screens("settings/list")
        data object SettingsPuzzleSelect: Screens("settings/puzzleSelect")
        data object SettingsPuzzleColorSelect: Screens("settings/puzzleColorSelect")
    }
}

enum class PuzzleColor {
    STICKERLESS,
    BLACK,
    WHITE,
    UNDYED;

    val strokeColor
        get() = when(this) {
            STICKERLESS -> null
            BLACK -> org.worldcubeassociation.tnoodle.svglite.Color.BLACK
            WHITE -> org.worldcubeassociation.tnoodle.svglite.Color.WHITE
            UNDYED -> org.worldcubeassociation.tnoodle.svglite.Color(0xdfefe7df.toInt())
        }

    val composableText
        @Composable
        get() = when(this) {
            STICKERLESS -> stringResource(id = R.string.puzzle_color_stickerless)
            BLACK -> stringResource(id = R.string.puzzle_color_black)
            WHITE -> stringResource(id = R.string.puzzle_color_white)
            UNDYED -> stringResource(id = R.string.puzzle_color_undyed)
        }
}

private val PuzzleRegistry.composableText
    @Composable
    get() = when(this) {
        PuzzleRegistry.TWO -> stringResource(id = R.string.scrambler_two)
        PuzzleRegistry.THREE -> stringResource(id = R.string.scrambler_three)
        PuzzleRegistry.FOUR -> stringResource(id = R.string.scrambler_four)
        PuzzleRegistry.FOUR_FAST -> stringResource(id = R.string.scrambler_four_fast)
        PuzzleRegistry.FIVE -> stringResource(id = R.string.scrambler_five)
        PuzzleRegistry.SIX -> stringResource(id = R.string.scrambler_six)
        PuzzleRegistry.SEVEN -> stringResource(id = R.string.scrambler_seven)
        PuzzleRegistry.THREE_NI -> stringResource(id = R.string.scrambler_three_ni)
        PuzzleRegistry.FOUR_NI -> stringResource(id = R.string.scrambler_four_ni)
        PuzzleRegistry.FIVE_NI -> stringResource(id = R.string.scrambler_five_ni)
        PuzzleRegistry.THREE_FM -> stringResource(id = R.string.scrambler_three_fm)
        PuzzleRegistry.PYRA -> stringResource(id = R.string.scrambler_pyra)
        PuzzleRegistry.SQ1 -> stringResource(id = R.string.scrambler_sq1)
        PuzzleRegistry.MEGA -> stringResource(id = R.string.scrambler_mega)
        PuzzleRegistry.CLOCK -> stringResource(id = R.string.scrambler_clock)
        PuzzleRegistry.SKEWB -> stringResource(id = R.string.scrambler_skewb)
    }

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun <T> SelectionColumn(
    columnState: ScalingLazyColumnState,
    modifier: Modifier = Modifier,
    options: Collection<T>,
    selected: T,
    onSelect: (T) -> Unit = {},
    title: String,
    optionTextGenerator: @Composable (T) -> String = { it.toString() }
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = modifier
    ) {
        item {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onBackground,
            )
        }
        options.map { option ->
            item {
                ToggleChip(
                    checked = option == selected,
                    onCheckedChanged = {
                        if (it) {
                            onSelect(option)
                        }
                    },
                    label = optionTextGenerator(option),
                    toggleControl = ToggleChipToggleControl.Radio,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun <T> SelectionScreen(
    options: Collection<T>,
    selected: T,
    onSelect: (T) -> Unit = {},
    title: String,
    optionTextGenerator: @Composable (T) -> String = { it.toString() }
) {
    val columnState =
        rememberColumnState(ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = true))
    ScreenScaffold (scrollState = columnState) {
        SelectionColumn(
            columnState = columnState,
            options = options,
            selected = selected,
            onSelect = onSelect,
            title = title,
            optionTextGenerator = optionTextGenerator
        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun SettingsScreen(
    drawScramble: Boolean,
    onDrawScrambleChange: (Boolean)->Unit,
    selectedPuzzle: PuzzleRegistry,
    onClickPuzzle: ()->Unit,
    selectedPuzzleColor: PuzzleColor,
    onClickPuzzleColor: ()->Unit,
) {
    val columnState =
        rememberColumnState(ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = true))
    ScreenScaffold (scrollState = columnState) {
        ScalingLazyColumn(columnState = columnState){
            item {
                Chip(
                    label = stringResource(id = R.string.settings_puzzle_select_chip, selectedPuzzle.composableText),
                    onClick = onClickPuzzle,
                    icon = Icons.Rounded.ArrowDropDown.asPaintable(),
                    largeIcon = true
                )
            }
            item {
                ToggleChip(
                    checked = drawScramble,
                    onCheckedChanged = onDrawScrambleChange,
                    label = "Scramble image",
                    toggleControl = ToggleChipToggleControl.Switch,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
            item {
                Chip(
                    stringResource(id = R.string.settings_puzzle_color_select_chip, selectedPuzzleColor.composableText),
                    onClick = onClickPuzzleColor,
                    icon = Icons.Rounded.ArrowDropDown.asPaintable(),
                    largeIcon = true,
                    enabled = drawScramble,
                )
            }
        }
    }
}

@Composable
fun ScrambleImageCard(puzzle: PuzzleRegistry, puzzleColor: PuzzleColor = PuzzleColor.STICKERLESS, scramble: String) {
    val svg = remember(scramble,puzzle) {
        try {
            puzzle.scrambler.drawScramble(scramble, puzzle.scrambler.defaultColorScheme)
        }
        catch (e: InvalidScrambleException){
            Svg(Dimension(0,0))
        }
    }
    if (puzzle.scrambler !is ClockPuzzle){
        svg.setAllStrokes(puzzleColor.strokeColor)
    }
    svg.recursive { it.setAttribute("shape-rendering","crispEdges") }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .decoderFactory(SvgDecoder.Factory())
            .data(ByteBuffer.wrap(svg.toString().toByteArray()))
            .build()
    )
    if (svg.size.height != 0) {
        Card(onClick = {}) {
            Image(
                modifier = Modifier
                    .aspectRatio(svg.size.width / svg.size.height.toFloat()),
                contentScale = ContentScale.Fit,
                painter = painter,
                contentDescription = stringResource(R.string.scramble_image_content_desc, scramble)
            )
        }
    }
}

private fun org.worldcubeassociation.tnoodle.svglite.Color.fromColor(color:Color): org.worldcubeassociation.tnoodle.svglite.Color{
    return org.worldcubeassociation.tnoodle.svglite.Color((color.convert(ColorSpaces.Srgb).value shr 32).toInt())
}

private fun org.worldcubeassociation.tnoodle.svglite.Element.recursive(action: (org.worldcubeassociation.tnoodle.svglite.Element)->Unit){
    action(this)
    children.forEach {
        it.recursive(action)
    }
}

private fun org.worldcubeassociation.tnoodle.svglite.Element.setAllStrokes(strokeColor: org.worldcubeassociation.tnoodle.svglite.Color?) {
    recursive {
        if(it.getAttribute("stroke") != null) {
            if (strokeColor == null) {
                it.attributes.remove("stroke")
            }
            else {
                it.setStroke(strokeColor)
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ScrambleScreen(
    puzzle: PuzzleRegistry,
    puzzleColor: PuzzleColor,
    scramble: String?,
    isGenerating: Boolean,
    drawScramble: Boolean = false,
    onRefresh: ()->Unit = {},
    onSettingsPressed: ()->Unit = {},
){
    val columnState =
        rememberColumnState(ScalingLazyColumnDefaults.responsive(firstItemIsFullWidth = true))
    ScreenScaffold (scrollState = columnState) {
        ScalingLazyColumn(
            columnState = columnState,
        ){
            item {
                TitleCard(
                    onClick = {},
                    title = {
                        Text(
                            puzzle.composableText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            indicatorColor = MaterialTheme.colors.secondary,
                            trackColor = MaterialTheme.colors.onSurface,
                        )
                    }
                    else {
                        Text(
                            textAlign = TextAlign.Justify,
                            color = MaterialTheme.colors.onBackground,
                            text = scramble ?: "",
                        )
                    }
                }
            }
            if (drawScramble && !isGenerating && scramble!=null) {
                item {
                    ScrambleImageCard(puzzle = puzzle, puzzleColor = puzzleColor, scramble = scramble)
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =  Arrangement.SpaceEvenly
                ){
                    Button(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(id = R.string.refresh_content_desc),
                        onClick = onRefresh,
                    )
                    Button(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = stringResource(id = R.string.settings_content_desc),
                        onClick = onSettingsPressed,
                    )
                }
            }
        }
    }
}

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()
    val puzzle = remember { mutableStateOf(PuzzleRegistry.THREE) }
    val scramble = remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope { Dispatchers.IO }
    val isGeneratingScramble = remember { mutableStateOf(false) }
    val drawScramble = remember { mutableStateOf(true) }
    val puzzleColor = remember { mutableStateOf(PuzzleColor.STICKERLESS) }

    WearAppTheme {
        AmbientAware {
            AppScaffold {
                LaunchedEffect(puzzle.value){
                    scope.launch {
                        launch(Dispatchers.Main){
                            isGeneratingScramble.value = true
                        }
                        val scrambleForPuzzle = puzzle.value
                        val newScramble = scrambleForPuzzle.scrambler.generateScramble()
                        launch(Dispatchers.Main){
                            if (scrambleForPuzzle == puzzle.value) {
                                scramble.value = newScramble
                            }
                            isGeneratingScramble.value = false
                        }
                    }
                }
                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = Screens.Scramble.route
                ) {
                    composable(Screens.Scramble.route) {
                        ScrambleScreen(
                            puzzle = puzzle.value,
                            puzzleColor = puzzleColor.value,
                            scramble = scramble.value,
                            isGenerating = isGeneratingScramble.value,
                            drawScramble = drawScramble.value,
                            onRefresh = {
                                scope.launch {
                                    launch(Dispatchers.Main){
                                        isGeneratingScramble.value = true
                                    }
                                    val scrambleForPuzzle = puzzle.value
                                    val newScramble = scrambleForPuzzle.scrambler.generateScramble()
                                    launch(Dispatchers.Main){
                                        if (scrambleForPuzzle == puzzle.value) {
                                            scramble.value = newScramble
                                        }
                                        isGeneratingScramble.value = false
                                    }
                                }
                            },
                            onSettingsPressed = {
                                navController.navigate(Screens.Settings.route)
                            },
                        )
                    }
                    navigation(
                        startDestination = Screens.Settings.SettingsList.route,
                        route = Screens.Settings.route
                    ) {
                        composable(Screens.Settings.SettingsList.route) {
                            SettingsScreen(
                                drawScramble = drawScramble.value,
                                onDrawScrambleChange = { drawScramble.value = it },
                                selectedPuzzle = puzzle.value,
                                onClickPuzzle = {
                                    navController.navigate(Screens.Settings.SettingsPuzzleSelect.route)
                                },
                                selectedPuzzleColor = puzzleColor.value,
                                onClickPuzzleColor = {
                                    navController.navigate(Screens.Settings.SettingsPuzzleColorSelect.route)
                                }
                            )
                        }
                        composable(Screens.Settings.SettingsPuzzleSelect.route) {
                            SelectionScreen(
                                options = PuzzleRegistry.entries,
                                selected = puzzle.value,
                                onSelect = {
                                    puzzle.value = it
                                    scope.launch {
                                        launch(Dispatchers.Main){
                                            isGeneratingScramble.value = true
                                        }
                                        val scrambleForPuzzle = puzzle.value
                                        val newScramble = scrambleForPuzzle.scrambler.generateScramble()
                                        launch(Dispatchers.Main){
                                            if (scrambleForPuzzle == puzzle.value) {
                                                scramble.value = newScramble
                                            }
                                            isGeneratingScramble.value = false
                                        }
                                    }
                                    navController.popBackStack()
                                },
                                title = stringResource(id = R.string.puzzle_select_title),
                                optionTextGenerator = { it.composableText }
                            )
                        }
                        composable(Screens.Settings.SettingsPuzzleColorSelect.route) {
                            SelectionScreen(
                                options = PuzzleColor.entries,
                                selected = puzzleColor.value,
                                onSelect = {
                                    puzzleColor.value = it
                                    navController.popBackStack()
                                },
                                title = stringResource(id = R.string.puzzle_color_select_title),
                                optionTextGenerator = { it.composableText }
                            )
                        }
                    }

                }
            }
        }

    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp()
}
