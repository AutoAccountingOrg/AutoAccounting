/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.viewModes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ankio.auto.models.BaseModel
import net.ankio.auto.storage.Logger

abstract class BaseViewModel<T : BaseModel> : ViewModel() {
    private val _dataList = MutableLiveData<MutableList<T>>(mutableListOf())
    val dataList: LiveData<MutableList<T>> get() = _dataList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private var currentPage = 1
    private val pageSize = 20

    private val _params = HashMap<String, Any>()

    init {
        loadMoreData()
    }

    fun setParams(params: HashMap<String, Any>) {
        _dataList.value?.clear()
        currentPage = 1
        _params.clear()
        _params.putAll(params)
        loadMoreData()
    }

     fun loadMoreData() {
        if (_isLoading.value == true) return

        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val newData = fetchData(currentPage, pageSize, _params)
                val updatedList = _dataList.value ?: mutableListOf()
                updatedList.addAll(newData)
                _dataList.postValue(updatedList)
                _isLoading.postValue(false)
                currentPage++
            }.onFailure {
                _isLoading.postValue(false)
                Logger.e("BaseViewModel", it)
            }
        }
    }

    abstract suspend fun fetchData(page: Int, pageSize: Int, params: HashMap<String, Any>): MutableList<T>
}