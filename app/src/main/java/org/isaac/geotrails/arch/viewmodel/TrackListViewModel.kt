package org.isaac.geotrails.arch.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.isaac.geotrails.di.qualifier.DefaultDispatcher
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.entity.repository.TrackRepository

class TrackListViewModel @ViewModelInject constructor(
    val trackRepository: TrackRepository,
    @DefaultDispatcher
    val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val selectedTrackList: MutableList<Track> = mutableListOf()

    private val _trackListFromDb: MutableLiveData<List<Track>> =
        trackRepository.loadAllTracks().asLiveData() as MutableLiveData

    val trackList: LiveData<List<Track>> =
        Transformations.map(_trackListFromDb) { it as List<Track> }

//    init {
//        viewModelScope.launch(defaultDispatcher) {
//            trackRepository.loadAllTracks()
//        }
//    }

    fun onTrackClick(track: Track, isSelected: Boolean) {
        if (isSelected) {
            selectedTrackList.add(track)
        } else {
            selectedTrackList.remove(track)
        }
    }


}