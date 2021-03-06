/**
 * created by Kang Gumsil
 */
package com.santaistiger.gomourdeliveryapp.ui.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.santaistiger.gomourdeliveryapp.R
import com.santaistiger.gomourdeliveryapp.data.model.Place
import com.santaistiger.gomourdeliveryapp.data.repository.Repository
import com.santaistiger.gomourdeliveryapp.data.repository.RepositoryImpl
import com.santaistiger.gomourdeliveryapp.databinding.FragmentOrderDetailBinding
import com.santaistiger.gomourdeliveryapp.ui.base.BaseActivity
import com.santaistiger.gomourdeliveryapp.ui.customview.RoundedAlertDialog
import com.santaistiger.gomourdeliveryapp.ui.viewmodel.OrderDetailViewModel
import com.santaistiger.gomourdeliveryapp.ui.viewmodel.OrderDetailViewModelFactory
import com.santaistiger.gomourdeliveryapp.utils.NotEnteredException
import com.santaistiger.gomourdeliveryapp.utils.StatusException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView


class OrderDetailFragment : Fragment() {
    companion object {
        private val DANKOOKUNIV_LOCATION =
            MapPoint.mapPointWithGeoCoord(37.323177, 127.125758)
        private const val TAG = "OrderDetailFragment"
    }

    private lateinit var binding: FragmentOrderDetailBinding
    private lateinit var viewModel: OrderDetailViewModel
    private lateinit var mapView: MapView
    private val repository: Repository = RepositoryImpl

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        init(inflater, container)
        setObserver()

