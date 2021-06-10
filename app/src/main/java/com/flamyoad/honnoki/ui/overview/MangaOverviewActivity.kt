package com.flamyoad.honnoki.ui.overview

import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import com.bumptech.glide.Glide
import com.flamyoad.honnoki.R
import com.flamyoad.honnoki.adapter.MangaOverviewFragmentAdapter
import com.flamyoad.honnoki.databinding.ActivityMangaOverviewBinding
import com.flamyoad.honnoki.dialog.BookmarkDialog
import com.flamyoad.honnoki.data.model.MangaOverview
import com.flamyoad.honnoki.ui.reader.ReaderActivity
import com.flamyoad.honnoki.utils.ViewUtils
import com.flamyoad.honnoki.utils.ui.AppBarStateChangeListener
import com.flamyoad.honnoki.utils.ui.DepthPageTransformer
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

@ExperimentalPagingApi
class MangaOverviewActivity : AppCompatActivity() {
    private var _binding: ActivityMangaOverviewBinding? = null
    val binding get() = requireNotNull(_binding)

    private val viewModel: MangaOverviewViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMangaOverviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = ""

        // Transparent status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                window.navigationBarColor = getColor(R.color.sea_blue)
            }
        } else {
            binding.appbarLayout.updatePadding(top = 32)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }

        if (savedInstanceState == null) {
            viewModel.initializeAll(
                intent.getStringExtra(MANGA_URL) ?: "",
                intent.getStringExtra(MANGA_SOURCE) ?: "",
            )
        }

        initUi()
        initViewPager()
        observeUi()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    private fun initUi() {
        binding.appbarLayout.addOnOffsetChangedListener(object : AppBarStateChangeListener() {
            override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?) {
                with(binding) {
                    when (state) {
                        State.EXPANDED -> {
                            toolbarContent.alpha = 0f
                            showToolbarArea(ToolbarState.EXPANDED)
                            swipeRefreshLayout.isEnabled = false
                        }
                        State.COLLAPSED -> {
                            toolbarContent.alpha = 1f
                            showToolbarArea(ToolbarState.COLLAPSED)
                            swipeRefreshLayout.isEnabled = true
                        }
                        State.IDLE -> {
                        }
                    }
                }
            }

            /**
             * @param i Vertical offset of the AppBarLayout. When fully expanded, it shows 0
             * When not fully expanded, it shows values in the range [-∞,-1]
             */
            override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
                super.onOffsetChanged(appBarLayout, i)
                /**
                 * The condition below is to know when the scrolled distance falls between EXPANDED and COLLAPSED
                 *
                 * onOffsetChanged() returns negative value on scrolling.
                 * totalScrollRange() returns positive value. Therefore we have to convert it to negative value
                 */
                val totalScrollRange = appBarLayout.totalScrollRange.unaryMinus()
                if ((i < 0) && (i > totalScrollRange)) {
                    showToolbarArea(ToolbarState.EXPANDED)
                    binding.swipeRefreshLayout.isEnabled = false
                }

            }
        })

        binding.btnFavouriteExpanded.setOnClickListener {
            showBookmarkGroupDialog()
        }

        binding.btnFavouriteCollapsed.setOnClickListener {
            showBookmarkGroupDialog()
        }

        binding.btnReadCollapsed.setOnClickListener {
            startReading()
        }

        binding.btnReadExpanded.setOnClickListener {
            startReading()
        }
    }

    private fun initViewPager() {
        val tabList = listOf(TAB_NAME_SUMMARY)

        val pagerAdapter = MangaOverviewFragmentAdapter(tabList, this)

        with(binding.viewPager) {
            adapter = pagerAdapter
            setPageTransformer(DepthPageTransformer())
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabList[position]
            binding.viewPager.setCurrentItem(tab.position, true)
        }.attach()
    }

    private fun observeUi() {
        viewModel.hasBeenBookmarked.observe(this) {
            binding.btnFavouriteCollapsed.isChecked = it
            binding.btnFavouriteExpanded.isChecked = it
        }

        viewModel.authorList.observe(this) { authors ->
            binding.txtAuthor.text = authors.joinToString { it.name }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.mangaOverview.collectLatest {
                showMangaOverview(it)
            }
        }
    }

    private fun showMangaOverview(overview: MangaOverview) {
        with(binding) {
            Glide.with(this@MangaOverviewActivity)
                .load(overview.coverImage)
                .into(imageBackground)

            Glide.with(this@MangaOverviewActivity)
                .load(overview.coverImage)
                .placeholder(ViewUtils.getLoadingIndicator(this@MangaOverviewActivity))
                .into(imageManga)

            if (overview.alternativeTitle.isBlank()) {
                txtAlternateName.isVisible = false
            } else {
                txtAlternateName.text = overview.alternativeTitle
            }

            txtTitle.text = overview.mainTitle
            txtAlternateName.text = overview.alternativeTitle
            txtStatus.text = overview.status
        }
    }

    private fun showToolbarArea(state: ToolbarState) {
        with(binding) {
            btnReadExpanded.isVisible = state == ToolbarState.EXPANDED
            btnFavouriteExpanded.isVisible = state == ToolbarState.EXPANDED

            btnReadCollapsed.isVisible = state == ToolbarState.COLLAPSED
            btnFavouriteCollapsed.isVisible = state == ToolbarState.COLLAPSED
        }
    }

    private fun showBookmarkGroupDialog() {
        val overviewId = viewModel.overview.id ?: return
        if (overviewId == -1L) return

        val dialog = BookmarkDialog.newInstance(overviewId)
        dialog.show(supportFragmentManager, "bookmark_dialog")
    }

    private fun startReading() {
        val overview = viewModel.overview
        if (overview == MangaOverview.empty()) return
        val overviewId = overview.id ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val chapterId = if (overview.lastReadChapterId == -1L) {
                viewModel.getFirstChapter(overviewId)?.id
            } else {
                overview.lastReadChapterId
            }
            if (chapterId == null) return@launch
            ReaderActivity.start(this@MangaOverviewActivity, chapterId, overviewId)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val TAB_NAME_SUMMARY = "Summary"
        const val TAB_NAME_CHAPTERS = "Chapters"

        const val MANGA_URL = "manga_url"
        const val MANGA_SOURCE = "manga_source"
        const val MANGA_TITLE = "manga_title"
    }
}
