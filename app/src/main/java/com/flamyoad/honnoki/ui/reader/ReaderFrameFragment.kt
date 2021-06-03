package com.flamyoad.honnoki.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.ExperimentalPagingApi
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flamyoad.honnoki.adapter.ReaderImageAdapter
import com.flamyoad.honnoki.adapter.ReaderLoadingAdapter
import com.flamyoad.honnoki.databinding.FragmentReaderFrameBinding
import com.flamyoad.honnoki.ui.reader.model.LoadType
import com.flamyoad.honnoki.ui.reader.model.ReaderPage
import kotlinx.coroutines.flow.collectLatest

@ExperimentalPagingApi
class ReaderFrameFragment : Fragment() {

    private var _binding: FragmentReaderFrameBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val parentViewModel: ReaderViewModel by activityViewModels()
    private val viewModel: ReaderFrameViewModel by viewModels()

    private val concatAdapter = ConcatAdapter()
    private val readerAdapter = ReaderImageAdapter()
    private val loadingAdapter = ReaderLoadingAdapter()
    private val linearLayoutManager by lazy { LinearLayoutManager(requireContext()) }

    private var scrollingFromSeekbar: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentReaderFrameBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            val chapterId = requireActivity().intent?.getLongExtra(ReaderActivity.CHAPTER_ID, -1) ?: -1
            parentViewModel.fetchManga(chapterId, LoadType.INITIAL)
        }

        initUi()
        observeUi()
    }

    private fun initUi() {
        parentViewModel.setSideKickVisibility(false)

        concatAdapter.addAdapter(readerAdapter)

        with(binding) {
            with(listImages) {
                adapter = concatAdapter
                layoutManager = linearLayoutManager
                addItemDecoration(
                    DividerItemDecoration(
                        requireContext(),
                        DividerItemDecoration.VERTICAL
                    )
                )
            }

            listImages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    parentViewModel.setCurrentPage(linearLayoutManager.findFirstVisibleItemPosition())

                    // Show the bottom bar when the scrolling is done by seekbar
                    if (!scrollingFromSeekbar) {
                        parentViewModel.setSideKickVisibility(false)
                    }

                    syncCurrentChapterShown()

                    // Resets the boolean
                    scrollingFromSeekbar = false
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    // Prefetch when scrolled to the second last item (minus ads & last page)
                    if (linearLayoutManager.findLastVisibleItemPosition() >= readerAdapter.itemCount - 2) {
                        parentViewModel.loadNextChapter()
                    }

                    val lastVisiblePos = linearLayoutManager.findFirstVisibleItemPosition()
                    val lastVisibleView = linearLayoutManager.findViewByPosition(lastVisiblePos)
                    if (lastVisiblePos == 0 && lastVisibleView?.top == 0) {
                        viewModel.setPullToRefreshEnabled(true)
                    } else {
                        viewModel.setPullToRefreshEnabled(false)
                    }
                }
            })

            smartRefreshLayout.setOnRefreshListener {
                parentViewModel.loadPreviousChapter()
                it.finishRefresh(true)
            }
        }
    }

    private fun observeUi() {
        parentViewModel.pageList().observe(viewLifecycleOwner) {
            readerAdapter.submitList(it)
            parentViewModel.setTotalPages(it.size)
        }

        lifecycleScope.launchWhenResumed {
            parentViewModel.pageNumberScrolledBySeekbar().collectLatest {
                binding.listImages.scrollToPosition(it)
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.disablePullToRefresh().collectLatest {
                binding.smartRefreshLayout.isEnabled = it
            }
        }

        lifecycleScope.launchWhenResumed {
            parentViewModel.showBottomLoadingIndicator().collectLatest {
                if (it) {
                    concatAdapter.addAdapter(loadingAdapter)
                } else {
                    concatAdapter.removeAdapter(loadingAdapter)
                }
            }
        }
    }

    private fun syncCurrentChapterShown() {
        val currentPage = readerAdapter.currentList.getOrNull(linearLayoutManager.findFirstVisibleItemPosition())
        currentPage?.let {
            if (it is ReaderPage.Value) {
                val currentChapter = it.chapter
                parentViewModel.setCurrentChapter(currentChapter)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReaderFrameFragment()
    }
}
