package world.iaomessenger;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupsFragment extends Fragment {

    // Group Fragment view
    private View mGroupFragmentView;

    private static final String TAG = "GroupsFragment";
    private FirebaseAuth mAuth;
    private FloatingActionButton mFab;
    private RelativeLayout mRelativeLayout;
    private CoordinatorLayout mCoordinatorLayoutGrpoups;
    private FirebaseUser mCurrentUser;
    private BottomNavigationViewEx mBottomNavigationView;

    private DatabaseReference mDBRef;
    private DatabaseReference mGroupsRef;
    private DatabaseReference mGroupMessagesRef;
    private DatabaseReference mUsersRef;
    private DatabaseReference mGroupsBadge;
    private String mCurrentUserID;

    private RecyclerView mGroupsRecyclerView;
    private LinearLayoutManager mLinearLayout;
    private FirebaseRecyclerAdapter<GroupsModule, GroupsViewHolder> mGroupsRecyclerAdapter;
    private FirebaseRecyclerOptions<GroupsModule> mFirebaseRecyclerOptions;
    private long messageTime;
    private String groupKey;

    private IaoMessenger IM;
    private DatabaseReference mBadgesRef;

    public GroupsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mGroupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        IM = ((IaoMessenger) getActivity().getApplication());

        getViews();
        setBottomNavigationView();

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createNewGroup();
            }
        });


        return mGroupFragmentView;
    }

    private void retrieveGroupsFromDatabase() {

        if(mCurrentUserID != null) {

            mGroupsBadge.child(mCurrentUserID).child("value")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if(dataSnapshot.getValue() != null){

                                int count = Integer.parseInt(dataSnapshot.getValue().toString());

                                IM.setGroups_badge(count);
                            }

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

            mFirebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<GroupsModule>()
                    .setQuery(mGroupsRef, GroupsModule.class)
                    .build();

            mGroupsRecyclerAdapter = new FirebaseRecyclerAdapter<GroupsModule, GroupsViewHolder>(mFirebaseRecyclerOptions) {
                @Override
                protected void onBindViewHolder(@NonNull final GroupsViewHolder holder, int position, @NonNull GroupsModule model) {

                    final String groupID = getRef(position).getKey();
                    Log.d(TAG, "onBindViewHolder: Group Key => " + groupID);
                    final String groupName = model.getName();

                    if(groupID != null && groupName != null) {

                        if(holder.groupName != null) {
                            holder.groupName.setText(groupName);
                        }

                        holder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                Intent groupChatActivityIntent = new Intent(getActivity(), GroupChatActivity.class);
                                groupChatActivityIntent.putExtra("groupName", groupName);
                                groupChatActivityIntent.putExtra("groupID", groupID);
                                startActivity(groupChatActivityIntent);

                                Log.d(TAG, "onClick: Group Name -> " + groupName);

//                                mGroupsRef.child(groupID).child("time").setValue(ServerValue.TIMESTAMP);

                            }
                        });

                        Query lastMessageQuery = mGroupMessagesRef.child(groupID).limitToLast(1);
                        lastMessageQuery.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                                if(dataSnapshot.getValue() != null && dataSnapshot.exists()) {

                                    Log.d(TAG, "onChildAdded: DATASNAPSHOT _> " + dataSnapshot.getValue().toString());

                                    String lastMessage = dataSnapshot.child("message").getValue().toString();
                                    Log.d(TAG, "onChildAdded: Last Message -> " + lastMessage);
                                    String from = dataSnapshot.child("from").getValue().toString();
                                    Log.d(TAG, "onChildAdded: From -> " + from);

                                    if(dataSnapshot.hasChild("time")) {

                                        messageTime = (long) dataSnapshot.child("time").getValue();
                                        SimpleDateFormat CD = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                                        holder.groupLastMessageTime.setText(CD.format(new Date(messageTime)));
                                        holder.groupLastMessageTime.setVisibility(View.VISIBLE);

                                    }

                                    Log.d(TAG, "onChildAdded: " + from);

                                    if(from.equals(mCurrentUserID)) {

                                        holder.groupLastMessageSent.setText("You: " + lastMessage);

                                        if (getContext() != null) {
                                            holder.groupLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNavy));
                                        }

                                        holder.groupLastMessageSent.setVisibility(View.VISIBLE);

                                    } else {

                                        holder.groupLastMessageSent.setText(lastMessage);
                                        holder.groupLastMessageSent.setTextColor(ContextCompat.getColor(getContext(), R.color.colorNavy));
                                        holder.groupLastMessageSent.setVisibility(View.VISIBLE);

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

                    }

                }

                @NonNull
                @Override
                public GroupsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                    View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.groups_fragments_chats_display, viewGroup, false);
                    GroupsViewHolder groupsViewHolder = new GroupsViewHolder(view);

                    return groupsViewHolder;
                }
            };

            mGroupsRecyclerView.setAdapter(mGroupsRecyclerAdapter);
            mGroupsRecyclerAdapter.startListening();

        }

    }

    private void redirectUserToGroupChatActivity(String currentGroupName) {
        Intent groupsActivityIntent = new Intent(getContext(), GroupChatActivity.class);
        groupsActivityIntent.putExtra("groupName", currentGroupName);
        startActivity(groupsActivityIntent);
    }


    public void getViews() {

        mRelativeLayout = (RelativeLayout) mGroupFragmentView.findViewById(R.id.middle_rl_groups);
        mCoordinatorLayoutGrpoups = (CoordinatorLayout) mGroupFragmentView.findViewById(R.id.coordinatorLayoutGroups);
        mFab = (FloatingActionButton) mGroupFragmentView.findViewById(R.id.fab_groups);
        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        if(mCurrentUser != null) {

            mCurrentUserID = mCurrentUser.getUid();

        }

        mDBRef = FirebaseDatabase.getInstance().getReference();
        mGroupsRef = mDBRef.child("Groups");
        mGroupMessagesRef = mDBRef.child("Groups Messages");
        mUsersRef = mDBRef.child("Users");
        mBadgesRef = mDBRef.child("Badges");
        mGroupsBadge = mBadgesRef.child("groups_badges");
        mGroupsBadge.keepSynced(true);

        // Recycler
        mGroupsRecyclerView = (RecyclerView) mGroupFragmentView.findViewById(R.id.groups_fragment_recycler_view_list);

        mLinearLayout = new LinearLayoutManager(getContext());
        mLinearLayout.setReverseLayout(true);
        mLinearLayout.setStackFromEnd(true);
        mGroupsRecyclerView.setHasFixedSize(true);

        mGroupsRecyclerView.setLayoutManager(mLinearLayout);

    }

    public void setBottomNavigationView() {

        mBottomNavigationView = (BottomNavigationViewEx) mGroupFragmentView.findViewById(R.id.bottom_navigation_view_groups);
        mBottomNavigationView.enableAnimation(false);
        mBottomNavigationView.enableItemShiftingMode(false);
        Menu menu = mBottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);

        IM.addBadgeAt(0, IM.getChats_badge(), mBottomNavigationView);
        IM.addBadgeAt(1, IM.getGroups_badge(), mBottomNavigationView);
        IM.addBadgeAt(2, IM.getContacts_badge(), mBottomNavigationView);
        IM.addBadgeAt(3, IM.getRequests_badge(), mBottomNavigationView);

        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationViewEx.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.ic_chat:
                        ((MainActivity) getActivity()).setViewPager(0);
                        break;

                    case R.id.ic_groups:
                        mGroupsBadge.child(mCurrentUserID).child("value").setValue(0);
                        IM.setGroups_badge(0);
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
    }


    // Groups View holder, class that controls the groups display
    public class GroupsViewHolder extends RecyclerView.ViewHolder {

        TextView groupName;
        TextView groupLastMessageSent;
        TextView groupLastMessageTime;

        View mView;


        public GroupsViewHolder(@NonNull View itemView) {

            super(itemView);

            mView = itemView;
            groupName = (TextView) itemView.findViewById(R.id.groups_display_name);
            groupLastMessageSent = (TextView) itemView.findViewById(R.id.groups_display_message_body);
            groupLastMessageTime = (TextView) itemView.findViewById(R.id.groups_display_time);

        }

    }

    //TODO : Not working for now.
    private void createNewGroup() {
        final AlertDialog.Builder groupBuilder = new AlertDialog.Builder(getActivity(), R.style.MyDialogTheme);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.create_group_dialog_box_layout, null);

        TextView title = (TextView) view.findViewById(R.id.create_group_text_view);
        final TextInputEditText groupNameField = (TextInputEditText) view.findViewById(R.id.add_group_edit_text);
        /*
            final EditText groupNameField = new EditText(MainActivity.this);
            groupNameField.setHint("e.g: IAO Group");
        */
        groupBuilder.setView(view);

        groupBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                groupNameField.findFocus();
                final String groupName = groupNameField.getText().toString();

                if (TextUtils.isEmpty(groupName)) {

                    Snackbar snackbarGroupNameFieldError = Snackbar.make(mCoordinatorLayoutGrpoups, "You must enter a group name", Snackbar.LENGTH_LONG);
                    snackbarGroupNameFieldError.setAction("OK.", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
                    snackbarGroupNameFieldError.setActionTextColor(Color.YELLOW);
                    snackbarGroupNameFieldError.show();

                } else {

                    DatabaseReference groupNameDBRef = mGroupsRef.push();
                    groupKey = groupNameDBRef.getKey();

                    // Groups node is created, so is the groups names.
                    mGroupsRef.child(groupKey).child("name").setValue(groupName)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Snackbar snackbarGroupNameFieldError = Snackbar.make(mCoordinatorLayoutGrpoups, groupName + " is created successfully.", Snackbar.LENGTH_LONG);
                                        snackbarGroupNameFieldError.show();
                                    }
                                }
                            });
                }
            }
        });

        groupBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        groupBuilder.show();
    }

    @Override
    public void onStop() {
        super.onStop();

        if(mGroupsRecyclerAdapter != null) {
            mGroupsRecyclerAdapter.stopListening();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        retrieveGroupsFromDatabase();
    }
}