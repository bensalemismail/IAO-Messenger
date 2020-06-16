package world.iaomessenger;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class GroupChatAdapter extends RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder>{

    private static final String TAG = "GroupChatAdapter";

    private List<GroupsModule> messagesList;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;
    private String mCurrentUserID;
    private DatabaseReference mUserRef;

    long mLastMessageTime;

    public GroupChatAdapter(List<GroupsModule> messagesList) {
        this.messagesList = messagesList;
    }


    @NonNull
    @Override
    public GroupChatAdapter.GroupChatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.groups_item_message, viewGroup, false);

        return new GroupChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupChatViewHolder groupChatsViewHolder, int i) {

        mAuth = FirebaseAuth.getInstance();
        mCurrentUser = mAuth.getCurrentUser();

        if(mCurrentUser != null) {

            mCurrentUserID = mCurrentUser.getUid();

        }

        GroupsModule groupChats = messagesList.get(i);
        String from = groupChats.getFrom();
        mLastMessageTime = groupChats.getTime();


        SimpleDateFormat CD = new SimpleDateFormat("HH:mm");


        groupChatsViewHolder.currentUserMessage.setVisibility(View.INVISIBLE);
        groupChatsViewHolder.currentUserTime.setVisibility(View.INVISIBLE);
        groupChatsViewHolder.friendMessage.setVisibility(View.INVISIBLE);
        groupChatsViewHolder.friendName.setVisibility(View.INVISIBLE);
        groupChatsViewHolder.friendImage.setVisibility(View.INVISIBLE);
        groupChatsViewHolder.friendTime.setVisibility(View.INVISIBLE);

        if(mCurrentUserID.equals(from)) {

            groupChatsViewHolder.friendMessage.setVisibility(View.INVISIBLE);
            groupChatsViewHolder.friendName.setVisibility(View.INVISIBLE);
            groupChatsViewHolder.friendImage.setVisibility(View.INVISIBLE);
            groupChatsViewHolder.friendTime.setVisibility(View.INVISIBLE);

            groupChatsViewHolder.currentUserMessage.setText(groupChats.getMessage());
            groupChatsViewHolder.currentUserTime.setText(CD.format(new Date(mLastMessageTime)));
            groupChatsViewHolder.friendTime.setVisibility(View.INVISIBLE);
            groupChatsViewHolder.currentUserTime.setVisibility(View.VISIBLE);
            groupChatsViewHolder.currentUserMessage.setVisibility(View.VISIBLE);

        } else {
            groupChatsViewHolder.currentUserMessage.setVisibility(View.INVISIBLE);
            groupChatsViewHolder.currentUserTime.setVisibility(View.INVISIBLE);

            groupChatsViewHolder.friendTime.setText(CD.format(new Date(mLastMessageTime)));

            groupChatsViewHolder.friendMessage.setText(groupChats.getMessage());

            mUserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(from);
            mUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.hasChild("image")) {

                        String friendImage = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(friendImage).placeholder(R.drawable.ic_user_display_profile_image).into(groupChatsViewHolder.friendImage);

                    }

                    if(dataSnapshot.hasChild("name")) {
                        String friendName = dataSnapshot.child("name").getValue().toString();
                        Log.d(TAG, "onDataChange: NAME" + friendName);
                        groupChatsViewHolder.friendName.setText(friendName);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            groupChatsViewHolder.friendImage.setVisibility(View.VISIBLE);
            groupChatsViewHolder.friendTime.setVisibility(View.VISIBLE);
            groupChatsViewHolder.friendMessage.setVisibility(View.VISIBLE);
            groupChatsViewHolder.friendName.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    public class GroupChatViewHolder extends RecyclerView.ViewHolder {

        public TextView friendMessage;
        public TextView currentUserMessage;
        public CircleImageView friendImage;
        public TextView friendTime;
        public TextView friendName;
        public TextView currentUserTime;

        public String time;


        public GroupChatViewHolder(@NonNull View itemView) {
            super(itemView);

            friendMessage = (TextView) itemView.findViewById(R.id.groups_text_message_body_received);
            friendImage = (CircleImageView) itemView.findViewById(R.id.groups_image_message_profile_received);
            currentUserMessage = (TextView) itemView.findViewById(R.id.groups_text_message_body_sent);
            friendTime = (TextView) itemView.findViewById(R.id.groups_text_message_time_received);
            friendName = (TextView) itemView.findViewById(R.id.groups_text_message_name_received);
            currentUserTime = (TextView) itemView.findViewById(R.id.groups_text_message_time_sent);
        }
    }
}
