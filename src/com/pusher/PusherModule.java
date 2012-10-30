/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.pusher;

//import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.runtime.rhino.RhinoFunction;
import org.appcelerator.kroll.runtime.v8.V8Function;
import org.appcelerator.titanium.util.TiConvert;

import org.json.JSONException;
import org.json.JSONObject;

import com.emorym.android_pusher.Pusher;
import com.emorym.android_pusher.PusherCallback;
import com.emorym.android_pusher.PusherChannel;
import com.emorym.android_pusher.PusherConnection;
import com.emorym.android_pusher.PusherLogger;
import android.app.Activity;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.widget.Toast;

@Kroll.module(name = "Pusher", id = "com.pusher")
public class PusherModule extends KrollModule {
	// private static final boolean DBG = TiConfig.LOGD;
	private Pusher mPusher;
	private String mPusherKey;
	private Boolean mReconnectAutomatically;
	private Boolean mEncrypted;
	// private Integer mReconnectDelay;
	private KrollFunction mLogger = null;

	private List<KrollFunction> mGlobalCallbacks = new ArrayList<KrollFunction>();
	private Map<String, List<KrollFunction>> mLocalCallbacks = new HashMap<String, List<KrollFunction>>();

	// You can define constants with @Kroll.constant, for example:
	// @Kroll.constant public static final String EXTERNAL_NAME = value;

	public PusherModule() {
		super();
	}

