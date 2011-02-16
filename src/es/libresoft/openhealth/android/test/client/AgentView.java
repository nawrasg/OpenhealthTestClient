/*
Copyright (C) 2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>

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

import java.util.ArrayList;
import java.util.Vector;

import ieee_11073.part_10101.Nomenclature;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.IManagerClientCallback;
import es.libresoft.openhealth.android.aidl.IManagerService;
import es.libresoft.openhealth.android.aidl.IState;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.measures.IAgentMetric;
import es.libresoft.openhealth.android.aidl.types.measures.IDateMeasure;
import es.libresoft.openhealth.android.aidl.types.measures.IMeasure;
import es.libresoft.openhealth.android.aidl.types.measures.IMeasureArray;
import es.libresoft.openhealth.android.aidl.types.measures.IValueMeasure;

public class AgentView extends Activity {

	private IAgent agent = null;
	private IAgentService agentService = null;
	private boolean isBound = false;

	private Vector<IMeasure> measures = new Vector<IMeasure>();
	private Handler handler = new Handler();

	protected void showMeasure(ArrayList<String> strs) {
		if (strs.isEmpty()) return;

		TableLayout tl = (TableLayout)findViewById(R.id.agentviewmeasuretable);
		TableRow tr = new TableRow(this);
		TextView tv = null;
		String msg = "";
		for (String str : strs) {
			tv = new TextView(this);
			tv.setText(str);
			tr.addView(tv);
			msg += "\t" + str;
		}
		tl.addView(tr);
		Log.e("showMeasure", msg);
	}

	private void showMeasures(IMeasure m, ArrayList<String> strs) {
		try {
			if (m instanceof IMeasureArray) {
				IMeasureArray ma = (IMeasureArray) m;
				IMeasure measure;
				strs.add("Ar");
				for (Parcelable p : ma.getList()) {
					measure = (IMeasure)p;
					showMeasures(measure, strs);
				}
			}
			if (m instanceof IValueMeasure) {
				IValueMeasure value = (IValueMeasure) m;
				strs.add(""+value.getMeasureType());
				strs.add(""+value.getFloatType());
				showMeasure(strs);
			}
			if (m instanceof IDateMeasure) {
				IDateMeasure date = (IDateMeasure) m;
				strs.add(""+date.getMeasureType());
				strs.add(""+date.getTimeStamp());
				showMeasure(strs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Runnable doUpdateMeasureGUI = new Runnable(){
		public void run(){
			IMeasure m = null;
			while (!measures.isEmpty()) {
				m = measures.remove(0);
				showMeasures(m, new ArrayList<String>());
			}
		}
	};

	private IManagerClientCallback msc = new IManagerClientCallback.Stub() {

		@Override
		public void agentPlugged(IAgent ag) throws RemoteException {}

		@Override
		public void agentUnplugged(IAgent ag) throws RemoteException {
			if (agent == null) return;

			if (ag.getId() == agent.getId())
				AgentView.this.finish();
		}

		@Override
		public void error(IAgent ag, IError error) throws RemoteException {}

		@Override
		public void agentChangeState(IAgent ag, IState state)
				throws RemoteException {}

		@Override
		public void agentNewMeassure(IAgent ag, IAgentMetric metric)
				throws RemoteException {
			if (ag.getId() != agent.getId()) return;

			for (IMeasure measure: metric.getMeasures())
				measures.add(measure);
			handler.post(doUpdateMeasureGUI);
		}

	};

	private void registerManagerCallbacks() {
		IManagerService managerService = (IManagerService)
			GlobalStorage.getInstance().get(IManagerService.class.toString());

		if (managerService == null) {
			Log.e("AgentAttributeView", "ManagerService is null, cant get manager callbacks");
			return;
		}
		try {
			managerService.registerApplication(msc);
		} catch(RemoteException e) {
			e.printStackTrace();
			return;
		}
	}

	private void unregisterManagerCallbacks() {
		IManagerService managerService = (IManagerService)
		GlobalStorage.getInstance().get(IManagerService.class.toString());

		if (managerService == null) {
			Log.e("AgentAttributeView", "ManagerService is null, cant unregister manager callbacks");
			return;
		}
		try {
			managerService.registerApplication(msc);
		} catch(RemoteException e) {
			e.printStackTrace();
			return;
		}
	}

	private ServiceConnection agentConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			agentService = IAgentService.Stub.asInterface(service);
			GlobalStorage.getInstance().set(IAgentService.class.toString(), agentService);
			isBound = true;
			updateMDS();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("Service disconnected ");
			GlobalStorage.getInstance().del(IAgentService.class.toString());
			agentService = null;
			isBound = false;
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras  = getIntent().getExtras();
		if (extras != null){
			try {
				agent = (IAgent) extras.get("agent");
			} catch (Exception e) {
				Toast t = new Toast(this);
				t.setText("Error on agent, can't be displayed");
				t.show();
				finish();
				return;
			}
		}

		setContentView(R.layout.agentview);
		bindService(new Intent(IAgentService.class.getName()), agentConnection, Context.BIND_AUTO_CREATE);
		isBound = true;

		registerManagerCallbacks();
	}

	@Override
	protected void onDestroy() {
		unregisterManagerCallbacks();
		if (isBound) {
			unbindService(agentConnection);
			isBound = false;
		}

		super.onDestroy();
	}
	
	private void updateMDS() {
		try {
			AsyncTask<IAgent, Integer, Boolean> at = new AsyncTask<IAgent, Integer, Boolean>() {
				//TODO: Check more deeply if race condition can happen
				IError err = new IError();

				protected Boolean doInBackground(IAgent... agent) {
					try {
						return agentService.updateMDS(agent[0], err);
					} catch (RemoteException e) {
						return false;
					}
				}

				private void show(String msg) {
					Toast t;
					
					t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
					t.setGravity(Gravity.CENTER, 0, 0);
					t.show();
				}
				
				protected void onPostExecute(Boolean result) {
					IAttribute attrHandle = new IAttribute();
					IAttribute attrIdModel = new IAttribute();
					IAttribute attrSysId = new IAttribute();
					IAttribute attrDevConfigId = new IAttribute();

					if (!result) {
						show("MDS not updated, err: " + err.getErrMsg());
						return;
					}

					try {
						agentService.getAttribute(agent, Nomenclature.MDC_ATTR_ID_HANDLE, attrHandle, err);
						if (err.getErrCode() == 0)
							agentService.getAttribute(agent, Nomenclature.MDC_ATTR_ID_MODEL, attrIdModel, err);
						if (err.getErrCode() == 0)
							agentService.getAttribute(agent, Nomenclature.MDC_ATTR_SYS_ID, attrSysId, err);
						if (err.getErrCode() == 0)
							agentService.getAttribute(agent, Nomenclature.MDC_ATTR_DEV_CONFIG_ID, attrDevConfigId, err);
					} catch (RemoteException e) {
						show("Error getting attr: remote exception");
						e.printStackTrace();
					}
					if (err.getErrCode() != 0) {
						show("Error getting attr: " +  err.getErrMsg());
						return;
					}

					((TextView)findViewById(R.id.handle)).setText(attrHandle.getAttr().toString());
					((TextView)findViewById(R.id.systemModel)).setText(attrIdModel.getAttr().toString());
					((TextView)findViewById(R.id.systemId)).setText(attrSysId.getAttr().toString());
					((TextView)findViewById(R.id.devConfigurationId)).setText(attrDevConfigId.getAttr().toString());

					show("MDS updated");
				}
			};
			at.execute(agent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.agentviewmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.MENU_UPDATEMDS:
				updateMDS();
				break;
			case R.id.MENU_ATTRIBUTES:
				Intent intent = new Intent (AgentView.this,AgentAttributeView.class);
				Bundle extras = new Bundle();
				extras.putParcelable("agent", agent);
				intent.putExtras(extras);
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
