package com.surajpurohit.openinappassignment.ui.fragments.links.adapter

import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.surajpurohit.openinappassignment.R
import com.surajpurohit.openinappassignment.data.model.TopLink
import com.surajpurohit.openinappassignment.util.convertTimeStampToReadableTime

class TopLinksAdapter(private val mFeedList: List<TopLink>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    interface Callbacks {
        fun onClickLoadMoreTopLinks()
        fun onTopLinksItemClicked(topLink: TopLink)
    }

    private var mCallbacks: Callbacks? = null
    private var mWithHeader = false
    private var mWithFooter = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var itemView: View? = null
        return if (viewType == TYPE_FOOTER) {
            itemView = View.inflate(parent.context, R.layout.row_loadmore, null)
            itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            LoadMoreViewHolder(itemView)
        } else {
            itemView = View.inflate(parent.context, R.layout.links_list_view, null)
            ElementsViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is LoadMoreViewHolder) {
            holder.itemView.setOnClickListener { if (mCallbacks != null) mCallbacks!!.onClickLoadMoreTopLinks() }
        } else {
            val elementsViewHolder = holder as ElementsViewHolder?
            val elements = mFeedList[position]
            elementsViewHolder?.itemView?.setOnClickListener {
                if (mCallbacks != null) mCallbacks!!.onTopLinksItemClicked(elements)
            }
            val maxLength = 18
            if (elements.title.length > maxLength) {
                elementsViewHolder!!.name.text = elements.title.substring(0, maxLength - 3) + "..."
                elementsViewHolder!!.name.ellipsize = TextUtils.TruncateAt.END
            } else {
                elementsViewHolder!!.name.text = elements.title
                elementsViewHolder!!.name.ellipsize = null
            }

            elementsViewHolder!!.clicks.text = elements.total_clicks.toString()
            elementsViewHolder!!.date.text = convertTimeStampToReadableTime(elements.created_at)
            elementsViewHolder!!.link.text = elements.web_link

            //img code
            Glide.with(elementsViewHolder.itemView.context).load(elements.original_image)
                .into(elementsViewHolder.img)

        }
    }

    override fun getItemCount(): Int {
        var itemCount = mFeedList.size
        if (mWithHeader) itemCount++
        if (mWithFooter) itemCount++
        return itemCount
    }

    override fun getItemViewType(position: Int): Int {
        if (mWithHeader && isPositionHeader(position)) return TYPE_HEADER
        return if (mWithFooter && isPositionFooter(position)) TYPE_FOOTER else TYPE_ITEM
    }

    private fun isPositionHeader(position: Int): Boolean {
        return position == 0 && mWithHeader
    }

    fun isPositionFooter(position: Int): Boolean {
        return position == itemCount - 1 && mWithFooter
    }

    fun setWithHeader(value: Boolean) {
        mWithHeader = value
    }

    fun setWithFooter(value: Boolean) {
        mWithFooter = value
    }

    fun setCallback(callbacks: Callbacks?) {
        mCallbacks = callbacks
    }

    inner class ElementsViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    ) {
        val name: TextView
        val clicks: TextView
        val date: TextView
        val link: TextView
        val img: ImageView

        init {
            name = itemView!!.findViewById<View>(R.id.tvMainText) as TextView
            clicks = itemView!!.findViewById<View>(R.id.tvClickNumber) as TextView
            date = itemView!!.findViewById<View>(R.id.tvDate) as TextView
            link = itemView!!.findViewById<View>(R.id.tvLink) as TextView
            img = itemView!!.findViewById<View>(R.id.ivIcon) as ImageView
        }
    }

    inner class LoadMoreViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
    }
}