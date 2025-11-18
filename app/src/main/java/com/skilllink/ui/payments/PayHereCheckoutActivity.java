package com.skilllink.ui.payments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.skilllink.R;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PayHereCheckoutActivity extends AppCompatActivity {

    public static final int RESULT_PAYMENT_CANCELLED = Activity.RESULT_FIRST_USER;
    public static final int RESULT_PAYMENT_FAILED = Activity.RESULT_FIRST_USER + 1;

    private static final String EXTRA_ARGS = "com.skilllink.extra.PAYHERE_ARGS";
    private static final String SANDBOX_ENDPOINT = "https://sandbox.payhere.lk/pay/checkout";
    private static final String FALLBACK_EMAIL = "user@skilllink.app";
    private static final String FALLBACK_PHONE = "0770000000";
    private static final String FALLBACK_ADDRESS = "Colombo";

    private WebView webView;
    private ProgressBar progressBar;
    private Args args;
    private boolean awaitingResult;

    public static Intent createIntent(@NonNull Context context, @NonNull Args args) {
        Intent intent = new Intent(context, PayHereCheckoutActivity.class);
        intent.putExtra(EXTRA_ARGS, args);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payhere_checkout);

        args = getIntent().getParcelableExtra(EXTRA_ARGS);
        if (args == null) {
            setResult(RESULT_PAYMENT_FAILED);
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(R.string.payhere_checkout_title);
            toolbar.setNavigationOnClickListener(v -> {
                setResult(RESULT_PAYMENT_CANCELLED);
                finish();
            });
        }

        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);

        configureWebView();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            submitPayment();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.stopLoading();
            webView.clearHistory();
            webView.removeAllViews();
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_PAYMENT_CANCELLED);
        super.onBackPressed();
    }

    private void configureWebView() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.addJavascriptInterface(new PayHereBridge(), "androidPayHere");

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36");
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                toggleProgress(true);
                handleRedirectIfNeeded(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                toggleProgress(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String targetUrl = request != null ? request.getUrl().toString() : null;
                if (handleRedirectIfNeeded(targetUrl)) {
                    return true;
                }
                if (targetUrl != null && targetUrl.startsWith("mailto:")) {
                    try {
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SENDTO, Uri.parse(targetUrl)), getString(R.string.payhere_checkout_title)));
                    } catch (ActivityNotFoundException ignored) {
                        // no email apps
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (handleRedirectIfNeeded(url)) {
                    return true;
                }
                if (url != null && url.startsWith("mailto:")) {
                    try {
                        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)), getString(R.string.payhere_checkout_title)));
                    } catch (ActivityNotFoundException ignored) {
                        // ignore
                    }
                    return true;
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                toggleProgress(false);
                Toast.makeText(PayHereCheckoutActivity.this, R.string.payhere_checkout_error, Toast.LENGTH_LONG).show();
                setResult(RESULT_PAYMENT_FAILED);
                finish();
            }
        });
    }

    private void submitPayment() {
        if (webView == null) {
            return;
        }

        String amount = formatAmount(args.amount);
        String hash = generateHash(args.merchantId, args.merchantSecret, args.orderId, amount, args.currency);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("merchant_id", args.merchantId);
        params.put("sandbox", "true");
        params.put("platform", "android");
        params.put("return_url", args.returnUrl);
        params.put("cancel_url", args.cancelUrl);
        params.put("notify_url", args.notifyUrl);
        params.put("order_id", args.orderId);
        params.put("items", safe(args.itemDescription));
        params.put("currency", args.currency);
        params.put("amount", amount);
        params.put("hash", hash);
        params.put("first_name", fallbackFirstName(args.customerFirstName));
        params.put("last_name", fallbackLastName(args.customerLastName));
        params.put("email", fallbackEmail(args.customerEmail));
        params.put("phone", fallbackPhone(args.customerPhone));
        String sanitizedAddress = fallbackAddress(args.customerAddress);
        params.put("address", sanitizedAddress);
        params.put("city", resolveCity(sanitizedAddress));
        params.put("country", "LK");

        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>body{font-family:sans-serif;background:#F5F6FA;margin:0;padding:32px;text-align:center;color:#1F2937;}");
        html.append("h1{font-size:18px;margin:0 0 12px;}p{font-size:14px;margin:0 0 28px;color:#4B5563;}div.loader{display:inline-block;width:48px;height:48px;border:4px solid #CBD5F5;border-top-color:#3B82F6;border-radius:50%;animation:spin 0.9s linear infinite;}@keyframes spin{to{transform:rotate(360deg);}}</style>");
        html.append("<script>window.addEventListener('message',function(event){");
        html.append("if(event.origin==='https://sandbox.payhere.lk' && event.data && event.data.type==='PAYHERE_RESULT'){androidPayHere.postMessage(JSON.stringify(event.data));}});");
        html.append("setTimeout(function(){document.getElementById('phCheckoutForm').submit();},150);");
        html.append("</script></head><body>");
        html.append("<h1>").append(getString(R.string.payhere_checkout_title)).append("</h1>");
        html.append("<p>").append(getString(R.string.payhere_checkout_redirect)).append("</p><div class='loader'></div>");
        html.append("<form id='phCheckoutForm' action='https://sandbox.payhere.lk/pay/checkout' method='post'>");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("<input type='hidden' name='")
                    .append(htmlEscape(entry.getKey()))
                    .append("' value='")
                    .append(htmlEscape(entry.getValue()))
                    .append("'>");
        }
        html.append("</form></body></html>");

        webView.loadDataWithBaseURL("https://sandbox.payhere.lk/pay/", html.toString(), "text/html", "UTF-8", null);
        awaitingResult = true;
    }

    private boolean handleRedirectIfNeeded(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        if (awaitingResult) {
            if (url.startsWith(args.returnUrl)) {
                finishWithSuccess();
                return true;
            }
            if (url.startsWith(args.cancelUrl)) {
                finishWithCancellation();
                return true;
            }
        }
        return false;
    }

    private void toggleProgress(boolean visible) {
        if (progressBar != null) {
            progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String encodeParams(Map<String, String> params) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey()));
            builder.append('=');
            builder.append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception ignored) {
            return value;
        }
    }

    private String formatAmount(double amount) {
        BigDecimal decimal = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        return decimal.toPlainString();
    }

    private String generateHash(String merchantId, String merchantSecret, String orderId, String amount, String currency) {
        String secretHash = md5(merchantSecret).toUpperCase(Locale.US);
        return md5(merchantId + orderId + amount + currency + secretHash).toUpperCase(Locale.US);
    }

    private String md5(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] result = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : result) {
                String hex = Integer.toHexString((b & 0xff) | 0x100).substring(1);
                builder.append(hex);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private String safe(@Nullable String value) {
        return value != null ? value : "";
    }

    private String fallbackFirstName(@Nullable String candidate) {
        String value = safe(candidate).trim();
        if (!value.isEmpty()) {
            return value;
        }
        return getString(R.string.account_default_first_name);
    }

    private String fallbackLastName(@Nullable String candidate) {
        String value = safe(candidate).trim();
        if (!value.isEmpty()) {
            return value;
        }
        return getString(R.string.account_default_last_name);
    }

    private String fallbackEmail(@Nullable String candidate) {
        String value = safe(candidate).trim();
        if (!value.isEmpty()) {
            return value;
        }
        return FALLBACK_EMAIL;
    }

    private String fallbackPhone(@Nullable String candidate) {
        String value = safe(candidate).replaceAll("[^0-9+]", "");
        if (!value.isEmpty()) {
            return value;
        }
        return FALLBACK_PHONE;
    }

    private String fallbackAddress(@Nullable String candidate) {
        String value = safe(candidate).trim();
        if (!value.isEmpty()) {
            return value;
        }
        return FALLBACK_ADDRESS;
    }

    private String htmlEscape(@Nullable String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private void finishWithSuccess() {
        awaitingResult = false;
        setResult(RESULT_OK);
        finish();
    }

    private void finishWithCancellation() {
        awaitingResult = false;
        setResult(RESULT_PAYMENT_CANCELLED);
        finish();
    }

    private String resolveCity(@Nullable String address) {
        if (!TextUtils.isEmpty(address)) {
            String[] parts = address.split(",");
            if (parts.length > 0) {
                String candidate = parts[parts.length - 1].trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return FALLBACK_ADDRESS;
    }

    private final class PayHereBridge {
        @JavascriptInterface
        public void postMessage(String payload) {
            if (TextUtils.isEmpty(payload)) {
                return;
            }
            if (payload.contains("PAYMENT_SUCCESS")) {
                runOnUiThread(PayHereCheckoutActivity.this::finishWithSuccess);
            } else if (payload.contains("PAYMENT_CANCELLED") || payload.contains("PAYMENT_FAILED")) {
                runOnUiThread(PayHereCheckoutActivity.this::finishWithCancellation);
            }
        }
    }

    public static class Args implements Parcelable {
        public final String merchantId;
        public final String merchantSecret;
        public final String returnUrl;
        public final String cancelUrl;
        public final String notifyUrl;
        public final String orderId;
        public final String itemDescription;
        public final double amount;
        public final String currency;
        public final String customerFirstName;
        public final String customerLastName;
        public final String customerEmail;
        public final String customerPhone;
        public final String customerAddress;

        public Args(String merchantId,
                    String merchantSecret,
                    String returnUrl,
                    String cancelUrl,
                    String notifyUrl,
                    String orderId,
                    String itemDescription,
                    double amount,
                    String currency,
                    String customerFirstName,
                    String customerLastName,
                    String customerEmail,
                    String customerPhone,
                    String customerAddress) {
            this.merchantId = merchantId;
            this.merchantSecret = merchantSecret;
            this.returnUrl = returnUrl;
            this.cancelUrl = cancelUrl;
            this.notifyUrl = notifyUrl;
            this.orderId = orderId;
            this.itemDescription = itemDescription;
            this.amount = amount;
            this.currency = currency;
            this.customerFirstName = customerFirstName;
            this.customerLastName = customerLastName;
            this.customerEmail = customerEmail;
            this.customerPhone = customerPhone;
            this.customerAddress = customerAddress;
        }

        protected Args(Parcel in) {
            merchantId = in.readString();
            merchantSecret = in.readString();
            returnUrl = in.readString();
            cancelUrl = in.readString();
            notifyUrl = in.readString();
            orderId = in.readString();
            itemDescription = in.readString();
            amount = in.readDouble();
            currency = in.readString();
            customerFirstName = in.readString();
            customerLastName = in.readString();
            customerEmail = in.readString();
            customerPhone = in.readString();
            customerAddress = in.readString();
        }

        public static final Creator<Args> CREATOR = new Creator<Args>() {
            @Override
            public Args createFromParcel(Parcel in) {
                return new Args(in);
            }

            @Override
            public Args[] newArray(int size) {
                return new Args[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(merchantId);
            dest.writeString(merchantSecret);
            dest.writeString(returnUrl);
            dest.writeString(cancelUrl);
            dest.writeString(notifyUrl);
            dest.writeString(orderId);
            dest.writeString(itemDescription);
            dest.writeDouble(amount);
            dest.writeString(currency);
            dest.writeString(customerFirstName);
            dest.writeString(customerLastName);
            dest.writeString(customerEmail);
            dest.writeString(customerPhone);
            dest.writeString(customerAddress);
        }
    }
}
