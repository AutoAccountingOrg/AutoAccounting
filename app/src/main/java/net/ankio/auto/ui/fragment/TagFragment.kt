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

package net.ankio.auto.ui.fragment

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentTagBinding
import net.ankio.auto.http.api.TagAPI
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseFragment
import net.ankio.auto.ui.api.bindAs
import net.ankio.auto.ui.dialog.BottomSheetDialogBuilder
import net.ankio.auto.ui.fragment.book.TagComponent
import net.ankio.auto.ui.utils.DisplayUtils
import net.ankio.auto.ui.utils.TagUtils
import net.ankio.auto.ui.utils.ToastUtils

/**
 * 标签列表Fragment
 *
 * 该Fragment负责显示和管理标签列表，使用TagComponent显示标签
 */
class TagFragment : BaseFragment<FragmentTagBinding>(), Toolbar.OnMenuItemClickListener {

    private lateinit var tagComponent: TagComponent

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置返回按钮点击事件
        binding.topAppBar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        // 设置菜单点击监听器
        binding.topAppBar.setOnMenuItemClickListener(this)

        // 设置添加按钮点击事件
        binding.addButton.setOnClickListener {
            navigateToEditTag(0) // 传入0表示新建标签
        }

        // 初始化TagComponent
        setupTagComponent()
    }

    /**
     * 设置TagComponent
     */
    private fun setupTagComponent() {
        // 使用bindAs扩展函数创建TagComponent实例
        tagComponent = binding.tagComponent.bindAs<TagComponent>(lifecycle)
        binding.tagComponent.root.updatePadding(
            bottom = DisplayUtils.getNavigationBarHeight(requireContext())
        )
        binding.tagComponent.statusPage.swipeRefreshLayout = binding.swipeRefreshLayout
        tagComponent.setEditMode(true) // 编辑模式：显示删除按钮，长按编辑
        tagComponent.setOnTagSelectedListener { tag, type ->
            when (type) {
                "edit" -> navigateToEditTag(tag.id)
                "delete" -> showDeleteTagDialog(tag)
            }
        }
    }

    /**
     * 显示删除标签确认对话框
     * @param tag 要删除的标签
     */
    private fun showDeleteTagDialog(tag: org.ezbook.server.db.model.TagModel) {
        BottomSheetDialogBuilder.create(this)
            .setTitle(getString(R.string.delete_tag))
            .setMessage(getString(R.string.delete_tag_message, tag.name))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                deleteTag(tag)
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }

    /**
     * 删除标签
     * @param tag 要删除的标签
     */
    private fun deleteTag(tag: org.ezbook.server.db.model.TagModel) {
        lifecycleScope.launch {
            try {
                // 调用API删除标签
                TagAPI.remove(tag.id)

                // 显示成功提示
                ToastUtils.info(getString(R.string.tag_deleted_successfully))

                // 刷新标签列表
                tagComponent.refreshData()
            } catch (e: Exception) {
                // 显示错误提示
                ToastUtils.error(getString(R.string.delete_failed))
            }
        }
    }


    /**
     * 处理菜单项点击事件
     * @param item 被点击的菜单项
     * @return 是否处理了菜单点击事件
     */
    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_restore_default_tags -> {
                showRestoreDefaultTagsDialog()
                true
            }

            else -> false
        }
    }

    /**
     * 显示恢复默认标签确认对话框
     */
    private fun showRestoreDefaultTagsDialog() {
        BottomSheetDialogBuilder.create(this)
            .setTitle(getString(R.string.restore_default_tags_title))
            .setMessage(getString(R.string.restore_default_tags_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                restoreDefaultTags()
            }
            .setNegativeButton(getString(R.string.close)) { _, _ -> }
            .show()
    }

    /**
     * 恢复默认标签
     */
    private fun restoreDefaultTags() {
        lifecycleScope.launch {
            // 获取默认标签
            val tagUtils = TagUtils()
            val defaultTags = tagUtils.setDefaultTags()

            if (defaultTags.isNotEmpty()) {
                // 批量插入默认标签
                val result = TagAPI.batchInsert(defaultTags)

                if (result.get("code").asInt == 200) {

                    // 显示成功提示
                    ToastUtils.info(getString(R.string.restore_default_tags_success))

                    // 刷新标签列表
                    tagComponent.refreshData()
                } else {
                    ToastUtils.error(result.get("msg").asString)
                }

            } else {
                // 显示失败提示
                ToastUtils.error(getString(R.string.restore_default_tags_failed))
            }
        }
    }

    /**
     * 导航到标签编辑页面
     * @param tagId 标签ID，0表示新建
     */
    private fun navigateToEditTag(tagId: Long) {
        val bundle = Bundle().apply {
            putLong("tagId", tagId)
        }
        findNavController().navigate(R.id.action_tagFragment_to_tagEditFragment, bundle)
    }
}
