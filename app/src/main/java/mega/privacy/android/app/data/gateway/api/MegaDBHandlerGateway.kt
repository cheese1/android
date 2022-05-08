package mega.privacy.android.app.data.gateway.api

/**
 * MegaDBHandlerGateway gateway
 *
 * The gateway interface to the Mega DBhandler functionality
 */
interface MegaDBHandlerGateway {

    /**
     * Camera Uploads handle
     */
    val camSyncHandle: String?
    /**
     * Media Uploads handle
     */
    val megaHandleSecondaryFolder: String?
}