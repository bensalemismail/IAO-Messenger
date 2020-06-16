package world.iaomessenger;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class LatestFirebaseMessagingService extends FirebaseMessagingService {

    static String deviceToken;

    @Override
    public void onNewToken(String mToken) {
        super.onNewToken(mToken);
        deviceToken = mToken;
        Log.e("TOKEN",mToken);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }
}