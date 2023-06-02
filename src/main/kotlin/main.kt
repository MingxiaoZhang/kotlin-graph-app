import javafx.application.Application
import javafx.collections.FXCollections
import javafx.geometry.Insets
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.stage.Stage

class Main : Application() {
    override fun start(stage: Stage) {
        Model.addDataset("default")
        val graphView = GraphView().apply {
            HBox.setHgrow(this, Priority.ALWAYS)
        }
        val graphPane = StackPane(graphView).apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            maxWidth = Double.MAX_VALUE
            alignment = Pos.CENTER
            prefHeight = 600.0
            padding = Insets(20.0)
        }
        // Toolbar Controls
        val dataCtrl = DatasetController()
        val addCtrl = AddDataController()
        val semCtrl = DisableController(ViewMode.SEM)
        val pieCtrl = DisableController(ViewMode.PIE)
        val viewModeGroup = HBox(
            ViewController(ViewMode.LINE),
            ViewController(ViewMode.BAR),
            ViewController(ViewMode.HBAR),
            semCtrl,
            pieCtrl)
        val colorCtrl = ColorController()
        // Set listener for disable
        Model.currentSet.addListener(semCtrl.datasetListener)
        Model.currentSet.addListener(pieCtrl.datasetListener)
        val toolBar = HBox(
            dataCtrl,
            Separator().apply {
                orientation = Orientation.VERTICAL
            },
            addCtrl.datasetName,
            addCtrl.addBtn,
            Separator().apply {
                orientation = Orientation.VERTICAL
            },
            viewModeGroup,
            colorCtrl).apply {
            padding = Insets(0.0, 10.0, 0.0, 10.0)
            spacing = 10.0
        }
        // View for data
        val data = DataView()
        val dataView = ScrollPane(DataPane(data)).apply {
            minWidth = 130.0
            isFitToWidth = true
        }
        val view = SplitPane(dataView, graphPane).apply {
            setDividerPosition(0, 0.3)
        }
        // Dynamically set canvas size
        graphView.widthProperty().bind(graphPane.widthProperty().subtract(20.0))
        graphView.heightProperty().bind(graphPane.heightProperty().subtract(20.0))
        stage.apply {
            title = "Plot & Graph"
            scene = Scene(VBox(toolBar, Separator(), view),
                800.0, 600.0)
            minWidth = 640.0
            minHeight = 480.0
        }.show ()
        // Initial data
        Model.datasets["default"] = FXCollections.observableArrayList(4.8, 5.6, 1.5, 1.9, 0.2, 2.1, 1.6, 1.6, 1.0, 1.7, 2.7, 2.5, 2.3, 2.8, 1.9, 2.2, 2.0, 2.1, 2.4, 0.3, 1.8, 2.9, 1.5, 0.9, 1.9, 1.1, 1.4, 1.6, 2.3, 1.9, 0.7, 3.4, 6.8)
        dataCtrl.value = "default"
        colorCtrl.value = "Random"
        Model.setCurrentDataset("default")
        data.updateDataView()
        graphView.update(ViewMode.LINE)
    }
}