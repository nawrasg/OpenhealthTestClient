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

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TabHost;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.IManagerClientCallback;
import es.libresoft.openhealth.android.aidl.IManagerService;
import es.libresoft.openhealth.android.aidl.IPMStoreService;
import es.libresoft.openhealth.android.aidl.IState;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.measures.IAgentMetric;

public class AgentView extends TabActivity {
	private IAgent agent = null;
	private IAgentService agentService = null;
	private IPMStoreService pmStoreService = null;
	private ProgressDialog pd = null;

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
				throws RemoteException {}
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
			managerService.unregisterApplication(msc);
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
			if (pmStoreService != null) //all services are connected
			{
				initTabs();
				pd.dismiss();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.e("AgentView", "Service disconnected");
			GlobalStorage.getInstance().del(IAgentService.class.toString());
			agentService = null;
		}
	};

	private ServiceConnection pmStoreConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pmStoreService = IPMStoreService.Stub.asInterface(service);
			GlobalStorage.getInstance().set(IPMStoreService.class.toString(), pmStoreService);
			if (agentService != null) //all services are connected
			{
				initTabs();
				pd.dismiss();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("Service pmStore disconnected ");
			GlobalStorage.getInstance().del(IPMStoreService.class.toString());
			pmStoreService = null;
		}
	};

	private void initTabs() {
		TabHost tab = getTabHost();
		Intent intent;

		intent = new Intent(AgentView.this, AgentAttributeView.class);
		intent.putExtra("agent", agent);
		tab.addTab(tab.newTabSpec("Attributes").setIndicator("Attributes").setContent(intent));

		intent = new Intent(AgentView.this, AgentPMStoreView.class);
		intent.putExtra("agent", agent);
		tab.addTab(tab.newTabSpec("PMStores").setIndicator("PMStores").setContent(intent));

		intent = new Intent(AgentView.this, AgentMeasureView.class);
		intent.putExtra("agent", agent);
		tab.addTab(tab.newTabSpec("Measures").setIndicator("Measures").setContent(intent));

		tab.setCurrentTab(0);
	}
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

		registerManagerCallbacks();

		pd = ProgressDialog.show(this, "Connecting ...", "Waiting for connection");
		pd.show();

		bindService(new Intent(IAgentService.class.getName()), agentConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(IPMStoreService.class.getName()), pmStoreConnection, Context.BIND_AUTO_CREATE);

		setContentView(R.layout.agentview);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (agentConnection != null)
			unbindService(agentConnection);
		if (pmStoreConnection != null)
			unbindService(pmStoreConnection);
		unregisterManagerCallbacks();
	}