	@Override
	public void onDestroy(Activity activity) {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				PusherModule.this.disconnect();
			}
		});
	}

	// Methods
	@Kroll.method
	public void setup(@SuppressWarnings("rawtypes") HashMap map) {
		@SuppressWarnings("unchecked")
		KrollDict args = new KrollDict(map);

		mPusherKey = args.getString("key");
		mEncrypted = args.optBoolean("encrypted", false);
		mReconnectAutomatically = args.optBoolean("reconnectAutomatically",
				true);
		// mReconnectDelay = args.optInt("reconnectDelay", 5);

		if (mPusherKey == null || mPusherKey.length() == 0) {
			throw new RuntimeException("Pusher key is required");
		}

		Map<String, Map<String, String>> auth = new HashMap<String, Map<String, String>>();
		if (args.containsKey("params")) {
			KrollDict kparams = args.getKrollDict("params");
			Map<String, String> params = new HashMap<String, String>();
			Iterator<String> iter = kparams.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				String value = kparams.getString(key);
				params.put(key, value);
			}
			auth.put("params", params);
		}

		if (args.containsKey("headers")) {
			KrollDict kheaders = args.getKrollDict("headers");
			Map<String, String> headers = new HashMap<String, String>();
			Iterator<String> iter = kheaders.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				String value = kheaders.getString(key);
				headers.put(key, value);
			}
			auth.put("params", headers);
		}

		this.mPusher = new Pusher(mPusherKey, mEncrypted, auth);
		this.mPusher.setAutoReconnect(mReconnectAutomatically);
		this.mPusher.bindAll(new PusherCallback() {

			@Override
			public void onEvent(String eventName, JSONObject eventData,
					String channelName) {

				// We need to convert eventData to HashMap
				HashMap<String, String> eventHashData = new HashMap<String, String>();
				Iterator<String> iter = eventData.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					try {
						eventHashData.put(key, eventData.getString(key));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				for (KrollFunction callback : mGlobalCallbacks) {
					callback.call(getKrollObject(), eventHashData);
				}

				/* do we have a callback bound to that event? */
				if (mLocalCallbacks.containsKey(eventName)) {
					/* execute each callback */
					for (KrollFunction callback : mLocalCallbacks
							.get(eventName)) {
						callback.call(getKrollObject(), eventHashData);
					}
				}

			}

		});

	}

	@Kroll.method(runOnUiThread = true)
	public void connect() {
		this.mPusher.connect();
	}

	@Kroll.method(runOnUiThread = true)
	public void disconnect() {
		if (mPusher != null) {
			mPusher.disconnect();
		}
	}

	@Kroll.method(runOnUiThread = true)
	public ChannelProxy subscribeChannel(String channelName) {
		PusherChannel channel = mPusher.subscribe(channelName);
		ChannelProxy channelProxy = new ChannelProxy();
		channelProxy.configure(this, channel);

		return channelProxy;
	}

	@Kroll.method(runOnUiThread = true)
	public void unsubscribeChannel(String channelName) {
		mPusher.unsubscribe(channelName);
	}

	@Kroll.method
	public void sendEvent(String eventName, String channelName, Object data)
			throws org.json.JSONException {
		JSONObject eventData = new JSONObject(TiConvert.toString(data));
		mPusher.sendEvent(eventName, eventData, channelName);
	}

	@Kroll.setProperty
	@Kroll.method
	public void setLog(final KrollFunction customLogger) {
		mLogger = customLogger;
		mPusher.setLogger(new PusherLogger() {
			@Override
			public void log(String message) {
				Object[] arg = new Object[1];
				arg[0] = message;
				PusherModule.this.mLogger.call(
						PusherModule.this.getKrollObject(), arg);
			}
		});
	}

	// Bind methods
	@Kroll.method
	public void bindAll(KrollFunction func) {
		mGlobalCallbacks.add(func);
	}

	@Kroll.method
	public void bind(String event, KrollFunction func) {
		/*
		 * if there are no callbacks for that event assigned yet, initialize the
		 * list
		 */
		if (!mLocalCallbacks.containsKey(event)) {
			mLocalCallbacks.put(event, new ArrayList<KrollFunction>());
		}

		/* add the callback to the event's callback list */
		mLocalCallbacks.get(event).add(func);
	}

	@Kroll.method
	public void unbindAll() {
		/* remove all callbacks from the global callback list */
		mGlobalCallbacks.clear();
		/* remove all local callback lists, that is removes all local callbacks */
		mLocalCallbacks.clear();
	}

	@Kroll.method
	public void unbind(KrollFunction func) {
		Log.d("unbind", "func v8 pointer " + ((V8Function) func).getPointer());
		Log.d("unbind", "func v8 toString " + ((V8Function) func).toString());
		Log.d("unbind", "func v8 hash " + ((V8Function) func).hashCode());
		Log.d("unbind", "func v8 native hash "
				+ ((V8Function) func).getNativeObject().hashCode());
		/* remove all matching callbacks from the global callback list */
		Iterator<KrollFunction> iter = mGlobalCallbacks.iterator();
		while (iter.hasNext()) {
			KrollFunction item = iter.next();
			Log.d("unbind",
					"item v8 pointer " + ((V8Function) item).getPointer());
			Log.d("unbind",
					"item v8 tostring " + ((V8Function) item).toString());
			Log.d("unbind", "item v8 hash " + ((V8Function) item).hashCode());
			Log.d("unbind", "item v8 native hash "
					+ ((V8Function) item).getNativeObject().hashCode());
			if (compareKrollFunctions(func, item)) {
				mGlobalCallbacks.remove(item);
			}
		}

		/* remove all matching callbacks from each local callback list */
		for (List<KrollFunction> localCallbacks : mLocalCallbacks.values()) {
			Iterator<KrollFunction> it = localCallbacks.iterator();
			while (it.hasNext()) {
				KrollFunction item = it.next();
				if (compareKrollFunctions(func, item)) {
					localCallbacks.remove(item);
				}
			}
		}

	}

	private boolean compareKrollFunctions(KrollFunction a, KrollFunction b) {
		if (V8Function.class.isInstance(a) && V8Function.class.isInstance(b)) {
			return ((V8Function) a).getPointer() == ((V8Function) b)
					.getPointer();
		} else if (RhinoFunction.class.isInstance(a)
				&& RhinoFunction.class.isInstance(b)) {
			return ((RhinoFunction) a).getFunction() == ((RhinoFunction) b)
					.getFunction();
		} else {
			return a.equals(b);
		}
	}

	@Kroll.setProperty
	@Kroll.method
	public void setChannelAuthEndpoint(String url) {
		this.mPusher.setChannelAuthEndpoint(url);
	}

	@Kroll.getProperty
	@Kroll.method
	public String getChannelAuthEndpoint() {
		return this.mPusher.getChannelAuthEndpoint();
	}

	@Kroll.setProperty
	@Kroll.method
	public void setAutoReconnect(Boolean value) {
		this.mPusher.setAutoReconnect(value);
	}

	@Kroll.getProperty
	@Kroll.method
	public boolean setAutoReconnect() {
		return this.mPusher.getAutoReconnect();
	}

	@Kroll.getProperty
	@Kroll.method(runOnUiThread=true)
	public ConnectionProxy getConnection() {
		PusherConnection connection = this.mPusher.connection();
		ConnectionProxy connection_proxy = new ConnectionProxy();
		connection_proxy.configure(this, connection);
		return connection_proxy;
	}
}
