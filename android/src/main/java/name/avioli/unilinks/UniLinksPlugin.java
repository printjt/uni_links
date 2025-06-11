package name.avioli.unilinks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class UniLinksPlugin implements
        FlutterPlugin,
        MethodChannel.MethodCallHandler,
        EventChannel.StreamHandler,
        ActivityAware,
        io.flutter.embedding.engine.plugins.activity.NewIntentListener {

    private static final String MESSAGES_CHANNEL = "uni_links/messages";
    private static final String EVENTS_CHANNEL = "uni_links/events";

    private BroadcastReceiver changeReceiver;
    private String initialLink;
    private String latestLink;
    private Context context;
    private boolean initialIntent = true;

    private MethodChannel methodChannel;
    private EventChannel eventChannel;

    private void handleIntent(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        String dataString = intent.getDataString();

        if (Intent.ACTION_VIEW.equals(action)) {
            if (initialIntent) {
                initialLink = dataString;
                initialIntent = false;
            }
            latestLink = dataString;
            if (changeReceiver != null) changeReceiver.onReceive(context, intent);
        }
    }

    @NonNull
    private BroadcastReceiver createChangeReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String dataString = intent.getDataString();
                if (dataString == null) {
                    events.error("UNAVAILABLE", "Link unavailable", null);
                } else {
                    events.success(dataString);
                }
            }
        };
    }

    private void setupChannels(BinaryMessenger messenger) {
        methodChannel = new MethodChannel(messenger, MESSAGES_CHANNEL);
        methodChannel.setMethodCallHandler(this);

        eventChannel = new EventChannel(messenger, EVENTS_CHANNEL);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.context = binding.getApplicationContext();
        setupChannels(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        if (methodChannel != null) methodChannel.setMethodCallHandler(null);
        if (eventChannel != null) eventChannel.setStreamHandler(null);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "getInitialLink":
                result.success(initialLink);
                break;
            case "getLatestLink":
                result.success(latestLink);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onListen(Object args, EventChannel.EventSink eventSink) {
        changeReceiver = createChangeReceiver(eventSink);
    }

    @Override
    public void onCancel(Object args) {
        changeReceiver = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        binding.addOnNewIntentListener(this);
        handleIntent(context, binding.getActivity().getIntent());
    }

    @Override
    public void onDetachedFromActivity() {
        // no-op
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // no-op
    }

    @Override
    public boolean onNewIntent(Intent intent) {
        handleIntent(context, intent);
        return false;
    }
}