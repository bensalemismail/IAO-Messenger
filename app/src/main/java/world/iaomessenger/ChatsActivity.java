package world.iaomessenger;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsActivity extends AppCompatActivity {

    private static final String TAG = "ChatsActivity";
    private IaoMessenger IM;

    private ImageButton mSendMessageButton;
    private EditText mMessageField;
    private RecyclerView mChatsRecyclerView;
    private CircleImageView mFriendUserImage;
    private TextView mFriendName;
    private TextView mFriendLastSeen;

    private FirebaseAuth mAuth;
    private DatabaseReference mChatsDatabaseReference, mDatabaseRef, mUsersDatabaseReference, mMessagesDatabaseRef;
    private FirebaseUser mCurrentUser;
    private ValueEventListener mChatsDBValueEL;
    private String mCurrentUserID;

    // List of messages
    private final List<ChatsModule> mChatsList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private ChatsAdapter mChatsAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        IM = ((IaoMessenger) getApplication());

        final String friendUserID = getIntent().getStringExtra("visit_user_id");

        String friendUserName = getIntent().getStringExtra("friend_name");
        Log.d(TAG, "onCreate: " + friendUserID + " Name: " + friendUserName);

        Toolbar toolbar = findViewById(R.id.chats_activity_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toolbar_view = layoutInflater.inflate(R.layout.friend_chat_custom_bar, null);
        getSupportActionBar().setCustomView(toolbar_view);

        // Initialize Views
        getViews();

        // Set the friend name on the toolbar
        mFriendName.setText(friendUserName);

        // Bring friend data and display it on the toolbar (image, last seen status)
        mUsersDatabaseReference.child(friendUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.hasChild("lastSeen") && dataSnapshot.hasChild("online")) {

                    String friendOnlineStatus = dataSnapshot.child("online").getValue().toString();
                    String lastSeen = dataSnapshot.child("lastSeen").getValue().toString();


                    if(dataSnapshot.hasChild("image")) {
                        String friendImage = dataSnapshot.child("image").getValue().toString();
                        Log.d(TAG, "onDataChange: IMAGE: " + friendImage);
                        Picasso.get().load(friendImage).placeholder(R.drawable.ic_chats_friend_image_bar).into(mFriendUserImage);
                    }

                    if (friendOnlineStatus.equals("true")) {

                        String online = "Online";
                        mFriendLastSeen.setText(online);

                    } else {

                        long longTime = Long.parseLong(lastSeen);
                        String timeAgo = GetTimeAgo.getTimeAgo(longTime);
                        mFriendLastSeen.setText(timeAgo);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // Send Messages to the database
        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();

            }
        });

        //ici

        /*
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatsActivity.this,FriendProfilActivity.class));
            }
        });
        */
    }

    public void getViews() {

        mSendMessageButton = (ImageButton) findViewById(R.id.button_chatbox_send);
        mMessageField = (EditText) findViewById(R.id.chats_activity_message_field);

        // Recycler View
        mChatsRecyclerView = (RecyclerView) findViewById(R.id.chats_activity_rec_view);
        mLinearLayout = new LinearLayoutManager(this);
        mChatsRecyclerView.setHasFixedSize(true);
        mChatsRecyclerView.setLayoutManager(mLinearLayout);
        mChatsAdapter = new ChatsAdapter(mChatsList);
        mChatsRecyclerView.setAdapter(mChatsAdapter);


        mFriendName = (TextView) findViewById(R.id.chats_friend_name_bar);
        mFriendLastSeen = (TextView) findViewById(R.id.chats_friend_last_seen_bar);
        mFriendUserImage = (CircleImageView) findViewById(R.id.chats_friend_image_bar);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        if(mCurrentUser != null) {
            mCurrentUserID = mCurrentUser.getUid();
        }

        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mChatsDatabaseReference = mDatabaseRef.child("Chats");
        mChatsDatabaseReference.keepSynced(true);
        mUsersDatabaseReference = mDatabaseRef.child("Users");
        mMessagesDatabaseRef = mDatabaseRef.child("Messages");
        mMessagesDatabaseRef.keepSynced(true);

    }


    private void sendMessage() {

        String messageFieldOutput = mMessageField.getText().toString();

        if(!TextUtils.isEmpty(messageFieldOutput)) {

            final String friendID = getIntent().getStringExtra("visit_user_id");

            final DatabaseReference chatsBadgeRef = mDatabaseRef.child("Badges").child("chats_badges").child(friendID).child("value");

            chatsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                        Log.d(TAG, "onDataChange: COUNT -> " + dataSnapshot.getValue());
                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                        IM.setChats_badge(count);
                        chatsBadgeRef.setValue(IM.getChats_badge() + 1);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            Log.d(TAG, "sendMessage: BADGE COUNT -> " + IM.getChats_badge());

            String currentUserRef = "Messages/" + mCurrentUserID + "/" + friendID;
            String friendUserRef = "Messages/" + friendID + "/" + mCurrentUserID;

            DatabaseReference userMessageRef = mDatabaseRef.child("Messages").child(mCurrentUserID).child(friendUserRef).push();

            mChatsDatabaseReference.child(friendID).child(mCurrentUserID).child("seen").setValue("false");
            mChatsDatabaseReference.child(mCurrentUserID).child(friendID).child("seen").setValue("true");
            String messageID = userMessageRef.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", messageFieldOutput);
            messageMap.put("seen", "false");
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", mCurrentUserID);

            Log.d(TAG, "sendMessage: TIME:");

            Map messageUserMapRef = new HashMap();
            messageUserMapRef.put(currentUserRef + "/" + messageID, messageMap);
            messageUserMapRef.put(friendUserRef + "/" + messageID, messageMap);

            mMessageField.setText("");

            mDatabaseRef.updateChildren(messageUserMapRef, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                    if(databaseError != null) {

                        Log.d(TAG, "onComplete: Error -> " + databaseError.getMessage().toString());

                    }

                }
            });
            
            mChatsDatabaseReference.child(mCurrentUserID).child(friendID).child("time_stamp")
                    .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    
                    if(task.isSuccessful()) {
                        
                        mChatsDatabaseReference.child(friendID).child(mCurrentUserID).child("time_stamp")
                                .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                
                                if(task.isSuccessful()) {

                                    Log.d(TAG, "onComplete: SUCCESS");
                                    
                                } else {

                                    Log.d(TAG, "onComplete: FAILURE");
                                    
                                }
                                
                            }
                        });
                        
                    }
                    
                }
            });
        }
    }

    private void retrieveMessages() {

        String friendUserID = getIntent().getStringExtra("visit_user_id");
        Log.d(TAG, "retrieveMessages: ");

        final DatabaseReference messageDBRef = mMessagesDatabaseRef.child(mCurrentUserID).child(friendUserID);

        messageDBRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        ChatsModule chat = dataSnapshot.getValue(ChatsModule.class);

                        Log.d(TAG, "onChildAdded: " + chat);
                        mChatsList.add(chat);
                        mChatsAdapter.notifyDataSetChanged();

                        int itemCount = mChatsRecyclerView.getAdapter().getItemCount();

                        mChatsRecyclerView.smoothScrollToPosition(itemCount);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUsersDatabaseReference.child(mCurrentUserID).child("online").setValue("true");
        // Retrieve messages from database
        retrieveMessages();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mUsersDatabaseReference.child(mCurrentUserID).child("online").setValue("false");
    }
}