        return binding.root
    }

    /** viewModel, binding, ?????? ??? ?????? ?????? */
    private fun init(inflater: LayoutInflater, container: ViewGroup?) {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_order_detail,
            container,
            false
        )

        val orderId = OrderDetailFragmentArgs.fromBundle(requireArguments()).orderId
        viewModel = ViewModelProvider(this, OrderDetailViewModelFactory(orderId))
            .get(OrderDetailViewModel::class.java)
        binding.viewModel = viewModel
        binding.cvDestination.binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // ?????? ??????
        (requireActivity() as BaseActivity).setToolbar(
            requireContext(), true, resources.getString(R.string.lock_up_order), true
        )

        // ?????? ??????
        initKakaoMap()
    }

    private fun setObserver() {
        setOrderObserver()
        setPickupCompleteBtnObserver()
        setDeliveryCompleteBtnObserver()
        setCallBtnObserver()
        setTextBtnObserver()
    }

    /** ????????? ?????? MapView??? ????????? ???, POIITem ????????? ???????????? ???????????? ????????? ???????????? ?????????????????? ?????? */
    private fun initKakaoMap() {
        mapView = MapView(context).apply {
            binding.mapView.addView(this)
            setMapCenterPointAndZoomLevel(DANKOOKUNIV_LOCATION, 2, true)
        }
    }

    /** '??????????????? ????????????' ????????? ????????? ??????????????? ?????? */
    private fun setTextBtnObserver() {
        viewModel.isTextBtnClick.observe(viewLifecycleOwner, Observer { clicked ->
            if (clicked) {
                RoundedAlertDialog()
                    .setMessage(resources.getString(R.string.check_sms))
                    .setPositiveButton(resources.getString(R.string.ok)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val customerUid = viewModel.getCustomerUid()
                            val deferredPhone = async { repository.getCustomerPhone(customerUid) }
                            startActivity(
                                Intent(Intent.ACTION_SENDTO)
                                    .setData(Uri.parse("smsto:${deferredPhone.await()}"))
                                    .putExtra(
                                        "sms_body",
                                        resources.getString(R.string.message_sms_greeting)
                                    )
                            )
                        }
                        viewModel.doneTextBtnClick()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { viewModel.doneTextBtnClick() }
                    .show(requireActivity().supportFragmentManager, "rounded alert dialog")
            }
        })
    }

    /** '??????????????? ????????????' ????????? ????????? ??????????????? ?????? */
    private fun setCallBtnObserver() {
        viewModel.isCallBtnClick.observe(viewLifecycleOwner, Observer { clicked ->
            if (clicked) {
                RoundedAlertDialog()
                    .setMessage(resources.getString(R.string.check_call))
                    .setPositiveButton(resources.getString(R.string.ok)) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val customerUid = viewModel.getCustomerUid()
                            val deferredPhone = async { repository.getCustomerPhone(customerUid) }
                            startActivity(
                                Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:${deferredPhone.await()}"))
                            )
                        }
                        viewModel.doneCallBtnClick()
                    }
                    .setNegativeButton(resources.getString(R.string.cancel)) { viewModel.doneCallBtnClick() }
                    .show(requireActivity().supportFragmentManager, "rounded alert dialog")
            }
        })
    }

    private fun showExceptionDialog(e: Exception) {
        RoundedAlertDialog()
            .setMessage(e.message!!)
            .setPositiveButton(resources.getString(R.string.ok), null)
            .show(requireActivity().supportFragmentManager, "rounded alert dialog")
    }

    /** ?????? ?????? ?????? ?????? ??? ??????????????? ?????????, ?????? ?????? ?????? ??? ?????? ?????? ?????? */
    private fun setPickupCompleteBtnObserver() {
        viewModel.isPickupCompleteBtnClick.observe(viewLifecycleOwner, Observer { clicked ->
            if (clicked) {
                try {
                    viewModel.checkCostInput() // ?????? ?????? ??????
                    RoundedAlertDialog()
                        .setMessage(resources.getString(R.string.check_pick_up_complete))
                        .setPositiveButton(resources.getString(R.string.ok)) {
                            viewModel.completePickup()
                            binding.cvDestination.binding.btnPickupComplete.apply {
                                isClickable = false
                                text = resources.getString(R.string.message_pick_up_complete)
                            }
                        }
                        .setNegativeButton(resources.getString(R.string.cancel), null)
                        .show(requireActivity().supportFragmentManager, "rounded alert dialog")

                } catch (e: NotEnteredException) {
                    showExceptionDialog(e) // ?????? ????????? ??????????????? ????????? ?????????
                } finally {
                    viewModel.donePickupCompleteBtnClick()
                }
            }
        })
    }

    /** ?????? ?????? ?????? ?????? ??? ??????????????? ?????????, ?????? ?????? ?????? ??? ?????? ?????? ?????? */
    private fun setDeliveryCompleteBtnObserver() {
        viewModel.isDeliveryCompleteBtnClick.observe(viewLifecycleOwner, Observer { clicked ->
            if (clicked) {
                try {
                    viewModel.checkStatus()
                    RoundedAlertDialog()
                        .setMessage(resources.getString(R.string.check_delivery_complete))
                        .setPositiveButton(resources.getString(R.string.ok)) {
                            viewModel.completeDelivery()
                            binding.cvDestination.binding.btnDeliveryComplete.apply {
                                isClickable = false
                                text = resources.getString(R.string.message_delivery_complete)
                            }
                        }
                        .setNegativeButton(resources.getString(R.string.cancel), null)
                        .show(requireActivity().supportFragmentManager, "rounded alert dialog")
                    (requireActivity() as BaseActivity).currentOrder = null
                } catch (e: StatusException) {
                    showExceptionDialog(e)
                } finally {
                    viewModel.doneDeliveryCompleteBtnClick()
                }
            }
        })
    }

    /** ?????? ????????? ?????? ????????? pin(POIItem)??? ?????? ??????. */
    private fun setOrderObserver() {
        viewModel.order.observe(viewLifecycleOwner, Observer { order ->
            // POI??? ????????? POI ??????
            if (mapView.poiItems.isEmpty()) {
                for (store in order?.stores!!) {
                    setPOIItem(
                        store.place,
                        MapPOIItem.MarkerType.BluePin,
                        MapPOIItem.MarkerType.RedPin
                    )
                }
                order.destination?.let {
                    setPOIItem(
                        it,
                        MapPOIItem.MarkerType.RedPin,
                        MapPOIItem.MarkerType.BluePin
                    )
                }
            }
        })
    }

    /** ????????? pin(POIItem)??? ?????? ?????? */
    private fun setPOIItem(
        place: Place,
        marker: MapPOIItem.MarkerType,
        selectedMarker: MapPOIItem.MarkerType
    ) {
        MapPOIItem().apply {
            itemName = place.placeName
            mapPoint = MapPoint.mapPointWithGeoCoord(
                place.latitude!!,
                place.longitude!!
            )
            markerType = marker
            selectedMarkerType = selectedMarker
            userObject = place
            mapView.addPOIItem(this)
        }
    }
}