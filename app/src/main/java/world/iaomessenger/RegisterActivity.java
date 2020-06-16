package world.iaomessenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextView needLogin;
    private EditText registerFullName, registerEmail, registerPassword, registerRepeatPassword, profileFullName, profileEmail;
    private Button registerBtn;
    private String rFullName, rEmail, rPassword, rRepeatPassword;
    private ImageView fleche;

    private FirebaseAuth currAuth;
    private DatabaseReference databaseReference;

    ProgressDialog progressDialog;
    CoordinatorLayout coordinatorLayout;
    private String token;
    private String newToken;
    private DatabaseReference mBadgesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Log.d(TAG, "onCreate: STARTED");

        final String user_phone = getIntent().getStringExtra("user_phone");

        currAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        getFieldByIds();

        fleche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectUserToLoginActivity();
            }
        });

        needLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectUserToLoginActivity();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createANewAccount();
            }
        });
    }

    private void getFieldByIds() {
        needLogin = (TextView) findViewById(R.id.need_login_2nd_half);
        registerFullName = (EditText) findViewById(R.id.register_full_name);
        registerEmail = (EditText) findViewById(R.id.register_email);
        registerPassword = (EditText) findViewById(R.id.register_password);
        registerRepeatPassword = (EditText) findViewById(R.id.register_repeat_password);
        registerBtn = (Button) findViewById(R.id.register_btn);
        progressDialog = new ProgressDialog(this, R.style.MyDialogTheme);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayoutRegister);
        profileEmail = (EditText) findViewById(R.id.profile_email);
        profileFullName = (EditText) findViewById(R.id.profile_name_lastname);
        mBadgesRef = FirebaseDatabase.getInstance().getReference().child("Badges");
        fleche = findViewById(R.id.register_back_arrow);
    }

    private void createANewAccount() {

        final String userPhone = getIntent().getStringExtra("user_phone");

        rFullName = registerFullName.getText().toString();
        rEmail = registerEmail.getText().toString();
        rPassword = registerPassword.getText().toString();
        rRepeatPassword = registerRepeatPassword.getText().toString();



        if(TextUtils.isEmpty(rFullName) || TextUtils.isEmpty(rEmail) || TextUtils.isEmpty(rPassword) || TextUtils.isEmpty(rRepeatPassword)) {

            Snackbar snackbarGroupNameFieldError = Snackbar.make(coordinatorLayout, "All fields are required.", Snackbar.LENGTH_LONG);
            snackbarGroupNameFieldError.setAction("OK", new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            snackbarGroupNameFieldError.setActionTextColor(Color.YELLOW);
            snackbarGroupNameFieldError.show();

        } else if(!rPassword.equals(rRepeatPassword)) {

            registerRepeatPassword.setError("Passwords are not the same.");

        } else {

            progressDialog.setTitle("Creating New Account");
            progressDialog.setMessage("Please wait..");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            currAuth.createUserWithEmailAndPassword(rEmail, rPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {

                        String currentUserID = currAuth.getUid();
                        if(currentUserID != null) {

                            mBadgesRef.child("chats_badges").child(currentUserID).child("value").setValue(0);
                            mBadgesRef.child("groups_badges").child(currentUserID).child("value").setValue(0);
                            mBadgesRef.child("contacts_badges").child(currentUserID).child("value").setValue(0);
                            mBadgesRef.child("requests_badges").child(currentUserID).child("value").setValue(0);

                            final DatabaseReference usersDBRef = databaseReference.child("Users").child(currentUserID);

                            usersDBRef.child("uid").setValue("");
                            usersDBRef.child("username").setValue("");
                            usersDBRef.child("name").setValue(rFullName);
                            usersDBRef.child("email").setValue(rEmail);
                            usersDBRef.child("status").setValue("");
                            usersDBRef.child("lastSeen").setValue(ServerValue.TIMESTAMP);
                            Log.d(TAG, "onComplete: " + ServerValue.TIMESTAMP);
                            usersDBRef.child("online").setValue("true");


                            Log.d(TAG, "onComplete: " + rFullName + ":" + rEmail);

                            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(RegisterActivity.this,
                                    new OnSuccessListener<InstanceIdResult>() {
                                        @Override
                                        public void onSuccess(InstanceIdResult instanceIdResult) {
                                            newToken = instanceIdResult.getToken();
                                            Log.d("newToken", newToken);

                                            usersDBRef.child("device_token").setValue(newToken);

                                            SharedPreferences.Editor editor = getSharedPreferences("TOKEN_PREF", MODE_PRIVATE).edit();
                                            if (token!=null){
                                                editor.putString("token", newToken);
                                                editor.apply();
                                            }

                                        }
                                    });

                            redirectUserToMainActivity();
                            Snackbar.make(coordinatorLayout, "The account has been created successfully", Snackbar.LENGTH_LONG).show();
                            progressDialog.dismiss();

                        }

                    } else {

                        Snackbar snackbarLoginUnsuccessful = Snackbar.make(coordinatorLayout, task.getException().toString(), Snackbar.LENGTH_INDEFINITE);
                        snackbarLoginUnsuccessful.setAction("RETRY", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        });
                        snackbarLoginUnsuccessful.setActionTextColor(Color.YELLOW);
                        snackbarLoginUnsuccessful.show();
                        progressDialog.dismiss();

                    }
                }
            });
        }
    }

    private void redirectUserToLoginActivity() {
        Intent loginActivityIntent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(loginActivityIntent);
    }

    private void redirectUserToMainActivity() {
        Intent mainActivityIntent = new Intent(RegisterActivity.this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();
    }
}
