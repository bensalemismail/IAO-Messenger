package world.iaomessenger;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestsFragment extends Fragment {

    private static final String TAG = "RequestsFragment";

    // RequestsFragment view
    private View RequestsFragmentView;
    private RecyclerView requestRecyclerView;
    private DatabaseReference requestsDatabaseReference;
    private FirebaseRecyclerAdapter<RequestsModule, RequestsViewHolder> requestsRecyclerAdapter;
    private FirebaseRecyclerOptions<RequestsModule> firebaseRecyclerOptions;
    private DatabaseReference usersDatabaseReference;
    private DatabaseReference friendsDatabaseReference;
    private DatabaseReference mChatsRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mDBRef;
    private DatabaseReference mRequestsBadge;
    private FirebaseAuth mAuth;

    private FirebaseUser currentUser;
    private String currentUserID;

    private int request_state;
    private IaoMessenger IM;

    private ImageView mSadCloud;
    private BottomNavigationViewEx bottomNavigationView;
    private DatabaseReference mBadgesRef;

    public RequestsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        RequestsFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        IM = ((IaoMessenger) getActivity().getApplication());

        Log.d(TAG, "onCreateView: Started.");

        bottomNavigationView = (BottomNavigationViewEx) RequestsFragmentView.findViewById(R.id.bottom_navigation_view_requests);
        bottomNavigationView.enableAnimation(false);
        bottomNavigationView.enableItemShiftingMode(false);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(3);
        menuItem.setChecked(true);

        IM.addBadgeAt(0, IM.getChats_badge(), bottomNavigationView);
        IM.addBadgeAt(1, IM.getGroups_badge(), bottomNavigationView);
        IM.addBadgeAt(2, IM.getContacts_badge(), bottomNavigationView);
        IM.addBadgeAt(3, IM.getRequests_badge(), bottomNavigationView);

        // Initialize Fields
        getViews();


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationViewEx.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_chat:

                        ((MainActivity) getActivity()).setViewPager(0);
                        break;

                    case R.id.ic_groups:
                        ((MainActivity) getActivity()).setViewPager(1);
                        break;

                    case R.id.ic_contacts:
                        ((MainActivity) getActivity()).setViewPager(2);
                        break;

                    case R.id.ic_requests:
                        ((MainActivity) getActivity()).setViewPager(3);
                        break;
                }
                return false;
            }
        });



        requestRecyclerView.setHasFixedSize(true);
        requestRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return RequestsFragmentView;
    }

    @Override
    public void onStart() {

        super.onStart();

        if(currentUserID != null) {

            mRequestsBadge.child(currentUserID).child("value")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(dataSnapshot.exists() && dataSnapshot.getValue() != null) {
                                int count = Integer.parseInt(dataSnapshot.getValue().toString());

                                IM.setRequests_badge(count);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            requestsDatabaseReference.child(currentUserID).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(!dataSnapshot.exists()) {

                        Log.d(TAG, "onDataChange: NO CHILD");
                        mSadCloud.setVisibility(View.VISIBLE);

                    } else {

                        mSadCloud.setVisibility(View.INVISIBLE);

                        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<RequestsModule>()
                                .setQuery(requestsDatabaseReference.child(currentUserID), RequestsModule.class)
                                .build();

                        requestsRecyclerAdapter = new FirebaseRecyclerAdapter<RequestsModule, RequestsViewHolder>(firebaseRecyclerOptions) {
                            @Override
                            protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, final int position, @NonNull RequestsModule model) {

                                final String userListID = getRef(position).getKey();

                                if(userListID != null) {

                                    DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();

                                    getTypeRef.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                            if (dataSnapshot.getValue() != null) {
                                                String request_type = dataSnapshot.getValue().toString();

                                                if (request_type.equals("received")) {

                                                    holder.acceptBtn.setVisibility(View.VISIBLE);
                                                    holder.declineBtn.setVisibility(View.VISIBLE);

                                                    usersDatabaseReference.child(userListID).addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                            if (dataSnapshot.hasChild("image")) {
                                                                String image = dataSnapshot.child("image").getValue().toString();
                                                                Picasso.get().load(image).placeholder(R.drawable.ic_user_display_profile_image).into(holder.image);
                                                            }

                                                            if (dataSnapshot.hasChild("name")) {

                                                                String name = dataSnapshot.child("name").getValue().toString();
                                                                String requestTemplate = name + " has requested you as a friend.";
                                                                Log.d(TAG, "onBindViewHolder: FRIEND/RECEIVER ID: " + userListID);
                                                                holder.name.setText(requestTemplate);
                                                            }

                                                            holder.acceptBtn.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {

                                                                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());

                                                                    HashMap<String, String> friendMap = new HashMap<>();
                                                                    friendMap.put("date", currentDate);

                                                                    friendsDatabaseReference.child(currentUserID).child(userListID)
                                                                            .setValue(friendMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if (task.isSuccessful()) {

                                                                                HashMap<String, String> userMap = new HashMap<>();
                                                                                userMap.put("date", currentDate);

                                                                                friendsDatabaseReference.child(userListID).child(currentUserID)
                                                                                        .setValue(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                                        if (task.isSuccessful()) {

                                                                                            requestsDatabaseReference.child(currentUserID).child(userListID)
                                                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                                    if (task.isSuccessful()) {

                                                                                                        requestsDatabaseReference.child(userListID).child(currentUserID)
                                                                                                                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                            @Override
                                                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                                                if (task.isSuccessful()) {

                                                                                                                    final DatabaseReference contactsBadgeRef = mDBRef.child("Badges").child("contacts_badges");
                                                                                                                    contactsBadgeRef.child(userListID).child("value").addListenerForSingleValueEvent(new ValueEventListener() {

                                                                                                                        @Override
                                                                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                                                                            if(dataSnapshot.getValue() != null) {

                                                                                                                                int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                                                                                                                IM.setContacts_badge(count);
                                                                                                                                contactsBadgeRef.child(userListID).child("value").setValue(count + 1);
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
                                                                                                                                contactsBadgeRef.child(currentUserID).child("value").setValue(count + 1);
                                                                                                                            }

                                                                                                                        }

                                                                                                                        @Override
                                                                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                                                        }
                                                                                                                    });

                                                                                                                    IM.setRequestState(3);

                                                                                                                    final DatabaseReference requestsBadgeRef = mDBRef.child("Badges").child("requests_badges")
                                                                                                                            .child(currentUserID).child("value");

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
                                                                                                                }

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


                                                                }
                                                            });

                                                            holder.declineBtn.setOnClickListener(new View.OnClickListener() {
                                                                @Override
                                                                public void onClick(View v) {

                                                                    requestsDatabaseReference.child(currentUserID).child(userListID)
                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                            if (task.isSuccessful()) {

                                                                                requestsDatabaseReference.child(userListID).child(currentUserID)
                                                                                        .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task) {

                                                                                        if (task.isSuccessful()) {

                                                                                            IM.setRequestState(0);

                                                                                            final DatabaseReference requestsBadgeRef = mDBRef.child("Badges").child("requests_badges").child(currentUserID).child("value");

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

                                                                                            mChatsRef.child(currentUserID).child(userListID)
                                                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                @Override
                                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                                    mChatsRef.child(userListID).child(currentUserID)
                                                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onComplete(@NonNull Task<Void> task) {

                                                                                                            mMessagesRef.child(currentUserID).child(userListID)
                                                                                                                    .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                @Override
                                                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                                                    mMessagesRef.child(userListID).child(currentUserID)
                                                                                                                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                        @Override
                                                                                                                        public void onComplete(@NonNull Task<Void> task) {


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
                                                            });
                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });

                                                } else {

                                                    holder.acceptBtn.setVisibility(View.VISIBLE);
                                                    holder.declineBtn.setVisibility(View.INVISIBLE);

                                                    String sent = "REQUEST SENT";
                                                    holder.acceptBtn.setText(sent);
                                                    holder.acceptBtn.setTextColor(Color.WHITE);
                                                    holder.acceptBtn.setClickable(false);
                                                    holder.acceptBtn.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.startblue));


                                                    usersDatabaseReference.child(userListID).addValueEventListener(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                            if (dataSnapshot.hasChild("image")) {
                                                                String image = dataSnapshot.child("image").getValue().toString();
                                                                Picasso.get().load(image).placeholder(R.drawable.ic_user_display_profile_image).into(holder.image);
                                                            }

                                                            if (dataSnapshot.hasChild("name")) {

                                                                String name = dataSnapshot.child("name").getValue().toString();
                                                                String requestTemplate = "Waiting for " + name + " to accept.";
                                                                Log.d(TAG, "onBindViewHolder: FRIEND/RECEIVER ID: " + userListID);
                                                                holder.name.setText(requestTemplate);
                                                            }

                                                        }

                                                        @Override
                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                        }
                                                    });

                                                }

                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });



                                }

                            }

                            @NonNull
                            @Override
                            public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.friend_request_display, viewGroup, false);
                                RequestsViewHolder requestsViewHolder = new RequestsViewHolder(view);

                                return requestsViewHolder;
                            }
                        };

                        requestRecyclerView.setAdapter(requestsRecyclerAdapter);
                        requestsRecyclerAdapter.startListening();

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    // Requests view holder class to access users display layout fields
    public static class RequestsViewHolder extends RecyclerView.ViewHolder {

        TextView name;
        CircleImageView image;
        Button acceptBtn, declineBtn;
        View mView;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            name = mView.findViewById(R.id.friend_request_msg);
            image = mView.findViewById(R.id.friend_profile_avatar);
            acceptBtn = mView.findViewById(R.id.requests_accept_button);
            declineBtn = mView.findViewById(R.id.requests_decline_button);
        }
    }

    // get field contents by their IDs

    public void getViews() {

        requestRecyclerView = (RecyclerView) RequestsFragmentView.findViewById(R.id.requests_list_rec_view);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currentUserID = currentUser.getUid();

        Log.d(TAG, "getViews: " + currentUserID);

        mDBRef = FirebaseDatabase.getInstance().getReference();

        requestsDatabaseReference = mDBRef.child("Requests");
        requestsDatabaseReference.keepSynced(true);
        usersDatabaseReference = mDBRef.child("Users");
        usersDatabaseReference.keepSynced(true);
        friendsDatabaseReference = mDBRef.child("Friends");
        mChatsRef = mDBRef.child("Chats");
        mMessagesRef = mDBRef.child("Messages");
        mBadgesRef = mDBRef.child("Badges");
        mRequestsBadge = mBadgesRef.child("requests_badges");
        mRequestsBadge.keepSynced(true);

        mSadCloud = (ImageView) RequestsFragmentView.findViewById(R.id.requests_no_requests_sad_clound);

    }

    @Override
    public void onStop() {
        super.onStop();

        if(requestsRecyclerAdapter != null) {
            requestsRecyclerAdapter.stopListening();
        }
    }
}