package world.iaomessenger;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPasswordActivity extends AppCompatActivity {

    private EditText editemail;
    private Button btnsend;
    private TextView txtsignup;
    private ImageView fleche;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);
        initialise();

        txtsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inscriptionIntent = new Intent(ForgetPasswordActivity.this,RegisterActivity.class);
                startActivity(inscriptionIntent);
            }
        });

        fleche.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent inscriptionIntent = new Intent(ForgetPasswordActivity.this,LoginActivity.class);
                startActivity(inscriptionIntent);
            }
        });

        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editemail.getText().toString();
                if(email.equals("")){
                    Toast.makeText(ForgetPasswordActivity.this,"All fiels are required",Toast.LENGTH_SHORT).show();
                }else {
                    auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                    Toast.makeText(ForgetPasswordActivity.this,"Please check your email",Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(ForgetPasswordActivity.this,LoginActivity.class));
                            }else {
                                String error = task.getException().getMessage();
                                Toast.makeText(ForgetPasswordActivity.this,error,Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });



    }

    private void initialise() {

        editemail = findViewById(R.id.email2);
        btnsend = findViewById(R.id.send_btn);
        txtsignup = findViewById(R.id.linksignup);
        fleche = findViewById(R.id.fleche);
        auth = FirebaseAuth.getInstance();
    }
}
