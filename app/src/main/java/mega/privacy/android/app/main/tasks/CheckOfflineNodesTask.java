package mega.privacy.android.app.main.tasks;

import static mega.privacy.android.app.utils.FileUtil.deleteFolderAndSubfolders;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.OfflineUtils.OFFLINE_DIR;
import static mega.privacy.android.app.utils.OfflineUtils.getOfflineFile;
import static mega.privacy.android.app.utils.OfflineUtils.getOfflineFolder;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaOffline;
import timber.log.Timber;

/**
 * Background task to verify the offline nodes
 */
public class CheckOfflineNodesTask extends AsyncTask<String, Void, String> {
    Context context;
    DatabaseHandler dbH;

    public CheckOfflineNodesTask(Context context) {
        this.context = context;
        dbH = DatabaseHandler.getDbHandler(context.getApplicationContext());
    }

    @Override
    protected String doInBackground(String... params) {
        Timber.d("doInBackground-Async Task CheckOfflineNodesTask");

        ArrayList<MegaOffline> offlineNodes = dbH.getOfflineFiles();

        File file = getOfflineFolder(context, OFFLINE_DIR);

        if (isFileAvailable(file)) {

            for (int i = 0; i < offlineNodes.size(); i++) {
                MegaOffline mOff = offlineNodes.get(i);
                File fileToCheck = getOfflineFile(context, mOff);
                if (!isFileAvailable(fileToCheck)) {
                    int removed = dbH.deleteOfflineFile(mOff);
                    Timber.d("File removed: %s", removed);
                } else {
                    Timber.d("The file exists!");
                }
            }
            //Check no empty folders
            offlineNodes = dbH.getOfflineFiles();
            for (int i = 0; i < offlineNodes.size(); i++) {
                MegaOffline mOff = offlineNodes.get(i);
                //Get if its folder
                if (mOff.isFolder()) {
                    ArrayList<MegaOffline> children = dbH.findByParentId(mOff.getId());
                    if (children.size() < 1) {
                        Timber.d("Delete the empty folder: %s", mOff.getName());
                        dbH.deleteOfflineFile(mOff);
                        File folderToDelete = getOfflineFile(context, mOff);
                        try {
                            deleteFolderAndSubfolders(context, folderToDelete);
                        } catch (IOException e) {
                            Timber.e(e, "IOException mOff");
                        }
                    }
                }
            }

        } else {
            //Delete the DB if NOT empty
            if (offlineNodes.size() > 0) {
                //Delete the content
                Timber.d("Clear Offline TABLE");
                dbH.clearOffline();
            }
        }

        return null;
    }
}
