package com.example.inventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.google.api.services.sheets.v4.SheetsScopes;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final String TAG = "GoogleSignInActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> signInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sign_in);

        // Configure Google Sign-In with proper scopes
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(SheetsScopes.SPREADSHEETS))
                .requestIdToken(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up the sign-in launcher
        signInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
        );

        // Start sign-in process
        signIn();
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // Check if we have the required scope
            if (GoogleSignIn.hasPermissions(account, new Scope(SheetsScopes.SPREADSHEETS))) {
                // Successfully signed in with required permissions
                Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Sign in successful for: " + account.getEmail());
                
                // For now, we'll use a placeholder token
                // In a production app, you'd get the actual access token
                String accessToken = "placeholder_token";
                
                // You can store the account info or token here
                // For now, we'll just finish the activity
                setResult(RESULT_OK);
                finish();
            } else {
                // Missing required scope
                Log.w(TAG, "Missing required scope: " + SheetsScopes.SPREADSHEETS);
                Toast.makeText(this, "Missing required permissions for Google Sheets", Toast.LENGTH_LONG).show();
                setResult(RESULT_CANCELED);
                finish();
            }
            
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            
            String errorMessage = "Sign in failed";
            switch (e.getStatusCode()) {
                case 403:
                    errorMessage = "Access denied. Please check your Google Cloud Project configuration.";
                    break;
                case 12501:
                    errorMessage = "Sign in was cancelled by user";
                    break;
                case 12500:
                    errorMessage = "Sign in failed. Please try again.";
                    break;
                default:
                    errorMessage = "Sign in failed: " + e.getStatusMessage();
                    break;
            }
            
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
