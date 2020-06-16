package world.iaomessenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsFragment extends Fragment {
    
    // ContactsFragment view
    private View ContactsFragmentView;
    private RecyclerView contactsRecyclerView;
    private ImageView noContacts;
    private DatabaseReference mFriendsDBRef;
    private DatabaseReference mDBRef;
    private DatabaseReference mContactsBadge;
    private FirebaseRecyclerAdapter<FriendsModule, ContactsViewHolder> contactsRecyclerAdapter;
    private FirebaseRecyclerOptions<FriendsModule> firebaseRecyclerOptions;
    private DatabaseReference usersDatabaseReference;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserID;
    private BottomNavigationViewEx bottomNavigationView;

    private static final String TAG = "ContactsFragment";
    private FirebaseAuth user;

    private IaoMessenger IM;
    private DatabaseReference mBadgesRef;
    private ValueEventListener mChatsDBValueEL;
    private DatabaseReference mChatsDatabaseReference;

    public ContactsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        ContactsFragmentView = inflater.inflate(R.layout.fragment_contacts, container, false);

        IM = ((IaoMessenger) getActivity().getApplication());

        Log.d(TAG, "onCreateView: Started.");

        bottomNavigationView = (BottomNavigationViewEx) ContactsFragmentView.findViewById(R.id.bottom_navigation_view_contacts);
        bottomNavigationView.enableAnimation(false);
        bottomNavigationView.enableItemShiftingMode(false);

        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(2);
        menuItem.setChecked(true);

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

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currentUserID = currentUser.getUid();

        noContacts = (ImageView) ContactsFragmentView.findViewById(R.id.no_contacts_friends);

        mDBRef = FirebaseDatabase.getInstance().getReference();
        mFriendsDBRef = mDBRef.child("Friends").child(currentUserID);
        mFriendsDBRef.keepSynced(true);
        contactsRecyclerView = (RecyclerView) ContactsFragmentView.findViewById(R.id.contacts_recycler_view_list);
        contactsRecyclerView.setHasFixedSize(true);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        usersDatabaseReference = mDBRef.child("Users");
        usersDatabaseReference.keepSynced(true);
        mBadgesRef = mDBRef.child("Badges");
        mContactsBadge = mBadgesRef.child("contacts_badges");
        mContactsBadge.keepSynced(true);
        mChatsDatabaseReference = mDBRef.child("Chats");

        return ContactsFragmentView;
    }


    @Override
    public void onStart() {
        super.onStart();

        if(currentUserID != null) {

            IM.addBadgeAt(0, IM.getChats_badge(), bottomNavigationView);
            IM.addBadgeAt(1, IM.getGroups_badge(), bottomNavigationView);
            IM.addBadgeAt(2, IM.getContacts_badge(), bottomNavigationView);
            IM.addBadgeAt(3, IM.getRequests_badge(), bottomNavigationView);

            mFriendsDBRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(!dataSnapshot.exists()) {

                        noContacts.setVisibility(View.VISIBLE);

                    } else {

                        noContacts.setVisibility(View.INVISIBLE);

                        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<FriendsModule>()
                                .setQuery(mFriendsDBRef, FriendsModule.class)
                                .build();

                        contactsRecyclerAdapter = new FirebaseRecyclerAdapter<FriendsModule, ContactsViewHolder>(firebaseRecyclerOptions) {

                            @Override
                            protected void onBindViewHolder(@NonNull final ContactsViewHolder holder, final int position, @NonNull FriendsModule model) {

                                final String date = model.getDate();
                                final String list_user_id = getRef(position).getKey();

                                Log.d(TAG, "onBindViewHolder: " + list_user_id);

                                usersDatabaseReference.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        if (dataSnapshot.getValue() != null) {

                                            String online_status = dataSnapshot.child("online").getValue().toString();
                                            Log.d(TAG, "onDataChange: ONLINE: " + online_status);
                                            if (dataSnapshot.hasChild("image")) {
                                                String image = dataSnapshot.child("image").getValue().toString();
                                                Picasso.get().load(image).placeholder(R.drawable.ic_user_display_profile_image).into(holder.image);
                                            }

                                            final String name = dataSnapshot.child("name").getValue().toString();
                                            String status = dataSnapshot.child("status").getValue().toString();

                                            holder.name.setText(name);
                                            holder.status.setText(status);


                                            Log.d(TAG, "onBindViewHolder: " + online_status);

                                            if (online_status.equals("true")) {

                                                holder.online.setVisibility(View.VISIBLE);

                                            } else {

                                                holder.online.setVisibility(View.INVISIBLE);

                                            }


                                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {

                                                    CharSequence options[] = new CharSequence[]{"Open Profile", "Send Message"};
                                                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                    builder.setTitle("Select an option");
                                                    builder.setItems(options, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {

                                                            if (which == 0) {

                                                                Intent friendProfileActiity = new Intent(getActivity(), FriendProfilActivity.class);
                                                                friendProfileActiity.putExtra("visit_user_id", list_user_id);
                                                                startActivity(friendProfileActiity);

                                                            } else {

                                                                // Chats Database event listener
                                                                mChatsDBValueEL = new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                        mChatsDatabaseReference.child(currentUserID).child(list_user_id).child("time_stamp")
                                                                                .setValue(ServerValue.TIMESTAMP).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                            @Override
                                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                                if (task.isSuccessful()) {

                                                                                    mChatsDatabaseReference.child(list_user_id).child(currentUserID).child("time_stamp")
                                                                                            .setValue(ServerValue.TIMESTAMP);

                                                                                }

                                                                            }
                                                                        });
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                };

                                                                // Store chat in database
                                                                mChatsDatabaseReference.child(currentUserID).addValueEventListener(mChatsDBValueEL);

                                                                Intent chatActivityIntent = new Intent(getActivity(), ChatsActivity.class);
                                                                chatActivityIntent.putExtra("visit_user_id", list_user_id);
                                                                chatActivityIntent.putExtra("friend_name", name);
                                                                startActivity(chatActivityIntent);

                                                            }

                                                        }
                                                    });

                                                    builder.show();

                                                }
                                            });

                                        }

                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }

                            @NonNull
                            @Override
                            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.find_friends_display_layout, viewGroup, false);
                                ContactsViewHolder contactsViewHolder = new ContactsViewHolder(view);

                                return contactsViewHolder;
                            }

                        };

                        contactsRecyclerView.setAdapter(contactsRecyclerAdapter);
                        contactsRecyclerAdapter.startListening();

                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }


    @Override
    public void onStop() {
        super.onStop();

        if(contactsRecyclerAdapter != null) {
            contactsRecyclerAdapter.stopListening();
        }

        if(currentUserID != null && mChatsDBValueEL != null) {
            mChatsDatabaseReference.child(currentUserID).removeEventListener(mChatsDBValueEL);
        }
    }


    // Contacts view holder class to access users display layout fields
    public static class ContactsViewHolder extends RecyclerView.ViewHolder {

        TextView name, status;
        CircleImageView image;
        ImageView online;
        View mView;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            name = mView.findViewById(R.id.find_friends_username);
            status = mView.findViewById(R.id.find_friends_status);
            image = mView.findViewById(R.id.find_friends_profil_image);
            online = mView.findViewById(R.id.find_friends_online_status);
        }
    }



}