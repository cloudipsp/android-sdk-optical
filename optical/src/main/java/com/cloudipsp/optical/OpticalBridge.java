package com.cloudipsp.optical;

import android.app.Activity;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TextView;

import com.cloudipsp.android.CardInputView;
import com.cloudipsp.android.CardNumberEdit;
import com.cloudipsp.android.Cloudipsp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;

/**
 * Created by vberegovoy on 27.02.17.
 */

public class OpticalBridge {
    private static final int RQ_SCAN = 25320;

    public static void start(Activity activity) {
        final Intent intent = new Intent(activity, CardIOActivity.class);
        intent.putExtra(CardIOActivity.EXTRA_SUPPRESS_CONFIRMATION, true);
        intent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, true);
        intent.putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true);
        intent.putExtra(CardIOActivity.EXTRA_USE_PAYPAL_ACTIONBAR_ICON, false);
        activity.startActivityForResult(intent, RQ_SCAN);
    }

    public static CompleteResult complete(int requestCode, int responseCode, Intent data) {
        if (RQ_SCAN == requestCode &&
                data != null &&
                data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
            CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
            final String cardNumber = scanResult.cardNumber;


            return new CompleteResult() {
                @Override
                public void process(CardInputView cardInputView) {
                    try {
                        Field f = cardInputView.getClass().getDeclaredField("editCardNumber");
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        final CardNumberEdit cardNumberEdit = (CardNumberEdit) f.get(cardInputView);
                        Method m = cardNumberEdit.getClass().getDeclaredMethod("setCardNumber", String.class);
                        if (!m.isAccessible()) {
                            m.setAccessible(true);
                        }
                        m.invoke(cardNumberEdit, cardNumber);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        return null;
    }
}
