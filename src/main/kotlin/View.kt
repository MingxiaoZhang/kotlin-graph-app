import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.ArcType
import javafx.scene.shape.StrokeLineCap
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

class DataPane(vBox: VBox): VBox(vBox, AddPointController()) {
    init {
        padding = Insets(5.0)
        spacing = 5.0
        minWidth = 130.0
    }
}

class DataView: VBox() {
    // Listen to change in selected dataset
    private var datasetListener =
        ChangeListener {
                _: ObservableValue<out String>?,
                _: String?,
                _: String ->
            updateDataView()
        }
    // Listen for adding or deleting data
    private var dataSizeListener =
        ChangeListener {
                _: ObservableValue<out Number>?,
                _: Number?,
                _: Number ->
            updateDataView()
        }

    init {
        Model.currentSet.addListener(datasetListener)
        Model.currentSize.readOnlyProperty.addListener(dataSizeListener)
    }

    fun updateDataView() {
        children.clear()
        children.add(Label("Dataset name: ${Model.currentSet.value}"))
        Model.currentList?.forEachIndexed {
                index, element ->
            val dataPoint = DataPointController(index, element)
            children.add(HBox(
                Label("Entry #$index").apply {
                    HBox.setHgrow(this, Priority.NEVER)
                    minWidth = 55.0},
                dataPoint.entryValue,
                dataPoint.deleteBtn).apply {
                padding = Insets(5.0, 0.0, 5.0, 0.0)
                spacing = 5.0
                alignment = Pos.CENTER
            })
        }
    }
}

class GraphView : Canvas(360.0, 550.0) {
    // Listen to any change in current data (add, delete, value change)
    private var graphListener = ListChangeListener {
            _: ListChangeListener.Change<out Double>? ->
        update(Model.viewMode.value)
    }
    // Listen to change in selected dataset
    private var datasetListener =
        ChangeListener {
                _: ObservableValue<out String>?,
                _: String?,
                _: String ->
            update(Model.viewMode.value)
        }
    // Listen to change in selected view mode
    private var modeListener =
        ChangeListener {
                _: ObservableValue<out ViewMode>?,
                _: ViewMode?,
                new: ViewMode ->
            update(new)
        }
    // Listen to change in canvas size
    private var dimListener =
        ChangeListener {
                _: ObservableValue<out Number>?,
                _: Number?,
                _: Number ->
            if (Model.currentList != null) {
                update(Model.viewMode.value)
            }
        }

    private var colorListener =
        ChangeListener {
                _: ObservableValue<out ColorScheme>?,
                _: ColorScheme?,
                _: ColorScheme ->
            update(Model.viewMode.value)
        }

    init {
        Model.graphListener = graphListener
        Model.currentSet.addListener(datasetListener)
        Model.viewMode.addListener(modeListener)
        widthProperty().addListener(dimListener)
        heightProperty().addListener(dimListener)
        Model.colorScheme.addListener(colorListener)
    }

    fun update(viewMode: ViewMode) {
        when(viewMode) {
            ViewMode.LINE -> drawLine()
            ViewMode.BAR -> drawBar()
            ViewMode.HBAR -> drawHBar()
            ViewMode.SEM -> {
                // Check for negatives
                if (Model.currentList?.minOrNull()!! < 0.0) {
                    drawLine()
                } else {
                    drawSem()
                }
            }
            ViewMode.PIE -> {
                if (Model.currentList?.minOrNull()!! < 0.0) {
                    drawLine()
                } else {
                    drawPie()
                }
            }
        }
    }

