package com.flamyoad.honnoki.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flamyoad.honnoki.R
import com.flamyoad.honnoki.adapter.MangaAdapter
import com.flamyoad.honnoki.databinding.FragmentHomeListBinding
import com.flamyoad.honnoki.dialog.SourceSwitcherDialog
import com.flamyoad.honnoki.model.Manga
import com.flamyoad.honnoki.model.MangaType
import com.flamyoad.honnoki.model.Source
import com.flamyoad.honnoki.model.TabType
import com.flamyoad.honnoki.ui.overview.MangaOverviewActivity
import com.flamyoad.honnoki.utils.extensions.viewLifecycleLazy
import kotlinx.android.synthetic.main.fragment_home_list.view.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeListFragment : Fragment() {
    private val viewModel: HomeViewModel by activityViewModels()
    private val binding by viewLifecycleLazy { FragmentHomeListBinding.bind(requireView()) }

    private val mangaAdapter: MangaAdapter = MangaAdapter(this::openManga)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            mangaAdapter.refresh()
        }
    }

    private fun initRecyclerView() {
        val layoutManager = GridLayoutManager(requireContext(), 2)

        with(binding.listManga) {
            this.adapter = mangaAdapter
            this.layoutManager = layoutManager
        }

        val tabType = MangaType.fromName(arguments?.getString(TAB_TYPE))

        lifecycleScope.launch {
            val mangaFlow = when (tabType) {
                MangaType.RECENTLY -> viewModel.getRecentManga()
                MangaType.TRENDING -> viewModel.getTrendingManga()
            }
            mangaFlow.collectLatest {
                mangaAdapter.submitData(it)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }


        // Listener to determine whether to shrink or expand FAB (Shrink after 1st item in list is no longer visible)
        binding.listManga.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                if (firstVisiblePosition != 0) {
                    viewModel.setShouldShrinkFab(true)
                } else {
                    viewModel.setShouldShrinkFab(false)
                }
            }
        })
    }

    private fun openManga(manga: Manga) {
        val intent = Intent(requireContext(), MangaOverviewActivity::class.java).apply {

        }
        requireContext().startActivity(intent)
    }

    companion object {
        const val TAB_GENRE = "HomeListFragment.TAB_GENRE"
        const val TAB_TYPE = "HomeListFragment.TAB_TYPE"

        @JvmStatic
        fun newInstance(tab: TabType) =
            HomeListFragment().apply {
                arguments = bundleOf(
                    TAB_GENRE to tab.genre,
                    TAB_TYPE to tab.type.readableName
                )
            }
    }
}
