/*
Copyright (C) 2011 GSyC/LibreSoft, Universidad Rey Juan Carlos.

Author: Jose Antonio Santos Cadenas <jcaden@libresoft.es>
Author: Santiago Carot-Nemesio <scarot@libresoft.es>
Author: Jorge Fernández González <jfernandez@libresoft.es>

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

import android.app.Activity;
import android.os.Bundle;
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
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.objects.IDIMClass;
import es.libresoft.openhealth.android.aidl.types.objects.IMDS;

public class AgentAttributeView extends Activity {

	private IAgent agent = null;
	private IDIMClass idim = null;

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private boolean getAttributes(ArrayList<IAttribute> attrs) {
		IError err = new IError();
		try {
			IAgentService agentService = (IAgentService)
				GlobalStorage.getInstance().get(IAgentService.class.toString());
			if (agentService == null) {
				show("Error getting attributes:\nAgentService not exist");
				return false;
			}

			// If we have an IDIM object, we show its attributes. In other case
			// we request for the agent MDS to show its attributes.
			if (idim == null){
				idim = new IMDS();
				agentService.getMDS(agent, (IMDS)idim, err);
			}else {
				agentService.updateMDS(agent, err);
			}

			if (err.getErrCode() != 0) {
				show("Error getting|updating MDS " + err.getErrMsg());
				System.err.println("Error getting|updating MDS " + err.getErrMsg());
				return false;
			}

			agentService.getObjectAttrs(idim, attrs, err);
			if (err.getErrCode() != 0) {
				show("Error getting attributes " + err.getErrMsg());
				System.err.println("Error getting attributes " + err.getErrMsg());
				return false;
			}
		} catch (RemoteException e) {
			show("RemoteException in agentService.getAttributes");
			System.err.println("RemoteException in agentService.getAttributes" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return err.getErrCode() == 0;
	}

	private void showAttributes() {
		ArrayList<IAttribute> attrs = new ArrayList<IAttribute>();

		if (!getAttributes(attrs)) return;

		TableLayout tl = (TableLayout)findViewById(R.id.agentattributetable);
		tl.removeAllViews();
		TableRow tr = null;
		TextView tvname = null;
		TextView tvvalue = null;
		for (IAttribute attr : attrs) {
			tr = new TableRow(this);
			tvname = new TextView(this);
			tvvalue = new TextView(this);
			tvname.setText(attr.getAttrIdStr());
			tvvalue.setText(attr.getAttr().toString());
			tr.addView(tvname);
			tr.addView(tvvalue);
			tl.addView(tr);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentattributeview);

		Bundle extras  = getIntent().getExtras();
		if (extras != null && extras.containsKey("agent")) {
			agent = extras.getParcelable("agent");
			if (extras.containsKey("idim")){
				idim = extras.getParcelable("idim");
			}
		}else{
			show("Agent not received");
			finish();
			return;
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		showAttributes();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.agentattributeviewmenu, menu);
	    return true;
	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item) {

		switch (item.getItemId()) {
			case R.id.MENU_ATTRIBUTES:
				showAttributes();
				break;
			case R.id.MENU_SETTIME:
				new Thread(new Runnable() {
					public void run() {
						IAgentService agentService = (IAgentService)
							GlobalStorage.getInstance().get(IAgentService.class.toString());
						if (agentService != null) {
							try {
								IError error = new IError();
								agentService.setTime(agent, error);
								Log.e("AgentAttributeView", "setTime ret:" + error.getErrMsg());
								//show("setTime ret: " + error.getErrMsg());
							} catch (RemoteException e) {
								//show("Can't connect to the remote Service");
								Log.e("AgentAttributeView", "setTime exception: " + e.getMessage());
							}
						}      
					}
				}).start();
				break;
			case R.id.MENU_DISCONNECT:
				IAgentService agentService = (IAgentService)
					GlobalStorage.getInstance().get(IAgentService.class.toString());
				if (agentService != null) {
					try {
						IError error = new IError();
						if (!agentService.disconnect(agent, error)) {
							show("Error disconnecting: " + error.getErrMsg());
						}
					} catch (RemoteException e) {
						show("Can't connect to the remote Service");
					}
				}
				break;
		}
		return super.onOptionsItemSelected(item);
	}
/*	
	private IManagerClientCallback msc = new IManagerClientCallback.Stub() {

		@Override
		public void agentPlugged(IAgent ag) throws RemoteException {}

		@Override
		public void agentUnplugged(IAgent ag) throws RemoteException {
			if (agent == null) return;

			if (ag.getId() == agent.getId())
				AgentAttributeView.this.finish();
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
			managerService.registerApplication(msc);
		} catch(RemoteException e) {
			e.printStackTrace();
			return;
		}
	}

	private void show(String msg) {
		Toast t = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}

	private boolean getAttributes(ArrayList<IAttribute> attrs) {
		IError err = new IError();
		try {
			IAgentService agentService = (IAgentService)
				GlobalStorage.getInstance().get(IAgentService.class.toString());
			if (agentService == null) return false;

			IMDS mds = new IMDS();
			agentService.getMDS(agent, mds, err);
			if (err.getErrCode() != 0) {
				show("Error getting MDS " + err.getErrMsg());
				System.err.println("Error getting MDS " + err.getErrMsg());
				return false;
			}

			agentService.getObjectAttrs(mds, attrs, err);

			if (err.getErrCode() != 0) {
				show("Error getting attributes " + err.getErrMsg());
				System.err.println("Error getting attributes " + err.getErrMsg());
			}
		} catch (RemoteException e) {
			show("RemoteException in agentService.getAttributes");
			System.err.println("RemoteException in agentService.getAttributes" + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return err.getErrCode() == 0;
	}

	private void showAttributes() {
		ArrayList<IAttribute> attrs = new ArrayList<IAttribute>();

		if (!getAttributes(attrs)) return;

		TableLayout tl = (TableLayout)findViewById(R.id.agentattributetable);
		TableRow tr = null;
		TextView tvname = null;
		TextView tvvalue = null;
		for (IAttribute attr : attrs) {
			tr = new TableRow(this);
			tvname = new TextView(this);
			tvvalue = new TextView(this);
			tvname.setText(attr.getAttrIdStr());
			tvvalue.setText(attr.getAttr().toString());
			tr.addView(tvname);
			tr.addView(tvvalue);
			tl.addView(tr);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.agentattributeview);

		Bundle extras  = getIntent().getExtras();
		if (extras == null || !extras.containsKey("agent")) {
			show("Not sended agent");
			finish();
			return;
		}
		agent = extras.getParcelable("agent");
		showAttributes();

		registerManagerCallbacks();
	}

	@Override
	protected void onDestroy() {
		unregisterManagerCallbacks();

		super.onDestroy();
	}
*/
}
