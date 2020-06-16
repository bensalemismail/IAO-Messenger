package world.iaomessenger;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatsViewHolder>{

    private static final String TAG = "ChatsAdapter";
    
    private List<ChatsModule> messagesList;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserID;
    private DatabaseReference userRef;

    long lastMessageTime;

    public ChatsAdapter(List<ChatsModule> messagesList) {
        this.messagesList = messagesList;
    }


    @NonNull
    @Override
    public ChatsAdapter.ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_message, viewGroup, false);

        return new ChatsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatsAdapter.ChatsViewHolder chatsViewHolder, int i) {

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if(currentUser != null) {

            currentUserID = currentUser.getUid();

        }

        ChatsModule chats = messagesList.get(i);
        String from = chats.getFrom();
        lastMessageTime = chats.getTime();


        SimpleDateFormat CD = new SimpleDateFormat("HH:mm");


        chatsViewHolder.currentUserMessage.setVisibility(View.INVISIBLE);
        chatsViewHolder.friendMessage.setVisibility(View.INVISIBLE);
        chatsViewHolder.friendName.setVisibility(View.INVISIBLE);
        chatsViewHolder.friendImage.setVisibility(View.INVISIBLE);
        chatsViewHolder.friendTime.setVisibility(View.INVISIBLE);
        chatsViewHolder.currentUserTime.setVisibility(View.INVISIBLE);

        if(currentUserID.equals(from)) {

            chatsViewHolder.friendMessage.setVisibility(View.INVISIBLE);
            chatsViewHolder.friendName.setVisibility(View.INVISIBLE);
            chatsViewHolder.friendImage.setVisibility(View.INVISIBLE);
            chatsViewHolder.friendTime.setVisibility(View.INVISIBLE);

            chatsViewHolder.currentUserMessage.setText(chats.getMessage());
            chatsViewHolder.currentUserTime.setText(CD.format(new Date(lastMessageTime)));
            chatsViewHolder.friendTime.setVisibility(View.INVISIBLE);
            chatsViewHolder.currentUserTime.setVisibility(View.VISIBLE);
            chatsViewHolder.currentUserMessage.setVisibility(View.VISIBLE);

        } else {
            chatsViewHolder.currentUserMessage.setVisibility(View.INVISIBLE);
            chatsViewHolder.currentUserTime.setVisibility(View.INVISIBLE);

            chatsViewHolder.friendTime.setText(CD.format(new Date(lastMessageTime)));

            chatsViewHolder.friendMessage.setText(chats.getMessage());

            userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(from);
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if(dataSnapshot.hasChild("image")) {

                        String friendImage = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(friendImage).placeholder(R.drawable.ic_user_display_profile_image).into(chatsViewHolder.friendImage);

                    }

                    if(dataSnapshot.hasChild("name")) {
                        String friendName = dataSnapshot.child("name").getValue().toString();
                        Log.d(TAG, "onDataChange: NAME" + friendName);
                        chatsViewHolder.friendName.setText(friendName);
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            chatsViewHolder.friendImage.setVisibility(View.VISIBLE);
            chatsViewHolder.friendTime.setVisibility(View.VISIBLE);
            chatsViewHolder.friendMessage.setVisibility(View.VISIBLE);
            chatsViewHolder.friendName.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    public class ChatsViewHolder extends RecyclerView.ViewHolder {

        public TextView friendMessage;
        public TextView currentUserMessage;
        public CircleImageView friendImage;
        public TextView friendTime;
        public TextView friendName;
        public TextView currentUserTime;

        public String time;


        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            friendMessage = (TextView) itemView.findViewById(R.id.text_message_body_received);
            friendImage = (CircleImageView) itemView.findViewById(R.id.image_message_profile_received);
            currentUserMessage = (TextView) itemView.findViewById(R.id.text_message_body_sent);
            friendTime = (TextView) itemView.findViewById(R.id.text_message_time_received);
            friendName = (TextView) itemView.findViewById(R.id.text_message_name_received);
            currentUserTime = (TextView) itemView.findViewById(R.id.text_message_time_sent);
        }
    }
}
