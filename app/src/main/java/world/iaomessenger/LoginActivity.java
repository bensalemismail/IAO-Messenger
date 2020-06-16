package world.iaomessenger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import dmax.dialog.SpotsDialog;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import world.iaomessenger.Model.CheckUserResponse;
import world.iaomessenger.Retrofit.IIAOMessengerAPI;
import world.iaomessenger.Utils.Common;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText loginEmail, loginPassword;
    private Button loginBtn,phonebtn;
    private TextView needRegister, forgetPassword;
    private String lEmail, lPassword;
    private CoordinatorLayout coordinatorLayout;
    private DatabaseReference mUsersRef;
    ProgressDialog progressDialog;
    ImageView imglogo;
    private FirebaseAuth mAuth;
    private String token;
    private String newToken;
    private DatabaseReference mDBref;
    private DatabaseReference mBadgesRef;
    private static final int REQUEST_CODE = 1000;

    IIAOMessengerAPI mService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mService = Common.getAPI();
        mAuth = FirebaseAuth.getInstance();

        getFieldByIds();

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logIn();
            }
        });

        needRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectUserToRegisterActivity();
            }
        });

        forgetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirectUserToForgetPasswordActivity();
            }
        });

        loginPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    loginBtn.performClick();
                    return true;
                }

                return false;
            }
        });

        phonebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(LoginActivity.this,PhoneAuthentication.class));
                startPhoneLoginPage(LoginType.PHONE);
            }
        });

    }

    private void startPhoneLoginPage(LoginType loginType) {

        Intent intent = new Intent(this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder builder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(loginType, AccountKitActivity.ResponseType.TOKEN);

        intent.putExtra(AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION, builder.build());
        startActivityForResult(intent, REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE) {

            AccountKitLoginResult result = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);

            if(result.getError() != null) {

                Toast.makeText(this, "" + result.getError().getErrorType().getMessage(), Toast.LENGTH_SHORT).show();

            } else if(result.wasCancelled()) {

                Toast.makeText(this, "Cancel", Toast.LENGTH_SHORT).show();

            } else {

                if(result.getAccessToken() != null) {

                    final AlertDialog alertDialog = new SpotsDialog(LoginActivity.this);
                    alertDialog.show();
                    alertDialog.setMessage("Please wait..");

                    AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
                        @Override
                        public void onSuccess(final Account account) {

                            mService.checkUserExists(account.getPhoneNumber().toString())
                            .enqueue(new Callback<CheckUserResponse>() {
                                @Override
                                public void onResponse(Call<CheckUserResponse> call, Response<CheckUserResponse> response) {

                                    CheckUserResponse userResponse = response.body();

                                    if(userResponse != null) {
                                        if (userResponse.isExists()) {


                                            alertDialog.dismiss();

                                        } else {

                                            alertDialog.dismiss();
                                            Intent registerActivityIntent = new Intent(LoginActivity.this, RegisterActivity.class);
                                            registerActivityIntent.putExtra("user_phone", account.getPhoneNumber().toString());
                                            startActivity(registerActivityIntent);

                                        }
                                    }

                                }

                                @Override
                                public void onFailure(Call<CheckUserResponse> call, Throwable t) {

                                }
                            });

                        }

                        @Override
                        public void onError(AccountKitError accountKitError) {

                            Log.d(TAG, "onError: " + accountKitError.getErrorType().getMessage());

                        }
                    });

                    alertDialog.dismiss();
                }

            }

        }
    }

    private void logIn() {
        lEmail = loginEmail.getText().toString();
        lPassword = loginPassword.getText().toString();

        boolean fieldsAreEmpty = checkIfLoginFieldsAreEmpty(lEmail, lPassword);

        if(!fieldsAreEmpty) {

            progressDialog.setTitle("Logging In");
            progressDialog.setMessage("Please wait, while we sign you in..");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            mAuth.signInWithEmailAndPassword(lEmail, lPassword).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if(task.isSuccessful()) {

                        final String currentUserID = mAuth.getCurrentUser().getUid();

                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(LoginActivity.this,
                                new OnSuccessListener<InstanceIdResult>() {
                            @Override
                            public void onSuccess(InstanceIdResult instanceIdResult) {
                                newToken = instanceIdResult.getToken();
                                Log.d("newToken", newToken);

                                Log.d(TAG, "onComplete: TOKEN -> : " + newToken);
                                mUsersRef.child(currentUserID).child("device_token").setValue(newToken);

                                SharedPreferences.Editor editor = getSharedPreferences("TOKEN_PREF", MODE_PRIVATE).edit();
                                if (token!=null){
                                    editor.putString("token", newToken);
                                    editor.apply();
                                }
                            }
                        });

                        redirectUserToMainActivity();
                        Snackbar.make(coordinatorLayout, "Logged In Successfully.", Snackbar.LENGTH_LONG).show();
                        progressDialog.dismiss();

                    } else {

                        Log.w(TAG, "signInWithEmail:failed", task.getException());
                        Toast.makeText(LoginActivity.this, "User Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        sendTostart();
                        progressDialog.dismiss();

                    }
                }
            });

        }
    }

    private boolean checkIfLoginFieldsAreEmpty(String email, String pwd) {

        boolean isEmpty = false;

        if(TextUtils.isEmpty(email)) {
            loginEmail.setError("Email is required");
            isEmpty = true;
        }

        if(TextUtils.isEmpty(pwd)) {
            loginPassword.setError("Password is required");
            isEmpty = true;
        }

        return isEmpty;
    }

    private void getFieldByIds() {
        needRegister = (TextView) findViewById(R.id.need_register_2nd_half);
        loginEmail = (EditText) findViewById(R.id.login_email);
        loginPassword = (EditText) findViewById(R.id.login_password);
        forgetPassword = (TextView) findViewById(R.id.forget_password);
        loginBtn = (Button) findViewById(R.id.login_btn);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayoutLogin);
        progressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        mDBref = FirebaseDatabase.getInstance().getReference();
        mUsersRef = mDBref.child("Users");
        mBadgesRef = mDBref.child("Badges");
        phonebtn = findViewById(R.id.login_phone_btn);
        imglogo=findViewById(R.id.chaticon);
    }

    private void redirectUserToMainActivity() {
        Intent mainActivityIntent = new Intent(LoginActivity.this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();
    }

    private void redirectUserToRegisterActivity() {
        Intent registerActivityIntent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(registerActivityIntent);
    }

    private void redirectUserToForgetPasswordActivity() {
        Intent forgetPasswordActivity = new Intent(LoginActivity.this, ForgetPasswordActivity.class);
        startActivity(forgetPasswordActivity);
    }

    private void sendTostart() {
        Intent loginIntent = new Intent(LoginActivity.this, LoginActivity.class);
        startActivity(loginIntent);
    }
}
