package world.iaomessenger;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;

    private SectionsStatePagerAdapter mSectionsStatePagerAdapter;
    private ViewPager mViewPager;

    private CoordinatorLayout coordinatorLayoutMain;
    private String currentUserID;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Message
        Log.d(TAG, "onCreate: STARTED.");
        getViews();

        printKeyHash();

        setupViewPager(mViewPager);
    }

    private void printKeyHash() {

        try {

            PackageInfo info = getPackageManager().getPackageInfo("world.iaomessenger",
                    PackageManager.GET_SIGNATURES);

            for(Signature signature:info.signatures) {

                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d(TAG, "printKeyHash: KEYHASH -> " + Base64.encodeToString(md.digest(), Base64.DEFAULT));

            }

        } catch (PackageManager.NameNotFoundException e) {

            e.printStackTrace();

        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();

        }

    }


    // Initialize fields
    private void getViews() {

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if(currentUser != null) {
            currentUserID = currentUser.getUid();
        }

        Log.d(TAG, "onCreate: STARTED.");

        mSectionsStatePagerAdapter = new SectionsStatePagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.container);

        coordinatorLayoutMain = (CoordinatorLayout) findViewById(R.id.coordinatorLayoutMain);

        setupViewPager(mViewPager);
        progressDialog = new ProgressDialog(this, R.style.MyDialogTheme);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(currentUser == null) {
            sendTostart();
        } else {
            Log.d(TAG, "onStart: BACK TO IT AGAIN");
            // If the user is online then set his status as online
            databaseReference.child("Users").child(currentUserID).child("online").setValue("true");
            //VerifyUserExistense();


            databaseReference.child("Users").child(currentUserID).child("username").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.hasChild("username")) {
                        if (TextUtils.isEmpty(dataSnapshot.child("username").getValue().toString())) {
                            Log.d(TAG, "onDataChange: DEBUG" + dataSnapshot.getValue().toString());
                            redirectUserToProfileActivity();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            //redirectUserToProfileActivity();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void sendTostart() {
        Intent loginIntent = new Intent(MainActivity.this, GettingStarted.class);
        startActivity(loginIntent);
    }



    private void VerifyUserExistense() {
        // Send the user to the account activity immediately after logging to set up his username and status (because they are mandatory)
        databaseReference.child("Users").child(currentUserID).child("username").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild("username")) {
                    if (TextUtils.isEmpty(dataSnapshot.child("username").getValue().toString())) {
                    Log.d(TAG, "onDataChange: DEBUG" + dataSnapshot.getValue().toString());
                        redirectUserToProfileActivity();
                    }

                } else {

                    redirectUserToLoginActivity();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void redirectUserToLoginActivity() {

        Intent loginActivityActivityIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(loginActivityActivityIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void redirectUserToProfileActivity() {
        Intent profileActivityIntent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(profileActivityIntent);
    }

    private void setupViewPager(ViewPager viewPager){
        SectionsStatePagerAdapter adapter = new SectionsStatePagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ChatsFragment(), "Chats");
        adapter.addFragment(new GroupsFragment(), "Groups");
        adapter.addFragment(new ContactsFragment(), "Contacts");
        adapter.addFragment(new RequestsFragment(), "Requests");
        viewPager.setAdapter(adapter);
    }

    // When the bottom view items get clicked they call this method to be displayed!
    public void setViewPager(int fragmentNumber){
        mViewPager.setCurrentItem(fragmentNumber);
    }

    // Create the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_bar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    // Ask for a new group, then store the group name in the firebase database
    private void createNewGroup() {

        final AlertDialog.Builder groupBuilder = new AlertDialog.Builder(MainActivity.this, R.style.MyDialogTheme);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.create_group_dialog_box_layout, null);
        final TextInputEditText groupNameField = (TextInputEditText) view.findViewById(R.id.add_group_edit_text);

        groupBuilder.setView(view);

        groupBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                groupNameField.findFocus();
                final String groupName = groupNameField.getText().toString();

                if(TextUtils.isEmpty(groupName)) {

                    Snackbar snackbarGroupNameFieldError = Snackbar.make(coordinatorLayoutMain, "You must enter a group name", Snackbar.LENGTH_LONG);
                    snackbarGroupNameFieldError.setAction("OK.", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                        }
                    });
                    snackbarGroupNameFieldError.setActionTextColor(Color.YELLOW);
                    snackbarGroupNameFieldError.show();

                } else {

                    // TO THINK ABOUT: Each user has his own groups!
                    String currentUserID = currentUser.getUid();

                    // Groups node is created, so is the groups names.
                    databaseReference.child("Groups").child(groupName).setValue("")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Snackbar snackbarGroupNameFieldError = Snackbar.make(coordinatorLayoutMain, groupName + " is created successfully.", Snackbar.LENGTH_LONG);
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

    // Execute actions when menu items get clicked.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        // We can use switch here!
        if(item.getItemId() == R.id.menu_logout) {
            progressDialog.setTitle("Logging Out");
            progressDialog.setMessage("Please wait..");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            FirebaseAuth.getInstance().signOut();
            redirectUserToLoginActivity();

            progressDialog.dismiss();
        }

        if(item.getItemId() == R.id.menu_Account) {
            Intent profileActivityIntent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(profileActivityIntent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

        if(item.getItemId() == R.id.menu_create_group) {
            createNewGroup();
        }

        if(item.getItemId() == R.id.menu_search) {
            if(currentUserID != null) {
                databaseReference.child("Users").child(currentUserID).child("lastSeen").setValue(ServerValue.TIMESTAMP);
            }
            redirectUserToFindFriendsActivity();
        }

        return true;
    }

    private void redirectUserToFindFriendsActivity() {
        Intent findUserActivityIntent = new Intent(MainActivity.this, FindFriendsActivity.class);
        startActivity(findUserActivityIntent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(currentUserID != null) {
            databaseReference.child("Users").child(currentUserID).child("online").setValue("false");
        }
    }

    public void setActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }
}
