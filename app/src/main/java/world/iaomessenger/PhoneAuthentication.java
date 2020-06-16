package world.iaomessenger;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.concurrent.TimeUnit;

public class PhoneAuthentication extends AppCompatActivity {

    private Button signInbtn;
    private  int RC_SIGN_IN=1;
   private  EditText txtphone,mcode,mdptxt1,mdptxt2;
   private Button btnconfirmmdp;
    private Button nextbtn,mcommitcode,signout;
    private String TAG = "PhoneAuthentification";
   private  TextView txtlogin;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId;
    private String newToken;
    private DatabaseReference mUsersRef;
    private String token,mdpvalue1,mdpvalue2;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private FirebaseAuth mAuth;
    private ProgressDialog loadingBar;
     GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_authentication);
        initialise();
        mAuth = FirebaseAuth.getInstance();

        nextbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = txtphone.getText().toString();
                if(TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(PhoneAuthentication.this,"Phone number is required ...",Toast.LENGTH_SHORT).show();
                }else{

                    loadingBar.setTitle("Phone verification");
                    loadingBar.setMessage("Please wait ...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();
                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneAuthentication.this,               // Activity (for callback binding)
                            mCallbacks
                    );        // OnVerificationStateChangedCallbacks

                }


            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                    signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                loadingBar.dismiss();
                Toast.makeText(PhoneAuthentication.this,"Invalid Phone Number, please enter correct phone number whith your country code... ",Toast.LENGTH_SHORT);

            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {

                mVerificationId = verificationId;
                mResendToken = token;
                loadingBar.dismiss();
                Toast.makeText(PhoneAuthentication.this,"Code has been sent, please check ...",Toast.LENGTH_SHORT).show();

                AlertDialog.Builder mBuilder = new AlertDialog.Builder(PhoneAuthentication.this);
                View mView = getLayoutInflater().inflate(R.layout.dialogbox_phone_verification,null);
                mcode = mView.findViewById(R.id.codeinputtxt);
                mcommitcode = mView.findViewById(R.id.commitcode);
                mcommitcode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {


                        String verificationCode = mcode.getText().toString();
                        if(TextUtils.isEmpty(verificationCode)){
                            Toast.makeText(PhoneAuthentication.this,"Code verification is required ...",Toast.LENGTH_SHORT).show();
                        }else {
                            loadingBar.setTitle("Code verification");
                            loadingBar.setMessage("Please wait ...");
                            loadingBar.setCanceledOnTouchOutside(false);
                            loadingBar.show();
                            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId,verificationCode);
                                signInWithPhoneAuthCredential(credential);
                        }
                    }
                });
                mBuilder.setView(mView);
                AlertDialog mdialog = mBuilder.create();
                mdialog.show();
            }
        };


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.webclient))
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                 signIn();

            }
        });



    }//onCreate

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                // ...
            }
        }
    }


    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                            //redirectUserToMainActivity();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(PhoneAuthentication.this,"Login failed...",Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (acct != null) {
            String personName = acct.getDisplayName();
            String personGivenName = acct.getGivenName();
            String personFamilyName = acct.getFamilyName();
            String personEmail = acct.getEmail();
            String personId = acct.getId();
            Uri personPhoto = acct.getPhotoUrl();

            AlertDialog.Builder mBuilder = new AlertDialog.Builder(PhoneAuthentication.this);
            View mView = getLayoutInflater().inflate(R.layout.dialogbox_set_password,null);
            mdptxt1 = mView.findViewById(R.id.mdpinputtxt1);
            mdptxt2 = mView.findViewById(R.id.mdpinputtxt2);
            btnconfirmmdp = mView.findViewById(R.id.commitmdp);





            mAuth.fetchSignInMethodsForEmail(personEmail).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {

                    if(task.isSuccessful()){
                       // Toast.makeText(PhoneAuthentication.this,"yes",Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onComplete: yes");
                    }else {
                       // Toast.makeText(PhoneAuthentication.this,"no",Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onComplete: no");
                    }
                }
            });

            /*

            mAuth.fetchProvidersForEmail(personEmail).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                    if(task.isSuccessful()){

                        Toast.makeText(PhoneAuthentication.this,"yes",Toast.LENGTH_SHORT).show();


                        final String currentUserID = mAuth.getCurrentUser().getUid();

                        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(PhoneAuthentication.this,
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


                    }else {


                        Toast.makeText(PhoneAuthentication.this,"no",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        */



        }

    }




    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            Toast.makeText(PhoneAuthentication.this,"Congratulations, you're logged in successfully...",Toast.LENGTH_SHORT).show();
                            sendToMainActivity();
                            loadingBar.dismiss();
                        }else {

                            String message = task.getException().toString();
                            Toast.makeText(PhoneAuthentication.this,"Error : "+message,Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    private void sendToMainActivity() {
        startActivity(new Intent(PhoneAuthentication.this,MainActivity.class));
        finish();
    }

    private void redirectUserToMainActivity() {
        Intent mainActivityIntent = new Intent(PhoneAuthentication.this, MainActivity.class);
        mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainActivityIntent);
        finish();
    }

    private void initialise() {
        txtphone = findViewById(R.id.phoneinput);
        nextbtn = findViewById(R.id.nextbtn);
        txtlogin = findViewById(R.id.loginphoneactivity);
        loadingBar = new ProgressDialog(this);
        signInbtn = findViewById(R.id.google_btn);



    }
}
