package world.iaomessenger;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    // ChatsFragment view
    View ChatsFragmentView;

    private static final String TAG = "ChatsFragment";
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private String mCurrentUserID;
    private DatabaseReference mDBRef;
    private DatabaseReference mChatsDBRef;
    private DatabaseReference mUsersDBRef;
    private DatabaseReference mMessagesDBRef;
    private DatabaseReference mBadgesRef;
    private DatabaseReference mChatsBadgeRef;

    private RecyclerView mChatsRecyclerView;
    private LinearLayoutManager mLinearLayout;
    private FirebaseRecyclerAdapter<ChatsModule, ChatsFragment.ChatsViewHolder> mChatsRecyclerAdapter;
    private FirebaseRecyclerOptions<ChatsModule> mFirebaseRecyclerOptions;

    private long lastMessageTime;
    private BottomNavigationViewEx bottomNavigationView;
    private IaoMessenger IM;

    public ChatsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ChatsFragmentView = inflater.inflate(R.layout.fragment_chats, container, false);

        IM = ((IaoMessenger) getActivity().getApplication());

        Log.d(TAG, "onCreateView: Started.");

        bottomNavigationView = (BottomNavigationViewEx) ChatsFragmentView.findViewById(R.id.bottom_navigation_view_chats);
        bottomNavigationView.enableAnimation(false);
        bottomNavigationView.enableShiftingMode(0, false);
        bottomNavigationView.enableItemShiftingMode(false);
        Menu menu = bottomNavigationView.getMenu();

        MenuItem menuItem = menu.getItem(0);
        menuItem.setChecked(true);

        // Initialize views
        getViews();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationViewEx.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.ic_chat:
                        mChatsBadgeRef.child(mCurrentUserID).child("value").setValue(0);
                        IM.setChats_badge(0);
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

        mChatsRecyclerView.setHasFixedSize(true);
        mChatsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return ChatsFragmentView;
    }


    private void retrieveChats() {

        if(mCurrentUserID != null) {

            IM.addBadgeAt(0, IM.getChats_badge(), bottomNavigationView);
            IM.addBadgeAt(1, IM.getGroups_badge(), bottomNavigationView);
            IM.addBadgeAt(2, IM.getContacts_badge(), bottomNavigationView);
            IM.addBadgeAt(3, IM.getRequests_badge(), bottomNavigationView);

            mChatsBadgeRef.child(mCurrentUserID).child("value")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(dataSnapshot.getValue() != null) {
                                int count = Integer.parseInt(dataSnapshot.getValue().toString());
                                IM.setChats_badge(count);
                            }


                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            Query chatsQuery = mChatsDBRef.child(mCurrentUserID).orderByChild("time_stamp");

            mFirebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<ChatsModule>()
                    .setQuery(chatsQuery, ChatsModule.class)
                    .build();

            mChatsRecyclerAdapter = new FirebaseRecyclerAdapter<ChatsModule, ChatsViewHolder>(mFirebaseRecyclerOptions) {

                @Override
                protected void onBindViewHolder(@NonNull final ChatsViewHolder holder,
                                                final int position, @NonNull final ChatsModule model) {


                    final String friendUserID = getRef(position).getKey();

                    Log.d(TAG, "onBindViewHolder: " + friendUserID);

                    if (friendUserID != null) {

                        Query lastMessageQuery = mMessagesDBRef.child(mCurrentUserID).child(friendUserID).limitToLast(1);
                        lastMessageQuery.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                                if (dataSnapshot.getValue() != null && dataSnapshot.exists()) {

                                    String lastMessage = dataSnapshot.child("message").getValue().toString();
                                    String from = dataSnapshot.child("from").getValue().toString();

                                    lastMessageTime = (long) dataSnapshot.child("time").getValue();


                                    if (from.equals(mCurrentUserID)) {

                                        if (getContext() != null) {
                                            holder.friendLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNavy));
                                        }

                                        if(holder.friendLastMessageSent != null) {
                                            holder.friendLastMessageSent.setText("You: " + lastMessage);
                                        }
                                        holder.friendLastMessageSent.setVisibility(View.VISIBLE);


                                    } else {

                                        if(holder.friendLastMessageSent != null) {
                                            holder.friendLastMessageSent.setText(lastMessage);
                                            if(getContext() != null) {
                                                holder.friendLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNavy));
                                            }
                                            holder.friendLastMessageSent.setVisibility(View.VISIBLE);
                                        }

                                    }

                                    String seen = model.getSeen();

                                    if(seen != null) {
                                        if (seen.equals("false")) {
//                                    Log.d(TAG, "onChildAdded: SEEN: " + seen);

                                            if (holder.friendLastMessageSent != null && getContext() != null) {
                                                holder.friendLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.startblue));
                                                holder.friendLastMessageSent.setTypeface(null, Typeface.BOLD);
                                            }

                                        } else {

                                            if (holder.friendLastMessageSent != null) {
                                                holder.friendLastMessageSent.setTypeface(holder.friendLastMessageSent.getTypeface(), Typeface.NORMAL);
                                                if(getContext() != null) {
                                                    holder.friendLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNavy));
                                                }
                                            }
                                        }
                                    }

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

                        mUsersDBRef.child(friendUserID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.getValue() != null && dataSnapshot.exists()) {

                                    SimpleDateFormat CD = new SimpleDateFormat("dd/MM/yyyy HH:mm");

                                    if(holder.friendTime != null) {
                                        holder.friendTime.setText(CD.format(new Date(lastMessageTime)));
                                        holder.friendTime.setVisibility(View.VISIBLE);
                                    }

                                    String online_status = dataSnapshot.child("online").getValue().toString();
                                    Log.d(TAG, "onDataChange: ONLINE: " + online_status);

                                    if (dataSnapshot.hasChild("image")) {
                                        String image = dataSnapshot.child("image").getValue().toString();
                                        if(holder.friendName != null) {
                                            Picasso.get().load(image).into(holder.friendImage);
                                        }
                                    }

                                    final String name = dataSnapshot.child("name").getValue().toString();

                                    if(holder.friendName != null) {
                                        holder.friendName.setText(name);
                                    }

                                    if(holder.friendOnlineStatus != null) {

                                        if (online_status.equals("true")) {

                                            holder.friendOnlineStatus.setVisibility(View.VISIBLE);

                                        } else {


                                            holder.friendOnlineStatus.setVisibility(View.INVISIBLE);

                                        }

                                    }


                                    holder.mView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            mChatsBadgeRef.child(mCurrentUserID).child("value").setValue(0);
                                            IM.setChats_badge(0);
                                            Log.d(TAG, "onClick: CHATS BADGE UPDATE -> " + IM.getChats_badge());

                                            mChatsDBRef.child(mCurrentUserID).child(friendUserID).child("seen").setValue("true");

                                            Intent chatActivityIntent = new Intent(getActivity(), ChatsActivity.class);
                                            chatActivityIntent.putExtra("visit_user_id", friendUserID);
                                            chatActivityIntent.putExtra("friend_name", name);
                                            startActivity(chatActivityIntent);

                                            mUsersDBRef.child(mCurrentUserID).child("lastSeen").setValue(ServerValue.TIMESTAMP);

                                        }
                                    });

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
                public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                    View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.activity_chats_display_layout, viewGroup, false);
                    ChatsViewHolder chatsViewHolder = new ChatsViewHolder(view);

                    return chatsViewHolder;
                }

            };

            mChatsRecyclerView.setAdapter(mChatsRecyclerAdapter);
            mChatsRecyclerAdapter.startListening();
        }

    }


    public void getViews() {

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        if(mCurrentUser != null) {

            mCurrentUserID = mCurrentUser.getUid();

        }

        mDBRef = FirebaseDatabase.getInstance().getReference();
        mChatsDBRef = mDBRef.child("Chats");
        mUsersDBRef = mDBRef.child("Users");
        mMessagesDBRef = mDBRef.child("Messages");
        mBadgesRef = mDBRef.child("Badges");
        mChatsBadgeRef = mBadgesRef.child("chats_badges");
        mChatsBadgeRef.keepSynced(true);

        mChatsRecyclerView = (RecyclerView) ChatsFragmentView.findViewById(R.id.chats_recycler_view_list);
        mLinearLayout = new LinearLayoutManager(getContext());
        mLinearLayout.setReverseLayout(true);
        mLinearLayout.setStackFromEnd(true);

        mChatsRecyclerView.setHasFixedSize(true);
        mChatsRecyclerView.setLayoutManager(mLinearLayout);

    }

    @Override
    public void onStart() {
        super.onStart();

        // Retrieve chats from database
        retrieveChats();
    }

    // Chats View holder, class that controls the chat display view per person
    public class ChatsViewHolder extends RecyclerView.ViewHolder {

        CircleImageView friendImage;
        TextView friendName;
        TextView friendLastMessageSent;
        ImageView friendOnlineStatus;
        TextView friendTime;

        View mView;


        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            friendImage = (CircleImageView) itemView.findViewById(R.id.chats_display_profil_image);
            friendName = (TextView) itemView.findViewById(R.id.chats_display_name);
            friendLastMessageSent = (TextView) itemView.findViewById(R.id.chats_display_message_body);
            friendOnlineStatus = (ImageView) itemView.findViewById(R.id.chats_display_online_status);
            friendTime = (TextView) itemView.findViewById(R.id.chats_display_time);
        }

    }

    @Override
    public void onStop() {
        super.onStop();

        if(mChatsRecyclerAdapter != null) {
            mChatsRecyclerAdapter.stopListening();
        }
    }
}