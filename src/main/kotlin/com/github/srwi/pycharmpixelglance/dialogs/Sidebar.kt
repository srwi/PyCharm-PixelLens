package com.github.srwi.pycharmpixelglance.dialogs

import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.CardLayout
import javax.swing.*


enum class SidebarType {
    BatchSidebar,
    ChannelSidebar
}


class Sidebar {
    private val panel = JPanel(CardLayout())
    private val batchPanel: JPanel
    private val channelPanel: JPanel
    private val batchList: JBList<Int>
    private val channelList: JBList<Any>
    private var onBatchIndexChanged: ((Int) -> Unit)? = null
    private var onChannelIndexChanged: ((Int?) -> Unit)? = null

    init {
        batchList = createBatchList()
        channelList = createChannelList()

        batchPanel = createSubPanel("Layer", batchList)
        channelPanel = createSubPanel("Channel", channelList)

        panel.border = BorderFactory.createMatteBorder(0, 1, 0, 0, JBColor.LIGHT_GRAY)
        panel.add(batchPanel, SidebarType.BatchSidebar.name)
        panel.add(channelPanel, SidebarType.ChannelSidebar.name)
    }

    private fun createBatchList(): JBList<Int> {
        val listModel = DefaultListModel<Int>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener {
            onBatchIndexChanged?.invoke(list.selectedValue)
        }
        return list
    }

    private fun createChannelList(): JBList<Any> {
        val listModel = DefaultListModel<Any>()
        val list = JBList(listModel)
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.addListSelectionListener {
            onChannelIndexChanged?.invoke(if (list.selectedValue == "All") null else list.selectedValue as Int)
        }
        return list
    }

    private fun createSubPanel(title: String, list: JList<*>): JPanel {
        val titleLabel = SimpleColoredComponent().apply {
            append(title, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
            border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        }
        val scrollPane = JBScrollPane(list).apply {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        return JPanel(BorderLayout()).apply {
            add(titleLabel, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun getComponent(): JComponent = panel

    fun showPanel(type: SidebarType) {
        (panel.layout as CardLayout).show(panel, type.name)
    }

    fun updateChannelList(channels: Int) {
        val hasRgbChannels = channels == 3 || channels == 4
        channelList.model = DefaultListModel<Any>().apply {
            if (hasRgbChannels) addElement("All")
            for (i in 0 until channels) addElement(i)
        }
        if (channelList.model.size > 0) {
            channelList.selectedIndex = 0
            onChannelIndexChanged?.invoke(if (channelList.selectedValue == "All") null else channelList.selectedValue as Int)
        }
    }

    fun updateBatchList(batches: Int) {
        batchList.model = DefaultListModel<Int>().apply {
            for (i in 0 until batches) addElement(i)
        }
        if (batchList.model.size > 0) {
            batchList.selectedIndex = 0
            onBatchIndexChanged?.invoke(batchList.selectedValue)
        }
    }

    fun setSelectedBatchIndex(index: Int) {
        batchList.selectedIndex = index
    }

    fun setSelectedChannelIndex(index: Int?) {
        channelList.selectedIndex = if (index == null) 0 else index + 1
    }

    fun onBatchIndexChanged(listener: (Int) -> Unit) {
        onBatchIndexChanged = listener
    }

    fun onChannelIndexChanged(listener: (Int?) -> Unit) {
        onChannelIndexChanged = listener
    }
}