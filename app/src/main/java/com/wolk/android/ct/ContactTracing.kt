package com.wolk.android.ct


import android.app.PendingIntent
import android.content.Intent

interface ContactTracing {

    /**
     * Flags daily tracing keys as to be stored on the server.
     *
     * This should only be done after proper verification is performed on the
     * client side that the user is diagnosed positive.
     *
     * Calling this will invoke the
     * ContactTracingCallback.requestUploadDailyTracingKeys callback
     * provided via startContactTracing at some point in the future. Provided keys
     * should be uploaded to the server and distributed to other users.
     *
     * This shows a user dialog for sharing and uploading data to the server.
     * The status will also flip back off again after 14 days; in other words,
     * the client will stop receiving requestUploadDailyTracingKeys
     * callbacks after that time.
     *
     * Only 14 days of history are available.
     */
    fun startSharingDailyTracingKeys() : Status

    /**
     * Provides a list of diagnosis keys for contact checking. The keys are to be
     * provided by a centralized service (e.g. synced from the server).
     *
     * When invoked after the requestProvideDiagnosisKeys callback, this triggers a
     * recalculation of contact status which can be obtained via hasContact()
     * after the calculation has finished.
     *
     * Should be called with a maximum of N keys at a time.
     */
    fun provideDiagnosisKeys(keys: List<DailyTracingKey?>?): Status?

    /**
     * Handles an intent which was invoked via the contactTracingCallback and
     * calls the corresponding ContactTracingCallback methods.
     */
    fun handleIntent(intentCallback: Intent?, callback: ContactTracingCallback?)

    /**
     * Starts BLE broadcasts and scanning based on the defined protocol.
     *
     * If not previously used, this shows a user dialog for consent to start contact
     * tracing and get permission.
     *
     * Calls back when data is to be pushed or pulled from the client, see
     * ContactTracingCallback.
     *
     * Callers need to re-invoke this after each device restart, providing a new
     * callback PendingIntent.
     */
    fun startContactTracing(contactTracingCallback: PendingIntent?): Status?

    /**
     * Disables advertising and scanning related to contact tracing. Contents of the
     * database and keys will remain.
     *
     * If the client app has been uninstalled by the user, this will be automatically
     * invoked and the database and keys will be wiped from the device.
     */
    fun stopContactTracing(): Status?

    // Indicates whether contact tracing is currently running for the requesting app.
    fun isContactTracingEnabled(): Status?

    // getMaxDiagnosisKeys - The maximum number of keys to pass into provideDiagnosisKeys at any given time.
    fun getMaxDiagnosisKeys(): Int

    /**
     * Check if this user has come into contact with a provided key. Contact
     * calculation happens daily.
     */
    fun hasContact(k : DailyTracingKey): Boolean?

    /**
     * Check if this user has come into contact with a provided key. Contact
     * calculation happens daily.
     */
    fun getContactInformation(k : DailyTracingKey): List<ContactInfo?>?
}