/*
	private Vector<IAgentMetric> metrics = new Vector<IAgentMetric>();
	private Handler handler = new Handler();


	private TableRow getShowRowSeparator() {
		//TODO: Use a separator not String
		TableRow tr = new TableRow(this);
		TextView tv = new TextView(this);
		tv.setText("------------");
		tr.addView(tv);
		return tr;
	}

	private TableRow getShowRow(String str) {
		TableRow tr = new TableRow(this);
		TextView tv = new TextView(this);
		tv.setText(str);
		tr.addView(tv);
		return tr;
	}

	private TableRow getShowRow(ArrayList<String> strs) {
		TableRow tr = new TableRow(this);
		TextView tv = null;
		for (String str: strs) {
			tv = new TextView(this);
			tv.setText(str);
			tr.addView(tv);
		}
		return tr;
	}

	private void showRow(TableRow tr) {
		TableLayout tl = (TableLayout)findViewById(R.id.agentviewmeasuretable);
		tl.addView(tr,0);
	}

	private void showRow(ArrayList<TableRow> rows) {
		TableLayout tl = (TableLayout)findViewById(R.id.agentviewmeasuretable);
		while (!rows.isEmpty())
			tl.addView(rows.remove(rows.size()-1),0);
	}

	private void showUnknownAgentMetric(IAgentMetric am) {
		String str = null;
		ArrayList<TableRow> rows = new ArrayList<TableRow>();

		//add attributes
		for (IAttribute att: am.getAttributes()) {
			str = "At[" + att.getAttrId() + "]:" + att.getAttr();
			rows.add(getShowRow(str));
		}

		//add measures
		for (IMeasure me: am.getMeasures()) {
			str = "Me[" + me.getMeasureType() + "]:" + me;
			rows.add(getShowRow(str));
		}

		//add stop separator
		rows.add(getShowRowSeparator());

		//add rows to tablelayout in inverse order
		showRow(rows);
	}

	private boolean showSimpleAgentMetric(IAgentMetric am) {
		IAttribute typeMeasure = null;
		IAttribute unit = null;
		IMeasure value = null;
		IMeasure timeStamp = null;

		getAgentMetricAttribute(am, Nomenclature.MDC_ATTR_ID_TYPE, typeMeasure);
		getAgentMetricAttribute(am, Nomenclature.MDC_ATTR_UNIT_CODE, unit);

		getAgentMetricMeasure(am, Nomenclature.MDC_ATTR_NU_VAL_OBS_BASIC, value);
		getAgentMetricMeasure(am, Nomenclature.MDC_ATTR_TIME_STAMP_ABS, timeStamp);

		if (typeMeasure == null ||
			unit == null ||
			value == null
		)
			return false;

		showRow(getShowRowSeparator());
		if (timeStamp != null)
			showRow(getShowRow(""+timeStamp));
		ArrayList<String> strs = new ArrayList<String>(2);
		strs.add(0, ""+value);
		strs.add(1, unit.getAttrIdStr());
		showRow(getShowRow(strs));

		return true;
	}

	private Boolean getAgentMetricAttribute(IAgentMetric am, int attrId, IAttribute attribute) {
		for (IAttribute att: am.getAttributes()) {
			if (att.getAttrId() == attrId) {
				attribute = att;
				return true;
			}
		}
		return false;
	}

	private Boolean getAgentMetricMeasure(IAgentMetric am, int measureId, IMeasure measure) {
		for (IMeasure me: am.getMeasures()) {
			if (me.getMeasureType() == measureId) {
				measure = me;
				return true;
			}
		}
		return false;
	}

	private void showAgentMetric(IAgentMetric am) {
		if (showSimpleAgentMetric(am)) return;
		showUnknownAgentMetric(am);
	}

	private Runnable doUpdateAgentMetricGUI = new Runnable(){
		public void run(){
			IAgentMetric am = null;
			while (!metrics.isEmpty()) {
				am = metrics.remove(0);
				showAgentMetric(am);
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
			if (ag.getId() != agent.getId() || metric == null) return;

			metrics.add(metric);
			handler.post(doUpdateAgentMetricGUI);
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

	private ServiceConnection pmStoreConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pmStoreService = IPMStoreService.Stub.asInterface(service);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("Service pmStore disconnected ");
			pmStoreService = null;
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
		bindService(new Intent(IPMStoreService.class.getName()), pmStoreConnection, Context.BIND_AUTO_CREATE);
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
		if (pmStoreConnection != null) {
			unbindService(pmStoreConnection);
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
					ArrayList<IAttribute> attrs = new ArrayList<IAttribute>();

					if (!result) {
						show("MDS not updated, err: " + err.getErrMsg());
						return;
					}

					try {
						IMDS mds = new IMDS();
						agentService.getMDS(agent, mds, err);
						if (err.getErrCode() != 0) {
							show("Error getting MDS " + err.getErrMsg());
							System.err.println("Error getting MDS " + err.getErrMsg());
							return;
						}

						agentService.getObjectAttrs(mds, attrs, err);
						if (err.getErrCode() != 0) {
							show("Error getting attributes " + err.getErrMsg());
							System.err.println("Error getting attributes " + err.getErrMsg());
							return;
						}
					} catch (RemoteException e) {
						show("Error getting attr: remote exception");
						e.printStackTrace();
					}
					if (err.getErrCode() != 0) {
						show("Error getting attr: " +  err.getErrMsg());
						return;
					}

					for (IAttribute attr : attrs) {
						switch (attr.getAttrId()) {
						case Nomenclature.MDC_ATTR_ID_HANDLE:
							((TextView) findViewById(R.id.handle)).setText(attr
									.getAttr().toString());
							break;
						case Nomenclature.MDC_ATTR_ID_MODEL:
							((TextView) findViewById(R.id.systemModel))
									.setText(attr.getAttr().toString());
							break;
						case Nomenclature.MDC_ATTR_SYS_ID:
							((TextView) findViewById(R.id.systemId))
									.setText(attr.getAttr().toString());
							break;
						case Nomenclature.MDC_ATTR_DEV_CONFIG_ID:
							((TextView) findViewById(R.id.devConfigurationId))
									.setText(attr.getAttr().toString());
							break;
						default:
							break;
						}
					}

					show("MDS updated");
				}
			};
			at.execute(agent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private void updatePMStore() {
		if (agentService == null || pmStoreService == null)
			return;

		try {
			AsyncTask<IAgent, Integer, Boolean> at = new AsyncTask<IAgent, Integer, Boolean>() {
				//TODO: Check more deeply if race condition can happen
				IError err = new IError();

				protected Boolean doInBackground(IAgent... agent) {
					try {
						List<IPM_Store> nums = new ArrayList<IPM_Store>();
						agentService.getPM_Store(agent[0], nums, err);
						if (nums.isEmpty()) {
							err.setErrCode(0);
							err.setErrMsg("No PMSTORE");
							return false;
						}
						return pmStoreService.updatePMStore(nums.get(0), err);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
					return false;
				}

				private void show(String msg) {
					Toast t;

					t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
					t.setGravity(Gravity.CENTER, 0, 0);
					t.show();
				}

				protected void onPostExecute(Boolean result) {
					show("Update PMSTORE, err " + err.getErrCode() + " " + err.getErrMsg());
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
			case R.id.MENU_DISCONNECT:
				if (agentService != null) {
					try {
						IError error = new IError();
						if (!agentService.disconnect(agent, error)) {
							Toast t;
							t = Toast.makeText(getApplicationContext(), "Error disconnecting: " + error.getErrMsg(), Toast.LENGTH_SHORT);
							t.show();
						}
					} catch (RemoteException e) {
						Toast t;
						t = Toast.makeText(getApplicationContext(), "Can't connect to the remote Service", Toast.LENGTH_SHORT);
						t.show();
					}
				}
				break;
			case R.id.MENU_UPDATEPMSTORE:
				updatePMStore();
				break;
		}
		return super.onOptionsItemSelected(item);
	}
*/
}
