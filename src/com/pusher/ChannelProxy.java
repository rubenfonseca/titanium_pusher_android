package com.pusher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.util.TiConvert;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.emorym.android_pusher.PusherCallback;
import com.emorym.android_pusher.PusherChannel;


@Kroll.proxy(creatableInModule = PusherModule.class)
public class ChannelProxy extends KrollProxy
{
	
	PusherModule mPusherM;
	PusherChannel mChannel;
	
	private List<KrollFunction> mGlobalCallbacks = new ArrayList<KrollFunction>();
	private Map<String, List<KrollFunction>> mLocalCallbacks = new HashMap<String, List<KrollFunction>>();
	
	public ChannelProxy() {
		super();
	}
	
	public void configure(PusherModule _pusherModule, PusherChannel channel) {
		this.mPusherM = _pusherModule;
		this.mChannel = channel;
		
		this.mChannel.bindAll(new PusherCallback() {
			
			@Override
			public void onEvent(String eventName, JSONObject eventData, String channelName) {

				// We need to convert eventData to HashMap
				HashMap<String,String> eventHashData = new HashMap<String,String>();
				@SuppressWarnings("unchecked")
				Iterator<String> iter = eventData.keys();
				while( iter.hasNext() ){
					String key = iter.next();
					try {
						eventHashData.put(key, eventData.getString(key));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				Object[] global_params = { eventName, eventHashData };
				
				for (KrollFunction callback : mGlobalCallbacks) {
					callback.call(getKrollObject(), global_params); 
				}
				
				/* do we have a callback bound to that event? */
				if (mLocalCallbacks.containsKey(eventName)) {
					Object[] local_params = { eventHashData };
					/* execute each callback */
					for (KrollFunction callback : mLocalCallbacks.get(eventName)) {
						callback.call(getKrollObject(), local_params); 					
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
	
	@Kroll.method(runOnUiThread=true)
	public void unsubscribe() {
		this.mPusherM.unsubscribeChannel(this.getName());
	}
	
	@Kroll.method
	public void sendEvent(String eventName, Object data) throws org.json.JSONException {
		JSONObject jsonData = new JSONObject(TiConvert.toString(data));
		this.mPusherM.sendEvent(eventName, this.mChannel.getName(), jsonData);
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
	
	@Kroll.method
	public String getName(){
		return this.mChannel.getName();
	}
	
	@Kroll.method
	public MembersProxy getMembers(){
		Map<String, Map<String,String>> users =  this.mChannel.getUsers();
		MembersProxy members_proxy = new MembersProxy();
		members_proxy.configure(this, users);
		return members_proxy;
	}
	
	@Kroll.method
	public Map<String,String> getUser(String user_id){
		return this.mChannel.getUser(user_id);
	}
	
	public String getUserId(){
		return this.mChannel.getUserId();
	}
	
 }
