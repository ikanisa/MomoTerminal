package com.momoterminal.core.osintegration.deeplinks

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import javax.inject.Inject
import javax.inject.Singleton

// Generic deep link model
sealed class DeepLink(val route: String) {
    data class FeatureA(val itemId: String?) : DeepLink("featureA/${itemId ?: ""}")
    data class FeatureB(val actionId: String?) : DeepLink("featureB/${actionId ?: ""}")
    data class FeatureC(val param: String?) : DeepLink("featureC/${param ?: ""}")
    data object Home : DeepLink("home")
    data object Settings : DeepLink("settings")
}

// URI scheme definition
object DeepLinkScheme {
    const val SCHEME = "app"
    const val HOST = "momoterminal.com"
    
    // App scheme: app://feature/item/123
    // HTTPS scheme: https://momoterminal.com/feature/item/123
    
    fun buildUri(path: String): Uri = Uri.parse("$SCHEME://$path")
    fun buildHttpsUri(path: String): Uri = Uri.parse("https://$HOST/$path")
}

@Singleton
class DeepLinkHandler @Inject constructor() {
    
    fun parseDeepLink(uri: Uri): DeepLink? {
        return when {
            // app://featureA/item/{id} or https://momoterminal.com/featureA/item/{id}
            uri.pathSegments.firstOrNull() == "featureA" -> {
                val itemId = uri.pathSegments.getOrNull(2)
                DeepLink.FeatureA(itemId)
            }
            
            // app://featureB/action/{id}
            uri.pathSegments.firstOrNull() == "featureB" -> {
                val actionId = uri.pathSegments.getOrNull(2)
                DeepLink.FeatureB(actionId)
            }
            
            // app://featureC/{param}
            uri.pathSegments.firstOrNull() == "featureC" -> {
                val param = uri.pathSegments.getOrNull(1)
                DeepLink.FeatureC(param)
            }
            
            // app://home
            uri.path == "/home" -> DeepLink.Home
            
            // app://settings
            uri.path == "/settings" -> DeepLink.Settings
            
            else -> null
        }
    }
    
    fun handleDeepLink(navController: NavController, uri: Uri): Boolean {
        val deepLink = parseDeepLink(uri) ?: return false
        
        navController.navigate(deepLink.route) {
            // Clear back stack to avoid navigation issues
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        
        return true
    }
}

// Compose integration
@Composable
fun HandleDeepLinks(
    navController: NavHostController,
    deepLinkHandler: DeepLinkHandler = hiltViewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        val intent = (context as? ComponentActivity)?.intent
        intent?.data?.let { uri ->
            deepLinkHandler.handleDeepLink(navController, uri)
        }
    }
}

// MainActivity integration example removed

