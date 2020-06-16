package world.iaomessenger;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private EditText profileUsername, profileNameLastName, profileStatus, profileEmail;
    private ImageButton profileUsernameBtn, profileNameLastNameBtn, profileStatusBtn, profileConfirmChanges;

    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userDatabaseReference;

    private String currentUserID;

    private RelativeLayout profileRelativeLayout;

    private CircleImageView userImageBtn, profileAvatarImg;
    static int PReqCode = 1, REQUESTCODE = 1;

    private Uri userImagePickUri;
    private StorageReference userProfileImagesReference;
    ProgressDialog progressDialog;
    private String retrieveImage;

    private Button profileLogoutBtn;
    private String newToken;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);

        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        getViews();
        setListenersOnButtons();
        profileConfirmChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateProfilSettings();
            }
        });

        userImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(Build.VERSION.SDK_INT >= 22) {

                    checkAndRequestForPermission();

                } else {

                    openGallery();

                }
            }
        });

        retrieveUserInfo();

        profileLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setTitle("Logging Out");
                progressDialog.setMessage("Please wait..");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                FirebaseAuth.getInstance().signOut();
                sendToLogin();

                progressDialog.dismiss();
            }
        });
    }

    private void retrieveUserInfo() {
        userDatabaseReference.keepSynced(true);
        userDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.hasChild("username") && dataSnapshot.hasChild("image")) {

                    String retrieveUsername = dataSnapshot.child("username").getValue().toString();
                    String retrieveFullName = dataSnapshot.child("name").getValue().toString();
                    String retrieveEmail = dataSnapshot.child("email").getValue().toString();
                    retrieveImage = dataSnapshot.child("image").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                    profileUsername.setText(retrieveUsername);
                    profileNameLastName.setText(retrieveFullName);
                    profileEmail.setText(retrieveEmail);
                    profileStatus.setText(retrieveStatus);
                    Picasso.get().load(retrieveImage).networkPolicy(NetworkPolicy.OFFLINE)
                            .placeholder(R.drawable.ic_user_display_profile_image)
                            .into(profileAvatarImg, new Callback() {
                                @Override
                                public void onSuccess() {

                                }

                                @Override
                                public void onError(Exception e) {
                                    Picasso.get().load(retrieveImage).placeholder(R.drawable.ic_user_display_profile_image).into(profileAvatarImg);
                                }
                            });

                } else if(dataSnapshot.exists() && dataSnapshot.hasChild("username")) {

                    String retrieveUsername = dataSnapshot.child("username").getValue().toString();
                    String retrieveFullName = dataSnapshot.child("name").getValue().toString();
                    String retrieveEmail = dataSnapshot.child("email").getValue().toString();
                    String retrieveStatus = dataSnapshot.child("status").getValue().toString();

                    profileUsername.setText(retrieveUsername);
                    profileNameLastName.setText(retrieveFullName);
                    profileEmail.setText(retrieveEmail);
                    profileStatus.setText(retrieveStatus);

                } else {

                    Toast.makeText(ProfileActivity.this, "Please set a username.", Toast.LENGTH_SHORT).show();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

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

    public void getViews() {

        // EditTexts
        profileUsername = (EditText) findViewById(R.id.profile_username);
        profileNameLastName = (EditText) findViewById(R.id.profile_name_lastname);
        profileStatus = (EditText) findViewById(R.id.profile_status);
        profileEmail = (EditText) findViewById(R.id.profile_email);
        profileRelativeLayout = (RelativeLayout) findViewById(R.id.profile_main_rel);
        userImageBtn = (CircleImageView) findViewById(R.id.profile_change_image_icon);
        profileAvatarImg = (CircleImageView) findViewById(R.id.profile_image_avatar);
        progressDialog = new ProgressDialog(ProfileActivity.this, R.style.MyDialogTheme);

        // ImageButtons
        profileUsernameBtn = (ImageButton) findViewById(R.id.profile_username_edit_icon);
        profileNameLastNameBtn = (ImageButton) findViewById(R.id.profile_name__lastname_edit_icon);
        profileStatusBtn = (ImageButton) findViewById(R.id.profile_status_edit_icon);
        profileConfirmChanges = (ImageButton) findViewById(R.id.profile_confirm_changes);

        // Firebase Credentials
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            currentUserID = currentUser.getUid();
            userProfileImagesReference = FirebaseStorage.getInstance().getReference().child("Profile Images");
            userDatabaseReference = databaseReference.child(currentUserID);
        }

        // Logout button
        profileLogoutBtn = (Button) findViewById(R.id.profile_logout_button);
    }

    public void setListenersOnButtons() {
        profileUsernameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileUsername.setFocusableInTouchMode(true);

            }
        });

        profileNameLastNameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileNameLastName.setFocusableInTouchMode(true);
            }
        });

        profileStatusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileStatus.setFocusableInTouchMode(true);
            }
        });

        profileStatus.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {  // lost focus
                    profileStatus.setSelection(0,0);
                }
            }
        });
    }

    private boolean areProfileSettingsEmpty(String userName, String status) {

        return TextUtils.isEmpty(userName) || TextUtils.isEmpty(status);

    }

    private void updateProfilSettings() {
        final String userName, nameLastName, status, email;

        userName = profileUsername.getText().toString();
        nameLastName = profileNameLastName.getText().toString();
        status = profileStatus.getText().toString();
        email = profileEmail.getText().toString();


        if(areProfileSettingsEmpty(userName, status)) {

            Snackbar snackbar = Snackbar.make(profileRelativeLayout, "Username & Status are required, set them up to start.", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            snackbar.setActionTextColor(Color.YELLOW);
            snackbar.show();

        } else {

            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(ProfileActivity.this,
                    new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            newToken = instanceIdResult.getToken();
                            Log.d("newToken", newToken);

                            SharedPreferences.Editor editor = getSharedPreferences("TOKEN_PREF", MODE_PRIVATE).edit();
                            if (token!=null){
                                editor.putString("token", newToken);
                                editor.apply();
                            }

                            HashMap profileMap = new HashMap();
                            profileMap.put("device_token", newToken);
                            profileMap.put("uid", currentUserID);
                            profileMap.put("username", userName);
                            profileMap.put("status", status);
                            profileMap.put("name", nameLastName);
                            profileMap.put("email", email);
                            profileMap.put("image", retrieveImage);
                            profileMap.put("lastSeen", ServerValue.TIMESTAMP);

                            databaseReference.child(currentUserID).setValue(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful()) {
                                        Log.d("MOCHKIL", "onComplete: MOCHKIL");
                                        // If the user set his username then the app will redirect him to the main activity!
                                        redirectUserToMainActivity();

                                        Snackbar profileChangedSnackBar = Snackbar.make(profileRelativeLayout, "Profile has been updated successfully.", Snackbar.LENGTH_INDEFINITE);
                                        profileChangedSnackBar.setAction("COOL", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                            }
                                        });
                                        profileChangedSnackBar.setActionTextColor(Color.BLUE);
                                        profileChangedSnackBar.show();

                                    } else {

                                        // TODO: GET MORE IN DEPTH WITH THE EXCEPTIONS
                                        String errorMessage = task.getException().toString();

                                        Snackbar profileChangedErrorSnackBar = Snackbar.make(profileRelativeLayout, "Error: " + errorMessage, Snackbar.LENGTH_INDEFINITE);
                                        profileChangedErrorSnackBar.setAction("RETRY", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                            }
                                        });
                                        profileChangedErrorSnackBar.setActionTextColor(Color.YELLOW);
                                        profileChangedErrorSnackBar.show();
                                    }
                                }
                            });

                        }
                    });

        }
    }

    private void redirectUserToMainActivity() {
        Intent mainActivityIntent = new Intent(ProfileActivity.this, MainActivity.class);
        startActivity(mainActivityIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, REQUESTCODE);
    }

    private void checkAndRequestForPermission() {

        if(ContextCompat.checkSelfPermission(ProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if(ActivityCompat.shouldShowRequestPermissionRationale(ProfileActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                Toast.makeText(ProfileActivity.this, "Please accept for required permission", Toast.LENGTH_SHORT).show();

            } else {

                ActivityCompat.requestPermissions(ProfileActivity.this,
                                                    new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, PReqCode);
                openGallery();

            }

        } else {

            openGallery();

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == REQUESTCODE && data != null) {

            userImagePickUri = data.getData();
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if(resultCode == RESULT_OK) {

                progressDialog.setTitle("Updating Image");
                progressDialog.setMessage("Please wait..");
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                final Uri imageCroppedUri = result.getUri();
                final StorageReference resultImagePath = userProfileImagesReference.child(currentUserID + ".jpg");
                resultImagePath.putFile(imageCroppedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        resultImagePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String downloadUrl = uri.toString();
                                databaseReference.child(currentUserID).child("image").setValue(downloadUrl)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()) {

                                            Snackbar snackbarGroupNameFieldError = Snackbar.make(profileRelativeLayout, "Profile image updated successfully.", Snackbar.LENGTH_SHORT);
                                            snackbarGroupNameFieldError.setAction("COOL", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {

                                                }
                                            });
                                            snackbarGroupNameFieldError.setActionTextColor(Color.BLUE);
                                            snackbarGroupNameFieldError.show();
                                            progressDialog.dismiss();

                                        } else {

                                            Snackbar snackbarGroupNameFieldError = Snackbar.make(profileRelativeLayout, task.getException().toString(), Snackbar.LENGTH_SHORT);
                                            snackbarGroupNameFieldError.setAction(":(", new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {

                                                }
                                            });
                                            snackbarGroupNameFieldError.setActionTextColor(Color.RED);
                                            snackbarGroupNameFieldError.show();
                                            progressDialog.dismiss();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });

            }
        }
    }


    private void sendToLogin() {
        Intent loginActivityIntent = new Intent(ProfileActivity.this, LoginActivity.class);
        startActivity(loginActivityIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(currentUserID != null) {
            userDatabaseReference.child("online").setValue("true");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(currentUserID != null) {
            userDatabaseReference.child("online").setValue("false");
        }
    }
}
