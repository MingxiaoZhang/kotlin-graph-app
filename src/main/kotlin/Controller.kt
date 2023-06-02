import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.beans.value.ChangeListener
import javafx.collections.FXCollections

// Dataset selector
class DatasetController : ChoiceBox<String>(Model.datasetNames), ChangeListener<String> {
    init {
        prefWidth = 100.0
        onAction = EventHandler {
            Model.setCurrentDataset(value)
        }
        Model.currentSet.addListener(this)
    }
    override fun changed(observable: ObservableValue<out String>?, oldValue: String?, newValue: String?) {
        if (newValue != value) {
            value = newValue
        }
    }
}

// Textbox and button for adding new dataset
class AddDataController {
    val datasetName = TextField().apply {
        promptText = "Dataset Name"
    }
    val addBtn = Button("Create")
    init {
        addBtn.onAction = EventHandler {
            Model.addDataset(datasetName.text)
            datasetName.text = ""
        }
    }
}

// Textbox and button for changing data value and deletion
class DataPointController(index : Int, num : Double) {
    val entryValue = TextField("$num").apply {
        maxWidth = Double.MAX_VALUE
        HBox.setHgrow(this, Priority.ALWAYS)
    }
    val deleteBtn = Button("X")
    init {
        // Disable if only one
        if ((Model.currentList?.size ?: 0) == 1) {
            deleteBtn.isDisable = true
        }
        entryValue.textProperty().addListener {
                _,_,new ->
            try {
                Model.updateEntry(index, new.toDouble())
            } catch (e : Exception) {
                println(e)
            }
        }
        deleteBtn.onAction = EventHandler {
            Model.removeEntry(index)
        }
    }
}

// Add data entry button
class AddPointController : Button("Add Entry") {
    init {
        maxWidth = Double.MAX_VALUE
        HBox.setHgrow(this, Priority.ALWAYS)
        onAction = EventHandler {
            Model.addDataEntry()
        }
    }
}

// Buttons for changing view mode
open class ViewController(private val mode: ViewMode): Button(mode.text) {
    init {
        onAction = EventHandler {
            Model.setView(mode)
        }
    }
}

// View mode buttons that require disabling when negatives exist
class DisableController(mode: ViewMode) : ViewController(mode) {
    private var btnListener = ListChangeListener {
            _: ListChangeListener.Change<out Double>? ->
        disable()
    }
    var datasetListener =
        ChangeListener {
                _: ObservableValue<out String>?,
                _: String?,
                _: String ->
            disable()
        }
    init {
        Model.btnListeners?.add(btnListener)
    }
    private fun disable() {
        isDisabled = Model.currentList?.minOrNull()!! < 0.0
    }
}

// Color scheme controller
class ColorController()
    : ChoiceBox<String>(FXCollections.observableArrayList(
    "Random", "Red", "Green", "Blue")) {
    init {
        onAction = EventHandler {
            ColorScheme.fromText(value)?.let { it1 -> Model.setScheme(it1) }
        }
    }
}