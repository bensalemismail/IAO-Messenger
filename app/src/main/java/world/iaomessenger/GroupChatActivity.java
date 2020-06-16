package world.iaomessenger;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {

    private static final String TAG = "GroupChatActivity";
    private IaoMessenger IM;

    private ImageButton mSendMessageButton;
    private EditText mUserMessageInput;
    private ScrollView scrollView;
    private TextView mGroupNameTV;
    private RelativeLayout constraLaintyoutGroupChat;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersReference, mGroupReference, mGroupMessageKeyReference;
    private String mGroupName, mCurrentUserID, mCurrentName, mCurrentTime, mCurrentDate, mGroupID;
    private FirebaseUser mCurrentUser;
    private DatabaseReference mDBRef;
    private DatabaseReference mGroupNameRef;

    // List of messages
    private final List<GroupsModule> mChatsList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private GroupChatAdapter mGroupChatAdapter;
    private RecyclerView mGroupsRV;
    private DatabaseReference mGroupMessagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        IM = ((IaoMessenger) getApplication());

        Toolbar mGroupActivityToolbar = (Toolbar) findViewById(R.id.groups_activity_toolbar);

        setSupportActionBar(mGroupActivityToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getSupportActionBar().setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View toolbar_view = layoutInflater.inflate(R.layout.groups_chats_bar_display_layout, null);
        getSupportActionBar().setCustomView(toolbar_view);

        // Initialize Views
        getViews();

        // Set group name
        mGroupNameTV.setText(mGroupName);



        // Send a message
        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String messageFieldOutput = mUserMessageInput.getText().toString();

                if(!TextUtils.isEmpty(messageFieldOutput)) {

                    final String friendID = getIntent().getStringExtra("visit_user_id");

                    if(friendID != null ) {
                        final DatabaseReference groupsBadgeRef = mDBRef.child("Badges").child("groups_badges").child(friendID).child("value");

                        groupsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {

                                    int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                    Log.d(TAG, "onDataChange: GROUPS BADGE COUNT" + count);
                                    IM.setGroups_badge(count);
                                    groupsBadgeRef.setValue(IM.getGroups_badge() + 1);

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }


                    String currentGroupMessagesRef = "Groups Messages/" + mGroupID;

                    DatabaseReference userMessageRef = mGroupReference.child(mGroupID).push();
                    String messageID = userMessageRef.getKey();

                    Map messageMap = new HashMap();
                    messageMap.put("message", messageFieldOutput);
                    messageMap.put("time", ServerValue.TIMESTAMP);
                    messageMap.put("from", mCurrentUserID);

                    Log.d(TAG, "sendMessage: TIME:");

                    Map messageUserMapRef = new HashMap();
                    messageUserMapRef.put(currentGroupMessagesRef + "/" + messageID, messageMap);

                    mUserMessageInput.setText("");

                    mDBRef.updateChildren(messageUserMapRef, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                            if(databaseError != null) {

                                Log.d(TAG, "onComplete: Error -> " + databaseError.getMessage().toString());

                            }

                        }
                    });
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        mUsersReference.child(mCurrentUserID).child("online").setValue("true");

        // Retrieve messages from database
        retrieveGroupMessages();
    }


    // Find views by ID.
    private void getViews() {

        mGroupName = getIntent().getExtras().get("groupName").toString();
        mGroupID = getIntent().getExtras().get("groupID").toString();
        mGroupNameTV = (TextView) findViewById(R.id.groups_name_bar);

        mSendMessageButton = (ImageButton) findViewById(R.id.button_groups_box_send);
        mUserMessageInput = (EditText) findViewById(R.id.groups_activity_message_field);
        constraLaintyoutGroupChat = (RelativeLayout) findViewById(R.id.constraLaintyoutGroups);

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        mDBRef = FirebaseDatabase.getInstance().getReference();
        mUsersReference = mDBRef.child("Users");
        mGroupReference = mDBRef.child("Groups");
        mGroupMessagesRef = mDBRef.child("Groups Messages");

        if(mCurrentUser != null) {
            mCurrentUserID = mCurrentUser.getUid();
            mGroupNameRef = mGroupReference.child(mGroupID).child("name");
        }

        // Recycler View
        mGroupChatAdapter = new GroupChatAdapter(mChatsList);
        mGroupsRV = (RecyclerView) findViewById(R.id.groups_activity_rec_view);
        mLinearLayout = new LinearLayoutManager(this);
        mGroupsRV.setHasFixedSize(true);
        mGroupsRV.setLayoutManager(mLinearLayout);
        mGroupsRV.setAdapter(mGroupChatAdapter);

    }

    private void retrieveGroupMessages() {

        mGroupMessagesRef.child(mGroupID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                if(dataSnapshot.exists()) {
                    if (dataSnapshot.getValue().getClass() != String.class) {
                        GroupsModule chat = dataSnapshot.getValue(GroupsModule.class);
                        Log.d(TAG, "onChildAdded: Messages -> " + dataSnapshot.getValue(GroupsModule.class).message);
                        mChatsList.add(chat);
                    }

                    mGroupChatAdapter.notifyDataSetChanged();

                    int itemCount = mGroupsRV.getAdapter().getItemCount();

                    mGroupsRV.smoothScrollToPosition(itemCount);
                }
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
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUsersReference.child(mCurrentUserID).child("online").setValue("false");
    }
}
