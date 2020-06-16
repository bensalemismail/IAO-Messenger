package world.iaomessenger;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import q.rorbin.badgeview.Badge;
import q.rorbin.badgeview.QBadgeView;

public class IaoMessenger extends Application {

    private DatabaseReference userDatabaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserID;
    public int requestState;
    public int chats_badge, groups_badge, contacts_badge, requests_badge;
    private DatabaseReference mBadgesRef;

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        Picasso.Builder builder = new Picasso.Builder(this);
        builder.downloader(new OkHttp3Downloader(this, Integer.MAX_VALUE));
        Picasso build = builder.build();
        build.setIndicatorsEnabled(true);
        build.setLoggingEnabled(true);
        Picasso.setSingletonInstance(build);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            currentUserID = currentUser.getUid();
            userDatabaseReference = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("Users")
                    .child(currentUserID);

            userDatabaseReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.hasChild("online")) {

                        userDatabaseReference.child("online").onDisconnect().setValue("false");

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
    }

    public int getRequestState() {
        return requestState;
    }

    public void setRequestState(int requestState) {
        this.requestState = requestState;
    }

    public Badge addBadgeAt(int position, int number, BottomNavigationViewEx bnve) {
        // add badge
        return new QBadgeView(getApplicationContext())
                .setBadgeNumber(number)
                .setBadgeTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorBlack))
                .setBadgeBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.silver))
                .setGravityOffset(12, 2, true)
                .bindTarget(bnve.getBottomNavigationItemView(position))
                .setOnDragStateChangedListener(new Badge.OnDragStateChangedListener() {
                    @Override
                    public void onDragStateChanged(int dragState, Badge badge, View targetView) {
                        if (Badge.OnDragStateChangedListener.STATE_SUCCEED == dragState)
                            Toast.makeText(getApplicationContext(), R.string.tips_badge_removed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public int getChats_badge() {
        return chats_badge;
    }

    public void setChats_badge(int chats_badge) {
        this.chats_badge = chats_badge;
    }

    public int getGroups_badge() {
        return groups_badge;
    }

    public void setGroups_badge(int groups_badge) {
        this.groups_badge = groups_badge;
    }

    public int getContacts_badge() {
        return contacts_badge;
    }

    public void setContacts_badge(int contacts_badge) {
        this.contacts_badge = contacts_badge;
    }

    public int getRequests_badge() {
        return requests_badge;
    }

    public void setRequests_badge(int requests_badge) {
        this.requests_badge = requests_badge;
    }
}
