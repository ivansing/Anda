package com.ivan.andatest.activities.driver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.ivan.andatest.R;
import com.ivan.andatest.activities.cliente.RegisterActivity;
import com.ivan.andatest.includes.MyToolbar;
import com.ivan.andatest.models.Driver;
import com.ivan.andatest.providers.AuthProvider;
import com.ivan.andatest.providers.DriverProvider;

import dmax.dialog.SpotsDialog;

public class RegisterDriverActivity extends AppCompatActivity {

    // Authentication
    AuthProvider mAuthProvider;
    DriverProvider mDriverProvider;

    // Views
    Button mButtonRegister;
    TextInputEditText mTextInputName;
    TextInputEditText mTextInputEmail;
    TextInputEditText mTextInputVehicleBrand;
    TextInputEditText mTextInputPlate;
    TextInputEditText mTextInputPassword;

    // Alert Dialog
    AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_driver);
        MyToolbar.show(this, "Registro conductor", true);

        mAuthProvider = new AuthProvider();
        mDriverProvider = new DriverProvider();

        mDialog = new SpotsDialog.Builder()
                .setContext(RegisterDriverActivity.this)
                .setMessage("Espera un momento").build();

        mButtonRegister = findViewById(R.id.btnRegister);
        mTextInputEmail = findViewById(R.id.textInputEmail);
        mTextInputName = findViewById(R.id.textInputName);
        mTextInputVehicleBrand = findViewById(R.id.textInputVehicleBrand);
        mTextInputPlate = findViewById(R.id.textInputPlate);
        mTextInputPassword = findViewById(R.id.textInputPassword);

        mButtonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickRegister();
            }
        });

    }
    public void clickRegister() {
        final String name = mTextInputName.getText().toString();
        final String email = mTextInputEmail.getText().toString();
        final String vehicleBrand = mTextInputVehicleBrand.getText().toString();
        final String plate = mTextInputPlate.getText().toString();
        final String password = mTextInputPassword.getText().toString();

        if (!name.isEmpty() && !email.isEmpty() && !password.isEmpty() && !vehicleBrand.isEmpty() && !plate.isEmpty()) {
            if(password.length() >= 6) {
                mDialog.show();
                register(name, email, vehicleBrand, plate, password);
            } else {
                Toast.makeText(this, "La contraseña debe ser de al menos 6 caracteres", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ingresar todos los campos", Toast.LENGTH_SHORT).show();
        }

    }

    void register(final String name, String email, String vehicleBrand, String plate, String password) {
        mAuthProvider.register(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                mDialog.hide();
                if (task.isSuccessful()) {
                    String id = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    Driver driver = new Driver(id, name, email, vehicleBrand, plate);
                    create(driver);
                } else {
                    Toast.makeText(RegisterDriverActivity.this, "No se pudo crear el conductor", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void create(Driver driver) {
        mDriverProvider.create(driver).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(RegisterDriverActivity.this, MapDriverActivity.class);
                    intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK | intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterDriverActivity.this, "No se pudo crear el conductor", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}