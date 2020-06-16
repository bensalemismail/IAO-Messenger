package world.iaomessenger;

import android.app.Application;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendProfilActivity extends AppCompatActivity {

    private static final String TAG = "FriendProfilActivity";

    private String receiverUserID;
    private Toolbar toolbar;
    private DatabaseReference databaseReference;
    private CircleImageView friendImage;
    private Button sendMessageBtn, unfriendBtn, backBtn, callBtn;
    private TextView friendNameToolbar, friendNameInside, onlineStatus, statusField;

    private FirebaseUser currentUser;
    private DatabaseReference mDBref;
    private DatabaseReference requestsDatabaseReference;
    private DatabaseReference friendsDatabaseReference;
    private DatabaseReference usersDatabaseReference;
    private DatabaseReference mNotifRef;

    private String currentUserID;
    private String friendUserID;

    private int request_state;

    public IaoMessenger IM;
    private ProgressDialog progressDialog;

    private String friend_name;
    private String friend_name_image;
    private String friend_name_status;
    private String friend_online_status;

    private String currentUserName;
    private String currentUserImg;
    private String currentUserStatus;
    private DatabaseReference mChatsRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mPokesRef;
    private ValueEventListener mChatsDBValueEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profil);

        //TODO: change phone button to chats icon with number denoting how many chats are unopened

        toolbar = findViewById(R.id.friend_profile_toolbar);
        //setSupportActionBar(toolbar);

        IM = ((IaoMessenger) getApplication());

        // 0 means request is not sent yet, and 1 means the opposite
        IM.setRequestState(0);

        getViews();

        retrieveFriendInfo();

        currentUserName = databaseReference.child(currentUserID).child("name").getKey();
        currentUserImg = databaseReference.child(currentUserID).child("image").getKey();
        currentUserStatus = databaseReference.child(currentUserID).child("status").getKey();

        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessageBtn.setEnabled(false);

                // Not friends STATE
                request_state = IM.getRequestState();
                if(request_state == 0) {

                    requestsDatabaseReference.child(currentUserID).child(friendUserID).child("request_type")
                            .setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                requestsDatabaseReference.child(friendUserID).child(currentUserID).child("request_type")
                                        .setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        HashMap<String, String> requestNotifMap = new HashMap<>();
                                        requestNotifMap.put("from", currentUserID);
                                        requestNotifMap.put("type", "request");

                                        mNotifRef.child(friendUserID).push()
                                                .setValue(requestNotifMap)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {

                                                        if(task.isSuccessful()) {

                                                            final DatabaseReference requestsBadgeRef = mDBref.child("Badges").child("requests_badges").child(friendUserID).child("value");

                                                            requestsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                @Override
                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                    if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                                                        Log.d(TAG, "onDataChange: COUNT -> " + dataSnapshot.getValue());
                                                                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                        IM.setRequests_badge(count);
                                                                        requestsBadgeRef.setValue(count + 1);
                                                                    }

                                                                }

                                                                @Override
                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                }
                                                            });

                                                            sendMessageBtn.setEnabled(true);
                                                            IM.setRequestState(1);
                                                            sendMessageBtn.setText(R.string.cancel_request);
                                                            sendMessageBtn.setBackgroundResource(R.drawable.cancel_request_button_border);
                                                            //sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                                            sendMessageBtn.setTextColor(Color.BLACK);
                                                            Toast.makeText(FriendProfilActivity.this, "Request sent.", Toast.LENGTH_SHORT).show();

                                                        }

                                                    }
                                                });

                                    }
                                });

                            } else {

                                Toast.makeText(FriendProfilActivity.this, "Request failed.", Toast.LENGTH_SHORT).show();

                            }

                        }
                    });

                    // Request SENT STATE
                } else if (request_state == 1) {

                    Toast.makeText(FriendProfilActivity.this, "Request Canceled", Toast.LENGTH_SHORT).show();
                    requestsDatabaseReference.child(currentUserID).child(friendUserID)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                requestsDatabaseReference.child(friendUserID).child(currentUserID)
                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful()) {

                                            final DatabaseReference requestsBadgeRef = mDBref.child("Badges").child("requests_badges").child(friendUserID).child("value");

                                            requestsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                                        Log.d(TAG, "onDataChange: COUNT -> " + dataSnapshot.getValue());
                                                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                        IM.setRequests_badge(count);
                                                        requestsBadgeRef.setValue(count - 1);
                                                    }

                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                            sendMessageBtn.setEnabled(true);
                                            IM.setRequestState(0);
                                            sendMessageBtn.setText(R.string.add_friend);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setTextColor(Color.WHITE);

                                        }

                                    }
                                });

                            }

                        }
                    });

                    // Accept request and be friends STATE
                } else if(request_state == 2) {

                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                    HashMap<String, String> friendMap = new HashMap<>();
                    friendMap.put("date", currentDate);

                    friendsDatabaseReference.child(currentUserID).child(friendUserID)
                            .setValue(friendMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                HashMap<String, String> userMap = new HashMap<>();
                                userMap.put("date", currentDate);

                                friendsDatabaseReference.child(friendUserID).child(currentUserID)
                                        .setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful()) {

                                            requestsDatabaseReference.child(currentUserID).child(friendUserID)
                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if (task.isSuccessful()) {

                                                        requestsDatabaseReference.child(friendUserID).child(currentUserID)
                                                                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if (task.isSuccessful()) {

                                                                    final DatabaseReference contactsBadgeRef = mDBref.child("Badges").child("contacts_badges");
                                                                    contactsBadgeRef.child(friendUserID).child("value").addListenerForSingleValueEvent(new ValueEventListener() {

                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                            if(dataSnapshot.getValue() != null) {

                                                                                int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                IM.setContacts_badge(count);
                                                                                contactsBadgeRef.child(friendUserID).child("value").setValue(count + 1);
                                                                            }

                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                        }
                                                                    });

                                                                    contactsBadgeRef.child(currentUserID).child("value").addListenerForSingleValueEvent(new ValueEventListener() {

                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                            if(dataSnapshot.getValue() != null) {

                                                                                int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                IM.setContacts_badge(count);
                                                                                contactsBadgeRef.child(friendUserID).child("value").setValue(count + 1);
                                                                            }

                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                        }
                                                                    });


                                                                    final DatabaseReference requestsBadgeRef = mDBref.child("Badges").child("requests_badges").child(friendUserID).child("value");

                                                                    requestsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                                        @Override
                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                            if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                                                                Log.d(TAG, "onDataChange: COUNT -> " + dataSnapshot.getValue());
                                                                                int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                IM.setRequests_badge(count);
                                                                                requestsBadgeRef.setValue(count - 1);
                                                                            }

                                                                        }

                                                                        @Override
                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                        }
                                                                    });

                                                                    sendMessageBtn.setEnabled(true);
                                                                    sendMessageBtn.setText(R.string.send_message);

                                                                    // '3' means that they are friends now.
                                                                    IM.setRequestState(3);

                                                                }

                                                            }
                                                        });

                                                    }

                                                }
                                            });

                                            sendMessageBtn.setEnabled(true);
                                            sendMessageBtn.setText(R.string.send_message);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setTextColor(Color.WHITE);

                                            unfriendBtn.setEnabled(true);
                                            unfriendBtn.setText(R.string.unfriend);
                                            unfriendBtn.setBackgroundResource(R.drawable.profile_logout_button_border);
                                            unfriendBtn.setTextColor(Color.BLACK);

                                        }

                                    }
                                });


                            }

                        }
                    });

                    // Already friends, SEND MESSAGE STATE
                } else if(request_state == 3){
                    sendMessageBtn.setEnabled(true);

                    // Chats Database event listener
                    mChatsDBValueEL = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                mChatsRef.child(currentUserID).child(friendUserID).child("time_stamp")
                                        .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if (task.isSuccessful()) {

                                            mChatsRef.child(friendUserID).child(currentUserID).child("time_stamp")
                                                    .setValue(ServerValue.TIMESTAMP);

                                        }

                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    };

                    // Store chat in database
                    mChatsRef.child(currentUserID).addValueEventListener(mChatsDBValueEL);

                    Intent chatActivityIntent = new Intent(FriendProfilActivity.this, ChatsActivity.class);
                    chatActivityIntent.putExtra("visit_user_id", friendUserID);
                    chatActivityIntent.putExtra("friend_name", friend_name);
                    startActivity(chatActivityIntent);

                }

            }
        });

        unfriendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                unfriendBtn.setEnabled(false);

                request_state = IM.getRequestState();

                if(request_state == 0 || request_state == 1) {

                    //TODO: Poke Notification
                    unfriendBtn.setEnabled(true);

                    HashMap<String, String> pokeNotifMap = new HashMap<>();
                    pokeNotifMap.put("from", currentUserID);
                    pokeNotifMap.put("type", "poke");

                    mPokesRef.child(friendUserID).push().setValue(pokeNotifMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                Toast.makeText(FriendProfilActivity.this, "POKE SENT.", Toast.LENGTH_SHORT).show();

                            }

                        }
                    });

                } else if(request_state == 2) {

                    requestsDatabaseReference.child(currentUserID).child(friendUserID)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                requestsDatabaseReference.child(friendUserID).child(currentUserID)
                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful()) {

                                            final DatabaseReference requestsBadgeRef = mDBref.child("Badges").child("requests_badges").child(friendUserID).child("value");

                                            requestsBadgeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                                @Override
                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                    if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                                        Log.d(TAG, "onDataChange: COUNT -> " + dataSnapshot.getValue());
                                                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                        IM.setRequests_badge(count);
                                                        requestsBadgeRef.setValue(count - 1);
                                                    }
                                                }

                                                @Override
                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                }
                                            });

                                            IM.setRequestState(0);

                                            sendMessageBtn.setText(R.string.add_friend);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setTextColor(Color.WHITE);

                                            unfriendBtn.setText(R.string.poke);
                                            unfriendBtn.setBackgroundResource(R.drawable.profile_logout_button_border);
                                            unfriendBtn.setTextColor(Color.BLACK);

                                            unfriendBtn.setEnabled(true);

                                        }

                                    }
                                });

                            }

                        }
                    });

                    // Unfriend state
                } else if(request_state == 3) {

                    friendsDatabaseReference.child(currentUserID).child(friendUserID)
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if(task.isSuccessful()) {

                                friendsDatabaseReference.child(friendUserID).child(currentUserID)
                                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {

                                        if(task.isSuccessful()) {

                                            //TODO: ADD AN ALERT DIALOG BEFORE DELETING A FRIEND
                                            IM.setRequestState(0);

                                            sendMessageBtn.setTextColor(Color.WHITE);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setText(R.string.add_friend);

                                            unfriendBtn.setTextColor(Color.BLACK);
                                            unfriendBtn.setBackgroundResource(R.drawable.profile_logout_button_border);
                                            unfriendBtn.setText(R.string.poke);

                                            unfriendBtn.setEnabled(true);

                                            mChatsRef.child(currentUserID).child(friendUserID)
                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    mChatsRef.child(friendUserID).child(currentUserID)
                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {

                                                            mMessagesRef.child(currentUserID).child(friendUserID)
                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    mMessagesRef.child(friendUserID).child(currentUserID)
                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            final DatabaseReference contactsBadgeRef = mDBref.child("Badges").child("contacts_badges");
                                                                            contactsBadgeRef.child(friendUserID).child("value").addListenerForSingleValueEvent(new ValueEventListener() {

                                                                                @Override
                                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                                    if(dataSnapshot.getValue() != null) {

                                                                                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                        IM.setContacts_badge(count);
                                                                                        contactsBadgeRef.child(friendUserID).child("value").setValue(count - 1);
                                                                                    }

                                                                                }

                                                                                @Override
                                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                }
                                                                            });

                                                                            contactsBadgeRef.child(currentUserID).child("value").addListenerForSingleValueEvent(new ValueEventListener() {

                                                                                @Override
                                                                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                                    if(dataSnapshot.getValue() != null) {

                                                                                        int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                        IM.setContacts_badge(count);
                                                                                        contactsBadgeRef.child(friendUserID).child("value").setValue(count - 1);
                                                                                    }

                                                                                }

                                                                                @Override
                                                                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                }
                                                                            });

                                                                        }
                                                                    });

                                                                }
                                                            });

                                                        }
                                                    });

                                                }
                                            });

                                        }

                                    }
                                });

                            }

                        }
                    });

                }

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FriendProfilActivity.this.finish();
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

            }
        });
    }

    private void getViews() {

        mDBref = FirebaseDatabase.getInstance().getReference();
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();
        databaseReference = mDBref.child("Users");

        backBtn = (Button) findViewById(R.id.friend_profile_back_button);
        friendNameToolbar = (TextView) findViewById(R.id.friend_profile_friend_name_toolbar);
        callBtn = (Button) findViewById(R.id.friend_profile_phone);
        friendImage = (CircleImageView) findViewById(R.id.friend_profile_avatar);
        friendNameInside = (TextView) findViewById(R.id.friend_profile_friend_name_inside);
        onlineStatus = (TextView) findViewById(R.id.friend_profile_online_status);
        statusField = (TextView) findViewById(R.id.friend_profile_status);
        sendMessageBtn = (Button) findViewById(R.id.friend_profile_send_message);
        unfriendBtn = (Button) findViewById(R.id.friend_profile_unfriend);
        progressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);

        // Firebase credentials
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        requestsDatabaseReference = mDBref.child("Requests");
        currentUserID = currentUser.getUid();
        friendUserID = getIntent().getStringExtra("visit_user_id");
        friendsDatabaseReference = mDBref.child("Friends");
        usersDatabaseReference = mDBref.child("Users");
        mChatsRef = mDBref.child("Chats");
        mMessagesRef = mDBref.child("Messages");
        mNotifRef = mDBref.child("Notifications");
        mPokesRef = mDBref.child("Pokes");
    }

    public void retrieveFriendInfo() {

        progressDialog.setTitle("Fetching User Data");
        progressDialog.setMessage("Please wait..");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        databaseReference.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.hasChild("image")) {

                    friend_name = dataSnapshot.child("name").getValue().toString();
                    friend_name_image = dataSnapshot.child("image").getValue().toString();
                    friend_name_status = dataSnapshot.child("status").getValue().toString();
                    friend_online_status = dataSnapshot.child("online").getValue().toString();

                    requestsDatabaseReference.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(dataSnapshot.hasChild(friendUserID)) {

                                String request_type = dataSnapshot.child(friendUserID).child("request_type").getValue().toString();

                                if(request_type.equals("received")) {

                                    // '2' means request received
                                    IM.setRequestState(2);
                                    sendMessageBtn.setText(R.string.accept_request);
                                    sendMessageBtn.setBackgroundResource(R.drawable.accept_request_button_border);
                                    sendMessageBtn.setTextColor(Color.WHITE);

                                    unfriendBtn.setText(R.string.decline_request);
                                    unfriendBtn.setBackgroundResource(R.drawable.decline_request_button_border);
                                    unfriendBtn.setTextColor(Color.WHITE);

                                } else if(request_type.equals("sent")) {

                                    // '1' means request sent
                                    IM.setRequestState(1);
                                    sendMessageBtn.setText(R.string.cancel_request);
                                    sendMessageBtn.setBackgroundResource(R.drawable.cancel_request_button_border);
                                    sendMessageBtn.setTextColor(Color.BLACK);

                                }

                            } else {

                                friendsDatabaseReference.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.hasChild(friendUserID)) {
                                            // '3' means they are friends
                                            IM.setRequestState(3);
                                            sendMessageBtn.setText(R.string.send_message);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setTextColor(Color.WHITE);

                                            unfriendBtn.setText(R.string.unfriend);
                                            unfriendBtn.setBackgroundResource(R.drawable.profile_logout_button_border);
                                            unfriendBtn.setTextColor(Color.BLACK);

                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                    friendNameToolbar.setText(friend_name);
                    friendNameInside.setText(friend_name);
                    Picasso.get().load(friend_name_image).placeholder(R.drawable.ic_profile_image).into(friendImage);
                    statusField.setText(friend_name_status);

                    if(friend_online_status.equals("true")) {

                        String online = "Online";
                        onlineStatus.setText(online);
                        onlineStatus.setTextColor(ContextCompat.getColor(FriendProfilActivity.this, R.color.colorGreen));

                    } else {

                        String offline = "Offline";
                        onlineStatus.setText(offline);
                        onlineStatus.setTextColor(ContextCompat.getColor(FriendProfilActivity.this, R.color.colorRed));

                    }

                    progressDialog.dismiss();

                } else {

                    friend_name = dataSnapshot.child("name").getValue().toString();
                    friend_name_status = dataSnapshot.child("status").getValue().toString();

                    if(dataSnapshot.hasChild("online")) {
                        friend_online_status = dataSnapshot.child("online").getValue().toString();
                    }

                    requestsDatabaseReference.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(dataSnapshot.hasChild(friendUserID)) {

                                String request_type = dataSnapshot.child(friendUserID).child("request_type").getValue().toString();

                                if(request_type.equals("received")) {

                                    // '2' means request received
                                    IM.setRequestState(2);
                                    sendMessageBtn.setText(R.string.accept_request);
                                    sendMessageBtn.setBackgroundResource(R.drawable.accept_request_button_border);
                                    sendMessageBtn.setTextColor(Color.WHITE);

                                    unfriendBtn.setText(R.string.decline_request);
                                    unfriendBtn.setBackgroundResource(R.drawable.decline_request_button_border);
                                    unfriendBtn.setTextColor(Color.WHITE);

                                } else if(request_type.equals("sent")) {

                                    // '1' means request sent
                                    IM.setRequestState(1);
                                    sendMessageBtn.setText(R.string.cancel_request);
                                    sendMessageBtn.setBackgroundResource(R.drawable.cancel_request_button_border);
                                    sendMessageBtn.setTextColor(Color.BLACK);

                                }

                            } else {

                                friendsDatabaseReference.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if(dataSnapshot.hasChild(friendUserID)) {
                                            // '3' means they are friends
                                            IM.setRequestState(3);
                                            sendMessageBtn.setText(R.string.send_message);
                                            sendMessageBtn.setBackgroundResource(R.drawable.profile_addfriend_button_border);
                                            sendMessageBtn.setTextColor(Color.WHITE);

                                            unfriendBtn.setText(R.string.unfriend);
                                            unfriendBtn.setBackgroundResource(R.drawable.profile_logout_button_border);
                                            unfriendBtn.setTextColor(Color.BLACK);

                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                    friendNameToolbar.setText(friend_name);
                    friendNameInside.setText(friend_name);
                    statusField.setText(friend_name_status);

                    if(friend_online_status.equals("true")) {

                        String online = "Online";
                        onlineStatus.setText(online);
                        onlineStatus.setTextColor(ContextCompat.getColor(FriendProfilActivity.this, R.color.colorGreen));

                    } else {

                        String offline = "Offline";
                        onlineStatus.setText(offline);
                        onlineStatus.setTextColor(ContextCompat.getColor(FriendProfilActivity.this, R.color.colorRed));

                    }

                    progressDialog.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void redirectUserToFindFriendsActivity() {
        Intent FindFriendsActivityIntent = new Intent(FriendProfilActivity.this, FindFriendsActivity.class);
        startActivity(FindFriendsActivityIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(currentUserID != null) {
            usersDatabaseReference.child(currentUserID).child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(currentUserID != null && mChatsDBValueEL != null) {
            mChatsRef.child(currentUserID).removeEventListener(mChatsDBValueEL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(currentUserID != null) {
            usersDatabaseReference.child(currentUserID).child("online").setValue("false");
        }
    }
}
