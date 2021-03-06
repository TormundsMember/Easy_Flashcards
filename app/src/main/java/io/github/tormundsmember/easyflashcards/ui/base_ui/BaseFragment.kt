package io.github.tormundsmember.easyflashcards.ui.base_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.tormundsmember.easyflashcards.R
import io.github.tormundsmember.easyflashcards.ui.MainActivity

abstract class BaseFragment : Fragment() {

    abstract val layoutId: Int

    fun <Key : BaseKey> getKey(): Key = arguments?.getParcelable(BaseKey.KEY)
        ?: throw IllegalStateException("App attempted to access key in fragment ${javaClass.simpleName}")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = titleText
    }


    fun <T> LiveData<T>.observe(action: (T?) -> Unit) {
        observe(viewLifecycleOwner, Observer { action(it) })
    }


    inline fun <reified VM : ViewModel> Fragment.getViewModel(): VM {
        return ViewModelProvider(this)[VM::class.java]
    }

    inline fun <reified VM : ViewModel> Fragment.getSharedViewModel(): VM {
        return activity?.let { ViewModelProvider(it)[VM::class.java] }
            ?: throw IllegalStateException("attempted to get viewModel for fragment ${this.javaClass.simpleName} but activity was null")
    }


    fun goTo(key: BaseKey) {
        (activity as MainActivity?)?.backstack?.goTo(key)
    }

    fun goBack() {
        activity?.onBackPressed()
    }

    open val titleText: String
        get() = getString(R.string.app_name)

    open fun canGoBack(): Boolean {
        return true
    }

    fun handleBackPress() {
        if (canGoBack()) {
            (activity as? MainActivity)?.goBack()
        }
    }
}