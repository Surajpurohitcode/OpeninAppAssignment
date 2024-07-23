package com.surajpurohit.openinappassignment.ui.fragments.links

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.surajpurohit.openinappassignment.R
import com.surajpurohit.openinappassignment.data.model.RecentLink
import com.surajpurohit.openinappassignment.data.model.TopLink
import com.surajpurohit.openinappassignment.databinding.FragmentLinksBinding
import com.surajpurohit.openinappassignment.ui.fragments.links.adapter.RecentLinksAdapter
import com.surajpurohit.openinappassignment.ui.fragments.links.adapter.TopLinksAdapter
import com.surajpurohit.openinappassignment.util.DividerItemDecoration
import com.surajpurohit.openinappassignment.util.DrawableUtils
import com.surajpurohit.openinappassignment.util.Resource
import com.surajpurohit.openinappassignment.util.getGreetingMessage
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class LinksFragment : Fragment(), RecentLinksAdapter.Callbacks, TopLinksAdapter.Callbacks {

    private lateinit var linksViewModel: LinksViewModel

    private var _binding: FragmentLinksBinding? = null
    private val binding get() = _binding!!

    private var recentLinksAdapter: RecentLinksAdapter? = null
    private var mainList: MutableList<RecentLink> = ArrayList()
    private val dummyList: MutableList<RecentLink> = ArrayList()

    private var topLinksAdapter: TopLinksAdapter? = null

    private var topLinksList: MutableList<TopLink> = ArrayList()
    private var dummyTopLinksList: MutableList<TopLink> = ArrayList()

    lateinit var lineList: ArrayList<Entry>
    lateinit var lineDataSet: LineDataSet
    lateinit var lineData: LineData

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentLinksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        linksViewModel =
            ViewModelProvider(
                this,
                LinksViewModelProvider(requireActivity().application)
            )[LinksViewModel::class.java]
        linksViewModel.getDashboardApiData()

        bindView()
        bindObserver()
    }

    private fun bindObserver() {
        linksViewModel.dashboardResponseResult.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is Resource.Success -> {
                    handleShimmer(false)
                    response.data?.let { data ->
                        _binding?.apply {
                            todayClicksTextView.text = data.today_clicks.toString()
                            topSourceTextView.text = data.top_source
                            topLocationTextView.text = data.top_location
                        }

                        // Clear lists before updating
                        mainList.clear()
                        dummyList.clear()
                        topLinksList.clear()
                        dummyTopLinksList.clear()

                        // Update recent links
                        data.data?.recent_links?.let { recentLinks ->
                            mainList.addAll(recentLinks)
                            dummyList.addAll(recentLinks.take(4))
                            recentLinksAdapter?.notifyDataSetChanged()
                        }

                        // Update top links
                        data.data?.top_links?.let { topLinks ->
                            topLinksList.addAll(topLinks)
                            dummyTopLinksList.addAll(topLinks.take(4))
                            topLinksAdapter?.notifyDataSetChanged()
                        }

                        //Whatsapp support
                        _binding?.whatsappSupportButton?.setOnClickListener {
                            val number = response.data.support_whatsapp_number
                            setClickToChat(binding.root,number)
                        }

                        // Set graph properties and values
                        setGraphPropertiesAndValue(data.data.overall_url_chart)
                    }
                }

                is Resource.Error -> {
                    handleShimmer(false)
                    Toast.makeText(
                        requireContext(),
                        "Error loading data. Please try again later!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is Resource.Loading -> {
                    handleShimmer(true)
                }
            }
        })
    }


    private fun setGraphPropertiesAndValue(urlChartResponse: Map<String, Int>?) {
        lineList = ArrayList()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val calendar = Calendar.getInstance()

        val defaultDate = "2024-07-23"
        val defaultStartDate = try {
            fullDateFormat.parse("$defaultDate 00:00")
        } catch (e: Exception) {
            Log.e("ChartDebug", "Error parsing defaultStartDate: ${e.message}")
            null
        }

        val defaultEndDate = try {
            fullDateFormat.parse("$defaultDate 23:59")
        } catch (e: Exception) {
            Log.e("ChartDebug", "Error parsing defaultEndDate: ${e.message}")
            null
        }

        val startDateKey = urlChartResponse?.keys?.minOrNull()
        val endDateKey = urlChartResponse?.keys?.maxOrNull()

        val startDate = try {
            startDateKey?.let {
                fullDateFormat.parse("$defaultDate $it") ?: defaultStartDate
            } ?: defaultStartDate
        } catch (e: Exception) {
            Log.e("ChartDebug", "Error parsing startDate: ${e.message}")
            defaultStartDate
        }

        val endDate = try {
            endDateKey?.let {
                fullDateFormat.parse("$defaultDate $it") ?: defaultEndDate
            } ?: defaultEndDate
        } catch (e: Exception) {
            Log.e("ChartDebug", "Error parsing endDate: ${e.message}")
            defaultEndDate
        }

        // Update button text with the current date range
        val textFormat = SimpleDateFormat("d MMM", Locale.US)
        _binding?.durationButton?.text =
            "${textFormat.format(startDate)} - ${textFormat.format(endDate)}"

        // Debug log for date keys and parsed dates
        Log.d("ChartDebug", "StartDateKey: $startDateKey, EndDateKey: $endDateKey")
        Log.d("ChartDebug", "StartDate: $startDate, EndDate: $endDate")

        urlChartResponse?.forEach { (key, value) ->
            try {
                val date = fullDateFormat.parse("$defaultDate $key")
                // Ensure date and startDate are non-null before checking range
                if (date != null && startDate != null && endDate != null && date in startDate..endDate) {
                    val daysSinceStart = startDate?.let {
                        TimeUnit.MILLISECONDS.toDays(date.time - it.time).toFloat()
                    } ?: 0f
                    lineList.add(Entry(daysSinceStart, value.toFloat()))
                }
            } catch (e: Exception) {
                Log.e("ChartDebug", "Error parsing date for key $key: ${e.message}")
            }
        }

        lineDataSet = LineDataSet(lineList, null)
        lineDataSet.color = Color.parseColor("#0E6FFF")
        lineDataSet.setDrawValues(false)
        lineDataSet.setDrawCircles(false)
        lineDataSet.setDrawFilled(true)

        val startColor = Color.parseColor("#0E6FFF")
        val endColor = Color.TRANSPARENT
        val gradientFill = DrawableUtils.createGradientDrawable(startColor, endColor)
        lineDataSet.fillDrawable = gradientFill

        lineData = LineData(lineDataSet)
        _binding?.chart?.data = lineData

        val xAxis = _binding?.chart?.xAxis
        xAxis?.position = XAxis.XAxisPosition.BOTTOM
        xAxis?.setDrawAxisLine(true)
        xAxis?.setDrawLabels(true)

        val labelCount =
            TimeUnit.MILLISECONDS.toDays(startDate?.time?.let { endDate?.time?.minus(it) }
                ?: 0).toInt()
        val xAxisValueFormatter = object : ValueFormatter() {
            private val format = SimpleDateFormat("d MMM", Locale.US)
            override fun getFormattedValue(value: Float): String {
                calendar.time = startDate
                calendar.add(Calendar.DAY_OF_YEAR, value.toInt())
                val date = calendar.time
                return if (value.toInt() % 5 == 0) {
                    format.format(date)
                } else {
                    ""
                }
            }
        }
        xAxis?.valueFormatter = xAxisValueFormatter
        xAxis?.granularity = 1f
        xAxis?.labelCount = labelCount

        val yAxisRight = _binding?.chart?.axisRight
        yAxisRight?.isEnabled = false

        _binding?.chart?.description?.isEnabled = false

        _binding?.chart?.legend?.isEnabled = false

        _binding?.chart?.invalidate()
    }


    private fun bindView() {

        _binding?.rvRecentLinks?.layoutManager = LinearLayoutManager(requireContext())
        _binding?.rvTopLinks?.layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = DividerItemDecoration(requireContext())
        _binding?.rvRecentLinks?.addItemDecoration(dividerItemDecoration)
        _binding?.rvTopLinks?.addItemDecoration(dividerItemDecoration)

        topLinksAdapter = TopLinksAdapter(dummyTopLinksList)
        topLinksAdapter!!.setCallback(this)
        topLinksAdapter!!.setWithFooter(true)
        _binding?.rvTopLinks?.adapter = topLinksAdapter

        recentLinksAdapter = RecentLinksAdapter(dummyList)
        recentLinksAdapter!!.setCallback(this)
        recentLinksAdapter!!.setWithFooter(true)
        _binding?.rvRecentLinks?.adapter = recentLinksAdapter

        _binding?.chipGroup?.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chip1 -> {
                    _binding?.rvRecentLinks?.visibility = View.GONE
                    _binding?.rvTopLinks?.visibility = View.VISIBLE
                }

                R.id.chip2 -> {
                    _binding?.rvTopLinks?.visibility = View.GONE
                    _binding?.rvRecentLinks?.visibility = View.VISIBLE
                }
            }
        }

        _binding?.greetingTextView?.text = getGreetingMessage()
    }

    fun setClickToChat(view: View, toNumber: String) {
        val url = "https://api.whatsapp.com/send?phone=$toNumber"
        val context = view.context
        val packageManager = context.packageManager

        try {
            packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: PackageManager.NameNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        }
    }


    private fun handleShimmer(isShimmering: Boolean) {
        if (isShimmering) {
            _binding?.parentLayout?.setBackgroundColor(Color.TRANSPARENT)
            _binding?.shimmerFrameLayout?.root?.visibility = View.VISIBLE
            _binding?.mainLayout?.visibility = View.GONE
            _binding?.linearLayout?.visibility = View.GONE
        } else {
            _binding?.parentLayout?.setBackgroundColor(resources.getColor(R.color.primary))
            _binding?.shimmerFrameLayout?.root?.visibility = View.GONE
            _binding?.mainLayout?.visibility = View.VISIBLE
            _binding?.linearLayout?.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClickLoadMoreRecentLinks() {
        recentLinksAdapter!!.setWithFooter(false) // hide footer

        //dummyList.clear()

        for (i in 4 until mainList.size) {
            dummyList.add(mainList[i])
        }

        recentLinksAdapter!!.notifyDataSetChanged()
    }

    override fun onRecentLinksItemClicked(recentLink: RecentLink) {
        val clipboardManager =
            activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a new ClipData object
        val clipData = ClipData.newPlainText("text", recentLink.web_link)

        // Set the clipboard's primary clip
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Link Copied!", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClickLoadMoreTopLinks() {
        topLinksAdapter!!.setWithFooter(false)
        for (i in 4 until topLinksList.size) {
            dummyTopLinksList.add(topLinksList[i])
        }
        topLinksAdapter!!.notifyDataSetChanged()
    }

    override fun onTopLinksItemClicked(topLink: TopLink) {
        val clipboardManager =
            activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Create a new ClipData object
        val clipData = ClipData.newPlainText("text", topLink.web_link)

        // Set the clipboard's primary clip
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(requireContext(), "Link Copied!", Toast.LENGTH_SHORT).show()
    }

}