package com.tvfut.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.tvfut.app.databinding.ActivityMainBinding
import com.tvfut.app.databinding.DialogLinkBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: LinkAdapter
    private lateinit var repository: LinkRepository
    private val items = mutableListOf<LinkItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = LinkRepository(this)
        items.addAll(repository.load())

        adapter = LinkAdapter(items, onOpen = ::openItem, onMenu = ::showItemMenu)
        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fab.setOnClickListener { showLinkDialog(null) }

        binding.toolbar.inflateMenu(R.menu.main_menu)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_settings) {
                showSettings()
                true
            } else {
                false
            }
        }

        updateEmptyState()
    }

    private fun updateEmptyState() {
        val empty = items.isEmpty()
        binding.empty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recycler.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun openItem(item: LinkItem) {
        if (StreamUtils.looksLikeStream(item.url)) {
            openDirect(item)
        } else {
            startActivity(
                Intent(this, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.EXTRA_URL, item.url)
                    .putExtra(PlayerActivity.EXTRA_TITLE, item.title)
            )
        }
    }

    private fun openDirect(item: LinkItem) {
        startActivity(
            Intent(this, DirectPlayerActivity::class.java)
                .putExtra(DirectPlayerActivity.EXTRA_URL, item.url)
                .putExtra(DirectPlayerActivity.EXTRA_TITLE, item.title)
        )
    }

    private fun showItemMenu(item: LinkItem, anchor: View) {
        val popup = androidx.appcompat.widget.PopupMenu(this, anchor)
        popup.menu.add(0, MENU_PLAY, 0, R.string.play_direct)
        popup.menu.add(0, MENU_EDIT, 1, R.string.edit_link)
        popup.menu.add(0, MENU_COPY, 2, R.string.copy)
        popup.menu.add(0, MENU_DELETE, 3, R.string.delete)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                MENU_PLAY -> openDirect(item)
                MENU_EDIT -> showLinkDialog(item)
                MENU_COPY -> copyLink(item.url)
                MENU_DELETE -> confirmDelete(item)
            }
            true
        }
        popup.show()
    }

    private fun showLinkDialog(existing: LinkItem?) {
        val dialogBinding = DialogLinkBinding.inflate(layoutInflater)
        dialogBinding.etTitle.setText(existing?.title.orEmpty())
        dialogBinding.etUrl.setText(existing?.url.orEmpty())

        if (existing == null) {
            prefillFromClipboard(dialogBinding)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existing == null) R.string.add_link else R.string.edit_link)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title = dialogBinding.etTitle.text?.toString()?.trim().orEmpty()
                var url = dialogBinding.etUrl.text?.toString()?.trim().orEmpty()

                dialogBinding.tilTitle.error = null
                dialogBinding.tilUrl.error = null

                if (title.isEmpty()) {
                    dialogBinding.tilTitle.error = getString(R.string.required_name)
                    return@setOnClickListener
                }
                if (url.isEmpty()) {
                    dialogBinding.tilUrl.error = getString(R.string.invalid_url)
                    return@setOnClickListener
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val parsed = Uri.parse(url)
                if (!URLUtil.isNetworkUrl(url) || parsed.host.isNullOrBlank()) {
                    dialogBinding.tilUrl.error = getString(R.string.invalid_url)
                    return@setOnClickListener
                }

                if (existing == null) {
                    items.add(0, LinkItem(UUID.randomUUID().toString(), title, url))
                } else {
                    val index = items.indexOfFirst { it.id == existing.id }
                    if (index >= 0) {
                        items[index] = existing.copy(title = title, url = url)
                    }
                }
                persistAndRefresh()
                dialog.dismiss()
            }
        }
        dialog.show()

        if (existing == null && dialogBinding.etUrl.text.isNullOrBlank()) {
            dialogBinding.etTitle.requestFocus()
        }
    }

    private fun prefillFromClipboard(dialogBinding: DialogLinkBinding) {
        try {
            val clipboard = getSystemService(ClipboardManager::class.java)
            val clip = clipboard?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).coerceToText(this).toString().trim()
                if (text.startsWith("http://") || text.startsWith("https://")) {
                    dialogBinding.etUrl.setText(text)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun confirmDelete(item: LinkItem) {
        MaterialAlertDialogBuilder(this)
            .setMessage(getString(R.string.delete_confirm, item.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                items.removeAll { it.id == item.id }
                persistAndRefresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun copyLink(url: String) {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("enlace", url))
        Toast.makeText(this, R.string.copy_title, Toast.LENGTH_SHORT).show()
    }

    private fun persistAndRefresh() {
        repository.save(items)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun showSettings() {
        val adBlocker = AdBlocker(this)
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        val switch = MaterialSwitch(this).apply {
            text = getString(R.string.block_ads)
            isChecked = adBlocker.enabled
            setOnCheckedChangeListener { _, checked -> adBlocker.enabled = checked }
        }
        val description = TextView(this).apply {
            text = getString(R.string.block_ads_desc)
            setTextColor(getColor(R.color.muted))
            textSize = 13f
        }
        val strictSwitch = MaterialSwitch(this).apply {
            text = getString(R.string.strict_mode)
            isChecked = adBlocker.strictMode
            setPadding(0, padding / 2, 0, 0)
            setOnCheckedChangeListener { _, checked -> adBlocker.strictMode = checked }
        }
        val strictDescription = TextView(this).apply {
            text = getString(R.string.strict_mode_desc)
            setTextColor(getColor(R.color.muted))
            textSize = 13f
        }
        val customButton = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.custom_list)
            setOnClickListener { showCustomHostsDialog(adBlocker) }
        }
        val tip = TextView(this).apply {
            text = getString(R.string.dns_tip)
            setTextColor(getColor(R.color.muted))
            textSize = 13f
            setPadding(0, padding, 0, 0)
        }
        val version = TextView(this).apply {
            text = getString(R.string.version)
            setTextColor(getColor(R.color.muted))
            textSize = 12f
            setPadding(0, padding / 2, 0, 0)
        }

        container.addView(switch)
        container.addView(description)
        container.addView(strictSwitch)
        container.addView(strictDescription)
        container.addView(
            customButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = padding / 2 }
        )
        container.addView(tip)
        container.addView(version)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_title)
            .setView(container)
            .setPositiveButton(R.string.close, null)
            .setNeutralButton(R.string.clear_cache) { _, _ -> clearBrowserData() }
            .show()
    }

    private fun showCustomHostsDialog(adBlocker: AdBlocker) {
        val padding = (20 * resources.displayMetrics.density).toInt()
        val input = com.google.android.material.textfield.TextInputEditText(this).apply {
            setText(adBlocker.customHostsText())
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 6
            maxLines = 12
            gravity = android.view.Gravity.TOP or android.view.Gravity.START
            hint = getString(R.string.custom_list_hint)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }
        val description = TextView(this).apply {
            text = getString(R.string.custom_list_desc)
            setTextColor(getColor(R.color.muted))
            textSize = 13f
        }
        container.addView(description)
        container.addView(input)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.custom_list)
            .setView(container)
            .setPositiveButton(R.string.save_list) { _, _ ->
                adBlocker.saveCustomHosts(input.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun clearBrowserData() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        val webView = WebView(this)
        webView.clearCache(true)
        webView.clearHistory()
        webView.destroy()
        Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MENU_PLAY = 1
        private const val MENU_EDIT = 2
        private const val MENU_COPY = 3
        private const val MENU_DELETE = 4
    }
}
