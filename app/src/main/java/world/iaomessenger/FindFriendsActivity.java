package world.iaomessenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {

    private static final String TAG = "FindFriendsActivity";
    
    private RecyclerView findFriendsRecyclerView;
    private DatabaseReference databaseReference;
    private FirebaseRecyclerAdapter<ContactsModule, FindFriendsViewHolder> findFriendsRecyclerAdapter;
    private FirebaseRecyclerOptions<ContactsModule> firebaseRecyclerOptions;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        Log.d(TAG, "onCreate: ");

        Toolbar toolbar = findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        if(currentUser != null) {
            currentUserID = currentUser.getUid();
        }


        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        databaseReference.keepSynced(true);

        getSupportActionBar().setTitle("Find Friends");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        findFriendsRecyclerView = (RecyclerView) findViewById(R.id.find_friends_recycler_view_list);
        findFriendsRecyclerView.setHasFixedSize(true);
        findFriendsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        databaseReference.child(currentUserID).child("online").setValue("true");
        firebaseRecyclerOptions = new FirebaseRecyclerOptions.Builder<ContactsModule>()
                                                             .setQuery(databaseReference, ContactsModule.class)
                                                             .build();

        findFriendsRecyclerAdapter = new FirebaseRecyclerAdapter<ContactsModule, FindFriendsViewHolder>(firebaseRecyclerOptions) {
            @Override
            protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, final int position, @NonNull ContactsModule model) {

                String userID = model.getUid();

                Log.d(TAG, "onBindViewHolder: " + userID + " CURRENT: " + currentUserID);

                if(currentUserID != null) {

                    if(userID != null) {
                        if (!userID.equals(currentUserID)) {
                            holder.name.setText(model.getName());
                            holder.status.setText(model.getStatus());
                            Log.d(TAG, "onBindViewHolder: IMAGE: " + holder.image);
                            Picasso.get().load(model.getImage()).placeholder(R.drawable.ic_user_display_profile_image).into(holder.image);

                            holder.mView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    String visit_user_id = getRef(position).getKey();
                                    Intent friendProfileActiity = new Intent(FindFriendsActivity.this, FriendProfilActivity.class);
                                    friendProfileActiity.putExtra("visit_user_id", visit_user_id);
                                    startActivity(friendProfileActiity);
                                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                                }
                            });

                        } else {

                            holder.YOU.setVisibility(View.VISIBLE);
                            holder.name.setText(model.getName());
                            holder.status.setText(model.getStatus());
                            Picasso.get().load(model.getImage()).placeholder(R.drawable.ic_user_display_profile_image).into(holder.image);
                            holder.relMain.setBackgroundColor(ContextCompat.getColor(FindFriendsActivity.this, R.color.colorBlueStar));

                        }
                    }
                }

            }

            @NonNull
            @Override
            public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.find_friends_display_layout, viewGroup, false);
                FindFriendsViewHolder findFriendsViewHolder = new FindFriendsViewHolder(view);

                return findFriendsViewHolder;
            }

        };

        findFriendsRecyclerView.setAdapter(findFriendsRecyclerAdapter);
        findFriendsRecyclerAdapter.startListening();
    }

    // FindFriends view holder class to access users display layout fields
    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {

        TextView name, status;
        CircleImageView image;
        View mView;
        RelativeLayout relMain;
        TextView YOU;

        public FindFriendsViewHolder(@NonNull View itemView) {

            super(itemView);

            mView = itemView;
            name = mView.findViewById(R.id.find_friends_username);
            status = mView.findViewById(R.id.find_friends_status);
            image = mView.findViewById(R.id.find_friends_profil_image);
            relMain = mView.findViewById(R.id.find_friends_rel_main);
            YOU = mView.findViewById(R.id.find_friend_you);
        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        findFriendsRecyclerAdapter.stopListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        databaseReference.child(currentUserID).child("online").setValue("false");
    }
}
