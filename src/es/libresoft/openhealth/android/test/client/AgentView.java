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
import java.util.List;

import ieee_11073.part_10101.Nomenclature;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import es.libresoft.openhealth.android.aidl.IAgent;
import es.libresoft.openhealth.android.aidl.IAgentService;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;

public class AgentView extends Activity {

	private IAgent agent;
	private IAgentService agentService = null;
	private boolean isBound = false;

	private ServiceConnection agentConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			agentService = IAgentService.Stub.asInterface(service);
			isBound = true;
			updateMDS();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			System.err.println("Service disconnected ");
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
	}

	@Override
	protected void onDestroy() {

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
				IAttribute[] attrs = null;
				try {
					List<IAttribute> lattrs = new ArrayList<IAttribute>();
					IError err = new IError();
					agentService.getAttributes(agent, lattrs, err);
					if (err.getErrCode() != 0) {
						System.err.println("Error getting attributes " + err.getErrMsg());
						return super.onOptionsItemSelected(item);
					}
					attrs = new IAttribute[lattrs.size()];
					attrs = (IAttribute[])(lattrs.toArray(attrs));
				} catch (RemoteException e) {
					System.err.println("RemoteException in agentService.getAttributes" + e.getMessage());
					e.printStackTrace();
					return super.onOptionsItemSelected(item);
				}

				Intent intent = new Intent (AgentView.this,AgentAttributeView.class);
				intent.putExtra("attributes", attrs);
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
