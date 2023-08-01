package io.zafeapps.getnet_pos;

import android.content.Context;
import android.os.RemoteException;

import com.getnet.posdigital.PosDigital;
import com.getnet.posdigital.camera.ICameraCallback;
import com.getnet.posdigital.mifare.IMifareCallback;
import com.getnet.posdigital.printer.AlignMode;
import com.getnet.posdigital.printer.FontFormat;
import com.getnet.posdigital.printer.IPrinterCallback;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class GetnetPosPlugin implements MethodCallHandler {

    private static final Logger LOGGER = Logger.getLogger(GetnetPosPlugin.class.getName());
    private Context context;

    private GetnetPosPlugin(Context context) {
        this.context = context;
        registerPosDigital(new Callback() {
            @Override
            public void performAction() {
                LOGGER.info("Initialized!");
            }

            @Override
            public void onError(String message) {
                LOGGER.log(Level.SEVERE, message);
            }
        });
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "getnet_pos");
        channel.setMethodCallHandler(new GetnetPosPlugin(registrar.context()));
    }

    private void ensureInitialized(final Callback callback) {
        try {
            LOGGER.info("Checking service");
            if (!PosDigital.getInstance().isInitiated()) {
                LOGGER.log(Level.SEVERE, "Instance exists, but mainService is null.");
                callback.onError("PosDigital service is not initialized.");
            } else {
                LOGGER.info("PosDigital is registered. Performing action");
                callback.performAction();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "PosDigital not registered yet, trying now. "+e.getMessage());
            registerPosDigital(callback);
        }
    }

    private void registerPosDigital(final Callback callback) {
        try {
            PosDigital.register(context, new PosDigital.BindCallback() {
                @Override
                public void onError(Exception e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                    callback.onError(e.getMessage());
                }

                @Override
                public void onConnected() {
                    try {
                        callback.performAction();
                    } catch (Exception e) {
                        callback.onError(e.getMessage());
                    }
                }

                @Override
                public void onDisconnected() {
                    LOGGER.info("PosDigital service getnet disconnected");
                }
            });
        } catch (Exception e) {
            callback.onError("Failure on service initialization" + e.getMessage());
        }
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        getMifare(call, result, 10);
    }

    private void getMifare(final MethodCall call, final Result result, final int remainTries) {
        if (call.method.equals("getMifare")) {
            ensureInitialized(new Callback() {
                @Override
                public void performAction() {
                    try {
                        PosDigital.getInstance().getMifare().searchCard(new IMifareCallback.Stub() {
                            @Override
                            public void onCard(int i) throws RemoteException {
                                result.success(PosDigital.getInstance().getMifare().getCardSerialNo(i));
                                PosDigital.getInstance().getMifare().halt();
                            }

                            @Override
                            public void onError(String s) throws RemoteException {
                                if (remainTries > 0) {
                                    getMifare(call, result, remainTries - 1);
                                } else {
                                    result.error("Error on Mifare", s, null);
                                }
                            }
                        });
                    } catch (Exception e) {
                        result.error("Error", e.getMessage(), null);
                    }
                }

                @Override
                public void onError(String message) {
                    result.error("getMifare", message, null);
                }
            });
        }
    }

    private interface Callback {
        void performAction();

        void onError(String message);
    }

}
