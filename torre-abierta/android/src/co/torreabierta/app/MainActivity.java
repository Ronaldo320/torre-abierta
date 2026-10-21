package co.torreabierta.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private WebView web;
    private final ExecutorService pool = Executors.newFixedThreadPool(6);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<String, String> bodies = new ConcurrentHashMap<String, String>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new Bridge(), "NativeHttp");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternal(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternal(Uri.parse(url));
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private boolean openExternal(Uri uri) {
        if (uri == null || "file".equals(uri.getScheme())) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__back && window.__back()) ? '1' : '0'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"\"1\"".equals(value)) MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        web.destroy();
        super.onDestroy();
    }

    private void deliver(final String id, final int status) {
        final String js = "window.__nativeCb && window.__nativeCb(" + JSONObject.quote(id) + "," + status + ")";
        main.post(new Runnable() {
            @Override
            public void run() {
                web.evaluateJavascript(js, null);
            }
        });
    }

    public class Bridge {
        @JavascriptInterface
        public void get(final String id, final String url, final String headers) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    int status = 0;
                    String body = "";
                    HttpURLConnection c = null;
                    try {
                        if (!url.startsWith("https://")) throw new IllegalArgumentException("https only");
                        c = (HttpURLConnection) new URL(url).openConnection();
                        c.setConnectTimeout(10000);
                        c.setReadTimeout(20000);
                        c.setRequestProperty("Accept", "application/json");
                        c.setRequestProperty("User-Agent", "TorreAbierta/1.0 (Android)");
                        if (headers != null && headers.length() > 2) {
                            JSONObject h = new JSONObject(headers);
                            Iterator<String> it = h.keys();
                            while (it.hasNext()) {
                                String k = it.next();
                                c.setRequestProperty(k, h.getString(k));
                            }
                        }
                        status = c.getResponseCode();
                        InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
                        if (in != null) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            byte[] buf = new byte[16384];
                            int n;
                            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                            in.close();
                            body = out.toString("UTF-8");
                        }
                    } catch (Exception e) {
                        status = 0;
                    } finally {
                        if (c != null) c.disconnect();
                    }
                    bodies.put(id, body);
                    deliver(id, status);
                }
            });
        }

        @JavascriptInterface
        public String take(String id) {
            String b = bodies.remove(id);
            return b == null ? "" : b;
        }
    }
}