    private fun drawLine() {
        // Point offset
        val offset = 2.5
        // Set scale based on value range
        val min = (Model.currentList!!.minOrNull()!!).coerceAtMost(0.0)
        val max = (Model.currentList!!.maxOrNull()!!).coerceAtLeast(0.0)
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            // Draw lines
            stroke = Color.BLACK
            lineWidth = 2.0
            for (i in 1 until Model.currentList!!.size) {
                // Position based on linear transformation to scale
                strokeLine((i - 1).toDouble() * ((width - 20) / (Model.currentList!!.size - 1)) + offset,
                    height - ((height - 20) * (Model.currentList?.get(i - 1)!! - min) / (max - min + 1) + 20) + offset,
                    i.toDouble() * ((width - 20) / (Model.currentList!!.size - 1)) + offset,
                    height - ((height - 20) * (Model.currentList?.get(i)!! - min) / (max - min + 1) + 20) + offset)
            }
            // Draw points
            fill = Color.RED
            if (Model.currentList?.size == 1) {
                fillRect(width / 2, height - ((height - 20) * (Model.currentList?.get(0)!! - min) / (max - min + 1) + 20), 5.0, 5.0)
            } else {
                for (i in Model.currentList?.indices!!) {
                    // Position based on linear transformation to scale
                    fillRect(i.toDouble() * ((width - 20) / (Model.currentList!!.size - 1)),
                        height - ((height - 20) * (Model.currentList?.get(i)!! - min) / (max - min + 1) + 20),
                        5.0, 5.0)
                }
            }
        }
    }

    private fun drawBar() {
        // Set scale based on value range
        val min = (Model.currentList!!.minOrNull()!!).coerceAtMost(0.0)
        val max = (Model.currentList!!.maxOrNull()!!).coerceAtLeast(0.0)
        // Set variables for bar color change
        var red = 0
        var green = 0
        var blue = 0
        when(Model.colorScheme.value) {
            ColorScheme.RANDOM -> {
                red = Random.nextInt(0, 255)
                green = Random.nextInt(0, 255)
                blue = Random.nextInt(0, 255)
            }
            ColorScheme.RED -> {
                red = 255
            }
            ColorScheme.BLUE -> {
                blue = 255
            }
            ColorScheme.GREEN -> {
                green = 255
            }
        }
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            stroke = Color.BLACK
            for (i in Model.currentList?.indices!!) {
                fill = Color.rgb(red, green, blue)
                if (Model.currentList?.get(i)!! < 0) {
                    // Height based on transformation to scale
                    fillRect(i.toDouble() * (width / (Model.currentList!!.size)),
                        height - (20 - height) * min / (max - min + 1),
                        width / (Model.currentList!!.size) * 0.7,
                        (20 - height) * Model.currentList?.get(i)!! / (max - min + 1))
                } else {
                    // Height based on transformation to scale
                    fillRect(i.toDouble() * (width / (Model.currentList!!.size)),
                        height - (height - 20) * (Model.currentList?.get(i)!! - min) / (max - min + 1),
                        width / (Model.currentList!!.size) * 0.7,
                        (height - 20) * Model.currentList?.get(i)!! / (max - min + 1))
                }
                // Change color
                when(Model.colorScheme.value) {
                    ColorScheme.RANDOM -> {
                        red = Random.nextInt(0, 255)
                        green = Random.nextInt(0, 255)
                        blue = Random.nextInt(0, 255)
                    }
                    ColorScheme.RED -> {
                        red = (red + 226) % 255
                    }
                    ColorScheme.BLUE -> {
                        blue = (blue + 226) % 255
                    }
                    ColorScheme.GREEN -> {
                        green = (green + 226) % 255
                    }
                }
            }
            // Zero line
            lineWidth = 3.0
            strokeLine(0.0,
                height - (20 - height) * min / (max - min + 1),
                width,
                height - (20 - height) * min / (max - min + 1))
        }
    }

    private fun computeSem(): Double {
        var sum = 0.0
        val mean = Model.currentList!!.average()
        for (i in Model.currentList!!) {
            sum += (i - mean) * (i - mean)
        }
        return sqrt(sum / Model.currentList!!.size / Model.currentList!!.size)
    }

    private fun drawSem() {
        // Set scale
        val max = (Model.currentList!!.maxOrNull()!!).coerceAtLeast(0.0)
        // Compute attributes
        val mean = Model.currentList!!.average()
        val sem = computeSem()
        // Initial color
        var red = 0
        var green = 0
        var blue = 0
        when(Model.colorScheme.value) {
            ColorScheme.RANDOM -> {
                red = Random.nextInt(0, 255)
                green = Random.nextInt(0, 255)
                blue = Random.nextInt(0, 255)
            }
            ColorScheme.RED -> {
                red = 255
            }
            ColorScheme.BLUE -> {
                blue = 255
            }
            ColorScheme.GREEN -> {
                green = 255
            }
        }
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            stroke = Color.BLACK
            for (i in Model.currentList?.indices!!) {
                fill = Color.rgb(red, green, blue)
                // Height based on transformation to scale
                fillRect(i.toDouble() * (width / (Model.currentList!!.size)),
                    height - (height - 20) * (Model.currentList?.get(i)!!) / (max + 1),
                    width / (Model.currentList!!.size) * 0.7,
                    (height - 20) * Model.currentList?.get(i)!! / (max + 1))
                // Change color
                when(Model.colorScheme.value) {
                    ColorScheme.RANDOM -> {
                        red = Random.nextInt(0, 255)
                        green = Random.nextInt(0, 255)
                        blue = Random.nextInt(0, 255)
                    }
                    ColorScheme.RED -> {
                        red = (red + 226) % 255
                    }
                    ColorScheme.BLUE -> {
                        blue = (blue + 226) % 255
                    }
                    ColorScheme.GREEN -> {
                        green = (green + 226) % 255
                    }
                }
            }
            // Zero line
            lineWidth = 2.0
            lineCap = StrokeLineCap.SQUARE
            strokeLine(0.0,
                height - 1,
                width,
                height - 1)
            // Mean line
            strokeLine(0.0,
                height - (height - 20) * mean / (max + 1),
                width,
                height - (height - 20) * mean / (max + 1))
            // SEM lines
            setLineDashes(8.0)
            strokeLine(0.0,
                height - (height - 20) * (mean + sem) / (max + 1),
                width,
                height - (height - 20) * (mean + sem) / (max + 1))
            strokeLine(0.0,
                height - (height - 20) * (mean - sem) / (max + 1),
                width,
                height - (height - 20) * (mean - sem) / (max + 1))
            setLineDashes(0.0)
            // Text showing attribute values
            fill = Color.WHITE
            fillRect(25.0, 12.0, 65.0, 42.0)
            font = Font.font("verdana", FontWeight.LIGHT, FontPosture.REGULAR, 12.0)
            fill = Color.BLACK
            fillText(
                "Mean: ${(mean * 100).roundToInt() / 100.0} \n SEM: ${(sem * 100).roundToInt() / 100.0}",
                30.0, 30.0, 60.0)
        }
    }

    private fun drawPie() {
        val radius = (width).coerceAtMost(height) - 40
        var angle = 0.0
        val sum = Model.currentList?.sum()
        var red = 0
        var green = 0
        var blue = 0
        when(Model.colorScheme.value) {
            ColorScheme.RANDOM -> {
                red = Random.nextInt(0, 255)
                green = Random.nextInt(0, 255)
                blue = Random.nextInt(0, 255)
            }
            ColorScheme.RED -> {
                red = 255
            }
            ColorScheme.BLUE -> {
                blue = 255
            }
            ColorScheme.GREEN -> {
                green = 255
            }
        }
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            stroke = Color.WHITE
            for (i in Model.currentList!!) {
                fill = Color.rgb(red, green, blue)
                // Set arc angle by proportions
                fillArc(20.0, 20.0, radius, radius, angle, i * 360.0 / sum!!, ArcType.ROUND)
                angle += i * 360.0 / sum
                when(Model.colorScheme.value) {
                    ColorScheme.RANDOM -> {
                        red = Random.nextInt(0, 255)
                        green = Random.nextInt(0, 255)
                        blue = Random.nextInt(0, 255)
                    }
                    ColorScheme.RED -> {
                        red = (red + 226) % 255
                    }
                    ColorScheme.BLUE -> {
                        blue = (blue + 226) % 255
                    }
                    ColorScheme.GREEN -> {
                        green = (green + 226) % 255
                    }
                }
            }
        }
    }

    private fun drawHBar() {
        // Set scale based on value range
        val min = (Model.currentList!!.minOrNull()!!).coerceAtMost(-10.0)
        val max = (Model.currentList!!.maxOrNull()!!).coerceAtLeast(10.0)
        // Set variables for bar color change
        var red = 0
        var green = 0
        var blue = 0
        when(Model.colorScheme.value) {
            ColorScheme.RANDOM -> {
                red = Random.nextInt(0, 255)
                green = Random.nextInt(0, 255)
                blue = Random.nextInt(0, 255)
            }
            ColorScheme.RED -> {
                red = 255
            }
            ColorScheme.BLUE -> {
                blue = 255
            }
            ColorScheme.GREEN -> {
                green = 255
            }
        }
        graphicsContext2D.apply {
            clearRect(0.0, 0.0, width, height)
            stroke = Color.BLACK
            for (i in Model.currentList?.indices!!) {
                fill = Color.rgb(red, green, blue)
                if (Model.currentList?.get(i)!! > 0) {
                    // Width based on transformation to scale
                    fillRect((20 - width) * min / (max - min + 1),
                        i.toDouble() * (height / (Model.currentList!!.size)),
                        (width - 20) * Model.currentList?.get(i)!! / (max - min + 1),
                        height / (Model.currentList!!.size) * 0.7)
                } else {
                    // Width based on transformation to scale
                    fillRect((width - 20) * (Model.currentList?.get(i)!! - min) / (max - min + 1),
                        i.toDouble() * (height / (Model.currentList!!.size)),
                        (20 - width) * Model.currentList?.get(i)!! / (max - min + 1),
                        height / (Model.currentList!!.size) * 0.7)
                }
                // Change color
                when(Model.colorScheme.value) {
                    ColorScheme.RANDOM -> {
                        red = Random.nextInt(0, 255)
                        green = Random.nextInt(0, 255)
                        blue = Random.nextInt(0, 255)
                    }
                    ColorScheme.RED -> {
                        red = (red + 226) % 255
                    }
                    ColorScheme.BLUE -> {
                        blue = (blue + 226) % 255
                    }
                    ColorScheme.GREEN -> {
                        green = (green + 226) % 255
                    }
                }
            }
            // Zero line
            lineWidth = 3.0
            strokeLine((20 - width) * min / (max - min + 1),
                0.0,
                (20 - width) * min / (max - min + 1),
                height)
        }
    }

    override fun isResizable(): Boolean {
        return true
    }

    override fun maxHeight(width: Double): Double {
        return Double.MAX_VALUE
    }

    override fun maxWidth(height: Double): Double {
        return Double.MAX_VALUE
    }

    override fun minHeight(width: Double): Double {
        return 240.0
    }

    override fun minWidth(height: Double): Double {
        return 320.0
    }
}
