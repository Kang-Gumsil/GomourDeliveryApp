package com.santaistiger.gomourdeliveryapp.ui.view
/**
 * Created by Jangeunhye
 */
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.santaistiger.gomourdeliveryapp.R
import com.santaistiger.gomourdeliveryapp.data.model.AccountInfo
import com.santaistiger.gomourdeliveryapp.data.model.DeliveryMan
import com.santaistiger.gomourdeliveryapp.data.repository.Repository
import com.santaistiger.gomourdeliveryapp.data.repository.RepositoryImpl
import com.santaistiger.gomourdeliveryapp.databinding.FragmentModifyUserInfoBinding
import com.santaistiger.gomourdeliveryapp.ui.base.BaseActivity
import com.santaistiger.gomourdeliveryapp.ui.customview.RoundedAlertDialog
import com.santaistiger.gomourdeliveryapp.ui.viewmodel.ModifyUserInfoViewModel
import kotlinx.android.synthetic.main.activity_base.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class ModifyUserInfoFragment : Fragment() {

    private var auth: FirebaseAuth? = null
    private lateinit var binding: FragmentModifyUserInfoBinding
    private lateinit var viewModel: ModifyUserInfoViewModel
    val db = Firebase.firestore
    private val repository: Repository = RepositoryImpl


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // ?????? ??????
        (requireActivity() as BaseActivity).setToolbar(
            requireContext(), true, resources.getString(R.string.toolbar_title_modify_user_info), true)

        auth = Firebase.auth
        binding = DataBindingUtil.inflate<FragmentModifyUserInfoBinding>(
            inflater,
            R.layout.fragment_modify_user_info, container, false
        )
        viewModel = ViewModelProvider(this).get(ModifyUserInfoViewModel::class.java)

        CoroutineScope(Dispatchers.IO).launch {
            val deliveryMan =
                withContext(Dispatchers.Default) { repository.readDeliveryManInfo() }!!
            displayDeliveryManInfo(deliveryMan)


            binding.apply {

                //?????? ?????? ?????? ??? ???????????? ???????????? ??????
                passwordModify.addTextChangedListener(passwordChangeWatcher)
                passwordCheckModify.addTextChangedListener(passwordCheckChangeWatcher)


                // ???????????? ????????? ????????? ???????????? ?????????
                passwordModify.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        passwordModify.text.clear()
                    }
                }
                // ???????????? ????????? ????????? ???????????? ?????? ?????????
                passwordCheckModify.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        passwordCheckModify.text.clear()
                    }
                }
            }
        }


        //????????? ?????????
        binding.bankModify.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.modifyAccountLinearLayout.setBackgroundResource(R.drawable.edittext_focus)
            } else {
                binding.modifyAccountLinearLayout.setBackgroundResource(R.drawable.edittext_basic)
            }
        }

        //???????????? ?????????
        binding.accountModify.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.modifyAccountLinearLayout.setBackgroundResource(R.drawable.edittext_focus)
            } else {
                binding.modifyAccountLinearLayout.setBackgroundResource(R.drawable.edittext_basic)
            }
        }


        // ???????????? ?????? ?????? ???
        binding.modifyButton.setOnClickListener {
            val password = binding.passwordModify.text.toString()
            val passwordCheck = binding.passwordCheckModify.text.toString()
            if (password(password) && passwordEqual(passwordCheck)) {
                modifyUser()
            } else {
                Toast.makeText(context, R.string.confirm_fail, Toast.LENGTH_LONG).show()
            }
        }

        // ???????????? ?????? ???
        binding.withdrawalButton.setOnClickListener {
            showAlertDialog(resources.getString(R.string.withdrawal_dialog))
        }
        return binding.root
    }

    //???????????? ?????????????????? ??????
    private val passwordChangeWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null) {
                password(s)
            }
        }
    }

    //???????????? ?????? ?????????????????? ??????
    private val passwordCheckChangeWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s != null) {
                passwordEqual(s)
            }
        }
    }

    // ???????????? ?????? ??????
    private fun password(password: CharSequence): Boolean {
        val pwPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@\$%^&*-]).{8,16}\$"
        return if (Pattern.matches(pwPattern, password)) {
            binding.passwordModifyValid.visibility = View.GONE
            true
        } else {
            // ???????????? ?????? ?????? ?????????
            passwordValidWrong()
            binding.passwordModifyValid.setText(R.string.password_form_info)
            false
        }
    }

    private fun passwordEqual(passwordCheck: CharSequence): Boolean {
        val password = binding.passwordModify.text.toString()
        if (password == passwordCheck.toString()) {
            passwordValidCorrect()
            return true
        } else {
            // ???????????? ?????? ?????? ???
            passwordValidWrong()
            binding.passwordModifyValid.setText(R.string.password_different_info)
            return false
        }
    }


    // ???????????? ??????
    private fun modifyUser() {
        val currentUser = auth?.currentUser

        val password = binding.passwordModify.text.toString()
        val phone = binding.phoneModify.text.toString()
        val account = binding.accountModify.text.toString()
        val bank = binding.bankModify.text.toString()
        val accountInfo = AccountInfo(bank, account)

        //atuhentication ???????????? ????????????
        repository.updateAuthPassword(password)

        //?????????????????? ???????????? ????????????
        repository.updateFireStorePassword(currentUser!!.uid, password)

        //?????????????????? ???????????? ????????????
        repository.updatePhone(currentUser.uid, phone)

        //?????????????????? ????????? ???????????? ????????????
        repository.updateAccountInfo(currentUser.uid, accountInfo)

        (activity as BaseActivity).setNavigationDrawerHeader()  // ??????????????? ????????? ?????? ??????

        /*if (currentUser != null) {
            currentUser.updatePassword(password)
                .addOnSuccessListener { Toast.makeText(context,"???????????? ?????? ??????", Toast.LENGTH_LONG).show() }
                .addOnFailureListener {  Toast.makeText(context,"???????????? ?????? ??????", Toast.LENGTH_LONG).show()}

            db.collection("deliveryMan")
                .document(currentUser.uid!!)
                .update("password",password)
                .addOnSuccessListener { documentReference ->

                }
                .addOnFailureListener { e ->
                    Log.w("TEST", "Error adding document", e)
                }

            db.collection("deliveryMan")
                .document(currentUser.uid!!)
                .update("phone",phone)
                .addOnSuccessListener { documentReference ->
                    //findNavController().navigate(R.id.action_modifyUserInfoFragment_to_orderListFragment)
                }
                .addOnFailureListener { e ->
                    Log.w("TEST", "Error adding document", e)
                }

        }
        else{
            findNavController().navigate(R.id.action_modifyUserInfoFragment_to_loginFragment)
        }

        (activity as BaseActivity).setNavigationDrawerHeader()  // ??????????????? ????????? ?????? ??????
        */
    }

    //?????? ???
    private fun showAlertDialog(msg: String) {
        RoundedAlertDialog()
            .setMessage(msg)
            .setPositiveButton(resources.getString(R.string.ok)) { withdrawal() }
            .setNegativeButton(resources.getString(R.string.cancel), null)
            .show((requireActivity() as BaseActivity).supportFragmentManager, "rounded alert dialog")
    }

    // ??????
    @SuppressLint("RestrictedApi")
    private fun withdrawal() {
        // Athuentication ??????
        repository.deleteAuthDeliveryMan()

        // ????????? ????????? ??????
        repository.deleteFireStoreDeliveryMan(repository.getUid())

        //??????????????? ??????
        val auto = this.requireActivity()
            .getSharedPreferences("auto", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = auto.edit()
        editor.clear()
        editor.commit()

        // ????????? ??????
        for (i in 1..findNavController().backStack.count()) {
            findNavController().popBackStack()
        }

        // ????????? ???????????? ??????
        findNavController().navigate(R.id.loginFragment)

        /*var currentUser = Firebase.auth.currentUser
        currentUser.delete()
            .addOnFailureListener {
                Toast.makeText(context, "?????? ??????", Toast.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
            }

        db.collection("deliveryMan").document(currentUser.uid)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "?????? ??????", Toast.LENGTH_LONG).show()
                val auto = this.requireActivity()
                    .getSharedPreferences("auto", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = auto.edit()
                editor.clear()
                editor.commit()

                // ????????? ??????
                for (i in 1..findNavController().backStack.count()) {
                    findNavController().popBackStack()
                }

                // ????????? ???????????? ??????
                findNavController().navigate(R.id.loginFragment)
            }
            .addOnFailureListener { Toast.makeText(context, "?????? ??????", Toast.LENGTH_LONG).show() }
    */
    }

    // ???????????? ????????? ??? ?????? ??????
    private fun passwordValidWrong() {
        binding.passwordModifyValid.setTextColor(Color.parseColor("#FFF44336"))
        binding.passwordModifyValid.visibility = View.VISIBLE
    }

    // ???????????? ??????????????? ??? ??????
    private fun passwordValidCorrect() {
        binding.passwordModifyValid.setTextColor(Color.parseColor("#000000"))
        binding.passwordModifyValid.visibility = View.VISIBLE
        binding.passwordModifyValid.setText(R.string.password_available_info)
    }

    private fun displayDeliveryManInfo(deliveryMan: DeliveryMan) {
        binding.nameModify.text = deliveryMan.name
        binding.emailModify.text = deliveryMan.email
        binding.passwordModify.setText(deliveryMan.password)
        binding.passwordCheckModify.setText(deliveryMan.password)
        binding.phoneModify.setText(deliveryMan.phone)
        binding.accountModify.setText(deliveryMan.accountInfo?.account)
        binding.bankModify.setText(deliveryMan.accountInfo?.bank)
    }
}