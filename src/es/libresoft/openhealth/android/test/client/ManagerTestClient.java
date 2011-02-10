/*
Copyright (C) 2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>
Author: Bartolomé Marin Sanchez <zedd@libresoft.es>

This program is a (FLOS) free libre and open source implementation
of a multiplatform manager device written in java according to the
ISO/IEEE 11073-20601. Manager application is designed to work in
DalvikVM over android platform.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package es.libresoft.openhealth.android.test.client;

import java.util.Vector;

import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IManagerClientCallback;
import es.libresoft.openhealth.android.aidl.IManagerService;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.measures.IAgentMetric;
import es.libresoft.openhealth.android.aidl.types.measures.IDateMeasure;
import es.libresoft.openhealth.android.aidl.types.measures.IMeasureArray;
import es.libresoft.openhealth.android.aidl.types.measures.IMeasureAttribute;
import es.libresoft.openhealth.android.aidl.types.measures.IValueMeasure;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ManagerTestClient extends ListActivity {

	private boolean isBound = false;
	private static String serviceName = new String("es.libresoft.openhealth.android.OPENHEALTH_SERVICE");
	private IManagerService managerService = null;
	private Vector<IAgent> agents = new Vector<IAgent>();

	private Handler handler = new Handler();

	static class ListContent {
		TextView text;
	}

	private class ItemClickListener implements OnClickListener {

		private IAgent agent;

		public ItemClickListener(IAgent agent) {
			this.agent = agent;
		}

		@Override
		public void onClick(View v) {
			Intent intent = new Intent (ManagerTestClient.this,AgentView.class);
			intent.putExtra("agent", agent);
			startActivity(intent);
		}

	}

	private BaseAdapter adapter = new BaseAdapter() {

		@Override
		public int getCount() {
			return agents.size();
		}

		@Override
		public Object getItem(int position) {
			return agents.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			TextView tv = new TextView(ManagerTestClient.this);
			IAgent agent = agents.get(position);
			OnClickListener ocl = new ItemClickListener(agent);

			tv.setOnClickListener(ocl);
			tv.setText(agent.toString());
			return tv;
		}
	};


	private IManagerClientCallback msc = new IManagerClientCallback.Stub() {

		@Override
		public void agentPlugged(IAgent agent) throws RemoteException {
			agents.add(agent);
			handler.post(doUpdateGUI);
		}

		@Override
		public void agentUnplugged(IAgent agent) throws RemoteException {
			agents.removeElement(agent);
			handler.post(doUpdateGUI);
		}

		@Override
		public void error(IAgent agent, IError error) throws RemoteException {
			System.out.println("TODO: Notify the error in the GUI");
		}

		@Override
		public void agentChangeState(IAgent agent, String state)
				throws RemoteException {
			System.out.println("Agent " + agent.getId() + " Change to " + state);
		}

		@Override
		public void agentNewMeassure(IAgent agent, IAgentMetric metric)
				throws RemoteException {
			System.out.println("TODO: received measure from: " + agent);
			for (Parcelable parcel: metric.getAttributes()) {
				if (parcel instanceof IMeasureAttribute) {
					IMeasureAttribute att = (IMeasureAttribute) parcel;
					System.err.println("Att id:" + att.getAttrIdStr() + " Att code: " + att.getCodeStr());
				}
			}

			for (Parcelable parcel: metric.getMeasures()) {
				if (parcel instanceof IValueMeasure) {
					IValueMeasure value = (IValueMeasure) parcel;
					try {
						System.err.println("Measure type:" +  value.getMeasureType() + " Measure value: " + value.getFloatType());
					} catch (Exception e) {

					}
				} else if (parcel instanceof IDateMeasure) {
					IDateMeasure date = (IDateMeasure) parcel;
					System.err.println("Measure type: " + date.getMeasureType() + " Measure value: " + date.getTimeStamp());
				} else if (parcel instanceof IMeasureArray) {
					//TODO: this is so dirty, the only purpose is to show the unmarshall of arguments
					IMeasureArray array = (IMeasureArray) parcel;
					for (Parcelable parcel2: array.getList()) {
						if (parcel2 instanceof IValueMeasure) {
							IValueMeasure value = (IValueMeasure) parcel2;
							try {
								System.err.println("Measure type:" +  value.getMeasureType() + " Measure value: " + value.getFloatType());
							} catch (Exception e) {

							}
						} else if (parcel2 instanceof IDateMeasure) {
							IDateMeasure date = (IDateMeasure) parcel2;
							System.err.println("Measure type: " + date.getMeasureType() + " Measure value: " + date.getTimeStamp());
						}
					}
				}
			}
		}

	};

	private ServiceConnection healthConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			managerService = IManagerService.Stub.asInterface(service);
			GlobalStorage.getInstance().set(IManagerService.class.toString(), managerService);

			try {
				managerService.registerApplication(msc);
				managerService.agents(agents);
				handler.post(doUpdateGUI);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			isBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("Service disconnected ");
			GlobalStorage.getInstance().del(IManagerService.class.toString());
			managerService = null;
			isBound = false;
		}
	};

	private Runnable doUpdateGUI = new Runnable(){
		public void run(){
			updateGUI();
		}
	};

	void doBindService() {
		// Establish a connection with the service.  We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		//bindService(new Intent(serviceName), healthConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(IManagerService.class.getName()), healthConnection, Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (isBound) {
			// Detach our existing connection.
			unbindService(healthConnection);
			isBound = false;
		}
	}

	private void doStartService() {
		Intent intentDroid = new Intent(serviceName);
		startService(intentDroid);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		doStartService();
		doBindService();
	}

	@Override
	protected void onDestroy() {
		if (managerService != null && isBound) {
			try {
				managerService.unregisterApplication(msc);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		doUnbindService();

		super.onDestroy();
	}

	private void updateGUI() {
		this.setListAdapter(adapter);
	}
}