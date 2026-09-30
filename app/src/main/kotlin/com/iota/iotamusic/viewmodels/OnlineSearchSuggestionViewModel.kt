/*
 * IotaMusic Project (2026)
 * Shnwaz (github.com/shnwazdeveloper)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.iota.iotamusic.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iota.iotamusic.innertube.YouTube
import com.iota.iotamusic.innertube.models.SearchSuggestions
import com.iota.iotamusic.innertube.models.YTItem
import com.iota.iotamusic.innertube.models.filterExplicit
import com.iota.iotamusic.innertube.models.filterVideo
import com.iota.iotamusic.constants.HideExplicitKey
import com.iota.iotamusic.constants.HideVideoKey
import com.iota.iotamusic.db.MusicDatabase
import com.iota.iotamusic.db.entities.SearchHistory
import com.iota.iotamusic.utils.dataStore
import com.iota.iotamusic.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    init {
        // History flow: updates immediately from DB
        viewModelScope.launch {
            query
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(history = history)
                        }
                    } else {
                        database
                            .searchHistory(query)
                            .map { it.take(3) }
                            .map { history ->
                                SearchSuggestionViewState(history = history)
                            }
                    }
                }.collect {
                    _viewState.value = it
                }
        }

        // Suggestions flow: fetches from network independently with fast debounce
        viewModelScope.launch {
            query
                .debounce(150L)
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        flowOf<SearchSuggestions?>(null)
                    } else {
                        flow<SearchSuggestions?> {
                            emit(YouTube.searchSuggestions(query).getOrNull())
                        }
                    }
                }.collect { result ->
                    val history = _viewState.value.history
                    _viewState.value = _viewState.value.copy(
                        suggestions = result
                            ?.queries
                            ?.filter { s -> history.none { it.query == s } }
                            .orEmpty(),
                        items = result
                            ?.recommendedItems
                            ?.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                            ?.filterVideo(context.dataStore.get(HideVideoKey, false))
                            .orEmpty(),
                    )
                }
        }
    }
}

data class SearchSuggestionViewState(
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
)