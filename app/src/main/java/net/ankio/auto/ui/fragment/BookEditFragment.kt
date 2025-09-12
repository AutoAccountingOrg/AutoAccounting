/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentBookEditBinding
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.utils.ToastUtils
import org.ezbook.server.db.model.BookNameModel
import java.io.ByteArrayOutputStream
import androidx.core.graphics.scale
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.ui.utils.load
import org.ezbook.server.tools.runCatchingExceptCancel
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 账本编辑Fragment - 用于创建新账本或编辑现有账本
 */
class BookEditFragment : BaseFragment<FragmentBookEditBinding>() {

    private var currentBookModel: BookNameModel = BookNameModel()
    private var isEditMode = false
    private var bookId: Long = 0L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取参数并判断编辑模式
        arguments?.let { bundle ->
            bookId = bundle.getLong("bookId", 0L)
            isEditMode = bookId > 0
        }

        setupUI()
        setupEvents()

        // 加载现有数据（编辑模式）
        if (isEditMode) {
            loadBookData()
        }
    }

    /**
     * 设置UI界面
     */
    private fun setupUI() = with(binding) {
        // 设置标题和按钮文本
        toolbar.title = getString(if (isEditMode) R.string.edit_book else R.string.add_book)
        saveButton.text = getString(if (isEditMode) R.string.btn_save else R.string.btn_create)

        // 设置默认图标
        setDefaultIcon()

        // 返回按钮
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * 设置事件监听器
     */
    private fun setupEvents() = with(binding) {
        // 保存按钮
        saveButton.setOnClickListener { saveBook() }

        // 图标选择
        iconSelectCard.setOnClickListener { showIconSelector() }
    }

    /**
     * 加载现有账本数据
     */
    private fun loadBookData() {
        launch {
            val books = runCatchingExceptCancel { BookNameAPI.list() }.getOrDefault(emptyList())
            val book = books.find { it.id == bookId }
            if (book == null) {
                ToastUtils.error(getString(R.string.book_not_found))
                findNavController().popBackStack()
                return@launch
            }
            currentBookModel = BookNameModel().apply {
                id = book.id
                name = book.name
                icon = book.icon
                remoteId = book.remoteId
            }
            binding.bookNameEditText.setText(book.name)
            updateIconPreview()
        }
    }

    /**
     * 更新图标预览
     */
    private fun updateIconPreview() = with(binding) {
        if (currentBookModel.icon.isNotEmpty()) {
            bookIconPreview.load(currentBookModel.icon, R.drawable.ic_book)
        } else {
            setDefaultIcon()
        }
    }


    /**
     * 设置默认图标
     */
    private fun setDefaultIcon() = with(binding) {
        bookIconPreview.setImageResource(R.drawable.default_book)
        iconStatusText.text = getString(R.string.tap_to_select_icon)
    }

    /**
     * 显示图标选择器
     */
    private fun showIconSelector() {
        try {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
            imagePickerLauncher.launch(intent)
        } catch (e: Exception) {
            logger.error(e) { "Failed to open image picker" }
            ToastUtils.error(getString(R.string.icon_select_failed))
        }
    }

    /**
     * 图片选择结果处理
     */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleSelectedImage(uri) }
        }
    }

    /**
     * 处理选择的图片
     */
    private fun handleSelectedImage(imageUri: Uri) {
        launch {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                bitmap?.let {
                    val compressedBitmap = compressImage(it)
                    val base64String = bitmapToBase64(compressedBitmap)
                    currentBookModel.icon = "data:image/jpeg;base64,$base64String"
                    updateIconPreview()
                    ToastUtils.info(getString(R.string.icon_selected))
                } ?: run {
                    ToastUtils.error(getString(R.string.image_load_failed))
                }

            } catch (e: Exception) {
                logger.error(e) { "Failed to process selected image" }
                ToastUtils.error(getString(R.string.image_process_failed))
            }
        }
    }

    /**
     * 压缩图片到合适尺寸
     */
    private fun compressImage(bitmap: Bitmap): Bitmap {
        val maxSize = 512
        val ratio =
            (maxSize.toFloat() / bitmap.width).coerceAtMost(maxSize.toFloat() / bitmap.height)

        return if (ratio < 1) {
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            bitmap.scale(newWidth, newHeight)
        } else {
            bitmap
        }
    }

    /**
     * 将Bitmap转换为base64字符串
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * 保存账本
     */
    private fun saveBook() = with(binding) {
        val bookName = bookNameEditText.text.toString().trim()

        // 输入验证
        if (!validateInput(bookName)) return@with

        currentBookModel.name = bookName

        launch {
            // 显示保存状态
            saveButton.isEnabled = false
            saveButton.text = getString(R.string.saving)

            val result = runCatchingExceptCancel {
                if (isEditMode) BookNameAPI.update(currentBookModel) else BookNameAPI.add(
                    currentBookModel
                )
            }

            if (result.isSuccess) {
                ToastUtils.info(
                    getString(if (isEditMode) R.string.book_updated_success else R.string.book_created_success)
                )
                findNavController().popBackStack()
            } else {
                logger.error { "Failed to save book" }
                ToastUtils.error(
                    getString(if (isEditMode) R.string.book_update_failed else R.string.book_create_failed)
                )
            }

            // 恢复按钮状态
            saveButton.isEnabled = true
            saveButton.text = getString(if (isEditMode) R.string.btn_save else R.string.btn_create)
        }
    }

    /**
     * 验证输入
     */
    private fun validateInput(bookName: String): Boolean = with(binding) {
        when {
            TextUtils.isEmpty(bookName) -> {
                bookNameLayout.error = getString(R.string.book_name_required)
                bookNameEditText.requestFocus()
                return false
            }

            bookName.length > 20 -> {
                bookNameLayout.error = getString(R.string.book_name_too_long)
                bookNameEditText.requestFocus()
                return false
            }

            else -> {
                bookNameLayout.error = null
                return true
            }
        }
    }
} 