package com.example.inventory.ui.lottery;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.inventory.utils.GoogleSheetsHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LotteryViewModel extends AndroidViewModel {

    private static final String TAG = "LotteryViewModel";
    private final MutableLiveData<List<String>> lotteryCodes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<SubmitStatus> submitStatus = new MutableLiveData<>(SubmitStatus.IDLE);
    private final GoogleSheetsHelper googleSheetsHelper = new GoogleSheetsHelper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public enum SubmitStatus { IDLE, LOADING, SUCCESS, ERROR }

    public LotteryViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<String>> getLotteryCodes() { return lotteryCodes; }
    public LiveData<SubmitStatus> getSubmitStatus() { return submitStatus; }

    public void addLotteryCode(String code) {
        List<String> current = lotteryCodes.getValue();
        if (current == null) current = new ArrayList<>();
        current.add(code);
        lotteryCodes.setValue(current);
        Log.d(TAG, "Added code: " + code + ", total: " + current.size());
    }

    public void updateCodes(List<String> codes) {
        lotteryCodes.setValue(codes != null ? codes : new ArrayList<>());
    }

    public void clearCodes() {
        lotteryCodes.setValue(new ArrayList<>());
    }

    public void removeDuplicates() {
        List<String> current = lotteryCodes.getValue();
        if (current == null) return;
        List<String> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String code : current) {
            if (seen.add(code)) unique.add(code);
        }
        lotteryCodes.setValue(unique);
    }

    public void removeInvalidCodes() {
        List<String> current = lotteryCodes.getValue();
        if (current == null) return;
        List<String> valid = new ArrayList<>();
        for (String code : current) {
            if (code.length() >= 15) valid.add(code);
        }
        lotteryCodes.setValue(valid);
    }

    public void submitToGoogleSheets(List<String> codes) {
        submitStatus.setValue(SubmitStatus.LOADING);
        executor.execute(() -> {
            CountDownLatch latch = new CountDownLatch(1);
            final boolean[] success = {false};

            googleSheetsHelper.submitLotteryCodesViaWebhook(codes, new GoogleSheetsHelper.WebhookCallback() {
                @Override
                public void onSuccess(int codesSubmitted) {
                    success[0] = true;
                    latch.countDown();
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Webhook submission failed: " + error);
                    latch.countDown();
                }
            });

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "Submission interrupted", e);
            }

            mainHandler.post(() -> {
                if (success[0]) {
                    submitStatus.setValue(SubmitStatus.SUCCESS);
                    clearCodes();
                } else {
                    submitStatus.setValue(SubmitStatus.ERROR);
                }
            });
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
