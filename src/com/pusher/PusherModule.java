/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.pusher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.util.TiConvert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.emorym.android_pusher.Pusher;
import com.emorym.android_pusher.PusherCallback;
import com.emorym.android_pusher.PusherChannel;
import com.emorym.android_pusher.PusherConnection;
import com.emorym.android_pusher.PusherLogger;
import android.app.Activity;

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
	
	public ConnectionProxy connection;

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
	public void dosetup(@SuppressWarnings("rawtypes") HashMap map) {
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
				@SuppressWarnings("unchecked")
				Iterator<String> iter = eventData.keys();
				while (iter.hasNext()) {
					String key = iter.next();
					try {
						eventHashData.put(key, eventData.getString(key));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				Object[] params = { eventName, eventHashData, channelName };
				
				for (KrollFunction callback : mGlobalCallbacks) {
					callback.call(getKrollObject(), params);
				}

				/* do we have a callback bound to that event? */
				if (mLocalCallbacks.containsKey(eventName)) {
					/* execute each callback */
					for (KrollFunction callback : mLocalCallbacks
							.get(eventName)) {
						callback.call(getKrollObject(), params);
					}
				}

			}
			
			@Override
			public void onEvent(String eventName, JSONArray eventData, String channelName) {

				ArrayList<Map<String,String>> eventArrayData = new ArrayList<Map<String,String>>();
				for( int i = 0; i < eventData.length(); i++){
					try {
						JSONObject obj = eventData.getJSONObject(i);
						HashMap<String,String> data = new HashMap<String, String>();
						@SuppressWarnings("unchecked")
						Iterator<String> iter = obj.keys();
						while(iter.hasNext()){
							String key = iter.next();
							data.put(key, obj.getString(key));
						}
						eventArrayData.add(data);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
				}
					
				Object[] global_params = { eventName, eventArrayData.toArray() };
				
				for (KrollFunction callback : mGlobalCallbacks) {
					callback.call(getKrollObject(), global_params);
				}

				/* do we have a callback bound to that event? */
				if (mLocalCallbacks.containsKey(eventName)) {
					Object[] local_params = { eventArrayData.toArray() };
					/* execute each callback */
					for (KrollFunction callback : mLocalCallbacks
							.get(eventName)) {
						callback.call(getKrollObject(), local_params);
					}
				}
			}

		});
			
	}

	@Kroll.method(runOnUiThread = true)
	public void doconnect() {
		this.mPusher.connect();
	}

	@Kroll.method(runOnUiThread = true)
	public void disconnect() {
		if (mPusher != null) {
			mPusher.disconnect();
		}
	}

	@Kroll.method(runOnUiThread = true)
	public ChannelProxy subscribeChannelNative(String channelName) {
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
	public boolean sendEvent(String eventName, String channelName, Object data)
			throws org.json.JSONException {
		JSONObject eventData = new JSONObject(TiConvert.toString(data));
		return mPusher.sendEvent(eventName, eventData, channelName);
	}

	@Kroll.setProperty
	@Kroll.method
	public void setLog(final KrollFunction customLogger) {
		mLogger = customLogger;
		final KrollObject krollobj = getKrollObject();
		
		mPusher.setLogger(new PusherLogger() {
			
			@Override
			public void log(String message) {
				Object[] arg = new Object[1];
				arg[0] = message;
				customLogger.call(
						krollobj, arg);
			}
			
			@Override
			public void log(String tag, String message){
				this.log(tag + ":" + message);
			}
			
		});
	}

	// Bind methods
	@Kroll.method
	public long bindAllNative(KrollFunction func) {
		mGlobalCallbacks.add(func);
		return Helpers.uniqueId(func);
	}

	@Kroll.method
	public long bindNative(String event, KrollFunction func) {
		/*
		 * if there are no callbacks for that event assigned yet, initialize the
		 * list
		 */
		if (!mLocalCallbacks.containsKey(event)) {
			mLocalCallbacks.put(event, new ArrayList<KrollFunction>());
		}

		/* add the callback to the event's callback list */
		mLocalCallbacks.get(event).add(func);
		
		return Helpers.uniqueId(func);
	}

	@Kroll.method
	public void unbindAllNative() {
		/* remove all callbacks from the global callback list */
		mGlobalCallbacks.clear();
		/* remove all local callback lists, that is removes all local callbacks */
		mLocalCallbacks.clear();
	}

	@Kroll.method
	public void unbindNative(long uniqueID) {
			
		/* remove all matching callbacks from the global callback list */
		Iterator<KrollFunction> iter = mGlobalCallbacks.iterator();
		while (iter.hasNext()) {
			KrollFunction item = iter.next();
			if ( Helpers.uniqueId(item) == uniqueID) {
				mGlobalCallbacks.remove(item);
			}
		}

		/* remove all matching callbacks from each local callback list */
		for (List<KrollFunction> localCallbacks : mLocalCallbacks.values()) {
			Iterator<KrollFunction> it = localCallbacks.iterator();
			while (it.hasNext()) {
				KrollFunction item = it.next();
				if ( Helpers.uniqueId(item) == uniqueID) {
					localCallbacks.remove(item);
				}
			}
		}

	}

	@Kroll.setProperty
	@Kroll.method
	public void setChannel_auth_endpoint(String url) {
		this.mPusher.setChannelAuthEndpoint(url);
	}

	@Kroll.getProperty
	@Kroll.method
	public String getChannel_auth_endpoint() {
		return this.mPusher.getChannelAuthEndpoint();
	}

	@Kroll.setProperty
	@Kroll.method
	public void setAutoReconnect(Boolean value) {
		this.mPusher.setAutoReconnect(value);
	}

	@Kroll.getProperty
	@Kroll.method
	public boolean getAutoReconnect() {
		return this.mPusher.getAutoReconnect();
	}

	@Kroll.method(runOnUiThread=true)
	public ConnectionProxy getConnection() {
		PusherConnection connection = this.mPusher.connection();
		ConnectionProxy connection_proxy = new ConnectionProxy();
		connection_proxy.configure(this, connection);
		return connection_proxy;
	}

}
