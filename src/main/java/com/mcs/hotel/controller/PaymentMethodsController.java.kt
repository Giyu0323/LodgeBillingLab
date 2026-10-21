package com.mcs.hotel.controller

import com.mcs.hotel.model.PaymentMethod
import com.mcs.hotel.service.PaymentService
import com.mcs.hotel.view.Dialogs
import com.mcs.hotel.view.MethodDialog
import com.mcs.hotel.view.PaymentMethodsPanel
import java.util.*
import java.util.function.Consumer

/**
 * Connects PaymentMethodsPanel to PaymentService.
 * Manage asks "switch it off (or on)?", the service applies the change and
 * enforces the "at least one stays active" rule, and the cards are redrawn.
 */
class PaymentMethodsController(private val view: PaymentMethodsPanel, private val payments: PaymentService) {
    init {
        view.onManage(Consumer { method: PaymentMethod? -> this.manage(method!!) })
        refresh()
    }

    /** Reads every method's current state from the service and redraws the cards.  */
    private fun refresh() {
        val items = Arrays.stream<PaymentMethod?>(PaymentMethod.entries.toTypedArray())
            .map<PaymentMethodsPanel.Item?> { method: PaymentMethod? ->
                PaymentMethodsPanel.Item(
                    method,
                    payments.isEnabled(method)
                )
            }
            .toList()
        view.setMethods(items)
    }

    private fun manage(method: PaymentMethod) {
        val active = payments.isEnabled(method)
        if (!MethodDialog.confirmToggle(method.toString(), active)) {
            return  // the admin clicked Cancel
        }
        try {
            payments.setEnabled(method, !active)
        } catch (ex: IllegalArgumentException) {
            Dialogs.error(ex.message) // "At least one payment method must stay active."
        }
        refresh()
    }
}