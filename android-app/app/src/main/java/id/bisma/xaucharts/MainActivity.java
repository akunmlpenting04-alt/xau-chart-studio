package id.bisma.xaucharts;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final int FILE_CHOICE = 1722;
    private static final String KEY_ALIAS = "xau_chart_twelvedata_key";
    private static final String PREF_NAME = "xau_secure_prefs";
    private static final String PREF_KEY = "twelvedata_ciphertext";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;

    private SecretKey secretKey() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private SharedPreferences preferences() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    private synchronized boolean storeApiKey(String value) {
        if (value == null || value.length() < 16 || value.length() > 200) return false;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey());
            String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
            String encrypted = Base64.encodeToString(
                    cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            return preferences().edit().putString(PREF_KEY, iv + ":" + encrypted).commit();
        } catch (Exception ignored) {
            return false;
        }
    }

    private synchronized String readApiKey() {
        try {
            String stored = preferences().getString(PREF_KEY, "");
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) return "";
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(),
                    new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(
                    cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)),
                    StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return "";
        }
    }

    private void returnCandles(int requestId, JSONObject payload) {
        String script = "window.xauAndroidResponse(" + requestId + ","
                + JSONObject.quote(payload.toString()) + ");";
        mainHandler.post(() -> {
            if (webView != null) webView.evaluateJavascript(script, null);
        });
    }

    private static String readResponse(InputStream stream) throws Exception {
        if (stream == null) return "{}";
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                if (builder.length() > 2_000_000)
                    throw new Exception("Respons API terlalu besar");
            }
        }
        return builder.toString();
    }

    private static String interval(String frame) {
        switch (frame == null ? "" : frame) {
            case "M1": return "1min";
            case "M5": return "5min";
            case "M15": return "15min";
            case "M30": return "30min";
            case "H1": return "1h";
            case "H4": return "4h";
            case "D1": return "1day";
            default: return null;
        }
    }

    private static long parseUTC(String raw) throws Exception {
        String format = raw.length() == 10 ? "yyyy-MM-dd" : "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        dateFormat.setLenient(false);
        Date parsed = dateFormat.parse(raw);
        if (parsed == null) throw new Exception("Format timestamp salah");
        return parsed.getTime() / 1000L;
    }

    private void fetchCandles(int requestId, String frame) {
        String key = readApiKey();
        JSONObject output = new JSONObject();
        if (key.isEmpty()) {
            try { output.put("error", "Simpan API key Twelve Data lewat Setelan APK."); }
            catch (Exception ignored) {}
            returnCandles(requestId, output);
            return;
        }
        String selectedInterval = interval(frame);
        if (selectedInterval == null) {
            try { output.put("error", "Timeframe tidak dikenal"); }
            catch (Exception ignored) {}
            returnCandles(requestId, output);
            return;
        }
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://api.twelvedata.com/time_series?symbol=XAU%2FUSD"
                    + "&interval=" + selectedInterval + "&outputsize=500&timezone=UTC");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(12000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "apikey " + key);
            connection.setRequestProperty("Accept", "application/json");
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            JSONObject response = new JSONObject(readResponse(stream));
            if (code < 200 || code >= 300 || response.optString("status").equals("error")) {
                String message = response.optString("message",
                        "Twelve Data HTTP " + code + ". Periksa paket data dan kuota API.");
                throw new Exception(message.replace(key, "[REDACTED]"));
            }
            JSONArray values = response.optJSONArray("values");
            if (values == null || values.length() < 2)
                throw new Exception("Twelve Data tidak mengirim riwayat candle yang valid.");
            JSONArray bars = new JSONArray();
            // Twelve Data sends latest candle first. Build ascending time order.
            for (int i = values.length() - 1; i >= 0; i--) {
                JSONObject v = values.getJSONObject(i);
                JSONObject bar = new JSONObject();
                bar.put("time", parseUTC(v.getString("datetime")));
                bar.put("open", v.getDouble("open"));
                bar.put("high", v.getDouble("high"));
                bar.put("low", v.getDouble("low"));
                bar.put("close", v.getDouble("close"));
                bars.put(bar);
            }
            output.put("symbol", "XAU/USD");
            output.put("timeframe", frame);
            output.put("provider", "Twelve Data");
            output.put("updatedAt", new Date().toString());
            output.put("bars", bars);
        } catch (Exception error) {
            try {
                String message = error.getMessage() == null
                        ? "Gagal terhubung ke Twelve Data"
                        : error.getMessage().replace(key, "[REDACTED]");
                output.put("error", message.length() > 250
                        ? message.substring(0, 250) : message);
            } catch (Exception ignored) {}
        } finally {
            if (connection != null) connection.disconnect();
        }
        returnCandles(requestId, output);
    }

    private class AndroidApi {
        @JavascriptInterface
        public boolean hasApiKey() {
            return !readApiKey().isEmpty();
        }

        @JavascriptInterface
        public boolean setApiKey(String key) {
            return storeApiKey(key == null ? "" : key.trim());
        }

        @JavascriptInterface
        public void clearApiKey() {
            preferences().edit().remove(PREF_KEY).commit();
        }

        @JavascriptInterface
        public void requestCandles(String timeframe, int requestId) {
            if (requestId < 1 || requestId > 2000000000) return;
            new Thread(() -> fetchCandles(requestId, timeframe)).start();
        }
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 14, 25));
        getWindow().setNavigationBarColor(Color.rgb(8, 14, 25));

        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(8, 14, 25));
        webView.setWebViewClient(new WebViewClientCompat() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                return !("https".equals(uri.getScheme())
                        && "appassets.androidplatform.net".equals(uri.getHost()));
            }
        });

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setSupportZoom(false);

        webView.addJavascriptInterface(new AndroidApi(), "XAUAndroid");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOICE);
                    return true;
                } catch (Exception e) {
                    uploadCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });

        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOICE && uploadCallback != null) {
            uploadCallback.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
