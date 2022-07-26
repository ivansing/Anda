package com.ivan.andatest.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ivan.andatest.R;
import com.ivan.andatest.activities.cliente.RegisterActivity;
import com.ivan.andatest.activities.driver.RegisterDriverActivity;
import com.ivan.andatest.includes.MyToolbar;

public class SelectOptionAuthActivity extends AppCompatActivity {


    Button mButtonGoLogin;
    Button mButtonGoRegister;
    SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_option_auth);
        MyToolbar.show(this, getString(R.string.select_option), true);

        mButtonGoLogin = findViewById(R.id.btnGoLogin);
        mButtonGoRegister = findViewById(R.id.btnGoRegister);
        mButtonGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToLogin();
            }
        });
        mButtonGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToRegister();
            }
        });

        mPref = getApplicationContext().getSharedPreferences("typeUser", MODE_PRIVATE);

    }


    public void goToLogin() {
        Intent intent = new Intent(SelectOptionAuthActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void goToRegister() {
        String typeUser = mPref.getString("user", "");
        if (typeUser == "client") {
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(SelectOptionAuthActivity.this, RegisterDriverActivity.class);
            startActivity(intent);
        }
    }

}