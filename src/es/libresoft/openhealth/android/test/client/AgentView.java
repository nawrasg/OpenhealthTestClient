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
//import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IAttribute;
import es.libresoft.openhealth.android.aidl.types.IError;
import es.libresoft.openhealth.android.aidl.types.IHANDLE;

public class AgentView extends Activity {

	private IAgent agent;
	private IAgentService agentService = null;
	private boolean isBound = false;

	private ServiceConnection agentConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {

			agentService = IAgentService.Stub.asInterface(service);
			isBound = true;

			try
			{
				IAttribute agentHandle = new IAttribute();
				IError error = new IError();
				agentService.getAttribute(agent, 2337, agentHandle, error);
				if (error.getErrCode() != 0) {
					System.err.println("Error getting the attribute: " + error.getErrMsg());
					return;
				}
				System.out.println("attribute = " + agentHandle);
				IHANDLE h = (IHANDLE) agentHandle.getAttr();
				System.out.println("handle = " + h);
				System.out.println("Handle = " + h.getHandle());
				// TODO: Set correct nomenclature code here to avoid magic numbers
			} catch (RemoteException e) {
				e.printStackTrace();
			}

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
		TextView tv = (TextView) findViewById(R.id.text);
		tv.setText("Loading Agent id");
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

				protected void onPostExecute(Boolean result) {
					Toast toast;
					if (result)
						toast = Toast.makeText(getApplicationContext(), "MDS updated correctly", Toast.LENGTH_SHORT);
					else
						toast = Toast.makeText(getApplicationContext(), "MDS was not updated, error: " + err.getErrMsg(), Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
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
				startActivity(intent);
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}
