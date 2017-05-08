package com.cloudipsp.opticalexample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.cloudipsp.android.Card;
import com.cloudipsp.android.CardInputView;
import com.cloudipsp.android.Cloudipsp;
import com.cloudipsp.android.CloudipspWebView;
import com.cloudipsp.android.Currency;
import com.cloudipsp.android.Order;
import com.cloudipsp.android.Receipt;
import com.cloudipsp.optical.CompleteResult;
import com.cloudipsp.optical.OpticalBridge;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final int MERCHANT_ID = 1396424;

    private EditText editAmount;
    private Spinner spinnerCcy;
    private EditText editEmail;
    private EditText editDescription;
    private CardInputView cardInput;
    private CloudipspWebView webView;

    private Cloudipsp cloudipsp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_amount).setOnClickListener(this);
        editAmount = (EditText) findViewById(R.id.edit_amount);
        spinnerCcy = (Spinner) findViewById(R.id.spinner_ccy);
        editEmail = (EditText) findViewById(R.id.edit_email);
        editDescription = (EditText) findViewById(R.id.edit_description);
        cardInput = (CardInputView) findViewById(R.id.card_input);
        cardInput.setHelpedNeeded(BuildConfig.DEBUG);
        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_pay).setOnClickListener(this);

        webView = (CloudipspWebView) findViewById(R.id.web_view);
        cloudipsp = new Cloudipsp(MERCHANT_ID, webView);

        spinnerCcy.setAdapter(new ArrayAdapter<Currency>(this, android.R.layout.simple_spinner_item, Currency.values()));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_amount:
                fillTest();
                break;
            case R.id.btn_scan:
                OpticalBridge.start(this);
                break;
            case R.id.btn_pay:
                processPay();
                break;
        }
    }

    private void fillTest() {
        editAmount.setText("1");
        editEmail.setText("test@test.com");
        editDescription.setText("test payment");
    }

    private void processPay() {
        editAmount.setError(null);
        editEmail.setError(null);
        editDescription.setError(null);

        final int amount;
        try {
            amount = Integer.valueOf(editAmount.getText().toString());
        } catch (Exception e) {
            editAmount.setError(getString(R.string.e_invalid_amount));
            return;
        }

        final String email = editEmail.getText().toString();
        final String description = editDescription.getText().toString();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.setError(getString(R.string.e_invalid_email));
        } else if (TextUtils.isEmpty(description)) {
            editDescription.setError(getString(R.string.e_invalid_description));
        } else {
            final Card card = cardInput.confirm(new CardInputView.ConfirmationErrorHandler() {
                @Override
                public void onCardInputErrorClear(CardInputView view, EditText editText) {
                }

                @Override
                public void onCardInputErrorCatched(CardInputView view, EditText editText, String error) {
                }
            });

            if (card != null) {
                final Currency currency = (Currency) spinnerCcy.getSelectedItem();
                final Order order = new Order(amount, currency, "vb_" + System.currentTimeMillis(), description, email);
                order.setLang(Order.Lang.ru);

                cloudipsp.pay(card, order, new Cloudipsp.PayCallback() {
                    @Override
                    public void onPaidProcessed(Receipt receipt) {
                        Toast.makeText(MainActivity.this, "Paid " + receipt.status.name() + "\nPaymentId:" + receipt.paymentId, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPaidFailure(Cloudipsp.Exception e) {
                        if (e instanceof Cloudipsp.Exception.Failure) {
                            Cloudipsp.Exception.Failure f = (Cloudipsp.Exception.Failure) e;

                            Toast.makeText(MainActivity.this, "Failure\nErrorCode: " +
                                    f.errorCode + "\nMessage: " + f.getMessage() + "\nRequestId: " + f.requestId, Toast.LENGTH_LONG).show();
                        } else if (e instanceof Cloudipsp.Exception.NetworkSecurity) {
                            Toast.makeText(MainActivity.this, "Network security error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        } else if (e instanceof Cloudipsp.Exception.ServerInternalError) {
                            Toast.makeText(MainActivity.this, "Internal server error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        } else if (e instanceof Cloudipsp.Exception.NetworkAccess) {
                            Toast.makeText(MainActivity.this, "Network error", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Payment Failed", Toast.LENGTH_LONG).show();
                        }
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.waitingForConfirm()) {
            webView.skipConfirm();
        } else {
            super.onBackPressed();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        CompleteResult result = OpticalBridge.complete(requestCode, resultCode, data);
        if (result != null) {
            result.process(cardInput);
        }
    }
}
