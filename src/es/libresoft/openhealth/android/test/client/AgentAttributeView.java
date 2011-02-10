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

import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.IManagerClientCallback;
import es.libresoft.openhealth.android.aidl.IManagerService;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.measures.IAgentMetric;
import android.app.Activity;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class AgentAttributeView extends Activity {

	private IAgent agent = null;
	
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
		public void agentChangeState(IAgent ag, String state)
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

			agentService.getAttributes(agent, attrs, err);
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
}
