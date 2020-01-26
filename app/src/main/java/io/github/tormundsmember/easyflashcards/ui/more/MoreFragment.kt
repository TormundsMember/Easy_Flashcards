package io.github.tormundsmember.easyflashcards.ui.more

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Switch
import androidx.appcompat.widget.AppCompatTextView
import io.github.tormundsmember.easyflashcards.R
import io.github.tormundsmember.easyflashcards.ui.Dependencies
import io.github.tormundsmember.easyflashcards.ui.MainActivity
import io.github.tormundsmember.easyflashcards.ui.base_ui.BaseFragment
import io.github.tormundsmember.easyflashcards.ui.licenses.LicensesKey
import io.github.tormundsmember.easyflashcards.ui.set.model.Card
import io.github.tormundsmember.easyflashcards.ui.set_overview.model.Set
import io.github.tormundsmember.easyflashcards.ui.util.hasPermission
import io.github.tormundsmember.easyflashcards.ui.util.openUrlInCustomTabs
import io.github.tormundsmember.easyflashcards.ui.util.prepareLinkText
import io.github.tormundsmember.easyflashcards.ui.util.showGeneralErrorMessage
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class MoreFragment : BaseFragment() {

    companion object {
        const val RQ_STORAGE = 1
        const val PICKFILE_RESULT_CODE = 2
    }

    override val layoutId: Int = R.layout.screen_more

    private lateinit var switchDarkMode: Switch
    private lateinit var switchSpatialRepetition: Switch
    private lateinit var txtExportToCsv: AppCompatTextView
    private lateinit var txtLicenses: AppCompatTextView
    private lateinit var txtSourceCode: AppCompatTextView
    private lateinit var txtIssueTracker: AppCompatTextView
    private lateinit var hintSpatialRepetition: AppCompatTextView
    private lateinit var switchCrashUsageData: Switch


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        switchSpatialRepetition = view.findViewById(R.id.switchSpatialRepetition)
        txtExportToCsv = view.findViewById(R.id.txtExportToCsv)
        txtLicenses = view.findViewById(R.id.txtLicenses)
        txtSourceCode = view.findViewById(R.id.txtSourceCode)
        txtIssueTracker = view.findViewById(R.id.txtIssueTracker)
        hintSpatialRepetition = view.findViewById(R.id.hintSpatialRepetition)
        switchCrashUsageData = view.findViewById(R.id.switchCrashUsageData)

        switchCrashUsageData.text = getString(R.string.enableCrashReporting).prepareLinkText(view.context)
        switchCrashUsageData.isChecked = Dependencies.userData.allowCrashReporting
        switchCrashUsageData.setOnCheckedChangeListener { _, isChecked ->
            Dependencies.userData.allowCrashReporting = isChecked
        }

        switchDarkMode.isChecked = Dependencies.userData.useDarkMode
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            Dependencies.userData.useDarkMode = isChecked
            (activity as MainActivity?)?.setDarkMode()
        }

        switchSpatialRepetition.isChecked = Dependencies.userData.useSpacedRepetition
        switchSpatialRepetition.setOnCheckedChangeListener { _, isChecked ->
            Dependencies.userData.useSpacedRepetition = isChecked
        }

        hintSpatialRepetition.text = getString(R.string.explanationSpacedRepetition).prepareLinkText(view.context)
        hintSpatialRepetition.setOnClickListener {
            openUrlInCustomTabs(it.context, Uri.parse("https://en.wikipedia.org/wiki/Spaced_repetition"))
        }

        txtExportToCsv.setOnClickListener {
            exportToCsv()
        }
        txtLicenses.setOnClickListener {
            goTo(LicensesKey())
        }
        txtSourceCode.setOnClickListener {
            openUrlInCustomTabs(it.context, Uri.parse(getString(R.string.repository)))
        }
        txtIssueTracker.setOnClickListener {
            openUrlInCustomTabs(it.context, Uri.parse(getString(R.string.issueTracker)))
        }
    }

    private fun continueWithExport() {
        try {
            val chooseFile = Intent(Intent.ACTION_CREATE_DOCUMENT)
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
            chooseFile.type = "text/csv"

            chooseFile.putExtra(
                Intent.EXTRA_TITLE,
                "export_flashcards_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.csv"
            )
            startActivityForResult(
                Intent.createChooser(chooseFile, "Choose a file"),
                PICKFILE_RESULT_CODE
            )
        } catch (e: Exception) {
            context?.showGeneralErrorMessage()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val data1 = data?.data
        if (requestCode == PICKFILE_RESULT_CODE && data != null && data1 != null) {
            activity?.applicationContext?.contentResolver?.openFileDescriptor(data1, "w")?.use {

                val text =
                    (mutableListOf("setId;setName;cardId;frontText;backText;currentInterval;nextRecheck;checkCount;positiveCheckCount") +
                            Dependencies.database.getCardsWithSetNames().map { card ->
                                card.getCsv()
                            }).joinToString(separator = "\r\n")

                FileOutputStream(it.fileDescriptor).use { stream ->
                    stream.write(text.toByteArray())
                }

            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RQ_STORAGE) {
            if (permissions[0] == android.Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    continueWithExport()
                }
            }
        }
    }

    private fun exportToCsv() {

        activity?.let {
            if (it.hasPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                continueWithExport()
            } else {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), RQ_STORAGE)
            }
        }

    }

    private fun importFromCsv(csv: List<String>) {


        val setIdKey = "setId"
        val setNameKey = "setName"
        val cardIdKey = "cardId"
        val frontTextKey = "frontText"
        val backTextKey = "backText"
        val currentIntervalKey = "currentInterval"
        val nextRecheckKey = "nextRecheck"
        val checkCountKey = "checkCount"
        val positiveCheckCountKey = "positiveCheckCount"
        val requiredColumns = listOf(
            setIdKey,
            setNameKey,
            cardIdKey,
            frontTextKey,
            backTextKey,
            currentIntervalKey,
            nextRecheckKey,
            checkCountKey,
            positiveCheckCountKey
        )


        val columnNames = csv.first().split(";").withIndex().associate {
            Pair(it.value, it.index)
        }

        val setIds: MutableList<Int> = mutableListOf()

        val setId = columnNames[setIdKey]
        val setName = columnNames[setNameKey]
        val cardId = columnNames[cardIdKey]
        val frontText = columnNames[frontTextKey]
        val backText = columnNames[backTextKey]
        val currentInterval = columnNames[currentIntervalKey]
        val nextRecheck = columnNames[nextRecheckKey]
        val checkCount = columnNames[checkCountKey]
        val positiveCheckCount = columnNames[positiveCheckCountKey]

        try {
            if (columnNames.keys.containsAll(requiredColumns)) {
                csv.drop(1).map { it.split(";") }.forEach { row ->
                    if (setIds.none { it == row[setId!!].toInt() }) {
                        val set = Set(row[setId!!].toInt(), row[setName!!])
                        setIds += set.id
                        Dependencies.database.addOrUpdateSet(set)
                    }
                    val card = Card(
                        id = row[cardId!!].toInt(),
                        frontText = row[frontText!!],
                        backText = row[backText!!],
                        currentInterval = row[currentInterval!!].toInt(),
                        nextRecheck = row[nextRecheck!!].toLong(),
                        setId = row[setId!!].toInt(),
                        checkCount = row[checkCount!!].toInt(),
                        positiveCheckCount = row[positiveCheckCount!!].toInt()
                    )
                    Dependencies.database.addOrUpdateCard(card)
                }

            }
        } catch (e: java.lang.Exception) {

        }
    }
}