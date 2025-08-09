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
import java.security.MessageDigest
import androidx.core.graphics.scale
import net.ankio.auto.http.api.BookNameAPI
import net.ankio.auto.ui.utils.load

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
        lifecycleScope.launch {
            try {
                val books = BookNameAPI.list()
                val book = books.find { it.id == bookId }

                book?.let {
                    // 手动复制BookNameModel属性
                    currentBookModel = BookNameModel().apply {
                        id = it.id
                        name = it.name
                        icon = it.icon
                        remoteId = it.remoteId
                    }
                    binding.bookNameEditText.setText(it.name)
                    updateIconPreview()
                } ?: run {
                    ToastUtils.error(getString(R.string.book_not_found))
                    findNavController().popBackStack()
                }
            } catch (e: Exception) {
                Logger.e("Failed to load book data", e)
                ToastUtils.error(getString(R.string.book_load_failed))
                findNavController().popBackStack()
            }
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
     * 更新图标状态文字
     */
    private fun updateIconStatusText() = with(binding) {
        if (currentBookModel.icon.isNotEmpty()) {
            val bookName = bookNameEditText.text.toString().trim()
            iconStatusText.text = bookName.ifEmpty { getString(R.string.icon_selected) }
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
            Logger.e("Failed to open image picker", e)
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
        lifecycleScope.launch {
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
                Logger.e("Failed to process selected image", e)
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

        lifecycleScope.launch {
            try {
                // 显示保存状态
                saveButton.isEnabled = false
                saveButton.text = getString(R.string.saving)

                // 获取当前所有账本
                val existingBooks = BookNameAPI.list().toMutableList()

                // 检查是否存在同名账本
                val nameExists = existingBooks.any {
                    it.name == bookName && it.id != currentBookModel.id
                }

                if (nameExists) {
                    ToastUtils.error(getString(R.string.book_name_exists))
                    return@launch
                }

                if (isEditMode) {
                    // 更新现有账本
                    val index = existingBooks.indexOfFirst { it.id == currentBookModel.id }
                    if (index >= 0) {
                        existingBooks[index] = currentBookModel
                    }
                } else {
                    // 添加新账本（生成新ID）
                    val maxId = existingBooks.maxOfOrNull { it.id } ?: 0
                    currentBookModel.id = maxId + 1
                    currentBookModel.remoteId = currentBookModel.id.toString()
                    existingBooks.add(currentBookModel)
                }

                // 计算MD5校验值
                val md5 = calculateMD5(existingBooks)

                // 保存到服务器
                BookNameAPI.put(ArrayList(existingBooks), md5)

                ToastUtils.info(
                    getString(if (isEditMode) R.string.book_updated_success else R.string.book_created_success)
                )

                findNavController().popBackStack()

            } catch (e: Exception) {
                Logger.e("Failed to save book", e)
                ToastUtils.error(
                    getString(if (isEditMode) R.string.book_update_failed else R.string.book_create_failed)
                )
            } finally {
                // 恢复按钮状态
                saveButton.isEnabled = true
                saveButton.text =
                    getString(if (isEditMode) R.string.btn_save else R.string.btn_create)
            }
        }
    }

    /**
     * 计算账本列表的MD5校验值
     */
    private fun calculateMD5(books: List<BookNameModel>): String {
        return try {
            val content = books.sortedBy { it.id }.joinToString { "${it.id}-${it.name}-${it.icon}" }
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(content.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Logger.e("Failed to calculate MD5", e)
            System.currentTimeMillis().toString()
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