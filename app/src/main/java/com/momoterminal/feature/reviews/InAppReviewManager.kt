package com.momoterminal.feature.reviews

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Result of a review request operation.
 */
sealed class ReviewResult {
    /**
     * Review flow completed successfully.
     * Note: This doesn't mean the user left a review, just that the flow completed.
     */
    data object Success : ReviewResult()
    
    /**
     * Failed to request or launch review.
     */
    data class Failed(val exception: Exception) : ReviewResult()
}

/**
 * Manager class for handling in-app reviews using Play Core library.
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(context)
    
    private var cachedReviewInfo: ReviewInfo? = null
    
    /**
     * Request review info from Google Play.
     * This should be called ahead of time to pre-fetch the review info.
     * Returns true if the request was successful.
     */
    suspend fun requestReview(): Boolean = suspendCancellableCoroutine { continuation ->
        reviewManager.requestReviewFlow()
            .addOnSuccessListener { reviewInfo ->
                cachedReviewInfo = reviewInfo
                Timber.d("Review info request successful")
                continuation.resume(true)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to request review info")
                cachedReviewInfo = null
                continuation.resume(false)
            }
    }
    
    /**
     * Launch the review flow with the activity.
     * This will show the in-app review dialog if conditions are met.
     *
     * Note: Google Play may not always show the review dialog even when launched.
     * The API has built-in quotas to prevent review spamming.
     *
     * @param activity The activity to use for launching the review flow
     * @return ReviewResult indicating success or failure
     */
    suspend fun launchReviewFlow(activity: Activity): ReviewResult {
        // Use cached review info or request new one
        val reviewInfo = cachedReviewInfo ?: run {
            if (!requestReview()) {
                return ReviewResult.Failed(Exception("Failed to request review info"))
            }
            cachedReviewInfo
        }
        
        if (reviewInfo == null) {
            return ReviewResult.Failed(Exception("Review info is null"))
        }
        
        return suspendCancellableCoroutine { continuation ->
            reviewManager.launchReviewFlow(activity, reviewInfo)
                .addOnSuccessListener {
                    Timber.d("Review flow completed")
                    // Clear cached info after use
                    cachedReviewInfo = null
                    continuation.resume(ReviewResult.Success)
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "Failed to launch review flow")
                    cachedReviewInfo = null
                    continuation.resume(ReviewResult.Failed(exception as Exception))
                }
        }
    }
    
    /**
     * Pre-warm the review flow by requesting review info ahead of time.
     * Call this early in the app lifecycle to reduce latency when showing the review.
     */
    suspend fun preWarmReviewFlow() {
        if (cachedReviewInfo == null) {
            requestReview()
        }
    }
    
    /**
     * Check if a review info is cached and ready to use.
     */
    fun isReviewInfoCached(): Boolean {
        return cachedReviewInfo != null
    }
}
