package com.just_for_fun.synctax.presentation.ui.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.data.model.Format
import com.just_for_fun.synctax.data.model.FormatRecyclerView
import com.google.android.material.card.MaterialCardView

class FormatAdapter(
    private val onItemClickListener: OnItemClickListener,
    private val activity: Activity
) : ListAdapter<FormatRecyclerView?, FormatAdapter.ViewHolder>(AsyncDifferConfig.Builder(
    DIFF_CALLBACK
).build()) {

    var selectedFormat: Format? = null
    private var formats: MutableList<FormatRecyclerView?> = mutableListOf()

    init {
        // No multi-select for now, keeping it simple
    }

    class ViewHolder(itemView: View, onItemClickListener: OnItemClickListener?) : RecyclerView.ViewHolder(itemView) {
        val item: MaterialCardView? = itemView.findViewById(R.id.format_card_constraintLayout)
        val label: Button? = itemView.findViewById(R.id.title)
    }

    override fun submitList(list: MutableList<FormatRecyclerView?>?) {
        if (list != null) {
            formats = list
        }
        super.submitList(list ?: listOf())
    }

    override fun getItemViewType(position: Int): Int {
        val item = formats.getOrNull(position)
        return if (item?.label != null) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            val button = LayoutInflater.from(parent.context)
                .inflate(R.layout.format_type_label, parent, false)

            ViewHolder(button, onItemClickListener)
        } else {
            val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.format_item, parent, false)

            ViewHolder(cardView, onItemClickListener)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = getItem(position) ?: return
        val viewType = getItemViewType(position)

        if (viewType == 0) {
            val button = holder.label
            button?.text = itm.label
            return
        }

        val item = itm.format ?: return
        val card = holder.item ?: return

        // Populate format card
        populateFormatCard(card, item)

        // Handle selection
        card.isChecked = selectedFormat == item

        card.setOnClickListener {
            onItemClickListener.onItemSelect(item)
        }

        card.setOnLongClickListener {
            // Could show format details here
            true
        }
    }

    private fun populateFormatCard(formatCard: MaterialCardView, format: Format) {
        var formatNote = format.format_note
        if (formatNote.isEmpty()) formatNote = "Default"
        else if (formatNote == "best") formatNote = "Best Quality"
        else if (formatNote == "worst") formatNote = "Worst Quality"

        var container = format.container
        if (container == "Default" || container.isBlank()) container = "Default"

        formatCard.findViewById<TextView>(R.id.container).text = container.uppercase()
        formatCard.findViewById<TextView>(R.id.format_note).text = formatNote.uppercase()

        // Show codec information more intelligently
        val isAudioOnly = format.vcodec.isBlank() || format.vcodec == "none"
        val isVideoFormat = format.vcodec.isNotBlank() && format.vcodec != "none"

        formatCard.findViewById<TextView>(R.id.audio_formats).apply {
            if (isVideoFormat && format.acodec.isNotBlank() && format.acodec != "none") {
                // Show audio codec for combined video+audio formats
                text = "Audio: ${format.acodec}"
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        formatCard.findViewById<TextView>(R.id.format_id).apply {
            text = "ID: ${format.format_id}"
        }

        val codec = when {
            format.encoding.isNotBlank() -> format.encoding.uppercase()
            isVideoFormat -> format.vcodec.uppercase()
            else -> format.acodec.uppercase()
        }

        formatCard.findViewById<TextView>(R.id.codec).apply {
            if (codec.isBlank() || codec == "NONE") {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                text = codec
            }
        }

        val filesize = format.filesize
        formatCard.findViewById<TextView>(R.id.file_size).apply {
            if (filesize > 0) {
                text = formatFileSize(filesize)
            } else {
                text = "?"
            }
        }

        // Show bitrate for audio formats and total bitrate for video
        formatCard.findViewById<TextView>(R.id.bitrate).apply {
            val tbr = format.tbr
            if (tbr.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                val bitrateValue = tbr.toDoubleOrNull()?.toInt() ?: tbr
                text = if (isAudioOnly) {
                    "${bitrateValue}kbps"
                } else {
                    "Total: ${bitrateValue}kbps"
                }
            }
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    interface OnItemClickListener {
        fun onItemSelect(item: Format)
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<FormatRecyclerView> = object : DiffUtil.ItemCallback<FormatRecyclerView>() {
            override fun areItemsTheSame(oldItem: FormatRecyclerView, newItem: FormatRecyclerView): Boolean {
                return oldItem.label == newItem.label && oldItem.format?.format_id == newItem.format?.format_id
            }

            override fun areContentsTheSame(oldItem: FormatRecyclerView, newItem: FormatRecyclerView): Boolean {
                return oldItem.label == newItem.label && oldItem.format?.format_id == newItem.format?.format_id
            }
        }
    }
}
