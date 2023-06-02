import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList

object Model {
    val datasetNames: ObservableList<String> = FXCollections.observableArrayList()
    val datasets = mutableMapOf<String, ObservableList<Double>>()
    private val current = ReadOnlyStringWrapper(null)
    var currentSet: ReadOnlyStringProperty = current.readOnlyProperty
    var currentSize = ReadOnlyIntegerWrapper(0)
    var currentList = datasets[currentSet.value]
    var viewMode = ReadOnlyObjectWrapper<ViewMode>(ViewMode.LINE)
    var graphListener : ListChangeListener<Double>? = null
    var btnListeners : ObservableList<ListChangeListener<Double>?>? = FXCollections.observableArrayList()
    var colorScheme = ReadOnlyObjectWrapper(ColorScheme.RANDOM)

    fun addDataset(name : String) {
        if (datasetNames.contains(name)) {
            return
        }
        datasets[name] = FXCollections.observableArrayList(0.0)
        datasetNames.add(name)
        setCurrentDataset(name)
    }

    fun setCurrentDataset(name : String) {
        currentSize.value = datasets[name]?.size ?: 0
        // Temporarily remove current listeners to avoid duplicates
        removeListeners()
        currentList = datasets[name]
        addListeners()
        current.value = name
    }

    private fun removeListeners() {
        if (graphListener != null) {
            currentList?.removeListener(graphListener)
        }
        if (btnListeners != null) {
            for (i in btnListeners!!) {
                currentList?.removeListener(i)
            }
        }
    }

    private fun addListeners() {
        if (graphListener != null) {
            currentList?.addListener(graphListener)
        }
        if (btnListeners != null) {
            for (i in btnListeners!!) {
                currentList?.addListener(i)
            }
        }
    }

    fun addDataEntry() {
        datasets[current.value]?.add(0.0)
        currentSize.value = datasets[current.value]?.size ?: 0
    }

    fun updateEntry(index: Int, num: Double) {
        if (currentList?.minOrNull()!! >= 0.0 && num < 0.0) {
            viewMode.value = ViewMode.LINE
        }
        datasets[current.value]?.set(index, num)

    }

    fun removeEntry(index : Int) {
        datasets[current.value]?.removeAt(index)
        currentSize.value = datasets[current.value]?.size ?: 0
    }

    fun setView(mode : ViewMode) {
        viewMode.value = mode
    }

    fun setScheme(scheme: ColorScheme) {
        colorScheme.value = scheme
    }
}

enum class ViewMode(val text: String) {
    LINE("Line"), BAR("Bar"), SEM("Bar(SEM)"), PIE("Pie"), HBAR("HBar");
}

enum class ColorScheme(val text: String) {
    RANDOM("Random"), RED("Red"), GREEN("Green"), BLUE("Blue");
    // For reverse lookup
    companion object {
        private val mapping = values().associateBy(ColorScheme::text)
        fun fromText(text: String) = mapping[text]
    }
}