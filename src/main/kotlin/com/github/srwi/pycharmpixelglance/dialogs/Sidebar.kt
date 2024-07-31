package com.github.srwi.pycharmpixelglance.dialogs

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

        batchPanel = createSubPanel("Batch Index", batchList)
        channelPanel = createSubPanel("Channel Index", channelList)

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
        return JPanel(BorderLayout()).apply {
            add(JLabel(title), BorderLayout.NORTH)
            add(JBScrollPane(list), BorderLayout.CENTER)
        }
    }

    fun getComponent(): JComponent = panel

    fun showPanel(type: SidebarType) {
        (panel.layout as CardLayout).show(panel, type.name)
    }

    fun updateChannelList(channels: Int) {
        channelList.model = DefaultListModel<Any>().apply {
            if (channels == 3 || channels == 4) addElement("All")
            for (i in 0 until channels) addElement(i)
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