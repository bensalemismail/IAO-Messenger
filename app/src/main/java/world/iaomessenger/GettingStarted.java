package world.iaomessenger;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class GettingStarted extends AppCompatActivity {
     Button btngetStarted;
     TextView btnsignup;
     RelativeLayout imgLogoR,textR;
     Animation fromtop,frombuttom;
     ImageView imglogo;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.getstarted);
        initialise();
        fromtop = AnimationUtils.loadAnimation(this,R.anim.anim2);
        frombuttom = AnimationUtils.loadAnimation(this,R.anim.anim1);

        imgLogoR.setAnimation(fromtop);
        textR.setAnimation(frombuttom);

        btngetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 Intent getStartedIntent = new Intent(GettingStarted.this,LoginActivity.class);
                 Pair[] paires = new Pair[1];
                 paires[0] = new Pair<View,String>(imglogo,"imgtransaction");
                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(GettingStarted.this,paires);
                 startActivity(getStartedIntent,options.toBundle());
            }
        });

        btnsignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getStartedIntent = new Intent(GettingStarted.this,RegisterActivity.class);
                startActivity(getStartedIntent);
            }
        });



    }//onCreate()


    private void initialise() {
        imgLogoR = findViewById(R.id.logolayout);
        textR = findViewById(R.id.layouttxt);
        btngetStarted = findViewById(R.id.btnstart);
        btnsignup = findViewById(R.id.firsttimeheresignup);
        imglogo = findViewById(R.id.app_logo);


    }//initialise()


}// GettingStarted()
