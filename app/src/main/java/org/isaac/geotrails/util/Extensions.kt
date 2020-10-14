package org.isaac.geotrails.util

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<MutableList<T>>.add(item: T) {
    val list = value ?: mutableListOf()
    list.add(item)
    postValue(list)
}

fun <T> MutableLiveData<MutableList<T>>.add(index: Int, item: T) {
    val list = this.value ?: mutableListOf()
    list.add(index, item)
    postValue(list)
}

fun <T> MutableLiveData<MutableList<T>>.clear() {
    value?.clear()
    postValue(value ?: mutableListOf())
}
